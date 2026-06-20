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

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest()
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ BookCategoryService.class })
@DbTest
class BookCategoryServiceDBTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	BookCategoryService categoryService;

	@Test
	void list() {
		Page<BookCategory> page;
		page = this.categoryService.list(null, Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(45);

		page = this.categoryService.list("sky", Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(6);
	}

}
