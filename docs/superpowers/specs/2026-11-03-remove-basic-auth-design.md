# Remove HTTP Basic Authentication

## Intent

Finish the JWT migration: the API accepts only JWT bearer tokens. HTTP Basic was marked temporary in
`2026-10-17-jwt-auth-design.md`. No external client (monitoring, scripts, other services) uses Basic.

## Goals / Non-goals

- Goal: Basic credentials no longer authenticate any request (`/api/**` or actuator).
- Non-goal: changing login. `UserDetailsService` (`JdbcUserDetailsManager`) and `AuthenticationManager` stay;
  `POST /api/auth/login` uses them to verify passwords.
- Anti-goal: reintroducing a Basic challenge (`WWW-Authenticate: Basic`) or a browser login popup.

## Approaches

- **Delete `http.httpBasic(...)` (chosen).** Spring Security does not enable Basic unless configured, so removing
  the call is enough. The `BearerTokenAuthenticationEntryPoint` on `exceptionHandling` stays.
- Explicit `httpBasic(AbstractHttpConfigurer::disable)`: same effect, an extra line. Rejected as redundant.

## Changes

- `ApplicationSecurityConfiguration`: remove the `httpBasic` line.
- `SecurityRulesTests`: replace `basicWithJdbcUserWorks` and `badBasicCredentialsSendNoBasicChallenge` with one
  test: valid Basic credentials for a real JDBC user on `GET /api/books` → `401`, no `Basic` challenge header.
  Drop the then-unused `httpBasic` import.
- Delete `member/MemberApiTests.java` (its only test, `abc()`, is a scratch test calling `/members/0` with Basic
  and printing the result).
- `2026-10-17-jwt-auth-design.md`: one-line note that Basic was removed (link to this doc).

## Left to the builder

Test naming and exact assertion style, following the existing `SecurityRulesTests` helpers.
