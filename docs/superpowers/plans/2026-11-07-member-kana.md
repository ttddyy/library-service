# Member Name Kana Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add nullable `firstname_kana`/`lastname_kana` to members, expose them in member and checkout responses, and sort the roster by kana first.

**Architecture:** Column in `schema.sql`, fields on the `Member` JPA entity, additive fields on the record DTOs (MapStruct maps by name; `CheckoutMapper` needs explicit `@Mapping`), JPQL `order by` change in `MemberRepository.findRoster`.

**Tech Stack:** Spring Boot, JPA/Hibernate, MapStruct, Postgres 18, JUnit 5 + AssertJ.

**Spec:** `docs/superpowers/specs/2026-11-07-member-kana-design.md`

## Global Constraints

- Columns: `firstname_kana VARCHAR(64)`, `lastname_kana VARCHAR(64)`, nullable, placed after `lastname` in `members`.
- Java names: `firstnameKana`, `lastnameKana` (entity, `MemberDto`, `CheckoutHistoryDto.Member`); `memberFirstNameKana`, `memberLastNameKana` (`CheckoutDto`).
- Roster order: `m.classNumber, m.lastnameKana, m.firstnameKana, m.lastnameEn, m.firstnameEn`, no `NULLS` clause.
- Additive only: no existing DTO field renamed or reordered; new record components go right after the existing name fields.
- Existing raw `INSERT INTO members` in tests stay unchanged unless the test asserts kana.

## Review Focus

- Member with NULL kana in a class: sorts after kana members, ordered by en among themselves → `listRoster` row 90.
- Kana tie: en names break it → `listRoster` rows 10/20.
- Kana order disagrees with en order: kana wins → `listRoster` rows 30 and 100.
- Kana present on `GET /api/members` output → `MemberControllerDBTests.list` assertion.
- Null kana flows through mappers as null, not an error → `CheckoutMapperTests` / `CheckoutHistoryMapperTests` are unaffected for members without kana (MapStruct passes null through; no extra test).

---

### Task 1: Members schema, entity, DTO, roster sort

**Files:**
- Modify: `src/main/resources/schema.sql:103-116`
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/Member.java`
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/MemberDto.java`
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/MemberRepository.java` (`findRoster`)
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/MemberController.java` (`roster` `@Operation` description)
- Test: `src/test/java/net/ttddyy/book/libraryservice/member/MemberServiceDBTests.java` (`listRoster`)
- Test: `src/test/java/net/ttddyy/book/libraryservice/member/MemberControllerDBTests.java` (`list`)

**Interfaces:**
- Produces: `Member#getFirstnameKana()/setFirstnameKana(String)`, `Member#getLastnameKana()/setLastnameKana(String)`; `MemberDto` components `firstnameKana`, `lastnameKana` after `lastname`.

- [ ] **Step 1: Rewrite `listRoster` test data and assertions**

```java
String sql = """
			INSERT INTO members (id, firstname_en, lastname_en, firstname_kana, lastname_kana, school_id, grade, class_number, active)
			VALUES	(10, 'b', 'smith', 'はな', 'いとう', 'sky', 4, 2, true),
					(20, 'a', 'smith', 'はな', 'いとう', 'sky', 4, 2, true),
					(30, 'z', 'adams', 'けん', 'うえだ', 'sky', 4, 2, true),
					(100, 'c', 'smith', 'あい', 'いとう', 'sky', 4, 2, true),
					(90, 'a', 'aaron', null, null, 'sky', 4, 2, true),
					(40, 'c', 'brown', null, null, 'sky', 4, 1, true),
					(50, 'd', 'davis', null, null, 'sky', 4, 2, false),
					(60, 'e', 'evans', null, null, 'sky', 3, 2, true),
					(70, 'f', 'fox', null, null, 'ocean', 4, 2, true),
					(80, 'g', 'green', null, null, 'sky', 4, null, true);
		""";
// kana first (lastname, firstname), en breaks kana ties, null kana last
assertThat(this.memberService.listRoster("sky", 4, 2)).extracting(Member::getId)
	.containsExactly(100L, 20L, 10L, 30L, 90L);
// grade only: grouped by class, null class last
assertThat(this.memberService.listRoster("sky", 4, null)).extracting(Member::getId)
	.containsExactly(40L, 100L, 20L, 10L, 30L, 90L, 80L);
```
Keep the two existing `isEmpty()` assertions.

- [ ] **Step 2: In `MemberControllerDBTests.list`, set kana on `member1` and assert it reaches the DTO**

