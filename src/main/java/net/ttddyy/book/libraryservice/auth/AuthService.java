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

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Login, refresh and logout orchestration.
 *
 * @author Tadaya Tsuyukubo
 */
@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;

	private final AccountLookup accountLookup;

	private final AccessTokenIssuer issuer;

	private final RefreshTokenService refreshTokenService;

	AuthService(AuthenticationManager authenticationManager, AccountLookup accountLookup, AccessTokenIssuer issuer,
			RefreshTokenService refreshTokenService) {
		this.authenticationManager = authenticationManager;
		this.accountLookup = accountLookup;
		this.issuer = issuer;
		this.refreshTokenService = refreshTokenService;
	}

	public LoginResult login(String username, String password) {
		this.authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
		this.refreshTokenService.deleteExpired();
		Account account = this.accountLookup.find(username).orElseThrow();
		IssuedRefreshToken refreshToken = this.refreshTokenService.startFamily(username);
		return new LoginResult(this.issuer.issue(account), refreshToken, account);
	}

	public RefreshOutcome refresh(String refreshToken) {
		RefreshResult result = this.refreshTokenService.rotate(refreshToken);
		Account account = this.accountLookup.find(result.username()).filter(Account::enabled).orElse(null);
		if (account == null) {
			this.refreshTokenService.revokeFamily(refreshToken);
			throw new InvalidRefreshTokenException();
		}
		return new RefreshOutcome(this.issuer.issue(account), result.successor());
	}

	public void logout(@Nullable String refreshToken) {
		if (refreshToken != null) {
			this.refreshTokenService.revokeFamily(refreshToken);
		}
	}

	public record LoginResult(AccessToken accessToken, IssuedRefreshToken refreshToken, Account account) {
	}

	public record RefreshOutcome(AccessToken accessToken, @Nullable IssuedRefreshToken refreshToken) {
	}

}
