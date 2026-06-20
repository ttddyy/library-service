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

package net.ttddyy.book.libraryservice;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author Tadaya Tsuyukubo
 */

@Configuration(proxyBeanMethods = false)
class ApplicationSecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// require authentication on api endpoints but make health endpoint public
		http.securityMatchers(matchers -> {
			matchers.requestMatchers("/api/**");
			matchers.requestMatchers(EndpointRequest.toAnyEndpoint());
		}).authorizeHttpRequests(authz -> {
			authz.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll();
			authz.requestMatchers("/api/schools").permitAll();
			authz.anyRequest().authenticated();
		});
		http.csrf(csrf -> csrf.disable()); // need for POST requests
		http.httpBasic(withDefaults());
		return http.build();
	}

}
