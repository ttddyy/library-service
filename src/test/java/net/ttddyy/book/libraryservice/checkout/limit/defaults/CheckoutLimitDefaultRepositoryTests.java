/*
 * Copyright 2025 the original author or authors.
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

package net.ttddyy.book.libraryservice.checkout.limit.defaults;

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */

@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DbTest
class CheckoutLimitDefaultRepositoryTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	CheckoutLimitDefaultRepository checkoutLimitDefaultRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void findAllBySchoolId() {
		List<CheckoutLimitDefault> limits = this.checkoutLimitDefaultRepository.findAllBySchoolId("ocean");
		assertThat(limits).hasSize(8); // see data.sql
	}

	@Test
	void findAllBySchoolIdNotExists() {
		List<CheckoutLimitDefault> limits = this.checkoutLimitDefaultRepository.findAllBySchoolId("abc");
		assertThat(limits).isEmpty(); // see data.sql
	}

}
