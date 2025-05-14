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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ MemberService.class })
@DbTest
class MemberServiceDBTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	MemberService memberService;

	@Test
	void list() {
		String sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school, grade)
					VALUES	(10, 'foo', 'foo', 'sky', 7),
							(20, 'bar', 'bar', 'ocean', 2);
				""";
		this.jdbcTemplate.update(sql);

		Page<Member> page;
		page = this.memberService.list(null, Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(2);

		page = this.memberService.list("ocean", Pageable.unpaged());
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).first().extracting(Member::getId).isEqualTo(20L);
	}

}
