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

package net.ttddyy.book.libraryservice.checkout.limit.defaults;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tadaya Tsuyukubo
 */
@RestController
class CheckoutLimitDefaultController {

	private final CheckoutLimitDefaultService checkoutLimitDefaultService;

	public CheckoutLimitDefaultController(CheckoutLimitDefaultService checkoutLimitDefaultService) {
		this.checkoutLimitDefaultService = checkoutLimitDefaultService;
	}

	// Since this is a small set of data, no filtering for school-id for listing
	@GetMapping("/api/checkouts/limits/defaults")
	PagedModel<CheckoutLimitDefaultDto> list(@ParameterObject Pageable pageable) {
		// TODO: check size limit
		Page<CheckoutLimitDefault> page = this.checkoutLimitDefaultService.list(pageable);
		return new PagedModel<>(CheckoutLimitDefaultMapper.INSTANCE.toDtoPage(page));
	}

	@PutMapping("/api/checkouts/limits/defaults")
	List<CheckoutLimitDefaultDto> bulkUpdate(@RequestBody List<CheckoutLimitDefaultDtoUpdateBulk> dto) {
		List<CheckoutLimitDefault> updated = this.checkoutLimitDefaultService.update(dto);
		return CheckoutLimitDefaultMapper.INSTANCE.toDtoList(updated);
	}

	// For now, hiding the deletion from endpoint
	// @DeleteMapping("/api/checkouts/limits/defaults/{id}")
	void delete(@PathVariable long id) {
		this.checkoutLimitDefaultService.delete(id);
	}

}
