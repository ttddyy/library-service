# JWT Authentication Design

Date: 2026-10-17
Status: Draft (pending review)
Branch: `auth` (builds on WIP commit `dc48f75` "WIP: jdbc auth")

## Goal

Replace HTTP Basic authentication with self-issued JWT access tokens and rotating refresh tokens so the browser
UI never retains the user's password. Users can belong to multiple schools; the UI chooses which school to work on.

## Decisions

| Topic | Decision |
|---|---|
| Approach | Option 2b: self-issued JWTs (no external IdP / OAuth server) |
| Account store | Existing `JdbcUserDetailsManager` (`users`, `authorities`) + BCrypt; used only at login/refresh |
| Access token | JWT, RS256, lifetime 5 min (configurable), sent as `Authorization: Bearer <token>` |
| Refresh token | Opaque random value, HttpOnly cookie only, rotated on every refresh; **absolute** lifetime of 1 day from login (configurable), not extended by refreshes, so each library day starts with a login |
| Roles | Global (not per school): `ROLE_USER`, `ROLE_SCHOOL_ADMIN`, new `ROLE_SUPER_ADMIN` |
| Role hierarchy | `SUPER_ADMIN > SCHOOL_ADMIN > USER` |
| Schools | Many-to-many via `user_schools`; `SUPER_ADMIN` has implicit access to every school |
| Keys | RSA key pair mounted from a Kubernetes Secret; same pair on every pod |

## Endpoints

All under `/api/auth/**`, `permitAll` in the existing security filter chain.

### `POST /api/auth/login`

Request:
```http
POST /api/auth/login
Content-Type: application/json

{"username": "...", "password": "..."}
```

Response `200`:
```json
{
  "access_token": "<jwt>",
  "token_type": "Bearer",
  "expires_in": 300,
  "user": {
    "username": "ocean-admin",
    "roles": ["SCHOOL_ADMIN"],
    "schools": ["ocean"]
  }
}
```
plus `Set-Cookie` with the refresh token (see Cookie). For `SUPER_ADMIN`, `user.schools` lists all schools from
the `schools` table.

Failure (bad credentials or disabled user): `401` with a generic message; no detail about which part failed.

### `POST /api/auth/refresh`

Request: no body; refresh token comes from the cookie. Must include header `X-Requested-With` (any value).

Response `200`: same as login but without `user`; rotated refresh token in a new `Set-Cookie`.

Failure (missing, unknown, expired, revoked, or reused token; user disabled or deleted; missing header): `401`
and the cookie is cleared. UI redirects to login.

### `POST /api/auth/logout`

Request: no body; must include `X-Requested-With`. Revokes the presented refresh token's family and clears the
cookie. Always returns `204`, even if the cookie is missing or already invalid.

### Cookie

```
refresh_token=<value>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=<seconds until the family expires>
```
Cleared with the same attributes and `Max-Age=0`. `Secure` is configurable so local HTTP development works.

### CSRF

CSRF stays disabled globally (Bearer-token APIs are not CSRF-prone). The two cookie-authenticated endpoints
(`refresh`, `logout`) are protected by `SameSite=Strict` plus the required `X-Requested-With` header, which a
cross-site form cannot set and which forces a CORS preflight for cross-origin scripts.

## Access Token (JWT)

Header: `alg=RS256`, `kid=<configured key id>`.

Claims:

| Claim | Value |
|---|---|
| `iss` | configured issuer (e.g. `library-service`) |
| `sub` | username |
| `iat`, `exp` | issued-at, expiry |
| `jti` | random UUID |
| `roles` | role names without prefix, e.g. `["SCHOOL_ADMIN"]` |
| `schools` | school IDs from `user_schools`; omitted for `SUPER_ADMIN` |

Validation on every API request (Spring OAuth2 Resource Server): signature with the public key, `exp`/`nbf`
(default clock skew), and `iss`. A `JwtAuthenticationConverter` maps `roles` to `ROLE_*` authorities and uses
`sub` as the principal name, so `authentication.getName()` keeps returning the username.

Expired/invalid tokens produce `401` with `WWW-Authenticate: Bearer error="invalid_token"` (Spring default).

Claims can be stale for at most the access-token lifetime; refresh re-reads everything from the database.

## Refresh Token Lifecycle

Value: 32 bytes from `SecureRandom`, Base64URL-encoded. Only its SHA-256 hash is stored.