`member1.setFirstnameKana("めんばー");`, `member1.setLastnameKana("らすと");` (won't compile until Step 4), and after the "get all" call:
```java
assertThat(result.getContent().get(0).firstnameKana()).isEqualTo("めんばー");
assertThat(result.getContent().get(0).lastnameKana()).isEqualTo("らすと");
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./mvnw test -Dtest=MemberServiceDBTests,MemberControllerDBTests`
Expected: compilation failure (`setFirstnameKana` not found).

- [ ] **Step 4: Implement**

- `schema.sql`: add the two columns after `lastname` (aligned like neighbors).
- `Member`: `private String firstnameKana; private String lastnameKana;` after `lastname`, with getters/setters (no `@Column`, like `firstnameEn`).
- `MemberDto`: `String firstnameKana, String lastnameKana` after `lastname`.
- `MemberRepository.findRoster`: `order by m.classNumber, m.lastnameKana, m.firstnameKana, m.lastnameEn, m.firstnameEn`.
- `MemberController.roster` description: `"Optionally filtered by class. Ordered by class, lastnameKana, firstnameKana, lastnameEn, firstnameEn."`

- [ ] **Step 5: Run tests to verify they pass**

Run: `./mvnw test -Dtest=MemberServiceDBTests,MemberControllerDBTests`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/schema.sql src/main/java/net/ttddyy/book/libraryservice/member src/test/java/net/ttddyy/book/libraryservice/member
git commit -m "Add member name kana and sort roster by kana"
```

### Task 2: Kana in checkout and checkout-history responses

**Files:**
- Modify: `src/main/java/net/ttddyy/book/libraryservice/checkout/CheckoutDto.java`
- Modify: `src/main/java/net/ttddyy/book/libraryservice/checkout/CheckoutMapper.java`
- Modify: `src/main/java/net/ttddyy/book/libraryservice/checkout/history/CheckoutHistoryDto.java` (nested `Member`)
- Test: `src/test/java/net/ttddyy/book/libraryservice/checkout/CheckoutMapperTests.java`
- Test: `src/test/java/net/ttddyy/book/libraryservice/checkout/history/CheckoutHistoryMapperTests.java`

**Interfaces:**
- Consumes: `Member#getFirstnameKana()`, `Member#getLastnameKana()` (Task 1).
- Produces: `CheckoutDto` components `memberFirstNameKana`, `memberLastNameKana` after `memberLastName`; `CheckoutHistoryDto.Member(String firstname, String lastname, String firstnameKana, String lastnameKana)`.

- [ ] **Step 1: Extend mapper tests**

`CheckoutMapperTests.map`: `member.setFirstnameKana("foo-firstname-kana"); member.setLastnameKana("foo-lastname-kana");` and
```java
assertThat(dto.memberFirstNameKana()).isEqualTo("foo-firstname-kana");
assertThat(dto.memberLastNameKana()).isEqualTo("foo-lastname-kana");
```
`CheckoutHistoryMapperTests.map`: `member.setFirstnameKana("bar-firstname-kana"); member.setLastnameKana("bar-lastname-kana");` and inside the `dto.member()` block:
```java
assertThat(memberDto.firstnameKana()).isEqualTo("bar-firstname-kana");
assertThat(memberDto.lastnameKana()).isEqualTo("bar-lastname-kana");
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=CheckoutMapperTests,CheckoutHistoryMapperTests`
Expected: compilation failure (`memberFirstNameKana()` not found).

- [ ] **Step 3: Implement**

- `CheckoutDto`: add `String memberFirstNameKana, String memberLastNameKana` after `memberLastName`.
- `CheckoutMapper.toDto`: `@Mapping(target = "memberFirstNameKana", source = "member.firstnameKana")`, `@Mapping(target = "memberLastNameKana", source = "member.lastnameKana")`.
- `CheckoutHistoryDto.Member`: add `String firstnameKana, String lastnameKana`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -Dtest=CheckoutMapperTests,CheckoutHistoryMapperTests`
Expected: PASS

- [ ] **Step 5: Run the full suite**

Run: `./mvnw test`
Expected: BUILD SUCCESS (catches any JSON-shape assertions in `CheckoutControllerMockMvcTests` / `CheckoutHistoryControllerMockMvcTests`; update those only if they assert the full member field set).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/ttddyy/book/libraryservice/checkout src/test/java/net/ttddyy/book/libraryservice/checkout
git commit -m "Expose member kana in checkout responses"
```
