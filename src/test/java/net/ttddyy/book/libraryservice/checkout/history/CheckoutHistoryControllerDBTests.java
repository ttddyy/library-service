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

package net.ttddyy.book.libraryservice.checkout.history;

import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.school.SchoolController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SchoolController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ CheckoutHistoryController.class, CheckoutHistoryService.class })
@DbTest
class CheckoutHistoryControllerDBTests {

	@Autowired
	CheckoutHistoryController controller;

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void initialData() {
		String sql;
		// members
		sql = """
					INSERT INTO members (id, firstname_en, lastname_en, firstname, lastname, school, grade)
					VALUES	(10, 'foo', 'foo', 'FOO', 'FOO', 'sky', 7),
							(20, 'bar', 'bar', 'BAR', 'BAR', 'ocean', 2);
				""";
		this.jdbcTemplate.update(sql);

		// books
		sql = """
					INSERT INTO books (id, school_id, title, author, isbn, publisher, book_category_id, is_missing, date_lost, num_checkouts)
					VALUES  (1, 'sky', 'foo', 'foo', 'foo', 'foo', 3, false, null, 10),
							(2, 'sky', 'bar', 'bar', 'bar', 'bar', 3, true,'2020-02-22', 20),
							(3, 'ocean', 'baz', 'baz', 'baz', 'baz', 10, true,'2020-02-22', 30),
							(4, 'ocean', 'qux', 'baz', 'baz', 'baz', 10, true,'2020-02-22', 30);
				""";
		this.jdbcTemplate.update(sql);

		// checkouts_history
		sql = """
				INSERT INTO checkouts_history (book_id, member_id, checkout_date, due_date, operation)
				VALUES	( 1, 10, '2020-02-03', '2020-02-10', 'INSERT'),
						( 2, 10, '2020-02-03', '2020-02-10', 'INSERT'),
						( 3, 20, '2020-02-03', '2020-02-10', 'INSERT');
				""";
		this.jdbcTemplate.update(sql);
	}

	@Test
	void dto() {
		ResponseEntity<?> response = this.controller.history(2L, 10L, Pageable.unpaged());
		assertThat(response.getBody()).isInstanceOfSatisfying(CheckoutHistoryDto.class, (dto) -> {
			assertThat(dto.checkoutDate()).isEqualTo("2020-02-03");
			assertThat(dto.bookId()).isEqualTo(2);
			assertThat(dto.book().title()).isEqualTo("bar");
			assertThat(dto.memberId()).isEqualTo(10);
			assertThat(dto.member().firstname()).isEqualTo("FOO");
		});
	}

	@Test
	void notFound() {
		ResponseEntity<?> response = this.controller.history(100L, 200L, Pageable.unpaged());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
	}

	@Test
	void byBookId() {
		ResponseEntity<?> response = this.controller.history(2L, null, Pageable.unpaged());
		assertThat(response.getBody()).isInstanceOfSatisfying(Page.class, (page) -> {
			assertThat(page.getContent()).hasSize(1);
			assertThat(page.getContent().get(0)).isInstanceOfSatisfying(CheckoutHistoryDto.class, (dto) -> {
				assertThat(dto.checkoutDate()).isEqualTo("2020-02-03");
				assertThat(dto.bookId()).isEqualTo(2);
				assertThat(dto.book().title()).isEqualTo("bar");
				assertThat(dto.memberId()).isEqualTo(10);
				assertThat(dto.member().firstname()).isEqualTo("FOO");
			});
		});
	}

	@Test
	void byMemberId() {
		ResponseEntity<?> response = this.controller.history(null, 10L,
				Pageable.unpaged(Sort.by("book.id").ascending()));
		assertThat(response.getBody()).isInstanceOfSatisfying(Page.class, (page) -> {
			assertThat(page.getContent()).hasSize(2);
			assertThat(page.getContent().get(0)).isInstanceOfSatisfying(CheckoutHistoryDto.class, (dto) -> {
				assertThat(dto.bookId()).isEqualTo(1);
			});
			assertThat(page.getContent().get(1)).isInstanceOfSatisfying(CheckoutHistoryDto.class, (dto) -> {
				assertThat(dto.bookId()).isEqualTo(2);
			});
		});
	}

}
