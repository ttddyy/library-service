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

/**
 * Role names (without {@code ROLE_} prefix).
 *
 * @author Tadaya Tsuyukubo
 */
public final class Roles {

	public static final String SUPER_ADMIN = "SUPER_ADMIN";

	public static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";

	public static final String USER = "USER";

	private Roles() {
	}

}
