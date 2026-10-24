# About

TBD

# Development

Requires JDK 21.

## Java setup

Use [SDKMAN](https://sdkman.io/install) to manage JDK.

### Install JDK 21 with SDKMAN
```shell
> sdk install java 21.0.3-librca
```

## Run Application from source code
```shell
> ./mvnw spring-boot:run
```
* Requires postgres to be running. (See docker compose file in `docker` directory)

Or, use the development time support.
```shell
> ./mvnw spring-boot:test-run
```
* In this mode, local postgres is used if available; otherwise, it starts a postgres container via Testcontainers (Docker required).

Run with the `dev` profile (uses the dev JWT keys in `dev/jwt/` and `cookie-secure=false`).
```shell
> ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
`spring-boot:test-run` (above) also needs the profile: `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev`.

Using env variable to specify various values
```shell
SPRING_DATASOURCE_URL=jdbc:postgres://localhost:5432/library_service  \
SPRING_DATASOURCE_USERNAME=root  \
SPRING_DATASOURCE_PASSWORD=password  \
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application fails to start without a valid JWT key pair. See [Authentication](#authentication).

## Run Application from artifact(executable jar)

To build an artifact(executable jar)
```shell
> ./mvnw package
```
The artifact is generated under `target` directory. (e.g. `target/library-service-0.0.1-SNAPSHOT.jar`)
To run it:
```shell
> java -jar target/library-service-0.0.1-SNAPSHOT.jar
```

## Run Application from container image with Docker

Specify environment properties with `-e` option.
```shell
> docker run \
   -p 8080:8080  \
   -e SPRING_DATASOURCE_URL=jdbc:postgres://host.docker.internal:5432/library_service  \
   -e SPRING_DATASOURCE_USERNAME=root  \
   -e SPRING_DATASOURCE_PASSWORD=password  \
   docker.io/library/library-service:0.0.1-SNAPSHOT
```

NOTE:
- `-e` option needs to be specified before the container image name.
- Use `host.docker.internal` to talk to another service running in docker. (substitute to `localhost`)

## Run Tests
```shell
> ./mvnw test
```
* When postgres DB is not running locally, it automatically starts up a postgres container during the test.

## Container Image

### Local container image creation
```shell
>  ./mvnw -DskipTests spring-boot:build-image
```

### Create and push to remote registry
```shell
> DOCKER_REGISTRY_USER=<user> DOCKER_REGISTRY_TOKEN=<token or password> ./mvnw -DskipTests spring-boot:build-image \
  -Dspring-boot.build-image.publish=true \
  -Dspring-boot.build-image.imageName="<image name>"
```

Sample:
```shell
DOCKER_REGISTRY_USER=ttddyy DOCKER_REGISTRY_TOKEN=$CR_PAT ./mvnw -DskipTests spring-boot:build-image \
  -Dspring-boot.build-image.publish=true \
  -Dspring-boot.build-image.imageName="ghcr.io/ttddyy/library-service:snapshot"
```

## Format

Use [Spring Java Format](https://github.com/spring-io/spring-javaformat).

```shell
> ./mvnw spring-javaformat:apply
```

## `@author` tag

* Add `@author` tag to `main` source code `.java` files except `package-info.java`.
* Add yourself as an `@author` that you modify substantially (more than cosmetic changes).

# Endpoints

Swagger API: http://localhost:8080/swagger-ui.html (declares a `bearer-jwt` scheme; use "Authorize" with an access token)

# Authentication

Users log in with a username/password and receive a short-lived RS256 access token (JWT) in the response body and a
rotating refresh token in an HttpOnly cookie.

## Flow for the UI

1. `POST /api/auth/login` with `{"username": "...", "password": "..."}`.
   Response `200`:
   ```json
   {"access_token": "<jwt>", "token_type": "Bearer", "expires_in": 900,
    "user": {"username": "ocean-admin", "roles": ["SCHOOL_ADMIN"], "schools": ["ocean"]}}
   ```
   plus `Set-Cookie: refresh_token=<value>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=<seconds until expiry>`.
   Any failure returns `401 {"error":"invalid_credentials"}`.
   Keep the access token in memory only.
2. Call the API with `Authorization: Bearer <access_token>`.
3. Before the access token expires (or on a `401`), `POST /api/auth/refresh` with header `X-Requested-With` (any value;
   the browser sends the cookie automatically). Response `200`: `{"access_token": "<jwt>", "token_type": "Bearer", "expires_in": 900}`
   and a new refresh cookie. Every `401` is `{"error":"invalid_refresh_token"}` and clears the cookie; send the user back to login.
   * Requests to `/api/auth/**` ignore any `Authorization: Bearer` header, so refresh can be called while holding an
     expired access token.
   * Refresh tokens rotate on every use. Re-sending a just-rotated token within `refresh-reuse-grace` (default 10s,
     e.g. two tabs) returns `200` with a new access token but no `Set-Cookie`. Re-sending it later is treated as theft:
     the whole session (family) is deleted and `401` is returned.
   * The refresh session length is absolute from login (`refresh-token-ttl`); rotation does not extend it.
   * Role and school changes are picked up on the next refresh.
4. `POST /api/auth/logout` with header `X-Requested-With`: always `204`, deletes the session and clears the cookie.

Each login starts an independent session, so a user can be logged in on several machines. UI and API are expected to
share one origin (no CORS).

HTTP Basic against the same `users`/`authorities` tables still works but is **temporary** and will be removed once the UI
migrates. No Basic challenge is sent, so browsers do not show a login popup. `spring.security.user.*` is no longer used.

## Roles

Hierarchy: `SUPER_ADMIN` > `SCHOOL_ADMIN` > `USER`.

| Endpoints | Role |
|---|---|
| health, `GET /api/schools`, `POST /api/auth/**` | none |
| `POST /api/books`, `PUT /api/books/{id}`, `PUT /api/checkouts/limits/defaults`, `POST`/`PUT /api/checkouts/limits/schedules`, `DELETE /api/checkouts/limits/schedules/{id}` | `SCHOOL_ADMIN` |
| other actuator endpoints | `SUPER_ADMIN` |
| any other `/api/**` (all `GET`, checkout, return, book status change) | `USER` |

Unauthenticated requests get `401` (`WWW-Authenticate: Bearer`); missing role gets `403`.

## Configuration

Properties under `library-service.auth`:

| Property | Default | Notes |
|---|---|---|
| `issuer` | `library-service` | `iss` claim; validated on decode |
| `access-token-ttl` | `15m` | |
| `refresh-token-ttl` | `1d` | absolute session length |
| `refresh-reuse-grace` | `10s` | |
| `cookie-secure` | `true` | `Secure` attribute of the refresh cookie |
| `jwt.key-id` | required | JWS header `kid` |
| `jwt.private-key` | required | PKCS#8 PEM resource, e.g. `file:/etc/library-service/jwt/private.pem` |
| `jwt.public-key` | required | X.509 PEM resource, e.g. `file:/etc/library-service/jwt/public.pem` |

A missing, unreadable or mismatched key pair fails startup. The `dev` profile (`application-dev.properties`) points to
the key pair committed under `dev/jwt/` (for local development and tests only).

## Generate a key pair

```shell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
openssl pkey -in private.pem -pubout -out public.pem
```
`genpkey` writes PKCS#8, which is what the application expects.

Mount as a Kubernetes Secret at `/etc/library-service/jwt`:
```shell
kubectl create secret generic library-service-jwt --from-file=private.pem --from-file=public.pem
```
```yaml
# pod spec
volumes:
  - name: jwt
    secret:
      secretName: library-service-jwt
containers:
  - name: library-service
    env:
      - {name: LIBRARY_SERVICE_AUTH_JWT_KEY_ID, value: "2026-10"}
      - {name: LIBRARY_SERVICE_AUTH_JWT_PRIVATE_KEY, value: "file:/etc/library-service/jwt/private.pem"}
      - {name: LIBRARY_SERVICE_AUTH_JWT_PUBLIC_KEY, value: "file:/etc/library-service/jwt/public.pem"}
    volumeMounts:
      - {name: jwt, mountPath: /etc/library-service/jwt, readOnly: true}
```

## Database DDL for existing databases

`schema.sql` creates these for new databases. For an existing database, run:
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

## Provisioning users

Passwords are stored with the `{bcrypt}` prefix; authorities with the `ROLE_` prefix. Generate a hash:
```shell
htpasswd -bnBC 10 "" 'the-password' | tr -d ':\n'
```
This prints a `$2y$10$...` hash, which Spring Security's BCrypt accepts as is.

```sql
INSERT INTO users (username, password, enabled) VALUES ('ocean-admin', '{bcrypt}$2y$10$...', TRUE);
INSERT INTO authorities (username, authority) VALUES ('ocean-admin', 'ROLE_SCHOOL_ADMIN');
INSERT INTO user_schools (username, school_id) VALUES ('ocean-admin', 'ocean');
```
`SUPER_ADMIN` users (`ROLE_SUPER_ADMIN`) need no `user_schools` rows.
Because of the role hierarchy, one authority per user is enough.

Revoke all sessions of a user (their access tokens still work until they expire):
```sql
DELETE FROM refresh_tokens WHERE username = '<username>';
```

To disable a user: `UPDATE users SET enabled = FALSE WHERE username = '<username>'` (takes effect on next refresh).

# Development tips

Recreate database schema
```sql
drop schema library_service; create schema library_service;
drop schema library_service_test; create schema library_service_test;
```

Testcontainers trys to create container per test.  
Check the log message has the following:
```
Reuse was requested but the environment does not support the reuse of containers
To enable reuse of containers, you must set 'testcontainers.reuse.enable=true' in a file located at /Users/ttsuyukubo/.testcontainers.properties
```
If so, update `.testcontainers.properties` with `testcontainers.reuse.enable=true`.

## Docker

Start postgres container
```shell
docker compose -f docker/postgres/docker-compose.yml up
```

See docker pgdata volume
```shell
docker volume ls
```

Connect to database via psql
```shell
docker exec -it postgres-db psql -U root -W library_service -p 5432
```

## Date and Time Handling
All dates and timestamps in the system are handled in UTC to ensure consistency across services.
The system uses Java’s `Instant` type and PostgreSQL’s `timestamptz` column for storing these values.
All API inputs and outputs are expected to use UTC, and clients should send any date or time parameters accordingly.


## Trouble Shooting

Tests failed with the following exception:

```text
Caused by: org.hibernate.HibernateException: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)
```

This exception is misleading and masks the real issue. The failure is due to Hibernate being unable to retrieve JDBC metadata because it couldn’t obtain a connection from the database. If you check earlier in the logs, you may find messages like:

* `FATAL: sorry, too many clients already`
* `HHH000342: Could not obtain connection to query metadata`

This typically happens when the test context cache holds onto connections without closing them, eventually exhausting the database’s connection limit.

To diagnose and confirm:

* Run `show max_connections;` in PostgreSQL to check the configured limit.
* Run `SELECT COUNT(*) FROM pg_stat_activity;` to see how many connections are currently open.

**Solutions:**

* If you're using Testcontainers, you can increase the connection limit by chaining `.withCommand("postgres -c max_connections=100")` when configuring the container.
* On the Spring Boot side, set `"spring.datasource.hikari.maximum-pool-size=2"` for tests to minimize the consumption of connection pool.
