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

import net.ttddyy.book.libraryservice.DbTest;
import org.hibernate.SessionFactory;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Crud tests for {@link CheckoutLimitDefaultController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ CheckoutLimitDefaultController.class, CheckoutLimitDefaultService.class })
@DbTest
class CheckoutLimitDefaultControllerDBTests {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	CheckoutLimitDefaultController controller;

	@Autowired
	SessionFactory sessionFactory;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void bulkUpdate() {
		// ( 3, 'ocean', 3, 4, 4),
		Long id = 3L;

		CheckoutLimitDefaultDtoUpdateBulk dto = new CheckoutLimitDefaultDtoUpdateBulk(3L, 6, 4);
		List<CheckoutLimitDefaultDto> response = this.controller.bulkUpdate(List.of(dto));
		entityManager.flush();

		Statistics statistics = this.sessionFactory.getStatistics();
		EntityStatistics entityStatistics = statistics.getEntityStatistics(CheckoutLimitDefault.class.getName());
		assertThat(entityStatistics.getUpdateCount()).isEqualTo(1);

		// verify response
		assertThat(response).hasSize(1);
		CheckoutLimitDefaultDto updated = response.get(0);
		assertThat(updated.id()).isEqualTo(id);
		assertThat(updated.schoolId()).isEqualTo("ocean");
		assertThat(updated.grade()).isEqualTo(3);
		assertThat(updated.maxBooks()).isEqualTo(6);
		assertThat(updated.maxWeeks()).isEqualTo(4);

		// get from DB
		entityManager.clear();
		CheckoutLimitDefault updatedEntity = entityManager.find(CheckoutLimitDefault.class, id);
		assertThat(updatedEntity).isNotNull();
		assertThat(updatedEntity.getGrade()).isEqualTo(3);
		assertThat(updatedEntity.getSchoolId()).isEqualTo("ocean");
		assertThat(updatedEntity.getMaxBooks()).isEqualTo(6);
		assertThat(updatedEntity.getMaxWeeks()).isEqualTo(4);
	}

	@Test
	void delete() {
		Long id = 3L;
		this.controller.delete(id);
		this.entityManager.flush();

		CheckoutLimitDefault result = this.entityManager.find(CheckoutLimitDefault.class, id);
		assertThat(result).isNull();

		Statistics statistics = this.sessionFactory.getStatistics();
		EntityStatistics entityStatistics = statistics.getEntityStatistics(CheckoutLimitDefault.class.getName());
		assertThat(entityStatistics.getDeleteCount()).isEqualTo(1);
	}

	@Test
	void list() {
		JdbcTestUtils.deleteFromTables(this.jdbcTemplate, "checkout_limit_defaults");

		CheckoutLimitDefault entity1 = new CheckoutLimitDefault();
		entity1.setId(100L);
		entity1.setGrade(1);
		entity1.setSchoolId("river");
		entity1.setMaxBooks(11);
		entity1.setMaxWeeks(111);
		CheckoutLimitDefault entity2 = new CheckoutLimitDefault();
		entity2.setId(200L);
		entity2.setGrade(2);
		entity2.setSchoolId("river");
		entity2.setMaxBooks(22);
		entity2.setMaxWeeks(222);
		CheckoutLimitDefault entity3 = new CheckoutLimitDefault();
		entity3.setId(300L);
		entity3.setGrade(3);
		entity3.setSchoolId("river");
		entity3.setMaxBooks(33);
		entity3.setMaxWeeks(333);
		this.entityManager.persist(entity1);
		this.entityManager.persist(entity2);
		this.entityManager.persist(entity3);
		this.entityManager.flush();

		Pageable pageable;
		PagedModel<CheckoutLimitDefaultDto> result;

		// get all in one request
		pageable = PageRequest.of(0, 10, Sort.by("grade"));
		result = this.controller.list(pageable);
		assertThat(result).isNotNull();
		assertThat(result.getMetadata()).isNotNull().satisfies((metadata) -> {
			assertThat(metadata.size()).isEqualTo(10);
			assertThat(metadata.number()).isEqualTo(0);
			assertThat(metadata.totalPages()).isEqualTo(1);
			assertThat(metadata.totalElements()).isEqualTo(3);
		});
		assertThat(result.getContent()).hasSize(3).extracting(CheckoutLimitDefaultDto::grade).containsExactly(1, 2, 3);

		// one entry per page
		pageable = PageRequest.of(1, 1, Sort.by("grade"));
		result = this.controller.list(pageable);
		assertThat(result).isNotNull();
		assertThat(result.getMetadata()).isNotNull().satisfies((metadata) -> {
			assertThat(metadata.size()).isEqualTo(1);
			assertThat(metadata.number()).isEqualTo(1);
			assertThat(metadata.totalPages()).isEqualTo(3);
			assertThat(metadata.totalElements()).isEqualTo(3);
		});
		assertThat(result.getContent()).hasSize(1).first().extracting(CheckoutLimitDefaultDto::grade).isEqualTo(2);

		// two entries per page
		pageable = PageRequest.of(1, 2, Sort.by("grade"));
		result = this.controller.list(pageable);
		assertThat(result).isNotNull();
		assertThat(result.getMetadata()).isNotNull().satisfies((metadata) -> {
			assertThat(metadata.size()).isEqualTo(2);
			assertThat(metadata.number()).isEqualTo(1);
			assertThat(metadata.totalPages()).isEqualTo(2);
			assertThat(metadata.totalElements()).isEqualTo(3);
		});
		assertThat(result.getContent()).hasSize(1).first().extracting(CheckoutLimitDefaultDto::grade).isEqualTo(3);
	}

}
