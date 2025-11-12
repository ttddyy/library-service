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
import net.ttddyy.book.libraryservice.MockClocks.System;
import net.ttddyy.book.libraryservice.book.category.BookCategory;
import net.ttddyy.book.libraryservice.book.category.BookCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ BookService.class, BookCategoryService.class, System.class })
@DbTest
class BookServiceDBTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	BookService bookService;

	@Test
	void search() {
		String sql = """
					INSERT INTO books (id, school_id, title, title_kana, author, author_kana, isbn, book_category_id, publisher)
					VALUES  (10, 'ocean', 'title foo', 'タイトル フー', 'author foo', '著者 フー', 'isbn-123', 2, 'パブリッシャー'),
							(20, 'ocean', 'title bar', 'タイトル バー', 'author bar', '著者 バー', 'isbn-456', 2, 'パブリッシャー');
				""";
		this.jdbcTemplate.update(sql);

		Page<Book> page;

		// All empty
		page = this.bookService.search(null, null, null, null, null, null, null, null, Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(2);

		// school id
		page = this.bookService.search("ocean", null, null, null, null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(2);

		// isbn (exact match)
		page = this.bookService.search(null, "isbn-123", null, null, null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Book::getId).isEqualTo(10L);
		page = this.bookService.search(null, "isbn", null, null, null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(0);

		// title
		page = this.bookService.search(null, null, "foo", null, null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Book::getId).isEqualTo(10L);

		// titleKana
		page = this.bookService.search(null, null, null, "フー", null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Book::getId).isEqualTo(10L);

		// author
		page = this.bookService.search(null, null, null, null, "foo", null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Book::getId).isEqualTo(10L);

		// authorKana
		page = this.bookService.search(null, null, null, null, null, "フー", null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Book::getId).isEqualTo(10L);

		// publisher
		page = this.bookService.search(null, null, null, null, null, null, "パブ", null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(2);

		// categoryId
		page = this.bookService.search(null, null, null, null, null, null, null, 2L, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(2);

		// Multiple params as AND conditions
		page = this.bookService.search("ocean", null, "title", null, null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(2);
		page = this.bookService.search("ocean", null, "foo", null, null, null, null, null, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Book::getId).isEqualTo(10L);
		page = this.bookService.search("ocean", null, "no-match", null, null, null, null, 2L, Pageable.unpaged());
		assertThat(page.getTotalElements()).isEqualTo(0);

	}

	@Test
	void bulkCreate() {
		BookCategory category2 = new BookCategory();
		category2.setId(2L);
		BookCategory category3 = new BookCategory();
		category3.setId(3L);

		Book bookA = new Book();
		bookA.setCategory(category2);
		bookA.setSchoolId("ocean");
		bookA.setTitle("titleA");
		bookA.setAuthor("authorA");
		bookA.setIsbn("isbnA");
		bookA.setPublisher("publisherA");

		Book bookB = new Book();
		bookB.setCategory(category2);
		bookB.setSchoolId("ocean");
		bookB.setTitle("titleB");
		bookB.setAuthor("authorB");
		bookB.setIsbn("isbnB");
		bookB.setPublisher("publisherB");

		Book bookC = new Book();
		bookC.setCategory(category3);
		bookC.setSchoolId("ocean");
		bookC.setTitle("titleC");
		bookC.setAuthor("authorC");
		bookC.setIsbn("isbnC");
		bookC.setPublisher("publisherC");

		List<Book> result = this.bookService.create(List.of(bookA, bookB, bookC));
		assertThat(result).hasSize(3);
		// verify proper ids are generated
		assertThat(result).element(0).satisfies(book -> {
			assertThat(book.getTitle()).isEqualTo("titleA");
			assertThat(book.getId()).isEqualTo(1100000L);
		});
		assertThat(result).element(1).satisfies(book -> {
			assertThat(book.getTitle()).isEqualTo("titleB");
			assertThat(book.getId()).isEqualTo(1100001L);
		});
		assertThat(result).element(2).satisfies(book -> {
			assertThat(book.getTitle()).isEqualTo("titleC");
			assertThat(book.getId()).isEqualTo(1200000L);
		});
	}

}
