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

package net.ttddyy.book.libraryservice.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.ListQueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author Tadaya Tsuyukubo
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long>, ListQueryByExampleExecutor<Book> {

	@Query("SELECT CASE WHEN (max(id) IS NULL ) THEN :start ELSE max(id) + 1 END FROM book WHERE id BETWEEN :start AND :end")
	long nextId(long start, long end);

}
