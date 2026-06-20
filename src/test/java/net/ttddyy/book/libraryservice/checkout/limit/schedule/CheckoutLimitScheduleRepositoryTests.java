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

package net.ttddyy.book.libraryservice.checkout.limit.schedule;

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */

@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DbTest
class CheckoutLimitScheduleRepositoryTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	CheckoutLimitScheduleRepository checkoutLimitScheduleRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void findAllByScheduleDateAndSchoolId() {
		String sql;
		sql = """
					INSERT INTO checkout_limit_schedules (id, school_id, grade, schedule_date, max_books, max_days)
					VALUES	(1, 'ocean', 1, '2020-01-01', 10, 14),
							(2, 'ocean', 1, '2020-02-02', 11, 21),
							(3, 'ocean', 2, '2020-01-01', 1, 7),
							(10, 'mountain', 1, '2020-01-01', 2, 14);
				""";
		this.jdbcTemplate.update(sql);

		LocalDate date = LocalDate.parse("2020-01-01");
		List<CheckoutLimitSchedule> list = this.checkoutLimitScheduleRepository.findAllByScheduleDateAndSchoolId(date,
				"ocean");

		assertThat(list).hasSize(2).extracting(CheckoutLimitSchedule::getId).containsExactlyInAnyOrder(1L, 3L);
	}

}
