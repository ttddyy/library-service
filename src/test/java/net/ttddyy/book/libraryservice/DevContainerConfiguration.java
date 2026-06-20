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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.sql.DataSource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author Tadaya Tsuyukubo
 */
@Configuration(proxyBeanMethods = false)
class DevContainerConfiguration {

	@Bean
	@ConditionalOnLocalDatabase
	JdbcConnectionDetails localJdbcConnectionDetails() {
		return new JdbcConnectionDetails() {
			@Override
			public String getUsername() {
				return "root";
			}

			@Override
			public String getPassword() {
				return "password";
			}

			@Override
			public String getJdbcUrl() {
				return "jdbc:postgres://127.0.0.1:5432/library_service_test";
			}
		};
	}

	@Bean
	@ConditionalOnMissingLocalDatabase
	@ServiceConnection
	@SuppressWarnings("resource")
	PostgreSQLContainer postgreSQLContainer() {
		// Due to the test-context cache, tests keeps connections.
		// Need to increase the max connections for test.
		return new PostgreSQLContainer("postgres:17").withReuse(true)
			.withCommand("postgres -c max_connections=100")
			.waitingFor(Wait.forListeningPort());
		// using a static name causes re-run to fail unless removing the container.
		// .withCreateContainerCmdModifier((cmd) ->
		// cmd.withName("library-service-testcontainer"));

	}

	// TODO: think about initialization
	@Bean
	DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
		// to add a separator for schema, create populator for each
		ResourceDatabasePopulator dropAll = new ResourceDatabasePopulator(new ClassPathResource("drop-all.sql"));
		ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
		ResourceDatabasePopulator function = new ResourceDatabasePopulator(new ClassPathResource("function.sql"));
		ResourceDatabasePopulator trigger = new ResourceDatabasePopulator(new ClassPathResource("trigger.sql"));
		ResourceDatabasePopulator data = new ResourceDatabasePopulator(new ClassPathResource("data.sql"));

		// since function uses ';', need to define explicit separator
		function.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);

		CompositeDatabasePopulator populator = new CompositeDatabasePopulator(dropAll, schema, function, trigger, data);

		DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
		dataSourceInitializer.setDataSource(dataSource);
		dataSourceInitializer.setDatabasePopulator(populator);
		return dataSourceInitializer;
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnLocalDatabaseCondition.class)
	public @interface ConditionalOnLocalDatabase {

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnLocalDatabaseCondition.class)
	public @interface ConditionalOnMissingLocalDatabase {

	}

	static class OnLocalDatabaseCondition extends SpringBootCondition implements ConfigurationCondition {

		// TODO: use parameters instead of the hardcoded value
		static final String localJdbcUrl = "jdbc:postgres://127.0.0.1:5432/library_service_test?user=root&password=password";

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			// TODO: proper match messages
			boolean isLocal = isLocalDatabaseAvailable();
			MergedAnnotations annotations = metadata.getAnnotations();
			if (annotations.isPresent(ConditionalOnLocalDatabase.class)) {
				return isLocal ? ConditionOutcome.match() : ConditionOutcome.noMatch("Local database is not running.");
			}
			else if (annotations.isPresent(ConditionalOnMissingLocalDatabase.class)) {
				return isLocal ? ConditionOutcome.noMatch("Local database is running.") : ConditionOutcome.match();
			}
			throw new IllegalStateException("No matching conditions available.");
		}

		private boolean isLocalDatabaseAvailable() {
			// check local db is running or not
			try (Connection connection = DriverManager.getConnection(localJdbcUrl)) {
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

	}

}
