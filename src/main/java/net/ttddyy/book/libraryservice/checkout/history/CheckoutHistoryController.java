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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for the book checkout history endpoints.
 *
 * @author Tadaya Tsuyukubo
 */
@RestController
public class CheckoutHistoryController {

	private final CheckoutHistoryService checkoutHistoryService;

	CheckoutHistoryController(CheckoutHistoryService checkoutHistoryService) {
		this.checkoutHistoryService = checkoutHistoryService;
	}

	@GetMapping({ "/api/checkouts/history" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Checkout History(s) retrieved"),
			@ApiResponse(responseCode = "400", description = "Missing required parameters"),
			@ApiResponse(responseCode = "404", description = "Specific checkout not found") })
	@PageableAsQueryParam
	ResponseEntity<?> history(@Parameter(description = "ID of the book") @RequestParam @Nullable Long bookId,
			@Parameter(description = "ID of the member") @RequestParam @Nullable Long memberId,
			@Parameter(hidden = true) @ParameterObject Pageable pageable) {

		if (bookId == null && memberId == null) {
			return ResponseEntity.badRequest().body("Either bookId or memberId must be provided.");
		}
		if (bookId != null && memberId != null) {
			CheckoutHistory history = this.checkoutHistoryService.get(bookId, memberId);
			if (history == null) {
				return ResponseEntity.notFound().build();
			}
			CheckoutHistoryDto dto = CheckoutHistoryMapper.INSTANCE.toDto(history);
			return ResponseEntity.ok(dto);
		}

		// TODO: default sort by creation??
		Page<CheckoutHistory> page;
		if (bookId != null) {
			page = this.checkoutHistoryService.listByBook(bookId, pageable);
		}
		else {
			page = this.checkoutHistoryService.listByMember(memberId, pageable);
		}
		return ResponseEntity.ok(CheckoutHistoryMapper.INSTANCE.toDtoPage(page));
	}

}
