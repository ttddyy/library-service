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
import java.time.Duration;
import java.util.Map;

import net.ttddyy.book.libraryservice.LibraryServiceProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login, refresh and logout endpoints.
 *
 * @author Tadaya Tsuyukubo
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Login, access token refresh and logout")
@SecurityRequirements // public endpoints; overrides the global bearer requirement
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	private static final String COOKIE_NAME = "refresh_token";

	private static final String COOKIE_PATH = "/api/auth";

	private final AuthService service;

	private final LibraryServiceProperties.Auth auth;

	private final Clock clock;

	AuthController(AuthService service, LibraryServiceProperties properties, Clock clock) {
		this.service = service;
		this.auth = properties.getAuth();
		this.clock = clock;
	}

	// JSON body required (415 otherwise) on purpose: blocks cross-site form-post login
	// CSRF
	@PostMapping("/login")
	@Operation(summary = "Log in",
			description = "Returns an access token and sets the refresh token as an HttpOnly cookie. JSON body required.")
	@ApiResponse(responseCode = "200", description = "Logged in")
	@ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
	ResponseEntity<LoginResponse> login(@RequestBody(required = false) @Nullable LoginRequest request) {
		if (request == null || !StringUtils.hasText(request.username()) || !StringUtils.hasText(request.password())) {
			throw new BadCredentialsException("Missing credentials");
		}
		AuthService.LoginResult result = this.service.login(request.username(), request.password());
		Account account = result.account();
		LoginResponse body = new LoginResponse(result.accessToken().value(), "Bearer", result.accessToken().expiresIn(),
				new LoginResponse.User(account.username(), account.roles(), account.schools()));
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
			.body(body);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token",
			description = "Issues a new access token from the refresh token cookie. May rotate the refresh token cookie.")
	@ApiResponse(responseCode = "200", description = "Token refreshed")
	@ApiResponse(responseCode = "401", description = "Missing or invalid refresh token", content = @Content)
	ResponseEntity<TokenResponse> refresh(
			@Parameter(description = "Required (any value); CSRF guard") @RequestHeader(name = "X-Requested-With",
					required = false) @Nullable String requestedWith,
			@Parameter(in = ParameterIn.COOKIE, description = "Refresh token") @CookieValue(name = COOKIE_NAME,
					required = false) @Nullable String refreshToken) {
		if (requestedWith == null || refreshToken == null) {
			throw new InvalidRefreshTokenException();
		}
		AuthService.RefreshOutcome outcome = this.service.refresh(refreshToken);
		ResponseEntity.BodyBuilder response = ResponseEntity.ok();
		if (outcome.refreshToken() != null) {
			response.header(HttpHeaders.SET_COOKIE, refreshCookie(outcome.refreshToken()).toString());
		}
		return response
			.body(new TokenResponse(outcome.accessToken().value(), "Bearer", outcome.accessToken().expiresIn()));
	}

	@PostMapping("/logout")
	@Operation(summary = "Log out", description = "Revokes the refresh token and clears its cookie.")
	@ApiResponse(responseCode = "204", description = "Logged out")
	@ApiResponse(responseCode = "401", description = "Missing X-Requested-With header", content = @Content)
	ResponseEntity<Void> logout(
			@Parameter(description = "Required (any value); CSRF guard") @RequestHeader(name = "X-Requested-With",
					required = false) @Nullable String requestedWith,
			@Parameter(in = ParameterIn.COOKIE, description = "Refresh token") @CookieValue(name = COOKIE_NAME,
					required = false) @Nullable String refreshToken) {
		if (requestedWith == null) {
			throw new InvalidRefreshTokenException();
		}
		this.service.logout(refreshToken);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
	}

	@ExceptionHandler({ AuthenticationException.class, HttpMessageNotReadableException.class })
	ResponseEntity<Map<String, String>> invalidCredentials(Exception ex) {
		if (ex instanceof InternalAuthenticationServiceException) {
			logger.warn("Login failed due to an internal authentication error", ex);
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_credentials"));
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ResponseEntity<Map<String, String>> invalidRefreshToken() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
			.body(Map.of("error", "invalid_refresh_token"));
	}

	private ResponseCookie refreshCookie(IssuedRefreshToken token) {
		return cookie(token.value(), Duration.between(this.clock.instant(), token.expiresAt()));
	}

	private ResponseCookie cookie(String value, Duration maxAge) {
		return ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(this.auth.isCookieSecure())
			.sameSite("Strict")
			.path(COOKIE_PATH)
			.maxAge(maxAge)
			.build();
	}

}
