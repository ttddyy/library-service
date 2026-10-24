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
import java.util.List;

import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.MockClocks.MockClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AuthService} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@SpringBootTest
@DbTest
@ActiveProfiles("dev")
class AuthServiceDBTests {

	@Autowired
	AuthService service;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	JwtDecoder jwtDecoder;

	@BeforeEach
	void setUp() {
		cleanUp();
		user("as-admin", true);
		this.jdbcTemplate.update("INSERT INTO authorities (username, authority) VALUES ('as-admin', 'ROLE_USER')");
		this.jdbcTemplate.update("INSERT INTO user_schools (username, school_id) VALUES ('as-admin', 'ocean')");
		user("as-disabled", false);
	}

	@AfterEach
	void cleanUp() {
		this.jdbcTemplate.update("DELETE FROM refresh_tokens WHERE username LIKE 'as-%'");
		this.jdbcTemplate.update("DELETE FROM authorities WHERE username LIKE 'as-%'");
		this.jdbcTemplate.update("DELETE FROM user_schools WHERE username LIKE 'as-%'");
		this.jdbcTemplate.update("DELETE FROM users WHERE username LIKE 'as-%'");
	}

	@Test
	void loginReturnsAccountAndTokens() {
		AuthService.LoginResult result = this.service.login("as-admin", "secret");

		assertThat(result.account().schools()).containsExactly("ocean");
		assertThat(result.accessToken().value()).isNotBlank();
		assertThat(count("as-admin")).isEqualTo(1);
	}

	@Test
	void loginDeletesExpiredRows() {
		Instant past = Instant.now().minus(Duration.ofDays(1));
		this.jdbcTemplate.update(
				"INSERT INTO refresh_tokens (token_hash, family_id, username, expires_at) VALUES ('h', ?, 'as-disabled', ?)",
				java.util.UUID.randomUUID(), java.sql.Timestamp.from(past));

		this.service.login("as-admin", "secret");

		assertThat(count("as-disabled")).isZero();
	}

	@Test
	void loginFailures() {
		assertThatThrownBy(() -> this.service.login("as-nobody", "secret")).isInstanceOf(AuthenticationException.class);
		assertThatThrownBy(() -> this.service.login("as-admin", "wrong")).isInstanceOf(AuthenticationException.class);
		assertThatThrownBy(() -> this.service.login("as-disabled", "secret"))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void refreshForDisabledUserRevokesFamily() {
		AuthService.LoginResult login = this.service.login("as-admin", "secret");
		this.jdbcTemplate.update("UPDATE users SET enabled = FALSE WHERE username = 'as-admin'");

		assertThatThrownBy(() -> this.service.refresh(login.refreshToken().value()))
			.isInstanceOf(InvalidRefreshTokenException.class);

		assertThat(count("as-admin")).isZero();
	}

	@Test
	void refreshPicksUpRoleChange() {
		AuthService.LoginResult login = this.service.login("as-admin", "secret");
		this.jdbcTemplate
			.update("INSERT INTO authorities (username, authority) VALUES ('as-admin', 'ROLE_SCHOOL_ADMIN')");

		AuthService.RefreshOutcome outcome = this.service.refresh(login.refreshToken().value());

		List<String> roles = this.jwtDecoder.decode(outcome.accessToken().value()).getClaimAsStringList("roles");
		assertThat(roles).contains("SCHOOL_ADMIN");
		assertThat(outcome.refreshToken()).isNotNull();
	}

	private void user(String username, boolean enabled) {
		String password = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("secret");
		this.jdbcTemplate.update("INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)", username, password,
				enabled);
	}

	private int count(String username) {
		return this.jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_tokens WHERE username = ?", Integer.class,
				username);
	}

}
