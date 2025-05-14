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

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.ttddyy.book.libraryservice.book.category.BookCategory;

import org.springframework.lang.Nullable;

/**
 * @author Tadaya Tsuyukubo
 */
@Entity(name = "book")
@Table(name = "books")
public class Book {

	@Id
	@Column(name = "id")
	private Long id;

	@Version
	private Integer version;

	private String title;

	private String titleKana;

	private String author;

	private String authorKana;

	private String isbn;

	private String comments;

	private String publisher;

	private String schoolId;

	// TODO: make it eager for now to allow query by example on BookRepository#list
	// to retrieve books easily. Need to consider impl rather keep it eager OR change
	// the list not to use query-by-example.
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "book_category_id")
	private BookCategory category;

	// TODO: clean up date/timestamp related column names and method names
	@Column(name = "date_added")
	private Instant addedTime;

	@Column(name = "date_deleted")
	@Nullable
	private LocalDate deletedDate;

	@Column(name = "date_lost")
	@Nullable
	private LocalDate lostDate;

	@Column(name = "is_missing")
	@Nullable
	private Boolean missing;

	// readonly for now
	@Column(name = "num_checkouts", insertable = false, updatable = false)
	private Integer checkouts;

	@Column(insertable = false, updatable = false)
	private Instant createdAt;

	@Column(insertable = false, updatable = false)
	private Instant updatedAt;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitleKana() {
		return this.titleKana;
	}

	public void setTitleKana(String titleKana) {
		this.titleKana = titleKana;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthorKana() {
		return this.authorKana;
	}

	public void setAuthorKana(String authorKana) {
		this.authorKana = authorKana;
	}

	public String getIsbn() {
		return this.isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getComments() {
		return this.comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getPublisher() {
		return this.publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getSchoolId() {
		return this.schoolId;
	}

	public void setSchoolId(String schoolId) {
		this.schoolId = schoolId;
	}

	public BookCategory getCategory() {
		return this.category;
	}

	public void setCategory(BookCategory category) {
		this.category = category;
	}

	public Instant getAddedTime() {
		return this.addedTime;
	}

	public void setAddedTime(Instant addedTime) {
		this.addedTime = addedTime;
	}

	@Nullable
	public LocalDate getDeletedDate() {
		return this.deletedDate;
	}

	public void setDeletedDate(@Nullable LocalDate deletedDate) {
		this.deletedDate = deletedDate;
	}

	@Nullable
	public LocalDate getLostDate() {
		return this.lostDate;
	}

	public void setLostDate(@Nullable LocalDate lostDate) {
		this.lostDate = lostDate;
	}

	@Nullable
	public Boolean getMissing() {
		return this.missing;
	}

	public void setMissing(Boolean missing) {
		this.missing = missing;
	}

	public Integer getCheckouts() {
		return this.checkouts;
	}

	public void setCheckouts(Integer checkouts) {
		this.checkouts = checkouts;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

}
