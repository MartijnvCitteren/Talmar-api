# Application overview

How the jobzy-backend works today. Start here before changing code; read the aggregate page for the area you touch.
This describes what is on `main` — plans and roadmap live in `.claude/CLAUDE.md`, decisions in `docs/adr/`.

## Modules

| Module | Role |
|--------|------|
| `jobzy-contracts` | OpenAPI specs, the source of truth for the HTTP API: `VacancyApi.yml` and `IdentityApi.yml`. Consumed by the frontend team. |
| `jobzy-api` | The Spring Boot application. Generates the API interfaces and models from each contract at build time, one generator execution per contract (`app.jobzy.api.vacancy.adapter…`, `app.jobzy.api.identity.adapter…`). |

Aggregates and their pages: [vacancy](vacancy.md), [identity](identity.md) (company and user, registration).

## Request flow

A request passes through the hexagon in one direction; each arrow is an explicit mapping, no type crosses a layer it
does not belong to.

```
HTTP ─▶ Security filter chain (permits everything for now)
          ▼
        Controller (implements generated *Api interface)
          │  1. bean validation on the generated contract model (@Valid from the spec)
          │  2. adapter-level content validation (e.g. no raw HTML)
          │  3. request mapper: contract model ─▶ command (application/port/in/command)
          ▼
        Use case (application/service, implements a port/in interface)
          │  loads/creates the domain object, calls domain behaviour, saves via a port/out interface
          ▼
        Repository adapter (adapter/out/persistence, implements port/out)
          │  JPA mapper: domain ─▶ JPA entity, Spring Data repository
          ▼
        SQL Server (H2 in tests)
```

The response goes back up the same way: domain object ─▶ response mapper ─▶ generated contract model.

## Layers and what lives where

- **Domain** (`app.jobzy.api.domain`): plain Java, no framework or JSpecify. Aggregates guard their own invariants
  with `Objects.requireNonNull` in builders and checks in value-object constructors. Timestamps are passed in; the
  domain never reads a clock. IDs are UUIDv7 (`UuidV7Generator`), time-ordered so they index well.
- **Application** (`app.jobzy.api.application`): one service per use case, `@NullMarked`. Inbound ports are use-case
  interfaces with command records; outbound ports are repository interfaces. `@Transactional` is only allowed here.
- **Inbound REST adapter** (`adapter.in.rest`): controllers implement the generated `*Api` interfaces. MapStruct
  mappers per direction (`mapper/request`, `mapper/response`); validators for contract rules the YAML cannot express
  (`validation`, e.g. the public mail domain block list).
- **Inbound scheduling adapter** (`adapter.in.scheduling`): `@Scheduled` jobs that call a use case, like a controller
  does for HTTP.
- **Other outbound adapters**: `adapter.out.mail` (mail port, logging stand-in) and `adapter.out.security` (password
  hashing).
- **Outbound persistence adapter** (`adapter.out.persistence`): JPA entities separate from domain objects, MapStruct
  mappers in between. `BaseJpaEntity` carries the audit columns, filled by Spring Data JPA auditing.
- **Shared** (`app.jobzy.api.shared`): config (`Clock`, CORS, security filter chain, scheduling, identity
  properties), `BaseException`, the GDPR annotations and `Constants`.

## Cross-cutting behaviour

- **Errors** are RFC 9457 Problem Details (`application/problem+json`), shape defined in the contract. Per-aggregate
  `@RestControllerAdvice` classes run at highest precedence; `GlobalExceptionHandler` (lowest precedence) handles
  bean-validation failures and is the catch-all for anything else as a 500 without internal details. Validation
  errors name the field but never echo the rejected value, so personal data does not end up in responses or logs.
- **GDPR**: every persisted field is marked `@PersonalData` or `@ProcessData` (enforced by `ArchitectureTest`), so
  anonymization can wipe personal data and keep process data.
- **Time**: a `Clock` bean (`ClockConfig`, Europe/Amsterdam) is injected wherever "now" is needed.
- **CORS**: allowed origins come from `jobzy.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`); empty means no CORS
  mapping at all.
- **Security**: Spring Security is on the classpath with one `SecurityFilterChain` (`SecurityConfig`) that permits
  every request. CSRF, form login, HTTP basic and logout are disabled, sessions are stateless, and CORS defers to
  `WebConfig`. Every endpoint is still open: the vacancy contract declares bearer auth, but nothing checks it yet.
  Spring Security does add its default response headers (e.g. `X-Content-Type-Options: nosniff`). The login slice
  (ADR 0003, section 4) tightens this chain.
- **Scheduling**: `@EnableScheduling` (`SchedulingConfig`). Every instance runs every job, so a job must be safe to
  run on several instances at once. Today there is one job: the purge of unconfirmed registrations (see
  [identity](identity.md)).

## Configuration and running locally

- Context path `/api/v1`, port 8090.
- Identity settings (`IdentityProperties`):

  | Property | Env variable | Default |
  |----------|--------------|---------|
  | `jobzy.identity.confirmation-url` | `IDENTITY_CONFIRMATION_URL` | `https://jobzy.app/registration/confirm` (`local`: `http://localhost:5173/registration/confirm`) |
  | `jobzy.identity.unconfirmed-retention` | `IDENTITY_UNCONFIRMED_RETENTION` | `PT48H` (verification link validity and purge age) |
  | `jobzy.identity.purge-interval` | `IDENTITY_PURGE_INTERVAL` | `PT1H` |
- Profiles: `local` (SQL Server from `docker-compose.yaml`, CORS for the Vite dev server on `localhost:5173`), `dev`
  (datasource from `SPRING_DATASOURCE_*` environment variables), `test` (in-memory H2).
- **Schema management is `ddl-auto: update`**: Hibernate changes the schema from the entities on startup. There are no
  migrations. Any entity change is therefore a schema change and is flagged by `guardrail-diff` (`schema-change`).

## Known gaps

Open issues an agent should know about before building on top of them. Remove an entry in the PR that fixes it.

- `application.yml` still contains `spring.ai.openai` settings (model `gpt-5.2`), but there is no Spring AI
  dependency and no code using it. It conflicts with the EU-first/Mistral direction in `CLAUDE.md`; remove or replace it
  when vacancy text generation is built.
- Javadoc and code comments refer to ADR 0004 and ADR 0005, which are not in `docs/adr/`.
- `application-local.yml` connects to database `jobzy`, while `docker-compose.yaml` creates `jobzy_db`.
- `BaseJpaEntity.lastModifiedBy` (an `app_user` id) is filled only for companies, by the registration use case.
  For vacancies it stays empty until login exists: there is no `AuditorAware`, because nobody is authenticated yet.
