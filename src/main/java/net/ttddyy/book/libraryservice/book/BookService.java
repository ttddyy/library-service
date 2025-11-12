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

import net.ttddyy.book.libraryservice.book.category.BookCategory;
import net.ttddyy.book.libraryservice.book.category.BookCategoryRepository;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.ignoreCase;

/**
 * @author Tadaya Tsuyukubo
 */
@Service
@Transactional(readOnly = true)
public class BookService {

	private final BookRepository bookRepository;

	private final BookCategoryRepository categoryRepository;

	private final Clock clock;

	public BookService(BookRepository bookRepository, BookCategoryRepository categoryRepository, Clock clock) {
		this.bookRepository = bookRepository;
		this.categoryRepository = categoryRepository;
		this.clock = clock;
	}

	public Page<Book> list(@Nullable String schoolId, @Nullable Boolean isMissing, Pageable pageable) {
		Book book = new Book();
		book.setSchoolId(schoolId);
		book.setMissing(isMissing);
		Example<Book> example = Example.of(book);
		return this.bookRepository.findAll(example, pageable);
	}

	public Page<Book> search(@Nullable String schoolId, @Nullable String isbn, @Nullable String title,
			@Nullable String titleKana, @Nullable String author, @Nullable String authorKana,
			@Nullable String publisher, @Nullable Long categoryId, Pageable pageable) {
		Book book = new Book();
		if (StringUtils.hasText(schoolId)) {
			book.setSchoolId(schoolId);
		}
		if (StringUtils.hasText(isbn)) {
			book.setIsbn(isbn);
		}
		if (StringUtils.hasText(title)) {
			book.setTitle(title);
		}
		if (StringUtils.hasText(titleKana)) {
			book.setTitleKana(titleKana);
		}
		if (StringUtils.hasText(author)) {
			book.setAuthor(author);
		}
		if (StringUtils.hasText(authorKana)) {
			book.setAuthorKana(authorKana);
		}
		if (StringUtils.hasText(publisher)) {
			book.setPublisher(publisher);
		}
		if (categoryId != null) {
			BookCategory category = new BookCategory();
			category.setId(categoryId);
			book.setCategory(category);
		}
		ExampleMatcher matcher = ExampleMatcher.matching()
			.withMatcher("title", ignoreCase().contains())
			.withMatcher("titleKana", ignoreCase().contains())
			.withMatcher("author", ignoreCase().contains())
			.withMatcher("authorKana", ignoreCase().contains())
			.withMatcher("publisher", ignoreCase().contains());
		Example<Book> example = Example.of(book, matcher);
		return this.bookRepository.findAll(example, pageable);
	}

	public Book get(long id) {
		Book book = this.bookRepository.getReferenceById(id);
		Hibernate.initialize(book);
		return book;
	}

	@Transactional(readOnly = false)
	public Book create(Book book) {
		// resolve next id based on the id range specified in the category
		long categoryId = book.getCategory().getId();
		BookCategory category = this.categoryRepository.getReferenceById(categoryId);
		Long nextId = this.bookRepository.nextId(category.getStartingId(), category.getEndingId());
		book.setId(nextId);
		book.setCategory(category);
		book.setMissing(false);
		return this.bookRepository.save(book);
	}

	@Transactional(readOnly = false)
	public List<Book> create(List<Book> books) {
		List<Book> created = new ArrayList<>();
		for (Book book : books) {
			created.add(create(book));
		}
		return created;
	}

	@Transactional
	public Book update(long id, BookDtoUpdate dto) {
		Book book = this.bookRepository.getReferenceById(id);
		BookMapper.INSTANCE.updateBookFromDto(book, dto, this.clock);
		return this.bookRepository.save(book);
	}

	@Transactional
	public Book remove(long id) {
		Book book = this.bookRepository.getReferenceById(id);
		this.bookRepository.delete(book);
		return book;
	}

}
