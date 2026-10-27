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

import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs a test against a PostgreSQL database in a Testcontainers container.
 * <p>
 * Use it together with a test slice or {@code @SpringBootTest}. Slices need
 * {@code @AutoConfigureTestDatabase(replace = Replace.NONE)}, otherwise they replace the
 * datasource with an embedded database: <pre class="code">
 * &#064;DataJpaTest
 * &#064;AutoConfigureTestDatabase(replace = Replace.NONE)
 * &#064;DbTest
 * class BookRepositoryTests { ... }
 * </pre>
 * <ul>
 * <li>The container ({@link #postgres}) is a static field, so a single container is
 * shared by all test contexts in the JVM. The datasource connects to it via
 * {@code @ServiceConnection}.</li>
 * <li>{@link DevDatabaseInitializer} recreates the schema and data each time a test
 * context starts. Because the database is shared, tests should not rely on data committed
 * by other tests.</li>
 * <li>Annotate tests with {@code @Transactional} (test slices such as
 * {@code @DataJpaTest} already are) so that changes are rolled back. Tests that cannot be
 * transactional must delete the data they created after each test, so that other tests
 * are not affected.</li>
 * <li>Docker Compose support is skipped in tests, so the database used by the dev app is
 * never touched. Tests and the dev app can therefore run at the same time.</li>
 * </ul>
 *
 * @author Tadaya Tsuyukubo
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ImportTestcontainers(DbTest.class)
@Import(DevDatabaseInitializer.class)
// To avoid exceeding the max connection due to test-context caching, minimize the
// num of pool size for each test.
@TestPropertySource(properties = { "spring.datasource.hikari.maximum-pool-size=2" })
public @interface DbTest {

	// Shared by all test contexts in the JVM (fields in an annotation are static final).
	// withReuse(true) only takes effect when "testcontainers.reuse.enable=true" is set
	// in ~/.testcontainers.properties; otherwise a new container starts per run.
	// Due to the test-context cache, tests keep connections, so max_connections is
	// raised.
	@ServiceConnection
	@SuppressWarnings("resource")
	PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17").withReuse(true)
		.withCommand("postgres -c max_connections=100")
		.waitingFor(Wait.forListeningPort());

}
