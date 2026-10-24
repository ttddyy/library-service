# JWT Authentication Design (Spec A)

Date: 2026-10-23
Status: Draft (pending review)
Branch: `auth-new`

## Goal

Replace the single Basic-auth user with per-user login backed by Spring Security's JDBC user store. The UI logs in
once, calls the API with short-lived RS256 access tokens, and silently renews them with a rotating refresh token held
in an HttpOnly cookie. Roles gate write operations. Tokens carry the user's schools so per-school enforcement
(Spec B) can build on them.

## Decisions

| Topic | Decision |
|---|---|
| Token model | Self-issued JWTs (no external IdP); access token in `Authorization: Bearer`, refresh token in HttpOnly cookie |
| Signing | RS256; key pair from PEM files (Kubernetes Secret mount); stateless validation on every pod |
| Library | `spring-boot-starter-oauth2-resource-server` (`NimbusJwtDecoder` to validate, `NimbusJwtEncoder` to issue) |
| User store | Spring Security JDBC schema (`users`, `authorities`) via `JdbcUserDetailsManager`; BCrypt passwords |
| Schools | Many-to-many `user_schools`; `SUPER_ADMIN` has implicit access to all schools |
| Roles | Global: `ROLE_USER`, `ROLE_SCHOOL_ADMIN`, `ROLE_SUPER_ADMIN`; hierarchy `SUPER_ADMIN > SCHOOL_ADMIN > USER` |
| Access token TTL | Default `15m`, configurable |
| Refresh token TTL | Default `1d`, configurable; **absolute** from login (rotation does not extend it) |
| Rotation | Every refresh rotates; reuse of a rotated token revokes its family, except within a grace window (default `10s`) |
| Sessions | Each login starts an independent family, so one user may be logged in on several machines |
| Deployment | UI and API share one origin; no CORS |
| HTTP Basic | Kept temporarily, authenticating against JDBC users; removed in a later task |
| Expired rows | Deleted on each login; no scheduler |

## Data Model

Added to `src/main/resources/schema.sql`; the README documents the same DDL for existing databases.

```sql
CREATE TABLE users
(
    username VARCHAR(50)  NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled  BOOLEAN      NOT NULL
);

CREATE TABLE authorities
(
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities__users FOREIGN KEY (username) REFERENCES users (username),
    CONSTRAINT uc_authorities__username_authority UNIQUE (username, authority)
);

CREATE TABLE user_schools
(
    username  VARCHAR(50) NOT NULL,
    school_id VARCHAR(8)  NOT NULL,
    PRIMARY KEY (username, school_id),
    CONSTRAINT fk_user_schools__users FOREIGN KEY (username) REFERENCES users (username),
    CONSTRAINT fk_user_schools__schools FOREIGN KEY (school_id) REFERENCES schools (id)
);

CREATE TABLE refresh_tokens
(
    token_hash CHAR(64)    NOT NULL PRIMARY KEY,
    family_id  UUID        NOT NULL,
    username   VARCHAR(50) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    rotated_at TIMESTAMPTZ,
    CONSTRAINT fk_refresh_tokens__users FOREIGN KEY (username) REFERENCES users (username)
);
CREATE INDEX idx_refresh_tokens__family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens__expires_at ON refresh_tokens (expires_at);
```

- Passwords are stored with the `{bcrypt}` prefix (`DelegatingPasswordEncoder`).
- Authorities are stored with the `ROLE_` prefix.
- `SUPER_ADMIN` users need no `user_schools` rows.
- `refresh_tokens` stores only the lowercase hex SHA-256 of each token. Revocation means deleting the family's rows;
  there is no `revoked` flag.

## Configuration

Bound into `LibraryServiceProperties` under `library-service.auth`:

| Property | Default | Notes |
|---|---|---|
| `issuer` | `library-service` | `iss` claim; validated on decode |
| `access-token-ttl` | `15m` | `Duration` |
| `refresh-token-ttl` | `1d` | `Duration`; absolute session length |
| `refresh-reuse-grace` | `10s` | `Duration` |
| `cookie-secure` | `true` | `Secure` attribute on the refresh cookie |
| `jwt.key-id` | (required) | JWS header `kid` |
| `jwt.private-key` | (required) | `Resource`, PKCS#8 PEM, e.g. `file:/etc/library-service/jwt/private.pem` |
| `jwt.public-key` | (required) | `Resource`, X.509 PEM, e.g. `file:/etc/library-service/jwt/public.pem` |

