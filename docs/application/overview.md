# Application overview

How the talmar-backend works today. Start here before changing code; read the aggregate page for the area you touch.
This describes what is on `main` — plans and roadmap live in `.claude/CLAUDE.md`, decisions in `docs/adr/`.

## Modules

| Module | Role |
|--------|------|
| `talmar-contracts` | OpenAPI specs, the source of truth for the HTTP API: `VacancyApi.yml` and `IdentityApi.yml`. Consumed by the frontend team. |
| `talmar-api` | The Spring Boot application. Generates the API interfaces and models from each contract at build time, one generator execution per contract (`eu.talmar.api.vacancy.adapter…`, `eu.talmar.api.identity.adapter…`). The vacancy contract gets one interface per OpenAPI tag (`VacancyApi`, `GenerationApi`). |

Aggregates and their pages: [vacancy](vacancy.md) (the vacancy and its manual description), [generation](generation.md)
(AI-drafted vacancy descriptions) and [identity](identity.md) (company and user, registration).

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

- **Domain** (`eu.talmar.api.domain`): plain Java, no framework or JSpecify. Aggregates guard their own invariants
  with `Objects.requireNonNull` in builders and checks in value-object constructors. Timestamps are passed in; the
  domain never reads a clock. IDs are UUIDv7 (`UuidV7Generator`), time-ordered so they index well.
- **Application** (`eu.talmar.api.application`): one service per use case, `@NullMarked`. Inbound ports are use-case
  interfaces with command records; outbound ports are repository interfaces. `@Transactional` is only allowed here.
- **Inbound REST adapter** (`adapter.in.rest`): controllers implement the generated `*Api` interfaces. MapStruct
  mappers per direction (`mapper/request`, `mapper/response`); validators for contract rules the YAML cannot express
  (`validation`, e.g. the public mail domain block list).
- **Inbound scheduling adapter** (`adapter.in.scheduling`): `@Scheduled` jobs that only trigger a use case, like a
  controller does for HTTP.
- **Outbound AI adapter** (`adapter.out.ai`): LLM providers behind an application port; Spring AI is only used here.
- **Other outbound adapters**: `adapter.out.mail` (mail port, logging stand-in) and `adapter.out.security` (password
  hashing).
- **Outbound persistence adapter** (`adapter.out.persistence`): JPA entities separate from domain objects, MapStruct
  mappers in between. `BaseJpaEntity` carries the audit columns, filled by Spring Data JPA auditing.
- **Shared** (`eu.talmar.api.shared`): config (`Clock`, CORS, async, scheduling, security filter chain, identity
  properties), `BaseException`, the GDPR annotations, `Constants` and `TextContentRules` (the no-markup,
  no-control-character rules for vacancy text, shared by request validators and the check on AI drafts).

## Cross-cutting behaviour

- **Errors** are RFC 9457 Problem Details (`application/problem+json`), shape defined in the contract. Per-aggregate
  `@RestControllerAdvice` classes run at highest precedence; `GlobalExceptionHandler` (lowest precedence) handles
  bean-validation failures and unreadable bodies (malformed JSON, a value outside an enum: `400`), and is the
  catch-all for anything else as a 500 without internal details. Validation errors name the field but never echo the
  rejected value, so personal data does not end up in responses or logs. Handlers only map: each response has its own
  exception, so no handler inspects an exception to choose a status.
- **GDPR**: every persisted field is marked `@PersonalData` or `@ProcessData` (enforced by `ArchitectureTest`), so
  anonymization can wipe personal data and keep process data.
- **Time**: a `Clock` bean (`ClockConfig`, Europe/Amsterdam) is injected wherever "now" is needed.
- **Asynchronous work**: `@Async` (enabled in `AsyncConfig`) runs on Spring Boot's application task executor, which
  uses virtual threads (`spring.threads.virtual.enabled`, which also puts Tomcat on virtual threads). Background work is
  started from `@TransactionalEventListener(AFTER_COMMIT)` listeners, so it only starts once the data it needs is
  committed; see `generation.md`. Integration tests swap in an executor that runs each task on its own thread and
  waits for it, so the work has finished when the request returns and no transaction of the request leaks into it.