New table:
```sql
CREATE TABLE refresh_tokens
(
    token_hash  VARCHAR(64) NOT NULL PRIMARY KEY, -- hex SHA-256
    username    VARCHAR(50) NOT NULL,
    family_id   UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at     TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT fk_refresh_tokens__username FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens__family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens__expires_at ON refresh_tokens (expires_at);
```

- **Login:** new `family_id`, insert token row.
- **Refresh**, in one transaction with the row locked (`SELECT ... FOR UPDATE`):
  - Not found, expired, or `revoked_at` set → `401`.
  - `used_at` already set → **reuse detected**: revoke the whole family (`revoked_at = now()` on all rows) →
    `401`.
  - Otherwise: set `used_at`; reload the user (`enabled`, authorities, schools) — disabled or missing → revoke
    family, `401`; insert a new token with the same `family_id` and the **same `expires_at`** as the presented
    token (absolute session lifetime); issue a new access token.
- **Logout:** revoke the family of the presented token.
- **Cleanup:** `@Scheduled` job (hourly) runs `DELETE FROM refresh_tokens WHERE expires_at < now()`. Runs on
  every pod; idempotent, so no locking needed. Requires `@EnableScheduling`.

- **Concurrent sessions:** each login creates an independent family, so any number of PCs can be logged in
  with the same (shared) account at once. Reuse detection and logout on one PC affect only that PC's family.

No grace window for concurrent refreshes: the UI must single-flight refresh calls (see UI contract and Open
Items).

### Revoking sessions

Changing a user's password or disabling them does not, by itself, end existing sessions immediately (disabling
takes effect on the next refresh). When rotating a shared account's password, revoke all of that user's
sessions; the README documents:
```sql
UPDATE refresh_tokens SET revoked_at = now() WHERE username = '<username>' AND revoked_at IS NULL;
```
Logged-in PCs then fail on their next refresh (within the access-token lifetime) and must log in again.

## School Scoping

### Schema change

`user_schools` primary key becomes `(username, school_id)` so a user can have multiple schools:
```sql
CREATE TABLE user_schools
(
    username  VARCHAR(50) NOT NULL,
    school_id VARCHAR(8)  NOT NULL,
    CONSTRAINT pk_user_schools PRIMARY KEY (username, school_id),
    CONSTRAINT fk_user_schools__username FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE,
    CONSTRAINT fk_user_schools__school_id FOREIGN KEY (school_id) REFERENCES schools (id)
);
```

### `SchoolAccountAccess`

- Methods take `Authentication` instead of `String username`.
- Allowed schools come from the JWT `schools` claim (no `user_schools` query per request).
- `ROLE_SUPER_ADMIN` bypasses all school checks.
- `requireSchool(auth, schoolId)`: `schoolId` must be in the claim.
- `requireBooks/requireMember/requireCheckoutLimitDefaults/requireCheckoutLimitSchedules`: each resource's
  `school_id` (still looked up in the DB) must be in the claim.
- `schoolId(username)` (pick-the-first-school) is removed.

### Endpoints that inferred the school

These now require an explicit `schoolId` request parameter, checked with `requireSchool`:
- `GET /api/checkouts/limits/defaults?schoolId=...`
- `GET /api/checkouts/limits/schedules?schoolId=...` (the existing `/{schoolId}` path variant stays)

This is a breaking API change for the UI.

## Security Configuration

- Add dependency `spring-boot-starter-security-oauth2-resource-server`.
- `http.oauth2ResourceServer(o -> o.jwt(...))` with the custom `JwtAuthenticationConverter`.
- `RoleHierarchy` bean: `ROLE_SUPER_ADMIN > ROLE_SCHOOL_ADMIN > ROLE_USER`. Existing `hasRole(...)` rules stay unchanged.
- `/api/auth/**` → `permitAll`.
- Session policy `STATELESS`.
- `AuthenticationManager` bean built from `DaoAuthenticationProvider(JdbcUserDetailsManager, BCrypt)` for login.
- `JwtEncoder` = `NimbusJwtEncoder` from the RSA key pair; `JwtDecoder` = `NimbusJwtDecoder.withPublicKey(...)`.
- `httpBasic()` remains during the UI migration and is removed in the final step.
- CORS: if `library-service.auth.cors.allowed-origins` is set, enable CORS for `/api/**` with
  `allowCredentials(true)` and those origins. Not configured → no CORS (same-origin deployment).

## Configuration

`LibraryServiceProperties` gains an `auth` section:

```properties
library-service.auth.issuer=library-service
library-service.auth.access-token-ttl=5m
library-service.auth.refresh-token-ttl=1d
library-service.auth.cookie-secure=true
library-service.auth.jwt.key-id=key-1
library-service.auth.jwt.private-key=file:/etc/library-service/jwt/private.pem
library-service.auth.jwt.public-key=file:/etc/library-service/jwt/public.pem
library-service.auth.cors.allowed-origins=
```