`spring.security.user.*` is removed from `application.properties`.

### Local development

`src/main/resources/application-dev.properties`:

```properties
library-service.auth.cookie-secure=false
library-service.auth.jwt.key-id=dev
library-service.auth.jwt.private-key=file:dev/jwt/private.pem
library-service.auth.jwt.public-key=file:dev/jwt/public.pem
```

The dev key pair is generated with `openssl` and committed under `dev/jwt/` at the repo root, never under
`src/main/resources`. Tests use the same pair.

## Auth Endpoints

New package `net.ttddyy.book.libraryservice.auth`: `AuthController`, `AuthService`, `AccessTokenIssuer`,
`RefreshTokenService`, `JwtConfiguration`. All endpoints are `permitAll`. All time comes from the existing `Clock` bean.

### `POST /api/auth/login`

Request body: `{"username": "...", "password": "..."}`.

1. Authenticate through an `AuthenticationManager` backed by `JdbcUserDetailsManager`.
2. Load the user's schools from `user_schools` (ordered by `school_id`). For `SUPER_ADMIN`, load all school IDs from
   `schools` for the response body.
3. Delete expired refresh tokens: `DELETE FROM refresh_tokens WHERE expires_at <= now`.
4. Start a new family: insert a refresh token with a new `family_id` and `expires_at = now + refresh-token-ttl`.

Response `200`:

```json
{
  "access_token": "<jwt>",
  "token_type": "Bearer",
  "expires_in": 900,
  "user": {
    "username": "ocean-admin",
    "roles": ["SCHOOL_ADMIN"],
    "schools": ["ocean"]
  }
}
```

plus the refresh cookie (see Cookie).

Unknown user, wrong password, disabled user, malformed body, or missing or blank fields all return the identical
`401 {"error":"invalid_credentials"}`.

### `POST /api/auth/refresh`

Requires header `X-Requested-With` (any value) and the refresh cookie.

| Presented token | Result |
|---|---|
| Valid, not rotated, `expires_at > now` | Set `rotated_at = now`; insert successor (same `family_id`, same `expires_at`); `200` with new access token and new cookie |
| Rotated, `now - rotated_at <= refresh-reuse-grace`, `expires_at > now` | `200` with new access token; **no** `Set-Cookie` (the other tab already received the successor) |
| Rotated, outside grace | Delete the family; `401` |
| Unknown, expired (`expires_at <= now`), cookie missing, header missing | `401` |

The rotation reads the row with `SELECT ... FOR UPDATE` so concurrent refreshes of the same token serialize.

Response `200`: `{"access_token": "<jwt>", "token_type": "Bearer", "expires_in": 900}`.

Every `401` returns `{"error":"invalid_refresh_token"}` and clears the cookie.

Claims are rebuilt from the database on every refresh. Role, school, and `enabled` changes therefore take effect
within one access-token TTL. A refresh for a disabled or deleted user deletes the family and returns `401`.

### `POST /api/auth/logout`

Requires header `X-Requested-With`; a missing header returns `401`. If the cookie matches a stored token, delete its
family. Always returns `204` and clears the cookie, whether or not the token existed.

### Access token

RS256, JWS header `kid` = `jwt.key-id`.

| Claim | Value |
|---|---|
| `iss` | `issuer` |
| `sub` | username |
| `iat` | now |
| `exp` | now + `access-token-ttl` |
| `jti` | random UUID |
| `roles` | role names without `ROLE_` prefix, e.g. `["SCHOOL_ADMIN"]` |
| `schools` | school IDs ordered by ID; omitted for `SUPER_ADMIN` |

### Refresh token

32 bytes from `SecureRandom`, Base64URL without padding.

### Cookie

```
Set-Cookie: refresh_token=<value>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=<seconds until expires_at>
```

`Secure` follows `cookie-secure`. Clearing uses the same attributes with an empty value and `Max-Age=0`.

## Security Filter Chain

Changes to `ApplicationSecurityConfiguration`:

- `oauth2ResourceServer(jwt)` with a `NimbusJwtDecoder` built from the public key, restricted to RS256, validating
  `exp` and `iss`. A `JwtAuthenticationConverter` maps `roles` to `ROLE_*` authorities and uses `sub` as the
  principal name.
