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

import net.ttddyy.book.libraryservice.MockClocks;
import net.ttddyy.book.libraryservice.book.category.BookCategory;
import net.ttddyy.book.libraryservice.book.category.BookCategoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author Tadaya Tsuyukubo
 */
class BookServiceTests {

	@Test
	@SuppressWarnings("unchecked")
	void searchWithEmptyValues() {
		BookRepository bookRepository = mock(BookRepository.class);
		BookCategoryRepository categoryRepository = mock(BookCategoryRepository.class);
		BookStatusHistoryRepository statusHistoryRepository = mock(BookStatusHistoryRepository.class);
		Clock clock = mock(Clock.class);
		BookService bookService = new BookService(bookRepository, categoryRepository, statusHistoryRepository, clock);

		Pageable pageable = Pageable.unpaged();
		bookService.search(null, null, " ", " ", " ", " ", " ", null, pageable);

		ArgumentCaptor<Example<Book>> captor = ArgumentCaptor.forClass(Example.class);
		verify(bookRepository).findAll(captor.capture(), any(Pageable.class));

		Example<Book> example = captor.getValue();
		Book probe = example.getProbe();

		assertThat(probe.getTitle()).isNull();
		assertThat(probe.getTitleKana()).isNull();
		assertThat(probe.getAuthor()).isNull();
		assertThat(probe.getAuthorKana()).isNull();
		assertThat(probe.getPublisher()).isNull();
	}

	@Test
	void updateStatus() {
		BookRepository bookRepository = mock(BookRepository.class);
		BookCategoryRepository categoryRepository = mock(BookCategoryRepository.class);
		BookStatusHistoryRepository statusHistoryRepository = mock(BookStatusHistoryRepository.class);
		Clock clock = mock(Clock.class);
		BookService bookService = new BookService(bookRepository, categoryRepository, statusHistoryRepository, clock);

		Book book = new Book();
		book.setId(1L);
		book.setStatus(BookStatus.AVAILABLE);
		given(bookRepository.getReferenceById(1L)).willReturn(book);
		bookService.updateStatus(1L, BookStatus.LOST);

		verify(bookRepository).save(assertArg(arg -> {
			assertThat(arg.getId()).isEqualTo(1L);
			assertThat(arg.getStatus()).isEqualTo(BookStatus.LOST);
		}));
		verify(statusHistoryRepository).save(assertArg(history -> {
			assertThat(history.getBookId()).isEqualTo(1L);
			assertThat(history.getOldStatus()).isEqualTo(BookStatus.AVAILABLE);
			assertThat(history.getNewStatus()).isEqualTo(BookStatus.LOST);
		}));
		verifyNoInteractions(categoryRepository);
	}

	@Test
	void updateStatusNoChange() {
		BookRepository bookRepository = mock(BookRepository.class);
		BookCategoryRepository categoryRepository = mock(BookCategoryRepository.class);
		BookStatusHistoryRepository statusHistoryRepository = mock(BookStatusHistoryRepository.class);
		Clock clock = mock(Clock.class);
		BookService bookService = new BookService(bookRepository, categoryRepository, statusHistoryRepository, clock);

		Book book = new Book();
		book.setStatus(BookStatus.AVAILABLE);
		given(bookRepository.getReferenceById(1L)).willReturn(book);
		Book result = bookService.updateStatus(1L, BookStatus.AVAILABLE);

		assertThat(result).isSameAs(book);
		verifyNoInteractions(categoryRepository);
		verifyNoInteractions(statusHistoryRepository);
	}

	@Test
	void create() {
		BookRepository bookRepository = mock(BookRepository.class);
		BookCategoryRepository categoryRepository = mock(BookCategoryRepository.class);
		BookStatusHistoryRepository statusHistoryRepository = mock(BookStatusHistoryRepository.class);
		MockClocks.MockClock clock = new MockClocks.MockClock();
		BookService bookService = new BookService(bookRepository, categoryRepository, statusHistoryRepository, clock);

		Instant instant = Instant.parse("2022-12-01T00:00:00.00Z");
		clock.willReturn(instant);

		BookCategory category = new BookCategory();
		category.setId(1L);
		category.setStartingId(10L);
		category.setEndingId(20L);
		given(categoryRepository.getReferenceById(1L)).willReturn(category);
		given(bookRepository.nextId(10L, 20L)).willReturn(11L);

		Book book = new Book();
		book.setCategory(category);
		Book result = bookService.create(book);

		verify(bookRepository).save(assertArg(arg -> {
			assertThat(arg.getId()).isEqualTo(11L);
			assertThat(arg.getCategory()).isSameAs(category);
			assertThat(arg.getStatus()).isEqualTo(BookStatus.AVAILABLE);
			assertThat(arg.getStatusChangedAt()).isEqualTo(instant);
			assertThat(arg.getAddedTime()).isEqualTo(instant);
		}));
	}

}
