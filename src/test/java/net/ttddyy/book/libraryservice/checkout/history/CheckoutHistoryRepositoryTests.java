/*
 * Copyright 2024-2025 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DbTest
class CheckoutHistoryRepositoryTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	CheckoutHistoryRepository checkoutHistoryRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void initialData() {
		String sql;
		// members
		sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school_id, grade)
					VALUES	(10, 'foo', 'foo', 'sky', 7),
							(20, 'bar', 'bar', 'ocean', 2);
				""";
		this.jdbcTemplate.update(sql);

		// books
		sql = """
					INSERT INTO books (id, school_id, title, author, isbn, publisher, book_category_id, status, status_changed_at, num_checkouts)
					VALUES  (1, 'sky', 'foo', 'foo', 'foo', 'foo', 3, 'AVAILABLE', '2020-02-22', 10),
							(2, 'sky', 'bar', 'bar', 'bar', 'bar', 3, 'LOST','2020-02-22', 20),
							(3, 'ocean', 'baz', 'baz', 'baz', 'baz', 10, 'LOST','2020-02-22', 30),
							(4, 'ocean', 'qux', 'baz', 'baz', 'baz', 10, 'LOST','2020-02-22', 30);
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
	void findById() {
		CheckoutHistory history = this.checkoutHistoryRepository.findById(new CheckoutHistoryId(1L, 10L));
		assertThat(history).isNotNull().satisfies(checkoutHistory -> {
			assertThat(checkoutHistory.getBook().getId()).isEqualTo(1);
			assertThat(checkoutHistory.getMember().getId()).isEqualTo(10);
		});
	}

	@Test
	void findAllByBookId() {
		Page<CheckoutHistory> histories = this.checkoutHistoryRepository.findAllByBookId(1, Pageable.unpaged());
		assertThat(histories.getContent()).hasSize(1).first().satisfies((history) -> {
			assertThat(history.getBook().getId()).isEqualTo(1);
			assertThat(history.getMember().getId()).isEqualTo(10);
		});
	}

	@Test
	void findAllByMemberId() {
		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Direction.ASC, "book.id"));
		Page<CheckoutHistory> histories = this.checkoutHistoryRepository.findAllByMemberId(10, pageable);
		assertThat(histories.getContent()).hasSize(2);
		assertThat(histories.getContent().get(0)).satisfies((history) -> {
			assertThat(history.getMember().getId()).isEqualTo(10);
			assertThat(history.getBook().getId()).isEqualTo(1);
		});
		assertThat(histories.getContent().get(1)).satisfies((history) -> {
			assertThat(history.getMember().getId()).isEqualTo(10);
			assertThat(history.getBook().getId()).isEqualTo(2);
		});
	}

}
