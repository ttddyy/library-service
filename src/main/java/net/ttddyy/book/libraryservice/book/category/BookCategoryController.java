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

package net.ttddyy.book.libraryservice.book.category;

import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Book category controller.
 *
 * @author Tadaya Tsuyukubo
 */
@RestController
class BookCategoryController {

	private final BookCategoryService categoryService;

	BookCategoryController(BookCategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping("/api/books/categories")
	Page<BookCategoryDto> list(@ParameterObject Pageable pageable) {
		return list(null, pageable);
	}

	@GetMapping("/api/books/categories/{schoolId}")
	Page<BookCategoryDto> list(@PathVariable @Nullable String schoolId, @ParameterObject Pageable pageable) {
		Page<BookCategory> page = this.categoryService.list(schoolId, pageable);
		return BookCategoryMapper.INSTANCE.toDtoPage(page);
	}

}
