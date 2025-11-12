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

package net.ttddyy.book.libraryservice.book;

import net.ttddyy.book.libraryservice.DbTest;
import net.ttddyy.book.libraryservice.MockClocks.MockClock;
import org.hibernate.SessionFactory;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Crud tests for {@link BookController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@DbTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ BookController.class, BookService.class, MockClock.class })
class BookControllerDBTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	SessionFactory sessionFactory;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	BookController controller;

	@Autowired
	MockClock clock;

	@BeforeEach
	void resetClock() {
		this.clock.reset();
		this.sessionFactory.getStatistics().clear();
	}

	@Test
	void create() {
		// category: (3, 'C', 'SJ民話', 1200000, 1299999, '民話', 'ocean', '')
		BookDtoCreate request = new BookDtoCreate("foo", "", "foo", "", "foo", "", "foo", "river", 3);
		List<BookDto> result = this.controller.create(List.of(request));

		this.entityManager.flush();

		assertThat(result).hasSize(1);
		assertThat(result).first().extracting(BookDto::id).isEqualTo(1200000L);

		Statistics statistics = this.sessionFactory.getStatistics();
		EntityStatistics entityStatistics = statistics.getEntityStatistics(Book.class.getName());
		assertThat(entityStatistics.getInsertCount()).isEqualTo(1);

		statistics.clear();

		// create another with same category(3) should get the next id
		BookDtoCreate request2 = new BookDtoCreate("bar", "", "bar", "", "bar", "", "bar", "river", 3);

		result = this.controller.create(List.of(request2));
		this.entityManager.flush();

		assertThat(result).hasSize(1);
		assertThat(result).first().extracting(BookDto::id).isEqualTo(1200001L);

		entityStatistics = statistics.getEntityStatistics(Book.class.getName());
		assertThat(entityStatistics.getInsertCount()).isEqualTo(1);

	}

	@Test
	void update() {
		createTestData();
		BookDtoUpdate updateRequest = new BookDtoUpdate("title-updated", null, "author-updated", null, null, null, null,
				null, null);

		BookDto result = this.controller.update(1200000, updateRequest);
		this.entityManager.flush();

		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("title-updated");
		assertThat(result.author()).isEqualTo("author-updated");
		assertThat(result.category().id()).isEqualTo(3);
		assertThat(result.isbn()).isEqualTo("foo");

		// verify actually data in DB
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM books");
		assertThat(map).containsEntry("title", "title-updated")
			.containsEntry("author", "author-updated")
			.containsEntry("book_category_id", 3)
			.containsEntry("isbn", "foo");

		Statistics statistics = this.sessionFactory.getStatistics();
		EntityStatistics entityStatistics = statistics.getEntityStatistics(Book.class.getName());
		assertThat(entityStatistics.getUpdateCount()).isEqualTo(1);
	}

	@Test
	void deleteByUpdate() {
		createTestData();

		// delete=true
		BookDtoUpdate updateRequest = new BookDtoUpdate("title-updated", null, null, null, null, null, null, true,
				null);

		// for LocalDate.now(clock)
		Instant instant = Instant.parse("2022-12-01T00:00:00.00Z");
		this.clock.willReturn(instant);

		BookDto result = this.controller.update(1200000, updateRequest);
		this.entityManager.flush();

		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("title-updated");
		assertThat(result.deletedDate()).isEqualTo("2022-12-01");

		// verify actually data in DB
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM books");
		assertThat(map.get("date_deleted")).isInstanceOfSatisfying(Date.class, (date) -> {
			assertThat(date).isEqualTo("2022-12-01");
		});

		// it is a soft delete
		Statistics statistics = this.sessionFactory.getStatistics();
		EntityStatistics entityStatistics = statistics.getEntityStatistics(Book.class.getName());
		assertThat(entityStatistics.getUpdateCount()).isEqualTo(1);
		assertThat(entityStatistics.getDeleteCount()).isEqualTo(0);
	}

	@Test
	void missingByUpdate() {
		createTestData();

		// missing = true
		BookDtoUpdate updateRequest = new BookDtoUpdate("title-updated", null, null, null, null, null, null, null,
				true);

		// for LocalDate.now(clock)
		Instant instant = Instant.parse("2022-12-01T00:00:00.00Z");
		this.clock.willReturn(instant);

		BookDto result = this.controller.update(1200000, updateRequest);
		this.entityManager.flush();

		assertThat(result).isNotNull();
		assertThat(result.lostDate()).isEqualTo("2022-12-01");

		// verify actually data in DB
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM books");
		assertThat(map.get("date_lost")).isInstanceOfSatisfying(Date.class, (date) -> {
			assertThat(date).isEqualTo("2022-12-01");
		});
		assertThat(map).containsEntry("is_missing", true);

		// it is a soft delete
		Statistics statistics = this.sessionFactory.getStatistics();
		EntityStatistics entityStatistics = statistics.getEntityStatistics(Book.class.getName());
		assertThat(entityStatistics.getUpdateCount()).isEqualTo(1);
		assertThat(entityStatistics.getDeleteCount()).isEqualTo(0);
	}

	private void createTestData() {
		String sql = """
					INSERT INTO books (id, school_id, title, author, isbn, publisher, book_category_id, is_missing, date_lost)
					VALUES  (1200000, 'river', 'foo', 'foo', 'foo', 'foo', 3, false, null);
				""";
		this.jdbcTemplate.update(sql);
	}

	@Test
	void list() {
		String sql = """
					INSERT INTO books (id, school_id, title, author, isbn, publisher, book_category_id, is_missing, date_lost, num_checkouts)
					VALUES  (1, 'sky', 'foo', 'foo', 'foo', 'foo', 3, false, null, 10),
							(2, 'sky', 'bar', 'bar', 'bar', 'bar', 3, true,'2020-02-22', 20),
							(3, 'ocean', 'baz', 'baz', 'baz', 'baz', 10, true,'2020-02-22', 30)
					;
				""";
		this.jdbcTemplate.update(sql);

		Page<BookDto> page;

		page = this.controller.list("sky", null, Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent()).extracting(BookDto::id).containsExactlyInAnyOrder(1L, 2L);

		page = this.controller.list("sky", true, Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(BookDto::id).isEqualTo(2L);
	}

	@Test
	void get() {
		String sql = """
					INSERT INTO books (id, school_id, title, author, isbn, publisher, book_category_id, is_missing, date_lost)
					VALUES  (1, 'sky', 'foo', 'foo', 'foo', 'foo', 3, false,null),
							(2, 'sky', 'bar', 'bar', 'bar', 'bar', 3, true,'2020-02-22')
					;
				""";
		this.jdbcTemplate.update(sql);

		BookDto result;
		result = this.controller.get(2L);
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("bar");
		assertThat(result.lostDate()).isEqualTo("2020-02-22");
	}

}
