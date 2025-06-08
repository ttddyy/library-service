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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import net.ttddyy.book.libraryservice.book.Book;
import net.ttddyy.book.libraryservice.member.Member;
import net.ttddyy.book.libraryservice.school.SchoolController;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SchoolController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@WebMvcTest(CheckoutHistoryController.class)
class CheckoutHistoryControllerMockMvcTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	WebTestClient webTestClient;

	@MockitoBean
	CheckoutHistoryService checkoutHistoryService;

	@Test
	void get() {
		CheckoutHistory history = createCheckoutHistory();
		Book book = history.getBook();
		Member member = history.getMember();

		given(this.checkoutHistoryService.get(10, 20)).willReturn(history);

		this.webTestClient.get()
			.uri("/api/checkouts/history?bookId=10&memberId=20")
			.headers((header) -> header.setBasicAuth("user", "pass"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.APPLICATION_JSON)
			.expectBody()
			// @formatter:off
				.jsonPath("bookId").isEqualTo(history.getBook().getId())
				.jsonPath("memberId").isEqualTo(history.getMember().getId())
				.jsonPath("checkoutDate").isEqualTo(history.getCheckoutDate().toString())
				.jsonPath("dueDate").isEqualTo(history.getDueDate().toString())
				.jsonPath("createdAt").isEqualTo(history.getCreatedAt().toString())
				.jsonPath("updatedAt").isEqualTo(history.getUpdatedAt().toString())
				.jsonPath("changedAt").isEqualTo(history.getChangedAt().toString())
				.jsonPath("operation").isEqualTo(history.getOperation().toString())
				.jsonPath("book.title").isEqualTo(book.getTitle())
				.jsonPath("book.titleKana").isEqualTo(book.getTitleKana())
				.jsonPath("book.author").isEqualTo(book.getAuthor())
				.jsonPath("book.authorKana").isEqualTo(book.getAuthorKana())
				.jsonPath("book.isbn").isEqualTo(book.getIsbn())
				.jsonPath("book.comments").isEqualTo(book.getComments())
				.jsonPath("book.publisher").isEqualTo(book.getPublisher())
				.jsonPath("member.firstname").isEqualTo(member.getFirstname())
				.jsonPath("member.lastname").isEqualTo(member.getLastname())
		// @formatter:on
		;
		verify(this.checkoutHistoryService).get(10, 20);
	}

	@Test
	void byBookId() {
		CheckoutHistory history = createCheckoutHistory();

		Page<CheckoutHistory> page = new PageImpl<>(List.of(history));
		given(this.checkoutHistoryService.listByBook(eq(10L), any(Pageable.class))).willReturn(page);

		ResponseSpec spec = this.webTestClient.get()
			.uri("/api/checkouts/history?bookId=10")
			.headers((header) -> header.setBasicAuth("user", "pass"))
			.exchange();

		// verify
		spec.expectStatus().isOk();
		spec.expectHeader().contentType(MediaType.APPLICATION_JSON);
		verifyResponseJson(spec.expectBody(), history);

		verify(this.checkoutHistoryService).listByBook(eq(10L), any(Pageable.class));
	}

	@Test
	void byMemberId() {
		CheckoutHistory history = createCheckoutHistory();

		Page<CheckoutHistory> page = new PageImpl<>(List.of(history));
		given(this.checkoutHistoryService.listByMember(eq(20L), any(Pageable.class))).willReturn(page);

		ResponseSpec spec = this.webTestClient.get()
			.uri("/api/checkouts/history?memberId=20")
			.headers((header) -> header.setBasicAuth("user", "pass"))
			.exchange();

		// verify
		spec.expectStatus().isOk();
		spec.expectHeader().contentType(MediaType.APPLICATION_JSON);
		verifyResponseJson(spec.expectBody(), history);

		verify(this.checkoutHistoryService).listByMember(eq(20L), any(Pageable.class));
	}

	private CheckoutHistory createCheckoutHistory() {
		long bookId = 30;
		long memberId = 20;

		Book book = new Book();
		book.setId(bookId);
		book.setTitle("foo-title");
		book.setTitleKana("foo-title-kana");
		book.setAuthor("foo-author");
		book.setAuthorKana("foo-author-kana");
		book.setIsbn("foo-isbn");
		book.setComments("foo-comments");
		book.setPublisher("foo-publisher");
		book.setSchoolId("foo-school");

		Member member = new Member();
		member.setId(memberId);

		CheckoutHistoryId id = new CheckoutHistoryId(bookId, memberId);
		CheckoutHistory history = new CheckoutHistory();
		history.setId(id);
		history.setBook(book);
		history.setMember(member);
		history.setCheckoutDate(LocalDate.parse("2020-02-02"));
		history.setDueDate(LocalDate.parse("2020-03-03"));
		history.setCreatedAt(Instant.parse("2020-02-02T02:02:02.02Z"));
		history.setUpdatedAt(Instant.parse("2020-03-03T03:03:03.03Z"));
		history.setChangedAt(Instant.parse("2020-04-04T04:04:04.04Z"));
		history.setOperation(CheckoutHistoryOperation.INSERT);

		return history;
	}

	private void verifyResponseJson(BodyContentSpec spec, CheckoutHistory history) {
		Book book = history.getBook();
		Member member = history.getMember();
		spec
		// @formatter:off
				.jsonPath("content[0].bookId").isEqualTo(history.getBook().getId())
				.jsonPath("content[0].memberId").isEqualTo(history.getMember().getId())
				.jsonPath("content[0].checkoutDate").isEqualTo(history.getCheckoutDate().toString())
				.jsonPath("content[0].dueDate").isEqualTo(history.getDueDate().toString())
				.jsonPath("content[0].createdAt").isEqualTo(history.getCreatedAt().toString())
				.jsonPath("content[0].updatedAt").isEqualTo(history.getUpdatedAt().toString())
				.jsonPath("content[0].changedAt").isEqualTo(history.getChangedAt().toString())
				.jsonPath("content[0].operation").isEqualTo(history.getOperation().toString())
				.jsonPath("content[0].book.title").isEqualTo(book.getTitle())
				.jsonPath("content[0].book.titleKana").isEqualTo(book.getTitleKana())
				.jsonPath("content[0].book.author").isEqualTo(book.getAuthor())
				.jsonPath("content[0].book.authorKana").isEqualTo(book.getAuthorKana())
				.jsonPath("content[0].book.isbn").isEqualTo(book.getIsbn())
				.jsonPath("content[0].book.comments").isEqualTo(book.getComments())
				.jsonPath("content[0].book.publisher").isEqualTo(book.getPublisher())
				.jsonPath("content[0].member.firstname").isEqualTo(member.getFirstname())
				.jsonPath("content[0].member.lastname").isEqualTo(member.getLastname())
		// @formatter:on
		;
	}

}
