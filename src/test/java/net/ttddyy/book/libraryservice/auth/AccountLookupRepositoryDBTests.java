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
import java.util.Optional;

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccountLookupRepository} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(AccountLookupRepository.class)
@DbTest
class AccountLookupRepositoryDBTests {

	@Autowired
	AccountLookupRepository repository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		user("alice", "ROLE_SCHOOL_ADMIN", "ocean", "mountain");
		user("root", "ROLE_SUPER_ADMIN");
		user("bob", "ROLE_USER");
	}

	@Test
	void findsUserWithRolesAndSchools() {
		Account account = this.repository.find("alice").orElseThrow();
		assertThat(account.username()).isEqualTo("alice");
		assertThat(account.enabled()).isTrue();
		assertThat(account.roles()).containsExactly("SCHOOL_ADMIN");
		assertThat(account.schools()).containsExactly("mountain", "ocean");
		assertThat(account.isSuperAdmin()).isFalse();
	}

	@Test
	void superAdminGetsAllSchools() {
		Account account = this.repository.find("root").orElseThrow();
		List<String> all = this.jdbcTemplate.queryForList("SELECT id FROM schools ORDER BY id", String.class);
		assertThat(account.isSuperAdmin()).isTrue();
		assertThat(account.schools()).isNotEmpty().isEqualTo(all);
	}

	@Test
	void userWithoutSchools() {
		assertThat(this.repository.find("bob").orElseThrow().schools()).isEmpty();
	}

	@Test
	void unknownUserIsEmpty() {
		Optional<Account> account = this.repository.find("nobody");
		assertThat(account).isEmpty();
	}

	private void user(String username, String authority, String... schools) {
		this.jdbcTemplate.update("INSERT INTO users (username, password, enabled) VALUES (?, 'x', TRUE)", username);
		this.jdbcTemplate.update("INSERT INTO authorities (username, authority) VALUES (?, ?)", username, authority);
		for (String school : schools) {
			this.jdbcTemplate.update("INSERT INTO user_schools (username, school_id) VALUES (?, ?)", username, school);
		}
	}

}