- `httpBasic()` stays (temporary), authenticating against `JdbcUserDetailsManager`.
- `SessionCreationPolicy.STATELESS`; CSRF stays disabled (bearer tokens are not sent automatically; the refresh
  cookie is protected by `SameSite=Strict` plus `X-Requested-With`).
- `RoleHierarchy` bean: `ROLE_SUPER_ADMIN > ROLE_SCHOOL_ADMIN > ROLE_USER`.
- Unauthenticated requests get `401` with a Bearer challenge
  (`WWW-Authenticate: Bearer`, plus `error="invalid_token"` for a bad token). No Basic challenge is sent, so browsers
  never show a login popup; proactively sent Basic credentials still work.
- Authenticated requests lacking the role get `403`.

Authorization rules, evaluated in order:

| Matcher | Rule |
|---|---|
| health endpoint, `GET /api/schools`, `POST /api/auth/**` | `permitAll` |
| `POST /api/books`, `PUT /api/books/{id}` | `hasRole("SCHOOL_ADMIN")` |
| `PUT /api/checkouts/limits/defaults` | `hasRole("SCHOOL_ADMIN")` |
| `POST /api/checkouts/limits/schedules`, `PUT /api/checkouts/limits/schedules`, `DELETE /api/checkouts/limits/schedules/{id}` | `hasRole("SCHOOL_ADMIN")` |
| other actuator endpoints | `hasRole("SUPER_ADMIN")` |
| any other `/api/**` | `hasRole("USER")` |

The last rule covers all `GET` endpoints, `POST /api/checkouts/checkout`, `POST /api/checkouts/return`, and
`PUT /api/books/{id}/status` (allowed for `USER`). `PUT /api/books/{id}` does not match `/api/books/{id}/status`.
A user with no role gets `403`.

## Key Loading

`JwtConfiguration` reads the PEM `Resource`s at startup into `RSAPrivateKey` / `RSAPublicKey` and builds:

- `JwtEncoder`: `NimbusJwtEncoder` over an `RSAKey` JWK with `keyID = jwt.key-id`.
- `JwtDecoder`: `NimbusJwtDecoder.withPublicKey(...)` with the issuer and timestamp validators.

A missing, unreadable, or mismatched key pair fails startup.

## Testing

Follows existing conventions: `@DbTest` with Testcontainers PostgreSQL, `@WebMvcTest`, `spring-security-test`.

- `JwtConfigurationTests`: keys load; startup fails on a missing or bad key; encode/decode round trip.
- `AccessTokenIssuerTests`: claims, `kid`, TTL; no `schools` claim for `SUPER_ADMIN`.
- `RefreshTokenServiceDBTests`: rotation; grace window; reuse outside grace deletes the family; absolute expiry
  (`expires_at == now` is expired); successor keeps `expires_at`; logout; expired-row cleanup on login.
- `AuthControllerTests`: identical `401` for every login failure; cookie attributes; `X-Requested-With`
  requirement; cookie cleared on every refresh `401`; logout always `204`.
- `SecurityRulesTests`: the rule table per role, including status change for `USER`; no-role user gets `403`;
  token signed by another key, `alg=none`, or wrong `iss` gets `401` with the Bearer challenge; Basic with a JDBC
  user works; no Basic challenge header.
- `AuthFlowIntegrationTests`: login → API call → refresh → reuse of the rotated cookie → logout.
- Existing controller tests switch to `with(jwt())` or `@WithMockUser` where they need authentication.

## Documentation

README:

- Login, refresh, and logout flow for the UI.
- Running locally with `-Dspring-boot.run.profiles=dev`.
- Generating a key pair with `openssl` and mounting it as a Kubernetes Secret at `/etc/library-service/jwt`.
- All `library-service.auth.*` properties.
- DDL for existing databases.
- Provisioning users, roles, and schools with SQL.
- SQL to revoke all sessions of a user: `DELETE FROM refresh_tokens WHERE username = ?`.
- HTTP Basic is temporary; replace the "all endpoints protected with Basic Auth" text and the
  `SPRING_SECURITY_USER_*` instructions.

Swagger: add a bearer security scheme.

## Out of Scope

- Per-school access enforcement on API endpoints (Spec B).
- Removing HTTP Basic (later task, after the UI migrates).
- Login rate limiting / account lockout.
- Signing-key rotation with multiple verification keys.
- Account-management endpoints.
- Per-school roles.
