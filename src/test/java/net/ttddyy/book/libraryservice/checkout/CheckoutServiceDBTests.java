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

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.MockClocks.System;
import net.ttddyy.book.libraryservice.book.Book;
import net.ttddyy.book.libraryservice.checkout.limit.CheckoutLimitService;
import net.ttddyy.book.libraryservice.checkout.limit.defaults.CheckoutLimitDefaultService;
import net.ttddyy.book.libraryservice.checkout.limit.schedule.CheckoutLimitScheduleService;
import net.ttddyy.book.libraryservice.member.Member;
import org.hibernate.proxy.HibernateProxy;
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
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ CheckoutService.class, CheckoutLimitService.class, CheckoutLimitDefaultService.class,
		CheckoutLimitScheduleService.class, System.class })
@DbTest
class CheckoutServiceDBTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	CheckoutService service;

	@Test
	void bulkReturn() {
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
		this.service.bulkReturn(bookIds);

		this.entityManager.flush();

		int count = JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "checkouts");
		assertThat(count).isEqualTo(1);

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM checkouts");
		assertThat(map).containsEntry("book_id", 3);

		// deleting non-existing book-ids does not change data
		this.service.bulkReturn(bookIds);
		count = JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "checkouts");
		assertThat(count).isEqualTo(1);
	}

	@Test
	void checkout() {
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

		Set<Long> bookIds = Set.of(1L, 2L);
		this.service.checkout("sky", 10, bookIds, true);

		this.entityManager.flush();

		int count = JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "checkouts");
		assertThat(count).isEqualTo(2);
	}

	@Test
	void list() {
		String sql;
		// members
		sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school, grade)
					VALUES	(10, 'foo', 'foo', 'sky', 7),
							(20, 'bar', 'bar', 'ocean', 2),
							(30, 'baz', 'baz', 'ocean', 3);
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
						( 3, 20, '2020-02-03', '2020-02-10'),
						( 4, 30, '2020-02-03', '2020-02-10');
				""";
		this.jdbcTemplate.update(sql);

		Page<Checkout> page;
		List<Checkout> checkouts;
		page = this.service.list(null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(4);
		verifyJoinAssociation(page.getContent());

		page = this.service.list(10L, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(2);
		checkouts = page.getContent();
		assertThat(checkouts).extracting(Checkout::getId)
			.extracting(CheckoutId::getBookId)
			.containsExactlyInAnyOrder(1L, 2L);
		verifyJoinAssociation(page.getContent());

		page = this.service.list(null, "sky", Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent()).extracting(Checkout::getId)
			.extracting(CheckoutId::getBookId)
			.containsExactlyInAnyOrder(1L, 2L);
		verifyJoinAssociation(page.getContent());

		page = this.service.list(30L, "ocean", Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).extracting(Checkout::getId)
			.extracting(CheckoutId::getBookId)
			.containsExactlyInAnyOrder(4L);
		verifyJoinAssociation(page.getContent());
	}

	private void verifyJoinAssociation(List<Checkout> checkouts) {
		assertThat(checkouts).allSatisfy((checkout) -> {
			assertThat(checkout.getBook()).isNotInstanceOf(HibernateProxy.class).isInstanceOf(Book.class);
			assertThat(checkout.getMember()).isNotInstanceOf(HibernateProxy.class).isInstanceOf(Member.class);
		});
	}

}
