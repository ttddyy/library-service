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

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import net.ttddyy.book.libraryservice.LibraryServiceProperties;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues RS256 signed access tokens.
 *
 * @author Tadaya Tsuyukubo
 */
@Component
public class AccessTokenIssuer {

	private final JwtEncoder encoder;

	private final LibraryServiceProperties.Auth auth;

	private final Clock clock;

	AccessTokenIssuer(JwtEncoder encoder, LibraryServiceProperties properties, Clock clock) {
		this.encoder = encoder;
		this.auth = properties.getAuth();
		this.clock = clock;
	}

	public AccessToken issue(Account account) {
		Instant now = this.clock.instant();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(this.auth.getJwt().getKeyId()).build();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(this.auth.getIssuer())
			.subject(account.username())
			.issuedAt(now)
			.expiresAt(now.plus(this.auth.getAccessTokenTtl()))
			.id(UUID.randomUUID().toString())
			.claim("roles", account.roles());
		if (!account.isSuperAdmin()) {
			claims.claim("schools", account.schools());
		}
		String value = this.encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
		return new AccessToken(value, this.auth.getAccessTokenTtl().toSeconds());
	}

}
