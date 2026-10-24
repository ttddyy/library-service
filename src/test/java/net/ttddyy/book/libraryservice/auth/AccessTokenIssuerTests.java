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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import net.ttddyy.book.libraryservice.LibraryServiceProperties;
import net.ttddyy.book.libraryservice.MockClocks.MockClock;
import org.junit.jupiter.api.Test;

import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccessTokenIssuer}.
 *
 * @author Tadaya Tsuyukubo
 */
class AccessTokenIssuerTests {

	private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");

	private final NimbusJwtDecoder decoder;

	private final AccessTokenIssuer issuer;

	AccessTokenIssuerTests() throws Exception {
		RSAPrivateKey privateKey;
		RSAPublicKey publicKey;
		try (InputStream in = Files.newInputStream(Path.of("dev/jwt/private.pem"))) {
			privateKey = RsaKeyConverters.pkcs8().convert(in);
		}
		try (InputStream in = Files.newInputStream(Path.of("dev/jwt/public.pem"))) {
			publicKey = RsaKeyConverters.x509().convert(in);
		}
		RSAKey jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("dev").build();
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
		this.decoder = NimbusJwtDecoder.withPublicKey(publicKey).signatureAlgorithm(SignatureAlgorithm.RS256).build();
		// the decoder would reject the fixed (past) clock time as expired
		this.decoder
			.setJwtValidator((jwt) -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
		LibraryServiceProperties properties = new LibraryServiceProperties();
		properties.getAuth().getJwt().setKeyId("dev");
		this.issuer = new AccessTokenIssuer(encoder, properties, new MockClock(NOW));
	}

	@Test
	void issuesTokenWithSpecClaims() {
		Account account = new Account("ocean-admin", true, List.of("SCHOOL_ADMIN"), List.of("ocean"));
		AccessToken token = this.issuer.issue(account);

		Jwt jwt = this.decoder.decode(token.value());
		assertThat(jwt.getSubject()).isEqualTo("ocean-admin");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("library-service");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SCHOOL_ADMIN");
		assertThat(jwt.getClaimAsStringList("schools")).containsExactly("ocean");
		assertThat(jwt.getId()).isNotBlank();
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
		assertThat(jwt.getHeaders()).containsEntry("kid", "dev");
		assertThat(token.expiresIn()).isEqualTo(900);
	}

	@Test
	void omitsSchoolsForSuperAdmin() {
		Account account = new Account("root", true, List.of("SUPER_ADMIN"), List.of("ocean", "river"));
		Jwt jwt = this.decoder.decode(this.issuer.issue(account).value());
		assertThat(jwt.getClaims()).doesNotContainKey("schools");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SUPER_ADMIN");
	}

}
