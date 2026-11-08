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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CheckoutHistoryMapper}.
 *
 * @author Tadaya Tsuyukubo
 */
class CheckoutHistoryMapperTests {

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
		member.setFirstname("bar-firstname");
		member.setLastname("bar-lastname");
		member.setFirstnameKana("bar-firstname-kana");
		member.setLastnameKana("bar-lastname-kana");
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

		List<CheckoutHistoryDto> result = CheckoutHistoryMapper.INSTANCE.toDtoList(List.of(history));
		assertThat(result).hasSize(1);
		CheckoutHistoryDto dto = result.get(0);
		assertThat(dto.bookId()).isEqualTo(bookId);
		assertThat(dto.memberId()).isEqualTo(memberId);
		assertThat(dto.checkoutDate()).isEqualTo("2020-02-02");
		assertThat(dto.operation()).isEqualTo(CheckoutHistoryOperation.INSERT);
		assertThat(dto.createdAt()).isEqualTo("2020-02-02T02:02:02.02Z");
		assertThat(dto.updatedAt()).isEqualTo("2020-03-03T03:03:03.03Z");
		assertThat(dto.changedAt()).isEqualTo("2020-04-04T04:04:04.04Z");
		assertThat(dto.book()).satisfies((bookDto) -> {
			assertThat(bookDto.title()).isEqualTo("foo-title");
			assertThat(bookDto.titleKana()).isEqualTo("foo-title-kana");
			assertThat(bookDto.author()).isEqualTo("foo-author");
			assertThat(bookDto.authorKana()).isEqualTo("foo-author-kana");
			assertThat(bookDto.isbn()).isEqualTo("foo-isbn");
			assertThat(bookDto.comments()).isEqualTo("foo-comments");
			assertThat(bookDto.publisher()).isEqualTo("foo-publisher");
		});
		assertThat(dto.member()).satisfies((memberDto) -> {
			assertThat(memberDto.firstname()).isEqualTo("bar-firstname");
			assertThat(memberDto.lastname()).isEqualTo("bar-lastname");
			assertThat(memberDto.firstnameKana()).isEqualTo("bar-firstname-kana");
			assertThat(memberDto.lastnameKana()).isEqualTo("bar-lastname-kana");
		});
	}

}
