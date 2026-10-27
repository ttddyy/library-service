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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * @author Tadaya Tsuyukubo
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ DbTest.DevContainerConfiguration.class, DevDatabaseInitializer.class })
// To avoid exceeding the max connection due to test-context caching, minimize the
// num of pool size for each test.
@TestPropertySource(properties = { "spring.datasource.hikari.maximum-pool-size=2" })
public @interface DbTest {

	/**
	 * Postgres container for tests.
	 * <p>
	 * Not a {@code @Configuration} on purpose: {@code spring-boot:test-run} puts test
	 * classes on the classpath, and component scanning would start this container for the
	 * dev app.
	 */
	class DevContainerConfiguration {

		@Bean
		@ServiceConnection
		@SuppressWarnings("resource")
		PostgreSQLContainer postgreSQLContainer() {
			// Due to the test-context cache, tests keeps connections.
			// Need to increase the max connections for test.
			// withReuse(true) only takes effect when "testcontainers.reuse.enable=true"
			// is set in ~/.testcontainers.properties; otherwise a new container starts
			// per run.
			return new PostgreSQLContainer("postgres:17").withReuse(true)
				.withCommand("postgres -c max_connections=100")
				.waitingFor(Wait.forListeningPort());
			// using a static name causes re-run to fail unless removing the container.
			// .withCreateContainerCmdModifier((cmd) ->
			// cmd.withName("library-service-testcontainer"));

		}

	}

}
