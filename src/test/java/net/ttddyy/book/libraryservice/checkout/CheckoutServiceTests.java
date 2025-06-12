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

import java.time.Clock;
import java.util.Set;

import net.ttddyy.book.libraryservice.checkout.limit.CheckoutLimit;
import net.ttddyy.book.libraryservice.checkout.limit.CheckoutLimitService;
import net.ttddyy.book.libraryservice.member.Member;
import net.ttddyy.book.libraryservice.member.MemberRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Tadaya Tsuyukubo
 */
class CheckoutServiceTests {

	@Test
	@SuppressWarnings("unchecked")
	void bulkReturn() {
		Clock clock = mock(Clock.class);
		CheckoutRepository checkoutRepository = mock(CheckoutRepository.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		CheckoutLimitService checkoutLimitService = mock(CheckoutLimitService.class);
		CheckoutService checkoutService = new CheckoutService(clock, checkoutRepository, memberRepository,
				checkoutLimitService);

		Set<Long> bookIds = Set.of(1L, 2L, 3L);
		checkoutService.bulkReturn(bookIds);

		verify(checkoutRepository).deleteAllByIdBookIdIn(bookIds);
	}

	@Test
	void checkoutValidation() {
		Clock clock = mock(Clock.class);
		CheckoutRepository checkoutRepository = mock(CheckoutRepository.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		CheckoutLimitService checkoutLimitService = mock(CheckoutLimitService.class);

		CheckoutLimit limit = new CheckoutLimit(6, 3);

		Member member = new Member();
		member.setGrade(1);

		long memberId = 1;
		String schoolId = "my-school";
		// mock as if the member has currently 5 books checkouts
		given(checkoutRepository.countByIdMemberId(memberId)).willReturn(5L);
		given(memberRepository.getReferenceById(memberId)).willReturn(member);
		given(checkoutLimitService.getCheckoutLimit(schoolId, member.getGrade())).willReturn(limit);

		CheckoutService checkoutService = new CheckoutService(clock, checkoutRepository, memberRepository,
				checkoutLimitService);

		// it exceeds the max 6 books. 5+3
		Set<Long> bookIds = Set.of(1L, 2L, 3L);
		assertThatThrownBy(() -> checkoutService.checkout("school", memberId, bookIds, false))
			.isInstanceOf(RuntimeException.class);
	}

}
