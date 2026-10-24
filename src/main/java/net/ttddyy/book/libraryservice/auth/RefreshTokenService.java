/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.book.libraryservice.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import net.ttddyy.book.libraryservice.LibraryServiceProperties;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores refresh tokens (as SHA-256 hashes) and handles rotation, grace-window reuse and
 * reuse detection.
 *
 * @author Tadaya Tsuyukubo
 */
@Service
public class RefreshTokenService {

	private final SecureRandom random = new SecureRandom();

	private final JdbcTemplate jdbcTemplate;

	private final LibraryServiceProperties.Auth auth;

	private final Clock clock;

	RefreshTokenService(JdbcTemplate jdbcTemplate, LibraryServiceProperties properties, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.auth = properties.getAuth();
		this.clock = clock;
	}

	public IssuedRefreshToken startFamily(String username) {
		Instant now = this.clock.instant();
		return insert(UUID.randomUUID(), username, now.plus(this.auth.getRefreshTokenTtl()));
	}

	/**
	 * Rotate the presented token. Invalid-token failures do not roll back, so the family
	 * deletion on reuse detection is committed.
	 */
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public RefreshResult rotate(String value) {
		Instant now = this.clock.instant();
		String hash = hash(value);
		List<Row> rows = this.jdbcTemplate.query(
				"SELECT family_id, username, expires_at, rotated_at FROM refresh_tokens WHERE token_hash = ? FOR UPDATE",
				(rs, i) -> new Row(rs.getObject("family_id", UUID.class), rs.getString("username"),
						rs.getTimestamp("expires_at").toInstant(), toInstant(rs.getTimestamp("rotated_at"))),
				hash);
		if (rows.isEmpty()) {
			throw new InvalidRefreshTokenException();
		}
		Row row = rows.get(0);
		if (!row.expiresAt().isAfter(now)) {
			throw new InvalidRefreshTokenException();
		}
		if (row.rotatedAt() != null) {
			Duration grace = this.auth.getRefreshReuseGrace();
			if (Duration.between(row.rotatedAt(), now).compareTo(grace) <= 0) {
				return new RefreshResult(row.username(), null);
			}
			deleteFamily(row.familyId());
			throw new InvalidRefreshTokenException();
		}
		this.jdbcTemplate.update("UPDATE refresh_tokens SET rotated_at = ? WHERE token_hash = ?", Timestamp.from(now),
				hash);
		return new RefreshResult(row.username(), insert(row.familyId(), row.username(), row.expiresAt()));
	}

	public void revokeFamily(String value) {
		this.jdbcTemplate.update(
				"DELETE FROM refresh_tokens WHERE family_id IN (SELECT family_id FROM refresh_tokens WHERE token_hash = ?)",
				hash(value));
	}

	public void deleteExpired() {
		this.jdbcTemplate.update("DELETE FROM refresh_tokens WHERE expires_at <= ?",
				Timestamp.from(this.clock.instant()));
	}

	static String hash(String value) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private IssuedRefreshToken insert(UUID familyId, String username, Instant expiresAt) {
		byte[] bytes = new byte[32];
		this.random.nextBytes(bytes);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		this.jdbcTemplate.update(
				"INSERT INTO refresh_tokens (token_hash, family_id, username, expires_at) VALUES (?, ?, ?, ?)",
				hash(value), familyId, username, Timestamp.from(expiresAt));
		return new IssuedRefreshToken(value, expiresAt);
	}

	private void deleteFamily(UUID familyId) {
		this.jdbcTemplate.update("DELETE FROM refresh_tokens WHERE family_id = ?", familyId);
	}

	@Nullable
	private static Instant toInstant(@Nullable Timestamp timestamp) {
		return (timestamp != null) ? timestamp.toInstant() : null;
	}

	private record Row(UUID familyId, String username, Instant expiresAt, @Nullable Instant rotatedAt) {
	}

}
