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

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Recreates the schema and data on startup. Registered by {@link DbTest} and
 * {@link TestLibraryServiceApplication}.
 * <p>
 * Not a {@code @Component} on purpose: it must not be picked up by component scanning,
 * otherwise any test would run {@code drop-all.sql} against the configured database.
 *
 * @author Tadaya Tsuyukubo
 */
class DevDatabaseInitializer extends DataSourceInitializer {

	DevDatabaseInitializer(DataSource dataSource) {
		// to add a separator for schema, create populator for each
		ResourceDatabasePopulator dropAll = new ResourceDatabasePopulator(new ClassPathResource("drop-all.sql"));
		ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
		ResourceDatabasePopulator function = new ResourceDatabasePopulator(new ClassPathResource("function.sql"));
		ResourceDatabasePopulator trigger = new ResourceDatabasePopulator(new ClassPathResource("trigger.sql"));
		ResourceDatabasePopulator data = new ResourceDatabasePopulator(new ClassPathResource("data.sql"));

		// since function uses ';', need to define explicit separator
		function.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);

		setDataSource(dataSource);
		setDatabasePopulator(new CompositeDatabasePopulator(dropAll, schema, function, trigger, data));
	}

}
