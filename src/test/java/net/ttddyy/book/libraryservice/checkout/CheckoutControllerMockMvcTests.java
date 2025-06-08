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

import net.ttddyy.book.libraryservice.book.Book;
import net.ttddyy.book.libraryservice.member.Member;
import net.ttddyy.book.libraryservice.school.SchoolController;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link SchoolController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@WebMvcTest(CheckoutController.class)
class CheckoutControllerMockMvcTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	WebTestClient webTestClient;

	@MockBean
	CheckoutService checkoutService;

	@Test
	void list() throws Exception {
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

		CheckoutId id = new CheckoutId(bookId, memberId);
		Checkout checkout = new Checkout();
		checkout.setId(id);
		checkout.setBook(book);
		checkout.setMember(member);
		checkout.setCheckoutDate(LocalDate.parse("2020-02-02"));
		checkout.setDueDate(LocalDate.parse("2020-03-03"));

		Page<Checkout> page = new PageImpl<>(List.of(checkout));
		given(this.checkoutService.overdue(any())).willReturn(page);

		this.webTestClient.get()
			.uri("/api/checkouts/overdue")
			.headers((header) -> header.setBasicAuth("user", "pass"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.APPLICATION_JSON)
			.expectBodyList(CheckoutDto.class)
			.consumeWith(result -> {
				List<CheckoutDto> list = result.getResponseBody();
				assertThat(list).hasSize(1);
			});
	}

	@Test
	void overdue() {
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

		CheckoutId id = new CheckoutId(bookId, memberId);
		Checkout checkout = new Checkout();
		checkout.setId(id);
		checkout.setBook(book);
		checkout.setMember(member);
		checkout.setCheckoutDate(LocalDate.parse("2020-02-02"));
		checkout.setDueDate(LocalDate.parse("2020-03-03"));

		// provide Pageable
		// https://github.com/spring-cloud/spring-cloud-openfeign/issues/1018#issuecomment-2053878807
		given(this.checkoutService.overdue(any())).willReturn(new PageImpl<>(List.of(checkout), Pageable.ofSize(1), 1));

		this.webTestClient.get()
			.uri("/api/checkouts/overdue")
			.headers((header) -> header.setBasicAuth("user", "pass"))
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.APPLICATION_JSON)
			.expectBody()
			.jsonPath("page.totalElements")
			.isEqualTo(1)
			.jsonPath("content[0].bookId")
			.isEqualTo(bookId)
			.jsonPath("content[0].memberId")
			.isEqualTo(memberId);
	}

}
