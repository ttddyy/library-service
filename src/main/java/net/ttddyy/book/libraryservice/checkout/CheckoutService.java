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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.ttddyy.book.libraryservice.book.Book;
import net.ttddyy.book.libraryservice.checkout.limit.CheckoutLimit;
import net.ttddyy.book.libraryservice.checkout.limit.CheckoutLimitService;
import net.ttddyy.book.libraryservice.member.Member;
import net.ttddyy.book.libraryservice.member.MemberRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tadaya Tsuyukubo
 */
@Service
@Transactional(readOnly = true)
public class CheckoutService {

	private static final int DEFAULT_DELETE_BATCH_SIZE = 100;

	private final Clock clock;

	private final CheckoutRepository checkoutRepository;

	private final MemberRepository memberRepository;

	private final CheckoutLimitService checkoutLimitService;

	public CheckoutService(Clock clock, CheckoutRepository checkoutRepository, MemberRepository memberRepository,
			CheckoutLimitService checkoutLimitService) {
		this.clock = clock;
		this.checkoutRepository = checkoutRepository;
		this.memberRepository = memberRepository;
		this.checkoutLimitService = checkoutLimitService;
	}

	@Transactional
	public void checkout(String schoolId, long memberId, Set<Long> bookIds, boolean force) {
		Member member = this.memberRepository.getReferenceById(memberId);
		CheckoutLimit checkoutLimit = this.checkoutLimitService.getEffectiveCheckoutLimit(schoolId, member.getGrade());
		int maxBooks = checkoutLimit.maxBooks();
		int maxDays = checkoutLimit.maxDays();
		LocalDate dueDate = LocalDate.now().plusDays(maxDays);

		// return them first
		bulkReturn(bookIds);

		if (!force) {
			// perform validation. If it gets more complicated, move to a dedicated class.
			long current = this.checkoutRepository.countByIdMemberId(memberId);
			long total = current + bookIds.size();
			if (maxBooks < total) {
				String msg = String.format("Exceeded the maximum books. [max=%d, current=%d]", maxBooks, total);
				throw new RuntimeException(msg);
			}
		}

		LocalDate today = LocalDate.now(this.clock);
		List<Checkout> checkouts = new ArrayList<>();
		for (Long bookId : bookIds) {
			CheckoutId id = new CheckoutId(memberId, bookId);
			Checkout checkout = new Checkout();
			checkout.setId(id);
			checkout.setDueDate(dueDate);
			checkout.setCheckoutDate(today);
			// due to the current mapping, which maps member/book with entity even though
			// they are mapped as Long in id class, it needs to assign objects to the
			// associated entity objects.
			checkout.setMember(member);
			Book book = new Book();
			book.setId(bookId);
			book.setVersion(0);
			checkout.setBook(book);

			checkouts.add(checkout);
		}
		this.checkoutRepository.saveAll(checkouts);
		// TODO: consider the feature
		// this.activityRepository.saveAll(activities);
	}

	@Transactional
	public void bulkReturn(Set<Long> bookIds) {
		Set<Long> batch = new HashSet<>();
		int i = 1;
		for (Long bookId : bookIds) {
			batch.add(bookId);
			if (i++ % DEFAULT_DELETE_BATCH_SIZE == 0) {
				this.checkoutRepository.deleteAllByIdBookIdIn(batch);
				batch.clear();
			}
		}
		this.checkoutRepository.deleteAllByIdBookIdIn(batch);
	}

	Page<Checkout> list(@Nullable Long memberId, @Nullable String schoolId, Pageable pageable) {
		// Do not use query by example since it cannot provide fetch strategy.
		// The following queries always join fetch the books and members
		if (memberId != null) {
			return this.checkoutRepository.findAllByMemberId(memberId, pageable);
		}
		else if (schoolId != null) {
			return this.checkoutRepository.findAllBySchoolId(schoolId, pageable);
		}
		return this.checkoutRepository.findAll(pageable);
	}

	@Transactional
	public Page<Checkout> overdue(Pageable pageable) {
		LocalDate today = LocalDate.now(this.clock);
		return this.checkoutRepository.findOverdue(today, pageable);
	}

}
