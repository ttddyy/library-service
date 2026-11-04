/*
 * Copyright 2025 the original author or authors.
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsible for the effective checkout limit endpoints.
 *
 * @author Tadaya Tsuyukubo
 */
@RestController
@Tag(name = "Checkout Limits")
public class CheckoutLimitController {

	private final CheckoutLimitService checkoutLimitService;

	CheckoutLimitController(CheckoutLimitService checkoutLimitService) {
		this.checkoutLimitService = checkoutLimitService;
	}

	@GetMapping({ "/api/checkouts/limits" })
	@Operation(summary = "Get effective checkout limits of a school")
	List<CheckoutLimitDto> list(@RequestParam String schoolId, @Parameter(
			description = "Date to evaluate (default: today)") @RequestParam(required = false) LocalDate date) {
		List<CheckoutLimit> limits = this.checkoutLimitService.getEffectiveCheckoutLimits(schoolId, date);
		return CheckoutLimitMapper.INSTANCE.toDtoList(limits);
	}

}
