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
import java.util.List;

import net.ttddyy.book.libraryservice.ApplicationSecurityConfiguration;
import net.ttddyy.book.libraryservice.LibraryServiceProperties;
import net.ttddyy.book.libraryservice.MockClocks.MockClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link AuthController}.
 *
 * @author Tadaya Tsuyukubo
 */
@WebMvcTest(AuthController.class)
@Import({ ApplicationSecurityConfiguration.class, MockClock.class, AuthControllerTests.Config.class })
class AuthControllerTests {

	private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");

	@TestConfiguration
	@EnableWebSecurity
	@EnableConfigurationProperties(LibraryServiceProperties.class)
	static class Config {

	}

	@Autowired
	MockMvc mvc;

	@Autowired
	MockClock clock;

	@MockitoBean
	AuthService service;

	@MockitoBean
	JwtDecoder jwtDecoder;

	@MockitoBean
	DataSource dataSource;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.clock.willReturn(NOW);
	}

	private static final String CLEARED = "refresh_token=;";

	private ResultActions login(String body) throws Exception {
		return this.mvc.perform(
				MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions refresh(boolean header, boolean cookie) throws Exception {
		var request = MockMvcRequestBuilders.post("/api/auth/refresh");
		if (header) {
			request.header("X-Requested-With", "x");
		}
		if (cookie) {
			request.cookie(new Cookie("refresh_token", "old"));
		}
		return this.mvc.perform(request);
	}

	private void expectCleared(ResultActions result) throws Exception {
		result.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=;")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
	}

	@Test
	void loginSuccess() throws Exception {
		Account account = new Account("ocean-admin", true, List.of("SCHOOL_ADMIN"), List.of("ocean"));
		given(this.service.login("ocean-admin", "pw")).willReturn(new AuthService.LoginResult(
				new AccessToken("jwt", 900), new IssuedRefreshToken("rt", NOW.plusSeconds(86400)), account));

		login("{\"username\":\"ocean-admin\",\"password\":\"pw\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").value("jwt"))
			.andExpect(jsonPath("$.token_type").value("Bearer"))
			.andExpect(jsonPath("$.expires_in").value(900))
			.andExpect(jsonPath("$.user.username").value("ocean-admin"))
			.andExpect(jsonPath("$.user.roles[0]").value("SCHOOL_ADMIN"))
			.andExpect(jsonPath("$.user.schools[0]").value("ocean"))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=rt")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=86400")));
	}

	@Test
	void loginFailureIsUniform() throws Exception {
		given(this.service.login(any(), any())).willThrow(new BadCredentialsException("x"))
			.willThrow(new DisabledException("x"));

		for (int i = 0; i < 2; i++) {
			login("{\"username\":\"a\",\"password\":\"b\"}").andExpect(status().isUnauthorized())
				.andExpect(content().json("{\"error\":\"invalid_credentials\"}", true));
		}
	}

	@Test
	void blankFieldsAreInvalidCredentials() throws Exception {
		for (String body : new String[] { "{\"username\":\"\",\"password\":\"x\"}", "{}", "{\"username\":\"a\"}" }) {
			login(body).andExpect(status().isUnauthorized())
				.andExpect(content().json("{\"error\":\"invalid_credentials\"}", true));
		}
		verifyNoInteractions(this.service);
	}

	@Test
	void malformedBodyIsInvalidCredentials() throws Exception {
		for (String body : new String[] { "{not json", "" }) {
			login(body).andExpect(status().isUnauthorized())
				.andExpect(content().json("{\"error\":\"invalid_credentials\"}", true));
		}
	}

	@Test
	void refreshRotates() throws Exception {
		given(this.service.refresh("old")).willReturn(new AuthService.RefreshOutcome(new AccessToken("jwt2", 900),
				new IssuedRefreshToken("new", NOW.plusSeconds(100))));

		refresh(true, true).andExpect(status().isOk())
			.andExpect(jsonPath("$.access_token").value("jwt2"))
			.andExpect(jsonPath("$.token_type").value("Bearer"))
			.andExpect(jsonPath("$.expires_in").value(900))
			.andExpect(jsonPath("$.user").doesNotExist())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=new")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=100")));
	}

	@Test
	void refreshGraceHasNoCookie() throws Exception {
		given(this.service.refresh("old"))
			.willReturn(new AuthService.RefreshOutcome(new AccessToken("jwt2", 900), null));

		refresh(true, true).andExpect(status().isOk()).andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	@Test
	void refreshRequiresHeader() throws Exception {
		expectCleared(refresh(false, true).andExpect(status().isUnauthorized())
			.andExpect(content().json("{\"error\":\"invalid_refresh_token\"}", true)));
		verifyNoInteractions(this.service);
	}

	@Test
	void refreshMissingCookie() throws Exception {
		expectCleared(refresh(true, false).andExpect(status().isUnauthorized())
			.andExpect(content().json("{\"error\":\"invalid_refresh_token\"}", true)));
	}

	@Test
	void refreshInvalid() throws Exception {
		given(this.service.refresh("old")).willThrow(new InvalidRefreshTokenException());

		expectCleared(refresh(true, true).andExpect(status().isUnauthorized())
			.andExpect(content().json("{\"error\":\"invalid_refresh_token\"}", true)));
	}

	@Test
	void logoutAlways204() throws Exception {
		expectCleared(this.mvc
			.perform(MockMvcRequestBuilders.post("/api/auth/logout")
				.header("X-Requested-With", "x")
				.cookie(new Cookie("refresh_token", "old")))
			.andExpect(status().isNoContent()));
		verify(this.service).logout("old");

		expectCleared(this.mvc.perform(MockMvcRequestBuilders.post("/api/auth/logout").header("X-Requested-With", "x"))
			.andExpect(status().isNoContent()));
		verify(this.service).logout(null);
	}

	@Test
	void logoutRequiresHeader() throws Exception {
		this.mvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")).andExpect(status().isUnauthorized());
		verify(this.service, never()).logout(any());
	}

}
