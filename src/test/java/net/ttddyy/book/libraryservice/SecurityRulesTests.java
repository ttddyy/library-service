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

package net.ttddyy.book.libraryservice;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import net.ttddyy.book.libraryservice.auth.AccessTokenIssuer;
import net.ttddyy.book.libraryservice.auth.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the authorization rules in {@link ApplicationSecurityConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 */
@SpringBootTest
@AutoConfigureMockMvc
@DbTest
@ActiveProfiles("dev")
class SecurityRulesTests {

	private static final String ADMIN_PATHS = """
			POST, /api/books
			PUT, /api/books/1
			PUT, /api/checkouts/limits/defaults
			POST, /api/checkouts/limits/schedules
			PUT, /api/checkouts/limits/schedules
			DELETE, /api/checkouts/limits/schedules/1
			""";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	JwtEncoder jwtEncoder;

	@Autowired
	AccessTokenIssuer issuer;

	@AfterEach
	void cleanup() {
		this.jdbcTemplate.update("DELETE FROM authorities WHERE username = 'sec-user'");
		this.jdbcTemplate.update("DELETE FROM users WHERE username = 'sec-user'");
	}

	private ResultActions perform(HttpMethod method, String path, String role) throws Exception {
		return this.mvc.perform(MockMvcRequestBuilders.request(method, path)
			.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))));
	}

	private void assertAllowed(ResultActions result) throws Exception {
		int status = result.andReturn().getResponse().getStatus();
		assertThat(status).isNotIn(401, 403);
	}

	@ParameterizedTest
	@CsvSource({ "GET, /api/books", "GET, /api/members", "POST, /api/checkouts/checkout", "POST, /api/checkouts/return",
			"PUT, /api/books/1/status" })
	void userAllowed(HttpMethod method, String path) throws Exception {
		assertAllowed(perform(method, path, "USER"));
	}

	@ParameterizedTest
	@CsvSource(textBlock = ADMIN_PATHS)
	void userForbidden(HttpMethod method, String path) throws Exception {
		perform(method, path, "USER").andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@CsvSource(textBlock = ADMIN_PATHS)
	void schoolAdminAllowed(HttpMethod method, String path) throws Exception {
		assertAllowed(perform(method, path, "SCHOOL_ADMIN"));
	}

	@Test
	void superAdminInheritsSchoolAdmin() throws Exception {
		assertAllowed(perform(HttpMethod.PUT, "/api/checkouts/limits/defaults", "SUPER_ADMIN"));
	}

	@Test
	void noRoleIsForbidden() throws Exception {
		this.mvc.perform(MockMvcRequestBuilders.get("/api/books").with(jwt().authorities()))
			.andExpect(status().isForbidden());
	}

	@Test
	void actuatorInfoRequiresSuperAdmin() throws Exception {
		perform(HttpMethod.GET, "/actuator/info", "SCHOOL_ADMIN").andExpect(status().isForbidden());
		perform(HttpMethod.GET, "/actuator/info", "SUPER_ADMIN").andExpect(status().isOk());
	}

	@Test
	void publicEndpoints() throws Exception {
		this.mvc.perform(MockMvcRequestBuilders.get("/api/schools")).andExpect(status().isOk());
		this.mvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andExpect(status().isOk());
		assertAllowed(
				this.mvc.perform(MockMvcRequestBuilders.post("/api/auth/logout").header("X-Requested-With", "x")));
	}

	@Test
	void staleBearerIgnoredOnAuthEndpoints() throws Exception {
		this.mvc
			.perform(MockMvcRequestBuilders.post("/api/auth/logout")
				.header("X-Requested-With", "x")
				.header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
			.andExpect(status().isNoContent());
	}

	@Test
	void anonymousGetsBearerChallenge() throws Exception {
		this.mvc.perform(MockMvcRequestBuilders.get("/api/books"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.startsWith("Bearer")));
	}

	@Test
	void tokenFromOtherKeyRejected() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair pair = generator.generateKeyPair();
		RSAKey jwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate())
			.keyID("dev")
			.build();
		String token = encode(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk))), "library-service");
		expectInvalidToken(token);
	}

	@Test
	void algNoneRejected() throws Exception {
		String claims = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(("{\"iss\":\"library-service\",\"sub\":\"alice\",\"roles\":[\"USER\"],\"exp\":"
					+ (Instant.now().getEpochSecond() + 600) + "}")
				.getBytes());
		expectInvalidToken("eyJhbGciOiJub25lIn0." + claims + ".");
	}

	@Test
	void wrongIssuerRejected() throws Exception {
		expectInvalidToken(encode(this.jwtEncoder, "other"));
	}

	@Test
	void realTokenAccepted() throws Exception {
		String token = this.issuer.issue(new Account("alice", true, List.of("USER"), List.of("s1"))).value();
		assertAllowed(this.mvc
			.perform(MockMvcRequestBuilders.get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
	}

	@Test
	void basicWithJdbcUserWorks() throws Exception {
		insertUser();
		assertAllowed(this.mvc.perform(MockMvcRequestBuilders.get("/api/books").with(httpBasic("sec-user", "secret"))));
	}

	@Test
	void badBasicCredentialsSendNoBasicChallenge() throws Exception {
		insertUser();
		var response = this.mvc.perform(MockMvcRequestBuilders.get("/api/books").with(httpBasic("sec-user", "wrong")))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse();
		assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).noneMatch((value) -> value.startsWith("Basic"));
	}

	private void insertUser() {
		String password = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("secret");
		this.jdbcTemplate.update("INSERT INTO users (username, password, enabled) VALUES ('sec-user', ?, true)",
				password);
		this.jdbcTemplate.update("INSERT INTO authorities (username, authority) VALUES ('sec-user', 'ROLE_USER')");
	}

	private void expectInvalidToken(String token) throws Exception {
		this.mvc.perform(MockMvcRequestBuilders.get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
					org.hamcrest.Matchers.containsString("error=\"invalid_token\"")));
	}

	private String encode(JwtEncoder encoder, String issuer) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject("alice")
			.issuedAt(now)
			.expiresAt(now.plusSeconds(600))
			.claim("roles", List.of("USER"))
			.build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("dev").build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
