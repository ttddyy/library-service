/*
 * Copyright 2024 the original author or authors.
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

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * @author Tadaya Tsuyukubo
 */
@ConfigurationProperties("library-service")
public class LibraryServiceProperties {

	private Checkout checkout = new Checkout();

	private Auth auth = new Auth();

	public Auth getAuth() {
		return this.auth;
	}

	public void setAuth(Auth auth) {
		this.auth = auth;
	}

	public Checkout getCheckout() {
		return this.checkout;
	}

	public void setCheckout(Checkout checkout) {
		this.checkout = checkout;
	}

	public static class Checkout {

		private int maxBooks;

		private int maxDays;

		public int getMaxBooks() {
			return this.maxBooks;
		}

		public void setMaxBooks(int maxBooks) {
			this.maxBooks = maxBooks;
		}

		public int getMaxDays() {
			return this.maxDays;
		}

		public void setMaxDays(int maxDays) {
			this.maxDays = maxDays;
		}

	}

	public static class Auth {

		private String issuer = "library-service";

		private Duration accessTokenTtl = Duration.ofMinutes(15);

		private Duration refreshTokenTtl = Duration.ofDays(1);

		private Duration refreshReuseGrace = Duration.ofSeconds(10);

		private boolean cookieSecure = true;

		private Jwt jwt = new Jwt();

		public String getIssuer() {
			return this.issuer;
		}

		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}

		public Duration getAccessTokenTtl() {
			return this.accessTokenTtl;
		}

		public void setAccessTokenTtl(Duration accessTokenTtl) {
			this.accessTokenTtl = accessTokenTtl;
		}

		public Duration getRefreshTokenTtl() {
			return this.refreshTokenTtl;
		}

		public void setRefreshTokenTtl(Duration refreshTokenTtl) {
			this.refreshTokenTtl = refreshTokenTtl;
		}

		public Duration getRefreshReuseGrace() {
			return this.refreshReuseGrace;
		}

		public void setRefreshReuseGrace(Duration refreshReuseGrace) {
			this.refreshReuseGrace = refreshReuseGrace;
		}

		public boolean isCookieSecure() {
			return this.cookieSecure;
		}

		public void setCookieSecure(boolean cookieSecure) {
			this.cookieSecure = cookieSecure;
		}

		public Jwt getJwt() {
			return this.jwt;
		}

		public void setJwt(Jwt jwt) {
			this.jwt = jwt;
		}

		public static class Jwt {

			private String keyId;

			@Nullable
			private Resource privateKey;

			@Nullable
			private Resource publicKey;

			public String getKeyId() {
				return this.keyId;
			}

			public void setKeyId(String keyId) {
				this.keyId = keyId;
			}

			@Nullable
			public Resource getPrivateKey() {
				return this.privateKey;
			}

			public void setPrivateKey(@Nullable Resource privateKey) {
				this.privateKey = privateKey;
			}

			@Nullable
			public Resource getPublicKey() {
				return this.publicKey;
			}

			public void setPublicKey(@Nullable Resource publicKey) {
				this.publicKey = publicKey;
			}

		}

	}

}
