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

package net.ttddyy.book.libraryservice.checkout.limit.schedule;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
class CheckoutLimitScheduleMapperTests {

	@Test
	void toEntityList() {
		CheckoutLimitScheduleDtoCreate dto1 = new CheckoutLimitScheduleDtoCreate("foo-school",
				LocalDate.parse("2020-01-01"), 1, 2, 3);
		CheckoutLimitScheduleDtoCreate dto2 = new CheckoutLimitScheduleDtoCreate("bar-school",
				LocalDate.parse("2022-02-02"), 10, 20, 30);
		List<CheckoutLimitScheduleDtoCreate> list = List.of(dto1, dto2);
		List<CheckoutLimitSchedule> result = CheckoutLimitScheduleMapper.INSTANCE.toEntityList(list);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(CheckoutLimitSchedule::getGrade).containsExactly(1, 10);
		assertThat(result).extracting(CheckoutLimitSchedule::getMaxBooks).containsExactly(2, 20);
		assertThat(result).extracting(CheckoutLimitSchedule::getMaxDays).containsExactly(3, 30);
		assertThat(result).extracting(CheckoutLimitSchedule::getSchoolId).containsExactly("foo-school", "bar-school");
	}

}
