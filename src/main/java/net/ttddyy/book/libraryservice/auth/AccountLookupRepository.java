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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

/**
 * Loads {@link Account} from the JDBC user store.
 *
 * @author Tadaya Tsuyukubo
 */
@Repository
public class AccountLookupRepository {

	private static final String ROLE_PREFIX = "ROLE_";

	private final JdbcTemplate jdbcTemplate;

	AccountLookupRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Nullable
	public Account find(String username) {
		List<Boolean> enabled = this.jdbcTemplate.queryForList("SELECT enabled FROM users WHERE username = ?",
				Boolean.class, username);
		if (enabled.isEmpty()) {
			return null;
		}
		List<String> roles = this.jdbcTemplate
			.queryForList("SELECT authority FROM authorities WHERE username = ? ORDER BY authority", String.class,
					username)
			.stream()
			.map((authority) -> authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length())
					: authority)
			.toList();
		Account probe = new Account(username, enabled.get(0), roles, List.of());
		List<String> schools = probe.isSuperAdmin()
				? this.jdbcTemplate.queryForList("SELECT id FROM schools ORDER BY id", String.class)
				: this.jdbcTemplate.queryForList(
						"SELECT school_id FROM user_schools WHERE username = ? ORDER BY school_id", String.class,
						username);
		return new Account(username, probe.enabled(), roles, schools);
	}

}
