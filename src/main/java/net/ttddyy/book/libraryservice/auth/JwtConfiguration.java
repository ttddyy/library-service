
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

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import net.ttddyy.book.libraryservice.LibraryServiceProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.Assert;

/**
 * Loads the RSA key pair and exposes the {@link JwtEncoder} and {@link JwtDecoder}.
 *
 * @author Tadaya Tsuyukubo
 */
@Configuration(proxyBeanMethods = false)
class JwtConfiguration {

	private final LibraryServiceProperties.Auth auth;

	private final RSAPrivateKey privateKey;

	private final RSAPublicKey publicKey;

	JwtConfiguration(LibraryServiceProperties properties) throws IOException {
		this.auth = properties.getAuth();
		LibraryServiceProperties.Auth.Jwt jwt = this.auth.getJwt();
		Resource privateResource = jwt.getPrivateKey();
		Resource publicResource = jwt.getPublicKey();
		Assert.state(privateResource != null && privateResource.exists(), "JWT private key is missing");
		Assert.state(publicResource != null && publicResource.exists(), "JWT public key is missing");
		try (InputStream in = privateResource.getInputStream()) {
			this.privateKey = RsaKeyConverters.pkcs8().convert(in);
		}
		try (InputStream in = publicResource.getInputStream()) {
			this.publicKey = RsaKeyConverters.x509().convert(in);
		}
		Assert.state(this.privateKey.getModulus().equals(this.publicKey.getModulus()),
				"JWT private and public keys do not match");
	}

	@Bean
	JwtEncoder jwtEncoder() {
		RSAKey jwk = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey)
			.keyID(this.auth.getJwt().getKeyId())
			.build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
	}

	@Bean
	JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(this.publicKey)
			.signatureAlgorithm(SignatureAlgorithm.RS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(this.auth.getIssuer()));
		return decoder;
	}

}
