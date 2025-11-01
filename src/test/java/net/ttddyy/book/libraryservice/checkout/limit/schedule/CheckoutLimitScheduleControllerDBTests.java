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

package net.ttddyy.book.libraryservice.checkout.limit.schedule;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ CheckoutLimitScheduleService.class, CheckoutLimitScheduleController.class })
@DbTest
class CheckoutLimitScheduleControllerDBTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	CheckoutLimitScheduleController controller;

	@Test
	void bulkCreate() {
		CheckoutLimitScheduleDtoCreate dto1 = new CheckoutLimitScheduleDtoCreate("river", LocalDate.parse("2020-01-01"),
				1, 2, 3);
		CheckoutLimitScheduleDtoCreate dto2 = new CheckoutLimitScheduleDtoCreate("river", LocalDate.parse("2022-02-02"),
				2, 5, 6);
		List<CheckoutLimitScheduleDtoCreate> list = List.of(dto1, dto2);

		List<CheckoutLimitScheduleDto> created = this.controller.bulkCreate(list);
		assertThat(created).isNotNull().hasSize(2);

		this.entityManager.flush();

		int count = JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "checkout_limit_schedules");
		assertThat(count).isEqualTo(2);
	}

	@Test
	void bulkUpdate() {
		String sql = """
					INSERT INTO checkout_limit_schedules (id, school_id, grade, schedule_date, max_books, max_days)
					VALUES	( 1, 'river', 0, '2020-02-03', 1, 14),
							( 2, 'river', 0, '2020-03-03', 2, 21),
							( 3, 'river', 0, '2020-04-03', 3, 28),
							(10, 'ocean', 0, '2020-02-03', 11, 154),
							(11, 'ocean', 0, '2020-03-03', 22, 231)
				""";
		this.jdbcTemplate.update(sql);

		CheckoutLimitScheduleDtoUpdate dto1 = new CheckoutLimitScheduleDtoUpdate(2, LocalDate.parse("2020-01-01"), 1, 2,
				21, "river");
		CheckoutLimitScheduleDtoUpdate dto2 = new CheckoutLimitScheduleDtoUpdate(3, LocalDate.parse("2022-02-02"), 2, 5,
				42, "mountain");
		List<CheckoutLimitScheduleDtoUpdate> request = List.of(dto1, dto2);

		List<CheckoutLimitScheduleDto> updated = this.controller.bulkUpdate(request);
		assertThat(updated).hasSize(2).extracting(CheckoutLimitScheduleDto::id).containsExactlyInAnyOrder(2L, 3L);

		this.entityManager.flush();

		Map<String, Object> map;
		// verify id=2
		map = this.jdbcTemplate.queryForMap("SELECT * FROM checkout_limit_schedules WHERE id = 2");
		assertThat(map).containsEntry("school_id", "river")
			.containsEntry("grade", 1)
			.containsEntry("max_books", 2)
			.containsEntry("max_days", 21);
		assertThat(map.get("schedule_date")).isInstanceOfSatisfying(Date.class, (date) -> {
			assertThat(date).isEqualTo("2020-01-01");
		});
		// verify id=3
		map = this.jdbcTemplate.queryForMap("SELECT * FROM checkout_limit_schedules WHERE id = 3");
		assertThat(map).containsEntry("school_id", "mountain")
			.containsEntry("grade", 2)
			.containsEntry("max_books", 5)
			.containsEntry("max_days", 42);
		assertThat(map.get("schedule_date")).isInstanceOfSatisfying(Date.class, (date) -> {
			assertThat(date).isEqualTo("2022-02-02");
		});
	}

}
