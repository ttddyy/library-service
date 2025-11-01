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
import java.time.Instant;
import java.time.ZoneOffset;

import net.ttddyy.book.libraryservice.checkout.limit.defaults.CheckoutLimitDefault;
import net.ttddyy.book.libraryservice.checkout.limit.defaults.CheckoutLimitDefaultRepository;
import net.ttddyy.book.libraryservice.checkout.limit.schedule.CheckoutLimitSchedule;
import net.ttddyy.book.libraryservice.checkout.limit.schedule.CheckoutLimitScheduleRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link CheckoutLimitService}.
 *
 * @author Tadaya Tsuyukubo
 */
class CheckoutLimitServiceTests {

	@Test
	void getCheckoutLimit() {
		Clock clock = Clock.fixed(Instant.parse("2022-12-01T00:00:00.00Z"), ZoneOffset.UTC);
		CheckoutLimitDefaultRepository defaultRepository = mock(CheckoutLimitDefaultRepository.class);
		CheckoutLimitScheduleRepository scheduleRepository = mock(CheckoutLimitScheduleRepository.class);
		CheckoutLimitService checkoutLimitService = new CheckoutLimitService(clock, defaultRepository,
				scheduleRepository, 2, 15);

		CheckoutLimit result;

		// resolved by system default
		given(scheduleRepository.findByScheduleDateAndSchoolIdAndGrade(any(), anyString(), anyInt())).willReturn(null);
		given(defaultRepository.findBySchoolIdAndGrade(any(), anyInt())).willReturn(null);
		result = checkoutLimitService.getCheckoutLimit("my-school", 0);
		assertThat(result).isNotNull();
		assertThat(result.getMaxBooks()).isEqualTo(2);
		assertThat(result.getMaxDays()).isEqualTo(15);
		verify(scheduleRepository).findByScheduleDateAndSchoolIdAndGrade(any(), anyString(), anyInt());
		verify(defaultRepository).findBySchoolIdAndGrade(any(), anyInt());

		reset(defaultRepository, scheduleRepository);

		// resolved by default
		CheckoutLimitDefault defaultEntity = new CheckoutLimitDefault();
		defaultEntity.setMaxBooks(10);
		defaultEntity.setMaxDays(20);
		given(scheduleRepository.findByScheduleDateAndSchoolIdAndGrade(any(), anyString(), anyInt())).willReturn(null);
		given(defaultRepository.findBySchoolIdAndGrade(any(), anyInt())).willReturn(defaultEntity);

		result = checkoutLimitService.getCheckoutLimit("my-school", 0);

		assertThat(result).isNotNull();
		assertThat(result.getMaxBooks()).isEqualTo(10);
		assertThat(result.getMaxDays()).isEqualTo(20);
		verify(scheduleRepository).findByScheduleDateAndSchoolIdAndGrade(any(), anyString(), anyInt());
		verify(defaultRepository).findBySchoolIdAndGrade(any(), anyInt());

		reset(defaultRepository, scheduleRepository);

		// resolved by scheduled
		CheckoutLimitSchedule scheduledEntity = new CheckoutLimitSchedule();
		scheduledEntity.setMaxBooks(100);
		scheduledEntity.setMaxDays(200);
		given(scheduleRepository.findByScheduleDateAndSchoolIdAndGrade(any(), anyString(), anyInt()))
			.willReturn(scheduledEntity);

		result = checkoutLimitService.getCheckoutLimit("my-school", 0);

		assertThat(result).isNotNull();
		assertThat(result.getMaxBooks()).isEqualTo(100);
		assertThat(result.getMaxDays()).isEqualTo(200);
		verify(scheduleRepository).findByScheduleDateAndSchoolIdAndGrade(any(), anyString(), anyInt());
		verifyNoInteractions(defaultRepository);
	}

}
