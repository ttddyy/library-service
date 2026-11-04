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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * @author Tadaya Tsuyukubo
 */
@RestController
@Tag(name = "Books")
class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@GetMapping("/api/books")
	@Operation(summary = "List books", description = "Optionally filtered by school and status.")
	Page<BookDto> list(@RequestParam(required = false) @Nullable String schoolId,
			@RequestParam(required = false) @Nullable BookStatus status, @ParameterObject Pageable pageable) {
		Page<Book> page = this.bookService.list(schoolId, status, pageable);
		List<BookDto> books = BookMapper.INSTANCE.toDtoList(page.getContent());
		return new PageImpl<>(books, page.getPageable(), page.getTotalElements());
	}

	@GetMapping("/api/books/registered")
	@Operation(summary = "List books registered in a date range", description = "'to' defaults to today when omitted.")
	Page<BookDto> listRegisteredBooks(@RequestParam(required = false) @Nullable String schoolId,
			@Parameter(description = "UTC date") @RequestParam LocalDate from,
			@Parameter(description = "UTC date") @RequestParam(required = false) @Nullable LocalDate to,
			@ParameterObject Pageable pageable) {
		if (to == null) {
			to = LocalDate.now();
		}
		Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant toInstant = to.atStartOfDay(ZoneOffset.UTC).toInstant();
		Page<Book> page = this.bookService.listByAddedTime(schoolId, fromInstant, toInstant, pageable);
		List<BookDto> books = BookMapper.INSTANCE.toDtoList(page.getContent());
		return new PageImpl<>(books, page.getPageable(), page.getTotalElements());
	}

	@GetMapping("/api/books/{id}")
	@Operation(summary = "Get a book")
	BookDto get(@PathVariable long id) {
		// TODO: when not exist
		Book book = this.bookService.get(id);
		BookDto bookDto = BookMapper.INSTANCE.toDto(book);
		return bookDto;
	}

	@PostMapping("/api/books")
	@Operation(summary = "Create books", description = "Bulk create. Requires SCHOOL_ADMIN role.")
	List<BookDto> create(@RequestBody List<BookDtoCreate> input) {
		// TODO: validation for required fields
		List<Book> toCreate = BookMapper.INSTANCE.toBookList(input);
		List<Book> books = this.bookService.create(toCreate);
		List<BookDto> response = BookMapper.INSTANCE.toDtoList(books);
		return response;
	}

	@PutMapping("/api/books/{id}")
	@Operation(summary = "Update a book", description = "Requires SCHOOL_ADMIN role.")
	BookDto update(@PathVariable long id, @RequestBody BookDtoUpdate dto) {
		// TODO: validation
		Book book = this.bookService.update(id, dto);
		return BookMapper.INSTANCE.toDto(book);
	}

	@Operation(summary = "Update book status")
	@PutMapping("/api/books/{id}/status")
	BookDto updateStatus(@PathVariable long id, @RequestBody BookStatusDtoUpdate request) {
		Book book = this.bookService.updateStatus(id, request.status());
		return BookMapper.INSTANCE.toDto(book);
	}

	// Currently, not supporting hard delete.
	// @DeleteMapping("/api/books/{id}")
	// public BookDto remove(@PathVariable long id) {
	// Book deleted = this.bookService.remove(id);
	// return BookMapper.INSTANCE.bookToBookDto(deleted);
	// }

	@GetMapping("/api/books/search")
	@Operation(summary = "Search books",
			description = "When multiple parameters are provided, they work as AND conditions.")
	Page<BookDto> search(
	// @formatter:off
			@Nullable @RequestParam String schoolId,
			@Nullable @RequestParam String isbn,
			@Nullable @RequestParam String title,
			@Nullable @RequestParam String titleKana,
			@Nullable @RequestParam String author,
			@Nullable @RequestParam String authorKana,
			@Nullable @RequestParam String publisher,
			@Nullable @RequestParam Long categoryId,
			@ParameterObject Pageable pageable
	// @formatter:on
	) {
		Page<Book> page = this.bookService.search(schoolId, isbn, title, titleKana, author, authorKana, publisher,
				categoryId, pageable);
		return BookMapper.INSTANCE.toDtoPage(page);
	}

}
