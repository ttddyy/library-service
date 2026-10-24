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

import javax.sql.DataSource;

import net.ttddyy.book.libraryservice.auth.Roles;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author Tadaya Tsuyukubo
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationSecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenResolver bearerTokenResolver)
			throws Exception {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("roles");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		converter.setPrincipalClaimName("sub");

		// Bearer challenge only; no Basic challenge so browsers never show a login popup
		BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();

		http.securityMatchers(matchers -> {
			matchers.requestMatchers("/api/**");
			matchers.requestMatchers(EndpointRequest.toAnyEndpoint());
		}).authorizeHttpRequests(authz -> {
			authz.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll();
			authz.requestMatchers(HttpMethod.GET, "/api/schools").permitAll();
			authz.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll();
			authz.requestMatchers(HttpMethod.POST, "/api/books").hasRole(Roles.SCHOOL_ADMIN);
			authz.requestMatchers(HttpMethod.PUT, "/api/books/{id}").hasRole(Roles.SCHOOL_ADMIN);
			authz.requestMatchers(HttpMethod.PUT, "/api/checkouts/limits/defaults").hasRole(Roles.SCHOOL_ADMIN);
			authz.requestMatchers(HttpMethod.POST, "/api/checkouts/limits/schedules").hasRole(Roles.SCHOOL_ADMIN);
			authz.requestMatchers(HttpMethod.PUT, "/api/checkouts/limits/schedules").hasRole(Roles.SCHOOL_ADMIN);
			authz.requestMatchers(HttpMethod.DELETE, "/api/checkouts/limits/schedules/{id}")
				.hasRole(Roles.SCHOOL_ADMIN);
			authz.requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole(Roles.SUPER_ADMIN);
			authz.requestMatchers("/api/**").hasRole(Roles.USER);
			authz.anyRequest().denyAll();
		});
		http.csrf(csrf -> csrf.disable()); // bearer tokens are not sent automatically
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver)
			.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
		http.httpBasic(basic -> basic.authenticationEntryPoint(entryPoint));
		http.exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint));
		return http.build();
	}

	/**
	 * Ignore any (possibly stale) Bearer header on auth endpoints so that expired access
	 * tokens cannot block login, refresh or logout.
	 */
	@Bean
	BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
		RequestMatcher authEndpoints = PathPatternRequestMatcher.withDefaults()
			.matcher(HttpMethod.POST, "/api/auth/**");
		return (request) -> authEndpoints.matches(request) ? null : delegate.resolve(request);
	}

	@Bean
	RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
			.role(Roles.SUPER_ADMIN)
			.implies(Roles.SCHOOL_ADMIN)
			.role(Roles.SCHOOL_ADMIN)
			.implies(Roles.USER)
			.build();
	}

	@Bean
	UserDetailsService userDetailsService(DataSource dataSource) {
		return new JdbcUserDetailsManager(dataSource);
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		return new ProviderManager(provider);
	}

}
