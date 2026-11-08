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

import java.util.List;

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
					INSERT INTO members (id, firstname_en, lastname_en, school_id, grade)
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

	@Test
	void listClasses() {
		String sql = """
					INSERT INTO members (id, firstname_en, lastname_en, school_id, grade, class_number, active)
					VALUES	(10, 'a', 'a', 'sky', 2, 1, true),
							(20, 'b', 'b', 'sky', 1, 3, true),
							(30, 'c', 'c', 'sky', 1, 3, true),
							(40, 'd', 'd', 'sky', 1, 1, true),
							(50, 'e', 'e', 'sky', 3, 1, false),
							(60, 'f', 'f', 'sky', 4, null, true),
							(70, 'g', 'g', 'ocean', 5, 5, true);
				""";
		this.jdbcTemplate.update(sql);

		List<MemberClassDto> result = this.memberService.listClasses("sky");
		assertThat(result).containsExactly(new MemberClassDto(1, 1), new MemberClassDto(1, 3),
				new MemberClassDto(2, 1));

		assertThat(this.memberService.listClasses("unknown")).isEmpty();
	}

	@Test
	void listRoster() {
		String sql = """
					INSERT INTO members (id, firstname_en, lastname_en, firstname_kana, lastname_kana, school_id, grade, class_number, active)
					VALUES	(10, 'b', 'smith', 'はな', 'いとう', 'sky', 4, 2, true),
							(20, 'a', 'smith', 'はな', 'いとう', 'sky', 4, 2, true),
							(30, 'z', 'adams', 'けん', 'うえだ', 'sky', 4, 2, true),
							(100, 'c', 'smith', 'あい', 'いとう', 'sky', 4, 2, true),
							(90, 'a', 'aaron', null, null, 'sky', 4, 2, true),
							(40, 'c', 'brown', null, null, 'sky', 4, 1, true),
							(50, 'd', 'davis', null, null, 'sky', 4, 2, false),
							(60, 'e', 'evans', null, null, 'sky', 3, 2, true),
							(70, 'f', 'fox', null, null, 'ocean', 4, 2, true),
							(80, 'g', 'green', null, null, 'sky', 4, null, true);
				""";
		this.jdbcTemplate.update(sql);

		// grade + class: kana first (lastname, firstname), en breaks kana ties, null kana
		// last
		assertThat(this.memberService.listRoster("sky", 4, 2)).extracting(Member::getId)
			.containsExactly(100L, 20L, 10L, 30L, 90L);
		// grade only: grouped by class, null class last
		assertThat(this.memberService.listRoster("sky", 4, null)).extracting(Member::getId)
			.containsExactly(40L, 100L, 20L, 10L, 30L, 90L, 80L);
		assertThat(this.memberService.listRoster("unknown", 4, null)).isEmpty();
		assertThat(this.memberService.listRoster("sky", 9, null)).isEmpty();
	}

}
