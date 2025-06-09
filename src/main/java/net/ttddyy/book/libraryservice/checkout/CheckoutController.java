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

import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for the book checkout and return endpoints.
 *
 * @author Tadaya Tsuyukubo
 */
@RestController
public class CheckoutController {

	private final CheckoutService checkoutService;

	CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/api/checkouts/checkout")
	@Operation(description = "'force' is optional (default: false)")
	void checkout(@RequestBody String schoolId, @RequestBody long memberId, @RequestBody Set<Long> bookIds,
			@RequestBody(required = false) boolean force) {
		// TODO: may return error due to validation
		this.checkoutService.checkout(schoolId, memberId, bookIds, force);
	}

	@PostMapping(value = "/api/checkouts/return")
	void bookReturn(@RequestBody Set<Long> bookIds) {
		// TODO: validation
		this.checkoutService.bulkReturn(bookIds);
	}

	// TODO: memberId has higher priority than schoolId
	@GetMapping({ "/api/checkouts" })
	Page<CheckoutDto> list(@RequestParam(required = false) @Nullable Long memberId,
			@RequestParam(required = false) @Nullable String schoolId, @ParameterObject Pageable pageable) {
		Page<Checkout> page = this.checkoutService.list(memberId, schoolId, pageable);
		return CheckoutMapper.INSTANCE.toDtoPage(page);
	}

	@GetMapping({ "/api/checkouts/overdue" })
	Page<CheckoutDto> overdue(@ParameterObject Pageable pageable) {
		// If response json requires more info, create a new dedicated dto class
		Page<Checkout> page = this.checkoutService.overdue(pageable);
		return CheckoutMapper.INSTANCE.toDtoPage(page);
	}

}
