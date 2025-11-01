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

import jakarta.persistence.EntityNotFoundException;
import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ CheckoutLimitScheduleService.class })
@DbTest
class CheckoutLimitScheduleServiceDBTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	CheckoutLimitScheduleService service;

	@Test
	void list() {
		String sql = """
					INSERT INTO checkout_limit_schedules (id, school_id, grade, schedule_date, max_books, max_days)
					VALUES	( 1, 'river', 0, '2020-02-03', 1, 14),
							( 2, 'river', 0, '2020-03-03', 2, 21),
							( 3, 'river', 0, '2020-04-03', 3, 28),
							(10, 'ocean', 0, '2020-02-03', 11, 154),
							(11, 'ocean', 0, '2020-03-03', 22, 231)
				""";
		this.jdbcTemplate.update(sql);

		Page<CheckoutLimitSchedule> page;
		page = this.service.list(null, Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(5);

		page = this.service.list("river", Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent()).extracting(CheckoutLimitSchedule::getId).containsExactlyInAnyOrder(1L, 2L, 3L);
	}

	@Test
	void create() {
		CheckoutLimitSchedule entity = new CheckoutLimitSchedule();
		entity.setSchoolId("river");
		entity.setGrade(1);
		entity.setScheduleDate(LocalDate.parse("2022-02-02"));
		entity.setMaxBooks(50);
		entity.setMaxDays(60);
		List<CheckoutLimitSchedule> created = this.service.create(List.of(entity));
		assertThat(created).hasSize(1);

		entityManager.flush();

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM checkout_limit_schedules");
		assertThat(map).containsEntry("school_id", "river")
			.containsEntry("grade", 1)
			.containsEntry("max_books", 50)
			.containsEntry("max_days", 60);
		assertThat(map.get("schedule_date")).isInstanceOfSatisfying(Date.class, (date) -> {
			assertThat(date).isEqualTo("2022-02-02");
		});
	}

	@Test
	void update() {
		String sql = """
					INSERT INTO checkout_limit_schedules (id, school_id, grade, schedule_date, max_books, max_days)
					VALUES	( 1, 'river', 0, '2020-02-03', 1, 14),
							( 2, 'river', 0, '2020-03-03', 2, 21),
							( 3, 'river', 0, '2020-04-03', 3, 28),
							(10, 'ocean', 0, '2020-02-03', 11, 154),
							(11, 'ocean', 0, '2020-03-03', 22, 231)
				""";
		this.jdbcTemplate.update(sql);

		CheckoutLimitScheduleDtoUpdate request = new CheckoutLimitScheduleDtoUpdate(1, LocalDate.parse("2022-02-02"), 1,
				50, 60, "ocean");
		List<CheckoutLimitSchedule> result = this.service.update(List.of(request));
		assertThat(result).hasSize(1);
		CheckoutLimitSchedule updated = result.get(0);
		assertThat(updated.getGrade()).isEqualTo(1);
		assertThat(updated.getScheduleDate()).isEqualTo("2022-02-02");
		assertThat(updated.getMaxBooks()).isEqualTo(50);
		assertThat(updated.getMaxDays()).isEqualTo(60);
		assertThat(updated.getSchoolId()).isEqualTo("ocean");

		entityManager.flush();

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM checkout_limit_schedules WHERE id = 1");
		assertThat(map).containsEntry("school_id", "ocean")
			.containsEntry("grade", 1)
			.containsEntry("max_books", 50)
			.containsEntry("max_days", 60)
			.containsEntry("school_id", "ocean");
		assertThat(map.get("schedule_date")).isInstanceOfSatisfying(Date.class, (date) -> {
			assertThat(date).isEqualTo("2022-02-02");
		});
	}

	@Test
	void updateNotExistingId() {
		CheckoutLimitScheduleDtoUpdate request = new CheckoutLimitScheduleDtoUpdate(1, LocalDate.parse("2022-02-02"), 1,
				50, 60, "none");
		assertThatThrownBy(() -> {
			this.service.update(List.of(request));
		}).isInstanceOf(EntityNotFoundException.class);
	}

}
