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

package net.ttddyy.book.libraryservice.checkout;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.book.Book;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */

@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DbTest
class CheckoutRepositoryTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	CheckoutRepository checkoutRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void deleteAllByIdBookIdIn() {
		String sql;
		// members
		sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school, grade)
					VALUES	(10, 'foo', 'foo', 'sky', 7),
							(20, 'bar', 'bar', 'ocean', 2);
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

		// checkouts
		sql = """
				INSERT INTO checkouts (book_id, member_id, checkout_date, due_date)
				VALUES	( 1, 10, '2020-02-03', '2020-02-10'),
						( 2, 10, '2020-02-03', '2020-02-10'),
						( 3, 20, '2020-02-03', '2020-02-10');
				""";
		this.jdbcTemplate.update(sql);

		Set<Long> bookIds = Set.of(1L, 2L);
		this.checkoutRepository.deleteAllByIdBookIdIn(bookIds);

		int count = JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "checkouts");
		assertThat(count).isEqualTo(1);

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM checkouts");
		assertThat(map.get("book_id")).isEqualTo(3);
	}

	@Test
	void bookDetailsByMemberId() {
		String sql;
		// members
		sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school, grade)
					VALUES	(10, 'foo', 'foo', 'sky', 7),
							(20, 'bar', 'bar', 'ocean', 2);
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

		// checkouts
		sql = """
				INSERT INTO checkouts (book_id, member_id, checkout_date, due_date)
				VALUES	( 1, 10, '2020-02-03', '2020-02-10'),
						( 2, 10, '2020-02-03', '2020-02-10'),
						( 3, 10, '2020-02-03', '2020-02-10');
				""";
		this.jdbcTemplate.update(sql);

		List<Checkout> result = this.checkoutRepository.bookDetailsByMemberId(10L);
		assertThat(result).hasSize(3).allSatisfy((checkout -> {
			assertThat(checkout.getBook()).isNotInstanceOf(HibernateProxy.class).isInstanceOf(Book.class);
		}));
	}

	@Test
	void overdue() {
		String sql;
		// members
		sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school, grade)
					VALUES	(10, 'foo', 'foo', 'sky', 7),
							(20, 'bar', 'bar', 'ocean', 2);
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

		// checkouts
		sql = """
				INSERT INTO checkouts (book_id, member_id, checkout_date, due_date)
				VALUES	( 1, 10, '2020-02-03', '2020-02-10'),
						( 2, 10, '2020-03-01', '2020-03-08'),
						( 3, 20, '2020-02-10', '2020-02-17');
				""";
		this.jdbcTemplate.update(sql);

		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Direction.ASC, "book.id"));
		Page<Checkout> checkouts = this.checkoutRepository.findOverdue(LocalDate.parse("2020-02-18"), pageable);

		assertThat(checkouts.getContent()).hasSize(2)
			.extracting(Checkout::getId)
			.extracting(CheckoutId::getBookId)
			.containsExactly(1L, 3L);
		// make sure associations are fetched
		assertThat(checkouts.getContent()).extracting(Checkout::getBook)
			.allSatisfy(book -> assertThat(Hibernate.isInitialized(book)).isTrue());
		assertThat(checkouts.getContent()).extracting(Checkout::getMember)
			.allSatisfy(member -> assertThat(Hibernate.isInitialized(member)).isTrue());
	}

}
