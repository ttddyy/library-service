
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import net.ttddyy.book.libraryservice.LibraryServiceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JwtConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(JwtConfiguration.class, PropertiesConfig.class)
		.withPropertyValues("library-service.auth.issuer=test-issuer", "library-service.auth.jwt.key-id=dev",
				"library-service.auth.jwt.private-key=file:dev/jwt/private.pem",
				"library-service.auth.jwt.public-key=file:dev/jwt/public.pem");

	@Test
	void roundTrip() {
		this.runner.run((context) -> {
			String token = encode(context.getBean(JwtEncoder.class), "test-issuer");
			Jwt jwt = context.getBean(JwtDecoder.class).decode(token);
			assertThat(jwt.getSubject()).isEqualTo("alice");
			assertThat(jwt.getHeaders()).containsEntry("kid", "dev");
		});
	}

	@Test
	void rejectsWrongIssuer() {
		this.runner.run((context) -> {
			String token = encode(context.getBean(JwtEncoder.class), "other");
			assertThatExceptionOfType(JwtValidationException.class)
				.isThrownBy(() -> context.getBean(JwtDecoder.class).decode(token));
		});
	}

	@Test
	void rejectsOtherKey() {
		this.runner.run((context) -> {
			KeyPair pair = generateKeyPair();
			RSAKey jwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey(pair.getPrivate())
				.keyID("dev")
				.build();
			String token = encode(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk))), "test-issuer");
			assertThatExceptionOfType(BadJwtException.class)
				.isThrownBy(() -> context.getBean(JwtDecoder.class).decode(token));
		});
	}

	@Test
	void missingKeyFailsStartup() {
		this.runner.withPropertyValues("library-service.auth.jwt.private-key=file:dev/jwt/missing.pem")
			.run((context) -> assertThat(context.getStartupFailure()).isNotNull());
	}

	@Test
	void mismatchedKeysFailStartup(@TempDir Path dir) throws Exception {
		byte[] encoded = generateKeyPair().getPublic().getEncoded();
		String pem = "-----BEGIN PUBLIC KEY-----\n"
				+ Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encoded)
				+ "\n-----END PUBLIC KEY-----\n";
		Path other = Files.writeString(dir.resolve("other.pem"), pem);
		this.runner.withPropertyValues("library-service.auth.jwt.public-key=" + other.toUri())
			.run((context) -> assertThat(context.getStartupFailure()).isNotNull());
	}

	@Test
	void defaults() {
		new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class).run((context) -> {
			LibraryServiceProperties.Auth auth = context.getBean(LibraryServiceProperties.class).getAuth();
			assertThat(auth.getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
			assertThat(auth.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(1));
			assertThat(auth.getRefreshReuseGrace()).isEqualTo(Duration.ofSeconds(10));
			assertThat(auth.isCookieSecure()).isTrue();
			assertThat(auth.getIssuer()).isEqualTo("library-service");
		});
	}

	private static String encode(JwtEncoder encoder, String issuer) {
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("dev").build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject("alice")
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(LibraryServiceProperties.class)
	static class PropertiesConfig {

	}

}
