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

Change basic auth user/pass from command line
```shell
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.security.user.name=foo,-Dspring.security.user.password=bar'
```

Using env variable to specify various values
```shell
SPRING_DATASOURCE_URL=jdbc:postgres://localhost:5432/library_service  \
SPRING_DATASOURCE_USERNAME=root  \
SPRING_DATASOURCE_PASSWORD=password  \
SPRING_SECURITY_USER_NAME=user  \
SPRING_SECURITY_USER_PASSWORD=pass  \
./mvnw spring-boot:run
```

Note:
`SPRING_SECURITY_USER_NAME` and `SPRING_SECURITY_USER_PASSWORD` are used for Basic auth user & password. 

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

For now, all endpoints is protected with Basic Auth.

Swagger API: http://localhost:8080/swagger-ui.html

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
