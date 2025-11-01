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

package net.ttddyy.book.libraryservice.checkout.limit;

import java.time.Clock;
import java.time.LocalDate;

import net.ttddyy.book.libraryservice.checkout.limit.defaults.CheckoutLimitDefault;
import net.ttddyy.book.libraryservice.checkout.limit.defaults.CheckoutLimitDefaultRepository;
import net.ttddyy.book.libraryservice.checkout.limit.schedule.CheckoutLimitSchedule;
import net.ttddyy.book.libraryservice.checkout.limit.schedule.CheckoutLimitScheduleRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tadaya Tsuyukubo
 */
@Service
@Transactional(readOnly = true)
public class CheckoutLimitService {

	private final Clock clock;

	private final CheckoutLimitDefaultRepository limitDefaultRepository;

	private final CheckoutLimitScheduleRepository limitScheduleRepository;

	private final int maxBooks;

	private final int maxDays;

	public CheckoutLimitService(Clock clock, CheckoutLimitDefaultRepository limitDefaultRepository,
			CheckoutLimitScheduleRepository limitScheduleRepository,
			@Value("${library-service.checkout.max-books}") int maxBooks,
			@Value("${library-service.checkout.max-days}") int maxDays) {
		this.clock = clock;
		this.limitDefaultRepository = limitDefaultRepository;
		this.limitScheduleRepository = limitScheduleRepository;
		this.maxBooks = maxBooks;
		this.maxDays = maxDays;
	}

	public CheckoutLimit getCheckoutLimit(String schoolId, int grade) {
		// check scheduled limit
		LocalDate today = LocalDate.now(this.clock);
		CheckoutLimitSchedule schedule = this.limitScheduleRepository.findByScheduleDateAndSchoolIdAndGrade(today,
				schoolId, grade);
		if (schedule != null) {
			return new CheckoutLimit(schedule.getMaxBooks(), schedule.getMaxDays());
		}
		// check school default
		CheckoutLimitDefault schoolDefault = this.limitDefaultRepository.findBySchoolIdAndGrade(schoolId, grade);
		if (schoolDefault != null) {
			return new CheckoutLimit(schoolDefault.getMaxBooks(), schoolDefault.getMaxDays());
		}
		// fallback to the system default
		return new CheckoutLimit(this.maxBooks, this.maxDays);
	}

}
