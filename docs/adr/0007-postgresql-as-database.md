# ADR 0007 — PostgreSQL as database

- **Status:** Accepted
- **Date:** 2026-11-20
- **Supersedes:** SQL Server as the database of the application and of Keycloak (`CLAUDE.md`, ADR 0006 section 8).

## Context

The application and Keycloak ran on SQL Server (`mssql-jdbc`, the `mcr.microsoft.com/mssql/server:2025-latest`
image). Hosting must be a European provider in the EU only; Scaleway is the candidate. Its managed database product,
Managed Database for PostgreSQL and MySQL, does not offer SQL Server. Running SQL Server ourselves next to the
application means a licence for production and operating the database ourselves.

SQL Server also cost us locally:

- The image is amd64-only and slow on Apple Silicon. That was the reason ADR 0001 deferred Flyway and Testcontainers.
- Its default `varchar` collation is Latin-1 and silently replaced characters such as `Ł` (ADR 0006, "Verified in
  PR 4"). Only the columns marked `@Nationalized` were safe; vacancy texts were not.
- Keycloak does not list SQL Server 2025 as tested (ADR 0006, section 8).

There is no production data yet, so switching now costs a local database reset and nothing else.

## Decision

1. **PostgreSQL for the application and for Keycloak**, each in its own database (`taldo_db`, `keycloak_db`) on one
   server, as before.
2. **Version 17**, the newest major version Scaleway's Managed Database for PostgreSQL offers on 2026-11-20 (its
   Serverless SQL Database already offers 18). Local runs the same major version as the hosted one, so a feature or
   behaviour of a newer version cannot slip in unnoticed. Docker Compose pins the minor version (`postgres:17.12`);
   Dependabot does not watch `docker-compose.yaml`, so bump it by hand. Move to 18 when the managed product offers it.
3. **Driver and dialect are detected**: `org.postgresql:postgresql` (version managed by Spring Boot), no
   `driver-class-name` or `hibernate.dialect` in the profiles. Spring Boot picks the driver from the JDBC URL and
   Hibernate the dialect from the database.
4. **UTF-8 everywhere.** PostgreSQL stores every text column in the database encoding, which is UTF-8 in the Docker
   image and must be UTF-8 on the hosted database. `@Nationalized` is removed: on PostgreSQL it maps to the same
   `varchar`, and its only purpose was SQL Server's collation.
5. **TLS to a hosted database** with certificate verification (`sslmode=verify-full` in `SPRING_DATASOURCE_URL`),
   not the driver's default `prefer`, which accepts an unencrypted connection. Locally there is no TLS.

## Alternatives considered

- **Stay on SQL Server.** Rejected: no managed SQL Server at the candidate provider, a licence for production, and
  the local drawbacks above.
- **MySQL**, also offered by Scaleway. Keycloak supports both. Rejected: PostgreSQL has a native `uuid` type for our
  UUIDv7 ids and transactional DDL (a failed Flyway migration rolls back as a whole).
- **PostgreSQL 18 locally.** Rejected: newer than what the hosted database runs (decision 2).

## Consequences

- Local data in the old SQL Server volume is not migrated. `docker compose up` creates a new volume
  (`taldo-postgresdata`); Hibernate (`ddl-auto: update`) creates the schema. The old volume can be removed with
  `docker volume rm taldo-sqlserverdata`.
- Docker Compose publishes PostgreSQL on host port 5433, not 5432. A PostgreSQL installed on the host (e.g.
  Postgres.app on macOS) binds `localhost:5432` first; the backend then connected to that server instead and failed
  on a missing `taldo_db`, and with a matching database it would have used the wrong one without any error.
- Keycloak runs with `KC_DB=postgres`. ADR 0006's fallback (a separate PostgreSQL container for Keycloak if SQL Server
  2025 broke) is no longer needed.
- The deferred Flyway and Testcontainers guardrails of ADR 0001 now use `flyway-database-postgresql` and the
  multi-arch PostgreSQL image. The amd64-only reason to defer them no longer holds; ADR 0001's revision records that.
- Tests still run on H2. A PostgreSQL-only behaviour (e.g. several `NULL`s in a unique column) is not covered until
  Testcontainers arrives.
- The hosting provider is still not decided. Nothing in the code depends on Scaleway; another EU provider with
  managed PostgreSQL works the same way.

## Verified

Checked on 2026-11-20 against the Docker Compose setup (PostgreSQL 17.12, Keycloak 26.8.1): `postgres-init` creates
`keycloak_db` and does nothing on a repeat, Keycloak becomes healthy and imports the realm `taldo`, `keycloak-config`
finishes, and the backend on the `local` profile starts and creates its schema in `taldo_db`.
