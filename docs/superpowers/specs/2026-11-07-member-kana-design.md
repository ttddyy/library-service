# Member Name Kana

## Intent

Members get kana readings of their names (`firstname_kana`, `lastname_kana`), exposed wherever member names are
exposed, and used as the primary roster sort key. The outside batch job that populates `members` will backfill
kana after the schema change, so the columns start nullable.

## Goals / Non-goals

- Goal: kana in `members`, `Member`, `MemberDto`, `CheckoutDto`, `CheckoutHistoryDto.Member`.
- Goal: roster ordered by `classNumber, lastnameKana, firstnameKana, lastnameEn, firstnameEn`.
- Non-goal: `NOT NULL` on the kana columns. That is a later change once the backfill is done.
- Non-goal: changing the batch job, or restructuring existing DTO fields.
- Anti-goal: breaking existing API clients (only additive fields).

## Approaches

- **Follow each DTO's existing shape (chosen).** Flat `memberFirstNameKana`/`memberLastNameKana` on `CheckoutDto`
  with explicit `@Mapping`; `firstnameKana`/`lastnameKana` on `CheckoutHistoryDto.Member` (auto-mapped). Additive.
- Nest `CheckoutDto`'s member fields into a `member` object like `CheckoutHistoryDto`. Rejected: renames fields
  clients already use.

## Changes

- `schema.sql`: `firstname_kana VARCHAR(64)`, `lastname_kana VARCHAR(64)` (nullable), after `lastname`.
  Existing databases (no migration tool) need, before the backfill:
  `ALTER TABLE members ADD COLUMN firstname_kana VARCHAR(64), ADD COLUMN lastname_kana VARCHAR(64);`
- `Member`: `firstnameKana`, `lastnameKana` with getters/setters (default column naming, like `firstnameEn`).
- `MemberDto`: `firstnameKana`, `lastnameKana` after `lastname`. `MemberMapper` unchanged (name-based).
- `MemberRepository.findRoster`: `order by m.classNumber, m.lastnameKana, m.firstnameKana, m.lastnameEn,
  m.firstnameEn`. Postgres default puts NULL kana last within a class (no explicit `NULLS` clause).
  `MemberController.roster` description updated to match.
- `CheckoutDto`: `memberFirstNameKana`, `memberLastNameKana` after `memberLastName`; `CheckoutMapper` gets two
  `@Mapping` lines.
- `CheckoutHistoryDto.Member`: `firstnameKana`, `lastnameKana`.

## Tests

- Existing raw `INSERT INTO members` statements (13, in 6 classes) stay valid since the columns are nullable;
  only touch them where a test asserts kana.
- Roster (`MemberServiceDBTests` / `MemberControllerDBTests`): kana order wins over en order, en breaks kana ties,
  a member without kana sorts after those with kana in the same class.
- `CheckoutMapperTests`, `CheckoutHistoryMapperTests`: kana mapped. Member DTO output includes kana.
- Any test constructing `MemberDto`, `CheckoutDto`, or `CheckoutHistoryDto.Member` positionally is updated.

## Left to the builder

Test data values and naming, following existing test helpers.
