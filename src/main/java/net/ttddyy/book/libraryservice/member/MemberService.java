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

import org.hibernate.Hibernate;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tadaya Tsuyukubo
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public Page<Member> list(@Nullable String schoolId, Pageable pageable) {
		// currently, this query by example impl depending on the fact that all fields
		// in entity class can have null, in order to only apply the filter condition.
		Member member = new Member();
		member.setSchoolId(schoolId);
		return this.memberRepository.findAll(Example.of(member), pageable);
	}

	public List<MemberClassDto> listClasses(String schoolId) {
		return this.memberRepository.findClasses(schoolId);
	}

	public Member get(long id) {
		Member member = this.memberRepository.getReferenceById(id);
		Hibernate.initialize(member);
		return member;
	}

}
