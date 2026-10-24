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

import java.time.Instant;

import jakarta.servlet.http.Cookie;
import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.MockClocks.MockClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for login, refresh and logout.
 *
 * @author Tadaya Tsuyukubo
 */
@SpringBootTest
@AutoConfigureMockMvc
@DbTest
@ActiveProfiles("dev")
class AuthFlowIntegrationTests {

	private static final String COOKIE = "refresh_token";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	MockClock clock;

	@BeforeEach
	void setUp() {
		cleanUp();
		String password = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("secret");
		this.jdbcTemplate.update("INSERT INTO users (username, password, enabled) VALUES ('flow-admin', ?, TRUE)",
				password);
		this.jdbcTemplate
			.update("INSERT INTO authorities (username, authority) VALUES ('flow-admin', 'ROLE_SCHOOL_ADMIN')");
		this.jdbcTemplate.update("INSERT INTO user_schools (username, school_id) VALUES ('flow-admin', 'ocean')");
	}

	@AfterEach
	void cleanUp() {
		this.jdbcTemplate.update("DELETE FROM refresh_tokens WHERE username = 'flow-admin'");
		this.jdbcTemplate.update("DELETE FROM authorities WHERE username = 'flow-admin'");
		this.jdbcTemplate.update("DELETE FROM user_schools WHERE username = 'flow-admin'");
		this.jdbcTemplate.update("DELETE FROM users WHERE username = 'flow-admin'");
	}

	@Test
	void fullFlow() throws Exception {
		Instant start = Instant.now();
		setNow(start);

		MvcResult login = login();
		String accessToken = login.getResponse()
			.getContentAsString()
			.replaceAll(".*\"access_token\":\"([^\"]+)\".*", "$1");
		String cookieA = login.getResponse().getCookie(COOKIE).getValue();

		this.mvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk());

		MvcResult second = refresh(cookieA);
		assertThat(second.getResponse().getStatus()).isEqualTo(200);
		String cookieB = second.getResponse().getCookie(COOKIE).getValue();
		assertThat(cookieB).isNotEqualTo(cookieA);

		setNow(start.plusSeconds(5));
		MvcResult withinGrace = refresh(cookieA);
		assertThat(withinGrace.getResponse().getStatus()).isEqualTo(200);
		assertThat(withinGrace.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();

		setNow(start.plusSeconds(15));
		assertThat(refresh(cookieA).getResponse().getStatus()).isEqualTo(401);
		assertThat(refresh(cookieB).getResponse().getStatus()).isEqualTo(401);

		setNow(start.plusSeconds(20));
		String cookieC = login().getResponse().getCookie(COOKIE).getValue();
		this.mvc
			.perform(post("/api/auth/logout").header("X-Requested-With", "test").cookie(new Cookie(COOKIE, cookieC)))
			.andExpect(status().isNoContent());
		assertThat(refresh(cookieC).getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void swaggerAdvertisesBearerScheme() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").value("JWT"));
	}

	private void setNow(Instant instant) {
		this.clock.reset();
		this.clock.willReturn(instant);
	}

	private MvcResult login() throws Exception {
		return this.mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"flow-admin\",\"password\":\"secret\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.schools[0]").value("ocean"))
			.andReturn();
	}

	private MvcResult refresh(String cookie) throws Exception {
		return this.mvc
			.perform(post("/api/auth/refresh").header("X-Requested-With", "test").cookie(new Cookie(COOKIE, cookie)))
			.andReturn();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockConfiguration {

		@Bean
		@Primary
		MockClock mockClock() {
			return new MockClock(Instant.now());
		}

	}

}
