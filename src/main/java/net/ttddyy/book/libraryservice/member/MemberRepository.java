
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

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.ListQueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author Tadaya Tsuyukubo
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, ListQueryByExampleExecutor<Member> {

	@Query("""
			select distinct new net.ttddyy.book.libraryservice.member.MemberClassDto(m.grade, m.classNumber)
			from Member m
			where m.schoolId = :schoolId and m.active = true and m.grade is not null and m.classNumber is not null
			order by m.grade, m.classNumber
			""")
	List<MemberClassDto> findClasses(String schoolId);

}
