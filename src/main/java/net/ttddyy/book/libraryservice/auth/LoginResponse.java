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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Login response: access token plus the user's summary.
 *
 * @author Tadaya Tsuyukubo
 */
public record LoginResponse(@JsonProperty("access_token") String accessToken,
		@JsonProperty("token_type") String tokenType, @JsonProperty("expires_in") long expiresIn, User user) {

	public record User(String username, List<String> roles, List<String> schools) {
	}

}
