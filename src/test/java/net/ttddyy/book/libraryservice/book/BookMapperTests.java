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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
class BookMapperTests {

	@Test
	void updateBookFromDto() {
		Book book = new Book();

		BookDtoUpdate dto = new BookDtoUpdate("bar-title", "bar-title-kana", "bar-author", "bar-author-kana",
				"bar-isbn", "bar-comments", "bar-publisher");

		BookMapper.INSTANCE.updateBookFromDto(book, dto);
		assertThat(book.getTitle()).isEqualTo("bar-title");
		assertThat(book.getTitleKana()).isEqualTo("bar-title-kana");
		assertThat(book.getAuthor()).isEqualTo("bar-author");
		assertThat(book.getAuthorKana()).isEqualTo("bar-author-kana");
		assertThat(book.getIsbn()).isEqualTo("bar-isbn");
		assertThat(book.getComments()).isEqualTo("bar-comments");
		assertThat(book.getPublisher()).isEqualTo("bar-publisher");
	}

	@Test
	void updateBookFromDtoWithNull() {
		Book book = new Book();
		book.setTitle("foo-title");
		book.setTitleKana("foo-title-kana");
		book.setAuthor("foo-author");
		book.setAuthorKana("foo-author-kana");
		book.setIsbn("foo-isbn");
		book.setComments("foo-comments");
		book.setPublisher("foo-publisher");

		BookDtoUpdate dto = new BookDtoUpdate(null, null, null, null, null, null, null);

		BookMapper.INSTANCE.updateBookFromDto(book, dto);
		assertThat(book.getTitle()).isEqualTo("foo-title");
		assertThat(book.getTitleKana()).isEqualTo("foo-title-kana");
		assertThat(book.getAuthor()).isEqualTo("foo-author");
		assertThat(book.getAuthorKana()).isEqualTo("foo-author-kana");
		assertThat(book.getIsbn()).isEqualTo("foo-isbn");
		assertThat(book.getComments()).isEqualTo("foo-comments");
		assertThat(book.getPublisher()).isEqualTo("foo-publisher");

	}

	@Test
	void updateBookFromDtoWithStatus() {
		Book book = new Book();
		book.setStatus(BookStatus.LOST);
		// delete=true
		BookDtoUpdate dto = new BookDtoUpdate("bar-title", null, null, null, null, null, null);

		BookMapper.INSTANCE.updateBookFromDto(book, dto);

		assertThat(book.getStatus()).as("status should not be updated").isEqualTo(BookStatus.LOST);
	}

}
