# Member roster API — design

## Intent
UI picker for finding a student: a member says "Ocean school, grade 4, class 1"; staff narrow by grade, then class, then pick the student. Grade/class options come from the existing `GET /api/members/classes`; this adds the student list.

## Goals
- `GET /api/members/roster?schoolId=&grade=&classNumber=` returns members of a school's grade, optionally one class.
- `schoolId` and `grade` required; `classNumber` optional (omitted → whole grade).
- Active members only (matches `/api/members/classes`, so picker options and list agree).
- Plain `List<MemberDto>`, unpaged (a grade is at most a few hundred).
- Order: `classNumber`, `lastnameEn`, `firstnameEn`.

## Non-goals
- No change to `GET /api/members` (paging, inactive members, existing callers untouched).
- No name search, no school-name→id mapping.

## Approaches
- A. Add `grade`/`classNumber` to `GET /api/members` — smallest diff, but paged and includes inactive members; UI would pass big page size and sort; making it active-only changes existing behavior.
- **B. New `/api/members/roster` endpoint — chosen.** Exactly what the picker needs in one call; sits beside `/api/members/classes`; no existing behavior changes.
- C. `/api/schools/{schoolId}/grades/{grade}/members` — reads well, but all member endpoints live under `/api/members`.

## How
- `MemberRepository`: one JPQL `@Query` with `(:classNumber is null or m.classNumber = :classNumber)`, `m.active = true`, `order by m.classNumber, m.lastnameEn, m.firstnameEn`.
- `MemberService.listRoster(schoolId, grade, classNumber)` delegates.
- `MemberController`: new `@GetMapping("/api/members/roster")` with `@Operation`, mapped via `MemberMapper.INSTANCE`.
- Security: covered by existing `/api/**` → `ROLE_USER` rule; no change.
- Tests: add cases to existing `MemberServiceDBTests` / `MemberControllerDBTests` (grade only, grade+class, inactive excluded, ordering).

## Left to the builder
- Exact test data and test placement within the existing member test classes.
