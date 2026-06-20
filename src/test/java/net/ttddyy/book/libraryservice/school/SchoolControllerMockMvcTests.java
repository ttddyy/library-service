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

package net.ttddyy.book.libraryservice.school;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link SchoolController} with DB.
 *
 * @author Tadaya Tsuyukubo
 */
@WebMvcTest(SchoolController.class)
class SchoolControllerMockMvcTests {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	SchoolService schoolService;

	@Test
	void list() throws Exception {
		School school1 = new School();
		school1.setId("foo");
		school1.setName("FOO");
		School school2 = new School();
		school2.setId("bar");
		school2.setName("BAR");

		given(this.schoolService.list()).willReturn(List.of(school1, school2));

		this.mvc.perform(get("/api/schools").with(httpBasic("user", "pass")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$..id", hasSize(2)))
			.andExpect(jsonPath("$..id", containsInAnyOrder("foo", "bar")));
	}

}
