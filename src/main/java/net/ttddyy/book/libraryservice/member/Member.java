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

package net.ttddyy.book.libraryservice.member;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Tadaya Tsuyukubo
 */
@Entity(name = "member")
@Table(name = "members")
public class Member {

	// This entity is populated by batch job in DB directly.
	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "date_added", insertable = false, updatable = false)
	private Instant addedTime;

	@Column(name = "active")
	private Boolean active;

	@Column(name = "school")
	private String schoolId;

	private Integer classNumber;

	private Integer grade;

	private String firstnameEn;

	private String lastnameEn;

	private String firstname;

	private String lastname;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getAddedTime() {
		return this.addedTime;
	}

	public void setAddedTime(Instant addedTime) {
		this.addedTime = addedTime;
	}

	public Boolean getActive() {
		return this.active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getSchoolId() {
		return this.schoolId;
	}

	public void setSchoolId(String schoolId) {
		this.schoolId = schoolId;
	}

	public Integer getClassNumber() {
		return this.classNumber;
	}

	public void setClassNumber(Integer classNumber) {
		this.classNumber = classNumber;
	}

	public Integer getGrade() {
		return this.grade;
	}

	public void setGrade(Integer grade) {
		this.grade = grade;
	}

	public String getFirstnameEn() {
		return this.firstnameEn;
	}

	public void setFirstnameEn(String firstnameEn) {
		this.firstnameEn = firstnameEn;
	}

	public String getLastnameEn() {
		return this.lastnameEn;
	}

	public void setLastnameEn(String lastnameEn) {
		this.lastnameEn = lastnameEn;
	}

	public String getFirstname() {
		return this.firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return this.lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

}
