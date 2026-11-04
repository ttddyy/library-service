# Member Roster API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/members/roster?schoolId=&grade=&classNumber=` returning active members of a school's grade (optionally one class) for the UI picker.

**Architecture:** One JPQL query in `MemberRepository`, a pass-through `MemberService` method, and a controller method beside `/api/members/classes`. Unpaged `List<MemberDto>`.

**Tech Stack:** Spring Boot 4.1, Spring Data JPA (Hibernate), PostgreSQL 17, JUnit 5 + AssertJ, `@DataJpaTest` with `@DbTest`.

**Spec:** `docs/superpowers/specs/2026-11-03-member-roster-design.md`

## Global Constraints

- Path: `/api/members/roster`; params `schoolId` (String, required), `grade` (int, required), `classNumber` (Integer, optional).
- Active members only (`m.active = true`).
- Order: `classNumber`, `lastnameEn`, `firstnameEn`.
- Response: `List<MemberDto>`, unpaged.
- `GET /api/members` unchanged. No security config change (`/api/**` → `ROLE_USER` covers it).

## Review Focus

1. `classNumber` omitted → query must not fail binding a null param (`could not determine data type of parameter` on Postgres) and must return the whole grade — pinned by `listRoster` grade-only assertion.
2. Members with `class_number` null in the grade, when `classNumber` omitted → included, sorted last (Postgres default for ASC) — pinned in `listRoster`.
3. Inactive member in the requested class → excluded — pinned in `listRoster`.
4. Same grade/class in another school → excluded — pinned in `listRoster`.
5. Unknown school or empty grade → empty list, not error — pinned in `listRoster`.

---

### Task 1: Roster query and service method

**Files:**
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/MemberRepository.java`
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/MemberService.java`
- Test: `src/test/java/net/ttddyy/book/libraryservice/member/MemberServiceDBTests.java`

**Interfaces:**
- Produces: `List<Member> MemberRepository.findRoster(String schoolId, int grade, @Nullable Integer classNumber)`; `List<Member> MemberService.listRoster(String schoolId, int grade, @Nullable Integer classNumber)`.

- [ ] **Step 1: Write the failing test** `listRoster()` in `MemberServiceDBTests`, inserting via `jdbcTemplate` like `listClasses()`:

```java
String sql = """
			INSERT INTO members (id, firstname_en, lastname_en, school_id, grade, class_number, active)
			VALUES	(10, 'b', 'smith', 'sky', 4, 2, true),
					(20, 'a', 'smith', 'sky', 4, 2, true),
					(30, 'z', 'adams', 'sky', 4, 2, true),
					(40, 'c', 'brown', 'sky', 4, 1, true),
					(50, 'd', 'davis', 'sky', 4, 2, false),
					(60, 'e', 'evans', 'sky', 3, 2, true),
					(70, 'f', 'fox', 'ocean', 4, 2, true),
					(80, 'g', 'green', 'sky', 4, null, true);
		""";
this.jdbcTemplate.update(sql);

// grade + class: active, this school, ordered by lastnameEn, firstnameEn
assertThat(this.memberService.listRoster("sky", 4, 2)).extracting(Member::getId).containsExactly(30L, 20L, 10L);
// grade only: grouped by class, null class last
assertThat(this.memberService.listRoster("sky", 4, null)).extracting(Member::getId)
	.containsExactly(40L, 30L, 20L, 10L, 80L);
assertThat(this.memberService.listRoster("unknown", 4, null)).isEmpty();
assertThat(this.memberService.listRoster("sky", 9, null)).isEmpty();
```

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw test -Dtest=MemberServiceDBTests#listRoster`
Expected: compilation failure, `listRoster` not found.

- [ ] **Step 3: Implement** `findRoster` as a `@Query` on `MemberRepository` (same text-block style as `findClasses`):
`where m.schoolId = :schoolId and m.grade = :grade and (:classNumber is null or m.classNumber = :classNumber) and m.active = true order by m.classNumber, m.lastnameEn, m.firstnameEn`.
`MemberService.listRoster` delegates.

- [ ] **Step 4: Run to verify it passes**

Run: `./mvnw test -Dtest=MemberServiceDBTests`
Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/ttddyy/book/libraryservice/member/MemberRepository.java src/main/java/net/ttddyy/book/libraryservice/member/MemberService.java src/test/java/net/ttddyy/book/libraryservice/member/MemberServiceDBTests.java
git commit -m "Add member roster query"
```

### Task 2: Roster endpoint

**Files:**
- Modify: `src/main/java/net/ttddyy/book/libraryservice/member/MemberController.java`
- Test: `src/test/java/net/ttddyy/book/libraryservice/member/MemberControllerDBTests.java`

**Interfaces:**
- Consumes: `MemberService.listRoster(String, int, @Nullable Integer)`.
- Produces: `List<MemberDto> MemberController.roster(String schoolId, int grade, @Nullable Integer classNumber)` mapped to `GET /api/members/roster`.

- [ ] **Step 1: Write the failing test** `roster()` in `MemberControllerDBTests`, persisting via `entityManager` like `classes()`: two active members of `school-A` grade 1 class 3 (ids 10, 20; lastnameEn `last-2`, `last-1`) and one of class 4 (id 30).

```java
assertThat(this.controller.roster("school-A", 1, 3)).extracting(MemberDto::id).containsExactly(20L, 10L);
assertThat(this.controller.roster("school-A", 1, null)).extracting(MemberDto::id).containsExactly(20L, 10L, 30L);
```

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw test -Dtest=MemberControllerDBTests#roster`
Expected: compilation failure, `roster` not found.

- [ ] **Step 3: Implement** `@GetMapping("/api/members/roster")` with `@Operation(summary = "List active members of a school grade", description = "Optionally filtered by class. Ordered by class, lastnameEn, firstnameEn.")`; params `@RequestParam String schoolId, @RequestParam int grade, @RequestParam(required = false) @Nullable Integer classNumber`. Map with `MemberMapper.INSTANCE` (add a `List<MemberDto> toDtoList(List<Member>)` method to `MemberMapper` only if none exists; otherwise stream + `toDto`). Place it before `/api/members/{id}`.

- [ ] **Step 4: Run to verify it passes**

Run: `./mvnw test -Dtest='Member*'`
Expected: all PASS (including `MemberApiTests`).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/ttddyy/book/libraryservice/member/ src/test/java/net/ttddyy/book/libraryservice/member/MemberControllerDBTests.java
git commit -m "Add GET /api/members/roster endpoint"
```