- **Scheduled jobs**: enabled in `SchedulingConfig` unless `talmar.scheduling.enabled=false` (the test profile). Every
  instance runs every job, so jobs must be idempotent. The only job is the generation retention clean-up.
- **Outbound HTTP**: `spring.http.clients.*` sets a 5 s connect and 15 s read timeout on the JDK HTTP client for every
  `RestClient` built from Spring Boot's builder. The Mistral adapter is the only user so far; give a new integration
  its own settings if these do not fit.
- **AI**: Mistral through Spring AI, chat model only (embedding, moderation and OCR auto-configuration are switched off
  with `spring.ai.model.*: none`). Retries are configured explicitly (`spring.ai.retry.*`, 3 attempts, 429 and 5xx
  only); the Spring AI default retries for minutes. Calls to the provider are a recurring cost per generation. Tests
  replace Mistral with WireMock (`MistralStub`), never with a mock of our own port.
- **CORS**: allowed origins come from `talmar.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`); empty means no CORS
  mapping at all.
- **Security**: Spring Security is on the classpath with one `SecurityFilterChain` (`SecurityConfig`) that permits
  every request. CSRF, form login, HTTP basic and logout are disabled, sessions are stateless, and CORS defers to
  `WebConfig`. Every endpoint is still open: the vacancy contract declares bearer auth, but nothing checks it yet.
  Spring Security does add its default response headers (e.g. `X-Content-Type-Options: nosniff`). The login slice
  (ADR 0003, section 4) tightens this chain.
  `SecurityConfig` only loads in a servlet web application, so a test that starts the context without a web
  environment (`WebEnvironment.NONE`, e.g. the Mistral adapter test) still starts.
- **Scheduling**: `@EnableScheduling` (`SchedulingConfig`). Every instance runs every job, so a job must be safe to
  run on several instances at once. Today there is one job: the purge of unconfirmed registrations (see
  [identity](identity.md)).

## Configuration and running locally

- Context path `/api/v1`, port 8090.
- Environment: `MISTRAL_API_KEY` is required; the application does not start without it. It is the only AI secret.
- `talmar.generation.retention` (default `PT32H`) and `talmar.generation.purge-interval` (default `PT1H`) control the
  generation clean-up.
- Identity settings (`IdentityProperties`):

  | Property | Env variable | Default |
  |----------|--------------|---------|
  | `talmar.identity.confirmation-url` | `IDENTITY_CONFIRMATION_URL` | `https://talmar.eu/registration/confirm` (`local`: `http://localhost:5173/registration/confirm`) |
  | `talmar.identity.unconfirmed-retention` | `IDENTITY_UNCONFIRMED_RETENTION` | `PT48H` (verification link validity and purge age) |
  | `talmar.identity.purge-interval` | `IDENTITY_PURGE_INTERVAL` | `PT1H` |
- Profiles: `local` (SQL Server from `docker-compose.yaml`, CORS for the Vite dev server on `localhost:5173`), `dev`
  (datasource from `SPRING_DATASOURCE_*` environment variables), `test` (in-memory H2).
- **Schema management is `ddl-auto: update`**: Hibernate changes the schema from the entities on startup. There are no
  migrations. Any entity change is therefore a schema change and is flagged by `guardrail-diff` (`schema-change`).
  `update` never alters existing columns: a new `NOT NULL` column on a table with rows (such as `vacancy.language`) or a
  changed column length needs a database reset locally.

## Known gaps

Open issues an agent should know about before building on top of them. Remove an entry in the PR that fixes it.

- Javadoc and code comments refer to ADR 0004 and ADR 0005, which are not in `docs/adr/`.
- `application-local.yml` connects to database `talmar`, while `docker-compose.yaml` creates `talmar_db`.
- `BaseJpaEntity.lastModifiedBy` (an `app_user` id) is filled only for companies, by the registration use case.
  For vacancies it stays empty until login exists: there is no `AuditorAware`, because nobody is authenticated yet.