The application fails at startup if the keys are missing or invalid.

### Local development (`dev` profile)

A fixed, committed, **non-secret** key pair lets developers run the app and keep tokens valid across restarts.

- Key files at the repository root: `dev/jwt/private.pem`, `dev/jwt/public.pem` (generated once with `openssl`,
  committed). They live outside `src/main/resources`, so they are **not packaged into the jar/image**; activating
  `dev` in a deployed container fails at startup because the files don't exist.
- `src/main/resources/application-dev.properties`:
  ```properties
  library-service.auth.issuer=library-service-dev
  library-service.auth.cookie-secure=false
  library-service.auth.jwt.key-id=dev
  library-service.auth.jwt.private-key=file:dev/jwt/private.pem
  library-service.auth.jwt.public-key=file:dev/jwt/public.pem
  # e.g. a local UI dev server
  # library-service.auth.cors.allowed-origins=http://localhost:5173
  ```
  Paths are relative to the project root, the working directory for `spring-boot:run`, `spring-boot:test-run`
  and Maven tests.
- Activation:
  - `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
  - `./mvnw spring-boot:test-run` — `TestLibraryServiceApplication` adds the `dev` profile automatically.
- When the `dev` profile is active, the app logs a `WARN` at startup that it is using the development JWT keys.
- Because the dev issuer differs and prod verifies with its own public key, dev-signed tokens are rejected in
  production.
- Tests that need real signing/verification (token issuer, end-to-end) use `@ActiveProfiles("dev")`; MockMvc
  security slice tests use spring-security-test's `jwt()` and need no keys.

Kubernetes: a Secret holds `private.pem` and `public.pem` (PKCS#8 / X.509 PEM), mounted read-only at
`/etc/library-service/jwt`. Key rotation is out of scope, but the `kid` header leaves room for it.

## UI Contract (summary)

- Keep the access token in memory only (not `localStorage`/`sessionStorage`).
- Send `Authorization: Bearer <access_token>` on API calls.
- On `401` with `error="invalid_token"` (or shortly before `expires_in`), call `/api/auth/refresh` **once**;
  other requests wait for that single call, then retry with the new token.
- Refresh `401` → go to the login page.
- `refresh`/`logout` must send `X-Requested-With`; if cross-origin, use `credentials: 'include'`.
- Send the selected `schoolId` on school-scoped requests.

## Error Handling

- Auth endpoint failures return `401` with a JSON body `{"error": "<code>"}` (`invalid_credentials`,
  `invalid_refresh_token`). No stack traces or internals.
- Resource checks keep throwing `AccessDeniedException` → `403`.

## Testing

- Unit: token issuer (claims, `kid`, expiry), refresh-token service (rotation, reuse → family revoked, expiry,
  disabled user, cleanup), `SchoolAccountAccess` (multi-school, super-admin bypass), roles converter.
- MockMvc: login/refresh/logout (cookie attributes, missing header → 401, response shape).
- Existing security tests (`CheckoutSecurityTests`, `JdbcSecurityTests`, `CheckoutLimitDefaultSecurityTests`,
  `CheckoutLimitScheduleSecurityTests`, `SchoolAccountAccessTests`) switch from `httpBasic`/mock users to
  `jwt()` with `roles`/`schools` claims.
- One end-to-end test: login → call API with token → refresh → old refresh token reused → 401.

## Documentation

README: replace the Basic-auth section with the login flow, running locally with the `dev` profile, key generation (`openssl` commands), Kubernetes
Secret mounting, new properties, the `user_schools` / `refresh_tokens` DDL migration for existing databases, and
how to provision a `ROLE_SUPER_ADMIN` account, and the SQL to revoke all sessions for a user. Swagger: add a bearer security scheme.

## Open Items

- **Multiple tabs on one PC** share a refresh-token family; uncoordinated parallel refreshes look like reuse and
  log the PC out. Preferred: the UI single-flights refresh across tabs (Web Locks API / `BroadcastChannel`).
  Fallback if the UI cannot: a short server-side grace window (~10 s) that tolerates reuse of a just-rotated
  token. To be decided with the UI team.

## Out of Scope

- Login rate limiting / account lockout (follow-up).
- Signing-key rotation with multiple verification keys.
- Account-management endpoints.
- Per-school roles.
- School scoping for endpoints that are not school-scoped today (books, members, categories reads).
