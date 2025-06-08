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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BookCategoryController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@DbTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ BookCategoryController.class, BookCategoryService.class })
class BookCategoryControllerDBTests {

	@Autowired
	SessionFactory sessionFactory;

	@Autowired
	BookCategoryController controller;

	@Test
	void list() {
		Statistics statistics = this.sessionFactory.getStatistics();
		PagedModel<BookCategoryDto> page;

		page = this.controller.list(Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getMetadata()).isNotNull();
		assertThat(page.getMetadata().totalElements()).isEqualTo(45);

		assertThat(statistics.getQueryExecutionCount()).isEqualTo(1);

		statistics.clear();

		page = this.controller.list("river", Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getMetadata()).isNotNull();
		assertThat(page.getMetadata().totalElements()).isEqualTo(13);

		assertThat(statistics.getQueryExecutionCount()).isEqualTo(1);
	}

}
