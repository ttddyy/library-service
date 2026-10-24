/*
 * Copyright 2026 the original author or authors.
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

package net.ttddyy.book.libraryservice;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) customization that advertises the bearer JWT security scheme.
 *
 * @author Tadaya Tsuyukubo
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

	private static final String SCHEME_NAME = "bearer-jwt";

	@Bean
	OpenAPI openApi() {
		SecurityScheme scheme = new SecurityScheme().type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT");
		return new OpenAPI().components(new Components().addSecuritySchemes(SCHEME_NAME, scheme))
			.addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
	}

}
