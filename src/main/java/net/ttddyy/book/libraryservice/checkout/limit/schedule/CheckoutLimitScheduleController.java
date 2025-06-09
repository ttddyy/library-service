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

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tadaya Tsuyukubo
 */
@RestController
class CheckoutLimitScheduleController {

	private final CheckoutLimitScheduleService checkoutLimitScheduleService;

	public CheckoutLimitScheduleController(CheckoutLimitScheduleService checkoutLimitScheduleService) {
		this.checkoutLimitScheduleService = checkoutLimitScheduleService;
	}

	@GetMapping("/api/checkouts/limits/schedules")
	Page<CheckoutLimitScheduleDto> list(@ParameterObject Pageable pageable) {
		return list(null, pageable);
	}

	@GetMapping("/api/checkouts/limits/schedules/{schoolId}")
	Page<CheckoutLimitScheduleDto> list(@PathVariable @Nullable String schoolId, @ParameterObject Pageable pageable) {
		// TODO: check size limit
		Page<CheckoutLimitSchedule> page = this.checkoutLimitScheduleService.list(schoolId, pageable);
		return CheckoutLimitScheduleMapper.INSTANCE.toDtoPage(page);
	}

	@PostMapping("/api/checkouts/limits/schedules")
	List<CheckoutLimitScheduleDto> bulkCreate(@RequestBody List<CheckoutLimitScheduleDtoCreate> dtoList) {
		// TODO: check id is null
		List<CheckoutLimitSchedule> entities = CheckoutLimitScheduleMapper.INSTANCE.toEntityList(dtoList);
		List<CheckoutLimitSchedule> created = this.checkoutLimitScheduleService.create(entities);
		return CheckoutLimitScheduleMapper.INSTANCE.toDtoList(created);
	}

	@PutMapping("/api/checkouts/limits/schedules")
	List<CheckoutLimitScheduleDto> bulkUpdate(@RequestBody List<CheckoutLimitScheduleDtoUpdate> dtoList) {
		List<CheckoutLimitSchedule> updated = this.checkoutLimitScheduleService.update(dtoList);
		return CheckoutLimitScheduleMapper.INSTANCE.toDtoList(updated);
	}

	@DeleteMapping("/api/checkouts/limits/schedules/{id}")
	void delete(@PathVariable long id) {
		this.checkoutLimitScheduleService.delete(id);
	}

}
