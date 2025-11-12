/*
 * Copyright 2024-2025 the original author or authors.
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import org.springframework.data.domain.Page;

/**
 * Mappings between entity and dto for books.
 *
 * @author Tadaya Tsuyukubo
 */
@Mapper
public interface BookMapper {

	BookMapper INSTANCE = Mappers.getMapper(BookMapper.class);

	BookDto toDto(Book book);

	List<BookDto> toDtoList(List<Book> book);

	@Mapping(target = "category.id", source = "categoryId")
	Book toBook(BookDtoCreate dto);

	List<Book> toBookList(List<BookDtoCreate> books);

	// "deletedDate" is populated by @AfterMapping
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(ignore = true, target = "deletedDate")
	void updateBookFromDto(@MappingTarget Book book, BookDtoUpdate dto, Clock clock);

	@AfterMapping
	default Book handleDeletedAndLost(@MappingTarget Book book, BookDtoUpdate dto, Clock clock) {
		// handle delete
		if (dto.delete() != null) {
			book.setDeletedDate(dto.delete() ? LocalDate.now(clock) : null);
		}
		// handle missing
		if (dto.missing() != null) {
			book.setMissing(dto.missing());
			book.setLostDate(dto.missing() ? LocalDate.now(clock) : null);
		}
		return book;
	}

	default Page<BookDto> toDtoPage(Page<Book> page) {
		return page.map(this::toDto);
	}

}
