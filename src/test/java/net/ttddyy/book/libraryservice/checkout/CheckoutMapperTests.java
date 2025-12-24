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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CheckoutMapper}.
 *
 * @author Tadaya Tsuyukubo
 */
class CheckoutMapperTests {

	@Test
	void map() {
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
		member.setFirstname("foo-firstname");
		member.setLastname("foo-lastname");
		member.setGrade(7);
		member.setClassNumber(2);

		CheckoutId id = new CheckoutId(bookId, memberId);
		Checkout checkout = new Checkout();
		checkout.setId(id);
		checkout.setBook(book);
		checkout.setMember(member);
		checkout.setCheckoutDate(LocalDate.parse("2020-02-02"));
		checkout.setDueDate(LocalDate.parse("2020-03-03"));

		List<CheckoutDto> result = CheckoutMapper.INSTANCE.toDtoList(List.of(checkout));
		assertThat(result).hasSize(1);
		CheckoutDto dto = result.get(0);
		assertThat(dto.bookId()).isEqualTo(bookId);
		assertThat(dto.memberId()).isEqualTo(memberId);
		assertThat(dto.memberFirstName()).isEqualTo("foo-firstname");
		assertThat(dto.memberLastName()).isEqualTo("foo-lastname");
		assertThat(dto.memberGrade()).isEqualTo(7);
		assertThat(dto.memberClassNumber()).isEqualTo(2);
		assertThat(dto.checkoutDate()).isEqualTo("2020-02-02");
		assertThat(dto.dueDate()).isEqualTo("2020-03-03");
		assertThat(dto.book()).satisfies(bookDto -> {
			assertThat(bookDto.title()).isEqualTo("foo-title");
			assertThat(bookDto.titleKana()).isEqualTo("foo-title-kana");
			assertThat(bookDto.author()).isEqualTo("foo-author");
			assertThat(bookDto.authorKana()).isEqualTo("foo-author-kana");
			assertThat(bookDto.isbn()).isEqualTo("foo-isbn");
			assertThat(bookDto.comments()).isEqualTo("foo-comments");
			assertThat(bookDto.publisher()).isEqualTo("foo-publisher");
		});
	}

}
