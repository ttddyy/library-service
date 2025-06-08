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

package net.ttddyy.book.libraryservice.member;

import net.ttddyy.book.libraryservice.DbTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MemberController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ MemberController.class, MemberService.class })
@DbTest
class MemberControllerDBTests {

	@Autowired
	MemberController controller;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void list() {
		Member member1 = new Member();
		member1.setId(10L);
		member1.setActive(true);
		member1.setFirstnameEn("member-1");
		member1.setLastnameEn("last-1");
		member1.setSchoolId("school-A");
		member1.setGrade(2);
		Member member2 = new Member();
		member2.setId(20L);
		member2.setActive(true);
		member2.setFirstnameEn("member-2");
		member2.setLastnameEn("last-2");
		member2.setSchoolId("school-B");
		member2.setGrade(2);
		Member member3 = new Member();
		member3.setId(30L);
		member3.setActive(true);
		member3.setFirstnameEn("member-3");
		member3.setLastnameEn("last-3");
		member3.setSchoolId("school-B");
		member3.setGrade(2);
		this.entityManager.persist(member1);
		this.entityManager.persist(member2);
		this.entityManager.persist(member3);
		this.entityManager.flush();

		PageRequest pageable;
		PagedModel<MemberDto> result;

		// get all
		pageable = PageRequest.of(0, 10, Sort.by("id"));
		result = this.controller.list(null, pageable);
		assertThat(result.getContent()).isNotNull().extracting(MemberDto::id).containsExactly(10L, 20L, 30L);

		// with schoolId filter
		pageable = PageRequest.of(0, 10, Sort.by("id"));
		result = this.controller.list("school-B", pageable);
		assertThat(result.getContent()).isNotNull().extracting(MemberDto::id).containsExactly(20L, 30L);

		// with pagination
		pageable = PageRequest.of(1, 1, Sort.by("id"));
		result = this.controller.list(null, pageable);
		assertThat(result.getContent()).isNotNull().extracting(MemberDto::id).containsExactly(20L);
		assertThat(result.getMetadata()).isNotNull().satisfies((metadata) -> {
			assertThat(metadata.size()).isEqualTo(1);
			assertThat(metadata.number()).isEqualTo(1);
			assertThat(metadata.totalPages()).isEqualTo(3);
			assertThat(metadata.totalElements()).isEqualTo(3);
		});

		// with filter and pagination
		pageable = PageRequest.of(1, 1, Sort.by("id"));
		result = this.controller.list("school-B", pageable);
		assertThat(result.getContent()).isNotNull().extracting(MemberDto::id).containsExactly(30L);
		assertThat(result.getMetadata()).isNotNull().satisfies((metadata) -> {
			assertThat(metadata.size()).isEqualTo(1);
			assertThat(metadata.number()).isEqualTo(1);
			assertThat(metadata.totalPages()).isEqualTo(2);
			assertThat(metadata.totalElements()).isEqualTo(2);
		});
	}

}
