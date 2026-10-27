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

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.LibraryServiceProperties;
import net.ttddyy.book.libraryservice.MockClocks.MockClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RefreshTokenService} with DB. Calls commit for real so that the family
 * deletion on reuse detection is observable.
 *
 * @author Tadaya Tsuyukubo
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ RefreshTokenService.class, MockClock.class, RefreshTokenServiceDBTests.Config.class })
@DbTest
// Not transactional: verifies that rotate() commits the family deletion even when it
// throws.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RefreshTokenServiceDBTests {

	private static final Instant T0 = Instant.parse("2026-10-23T00:00:00Z");

	@TestConfiguration
	@EnableConfigurationProperties(LibraryServiceProperties.class)
	static class Config {

	}

	@Autowired
	RefreshTokenService service;

	@Autowired
	MockClock clock;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanUp();
		this.jdbcTemplate.update("INSERT INTO users (username, password, enabled) VALUES ('rt-user', 'x', TRUE)");
		at(T0);
	}

	@AfterEach
	void cleanUp() {
		this.jdbcTemplate.update("DELETE FROM refresh_tokens WHERE username = 'rt-user'");
		this.jdbcTemplate.update("DELETE FROM users WHERE username = 'rt-user'");
	}

	@Test
	void startFamilyStoresHashOnly() {
		IssuedRefreshToken token = this.service.startFamily("rt-user");

		assertThat(token.value()).hasSize(43);
		assertThat(Base64.getUrlDecoder().decode(token.value())).hasSize(32);
		assertThat(token.expiresAt()).isEqualTo(T0.plus(Duration.ofDays(1)));
		Map<String, Object> row = this.jdbcTemplate.queryForMap("SELECT * FROM refresh_tokens WHERE username = ?",
				"rt-user");
		assertThat(row.get("token_hash")).isEqualTo(RefreshTokenService.hash(token.value()));
		assertThat((String) row.get("token_hash")).matches("[0-9a-f]{64}");
		assertThat(row.get("rotated_at")).isNull();
		assertThat(this.jdbcTemplate.queryForObject("SELECT expires_at FROM refresh_tokens", java.sql.Timestamp.class)
			.toInstant()).isEqualTo(token.expiresAt());
	}

	@Test
	void rotateIssuesSuccessorWithSameExpiry() {
		IssuedRefreshToken original = this.service.startFamily("rt-user");
		Instant later = T0.plus(Duration.ofHours(1));
		at(later);

		RefreshResult result = this.service.rotate(original.value());

		assertThat(result.username()).isEqualTo("rt-user");
		assertThat(result.successor()).isNotNull();
		assertThat(result.successor().expiresAt()).isEqualTo(original.expiresAt());
		assertThat(result.successor().value()).isNotEqualTo(original.value());
		assertThat(this.jdbcTemplate
			.queryForObject("SELECT rotated_at FROM refresh_tokens WHERE token_hash = ?", java.sql.Timestamp.class,
					RefreshTokenService.hash(original.value()))
			.toInstant()).isEqualTo(later);
		assertThat(familyCount(original.value())).isEqualTo(2);
	}

	@Test
	void reuseWithinGraceReturnsUsernameWithoutSuccessor() {
		IssuedRefreshToken original = this.service.startFamily("rt-user");
		this.service.rotate(original.value());
		at(T0.plus(Duration.ofSeconds(10)));

		RefreshResult result = this.service.rotate(original.value());

		assertThat(result.username()).isEqualTo("rt-user");
		assertThat(result.successor()).isNull();
		assertThat(familyCount(original.value())).isEqualTo(2);
	}

	@Test
	void reuseOutsideGraceDeletesFamily() {
		IssuedRefreshToken original = this.service.startFamily("rt-user");
		IssuedRefreshToken successor = this.service.rotate(original.value()).successor();
		at(T0.plus(Duration.ofSeconds(11)));

		assertThatThrownBy(() -> this.service.rotate(original.value()))
			.isInstanceOf(InvalidRefreshTokenException.class);

		assertThat(this.jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_tokens WHERE username = 'rt-user'",
				Integer.class))
			.isZero();
		assertThatThrownBy(() -> this.service.rotate(successor.value()))
			.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void expiredAtBoundaryIsRejected() {
		IssuedRefreshToken token = this.service.startFamily("rt-user");
		at(token.expiresAt());

		assertThatThrownBy(() -> this.service.rotate(token.value())).isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void unknownOrGarbageIsRejected() {
		String random = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes());
		for (String value : new String[] { "", "x", random }) {
			assertThatThrownBy(() -> this.service.rotate(value)).isInstanceOf(InvalidRefreshTokenException.class);
		}
	}

	@Test
	void revokeFamilyDeletesAllRows() {
		IssuedRefreshToken original = this.service.startFamily("rt-user");
		this.service.rotate(original.value());

		this.service.revokeFamily(original.value());

		assertThat(this.jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_tokens WHERE username = 'rt-user'",
				Integer.class))
			.isZero();
		this.service.revokeFamily("unknown");
	}

	@Test
	void deleteExpiredRemovesOnlyExpired() {
		IssuedRefreshToken old = this.service.startFamily("rt-user");
		at(T0.plus(Duration.ofHours(1)));
		IssuedRefreshToken fresh = this.service.startFamily("rt-user");
		at(old.expiresAt());

		this.service.deleteExpired();

		assertThat(familyCount(old.value())).isZero();
		assertThat(familyCount(fresh.value())).isEqualTo(1);
	}

	private void at(Instant instant) {
		this.clock.reset();
		this.clock.willReturn(instant);
	}

	private int familyCount(String value) {
		return this.jdbcTemplate.queryForObject("""
				SELECT count(*) FROM refresh_tokens WHERE family_id =
				(SELECT family_id FROM refresh_tokens WHERE token_hash = ?)""", Integer.class,
				RefreshTokenService.hash(value));
	}

}
