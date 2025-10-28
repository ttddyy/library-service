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

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tadaya Tsuyukubo
 */
@RestController
class MemberController {

	private final MemberService memberService;

	MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping("/api/members")
	Page<MemberDto> list(@RequestParam(required = false) @Nullable String schoolId,
			@ParameterObject Pageable pageable) {
		Page<Member> page = this.memberService.list(schoolId, pageable);
		return MemberMapper.INSTANCE.toDtoPage(page);
	}

	@GetMapping("/api/members/{id}")
	MemberDto member(@PathVariable long id) {
		Member member = this.memberService.get(id);
		MemberDto memberDto = MemberMapper.INSTANCE.toDto(member);
		return memberDto;
	}

}
