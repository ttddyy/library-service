# Remove HTTP Basic Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Basic credentials no longer authenticate any request; only JWT bearer tokens do.

**Architecture:** Delete the `httpBasic` call from the security filter chain (Spring does not enable Basic otherwise). Keep `UserDetailsService`/`AuthenticationManager` for `POST /api/auth/login`.

**Tech Stack:** Spring Boot, Spring Security, MockMvc, Testcontainers, Maven (`./mvnw`).

**Spec:** `docs/superpowers/specs/2026-11-03-remove-basic-auth-design.md`

## Global Constraints

- Unauthenticated responses carry a Bearer challenge only; never `WWW-Authenticate: Basic`.
- `UserDetailsService` and `AuthenticationManager` beans stay unchanged.

## Review Focus

- Login still works: `POST /api/auth/login` with valid username/password must still succeed (it uses `AuthenticationManager`, not the Basic filter). Covered by existing auth tests; Task 1 Step 4 runs the full suite.
- Actuator with Basic credentials must be `401`, same as `/api/**`. Covered by the same filter chain; no extra test.

---

### Task 1: Remove Basic from the filter chain

**Files:**
- Modify: `src/main/java/net/ttddyy/book/libraryservice/ApplicationSecurityConfiguration.java:87`
- Modify: `src/test/java/net/ttddyy/book/libraryservice/SecurityRulesTests.java:211-225`
- Delete: `src/test/java/net/ttddyy/book/libraryservice/member/MemberApiTests.java`
- Modify: `docs/superpowers/specs/2026-10-17-jwt-auth-design.md:204`

**Interfaces:**
- Consumes: existing `SecurityRulesTests.insertUser()` (inserts `sec-user` / `secret`).

- [ ] **Step 1: Replace the two Basic tests with one failing test**

Remove `basicWithJdbcUserWorks` and `badBasicCredentialsSendNoBasicChallenge`; add:

```java
@Test
void basicCredentialsRejected() throws Exception {
	insertUser();
	var response = this.mvc.perform(MockMvcRequestBuilders.get("/api/books").with(httpBasic("sec-user", "secret")))
		.andExpect(status().isUnauthorized())
		.andReturn()
		.getResponse();
	assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).noneMatch((value) -> value.startsWith("Basic"));
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./mvnw test -Dtest=SecurityRulesTests#basicCredentialsRejected`
Expected: FAIL — status is not 401 (valid Basic credentials currently authenticate).

- [ ] **Step 3: Delete `http.httpBasic(basic -> basic.authenticationEntryPoint(entryPoint));`** from `ApplicationSecurityConfiguration`. Delete `MemberApiTests.java`. In the JWT spec, change line 204 to: `` - `httpBasic()` was removed; see `2026-11-03-remove-basic-auth-design.md`. ``

- [ ] **Step 4: Run the full suite**

Run: `./mvnw test`
Expected: BUILD SUCCESS, `basicCredentialsRejected` passes.

- [ ] **Step 5: Commit**

```bash
git add -A src docs/superpowers/specs/2026-10-17-jwt-auth-design.md
git commit -m "Remove HTTP Basic authentication"
```
