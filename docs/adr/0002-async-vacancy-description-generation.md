# ADR 0002 — Asynchronous AI vacancy description generation

- **Status:** Accepted, revised 2026-10-11 (see "Revision: review of PR #96" at the end)
- **Date:** 2026-10-09

## Context

Epic 2 of the build order: a recruiter who has created a core vacancy (job title, category, location, workplace
type, hours) can let an LLM draft the description from three short answers (the most important tasks, the team, why
the job is attractive). The contract (`VacancyApi.yml`) already describes this as an asynchronous job:
`POST /vacancy/{id}/generate-description` returns `202` with a `generationId`, and the client polls
`GET /vacancy/{id}/generate-description/{generationId}` until the draft is there.

The frontend flow this serves: the user answers the three questions, presses *send*, and continues to the next step
(offer, contact person) while the text is generated. When the user reaches the description step, the draft is shown
if it is ready, otherwise a spinner until it is. The draft is a suggestion: the user edits it and saves it through
the existing `POST /vacancy/{id}/description`. Nothing generated is ever written to the vacancy automatically.

Constraints and requirements that shaped the decision:

- **AI advises, a human decides** (`CLAUDE.md`). The draft and the vacancy's description are separate things.
- **Nothing invented.** The model only gets facts we have (job title, language, the three answers); sections we have
  no facts for (`whatWeOffer`, `aboutUs`) are not generated at all until company data exists (epic 3).
- **Prompt injection.** The three answers are free text from the user. A system prompt is not a security boundary;
  the structure around the call has to limit what an injected instruction can achieve.
- **Fast and bounded.** The user should have a result within one minute, including retries.
- **No new infrastructure.** The application is not in production yet; recurring cost and operational surface must
  stay small. Hosting will be a European provider; no cloud SDK outside an adapter.
- **Robust against restarts and multiple instances**: a crash during generation or a second instance must not lose
  or hide a job.
- **Generated text is transient**: the draft lives long enough for the user to pick it up (32 hours), then it is
  deleted. An audit trail of recommendations (`CLAUDE.md`) is out of scope for this epic: vacancy text is not a
  candidate decision, so it is not high-risk; the trail gets designed with candidate matching (epic 5).
- **No authentication yet.** The endpoint is open; abuse protection (rate limits) waits for login. Until then the
  application only runs locally.

## Decision

### 1. A separate aggregate: `VacancyDescriptionGeneration`

The generation is its own aggregate (`domain/generation/`), not a field on `Vacancy`:

| Field | Classification | Notes |
|-------|----------------|-------|
| `id` | – | UUIDv7, the `generationId` in the contract. |
| `vacancyId` | `@ProcessData` | Reference only; the aggregate never loads the vacancy. |
| `status` | `@ProcessData` | `PENDING` → `COMPLETED` or `FAILED`. |
| `failureReason` | `@ProcessData` | `PROVIDER_ERROR`, `TIMEOUT`, `INPUT_REJECTED`, `INVALID_OUTPUT`; only when `FAILED`. |
| `input` | `@PersonalData` | Snapshot of job title, language and the three answers. Free text about a team routinely names people. |
| `draft` | `@PersonalData` | `summary`, `jobDescription`, `tasks`; only when `COMPLETED`. Same reasoning. |
| `model`, `promptVersion` | `@ProcessData` | What produced the draft. |
| `requestedAt`, `completedAt` | `@ProcessData` | |

Rules the aggregate owns:

- A generation is immutable once `COMPLETED` or `FAILED`.
- A `PENDING` generation older than 90 seconds is reported as `FAILED` / `TIMEOUT` when read. This covers a crash
  during generation without a watchdog process.
- Starting a new generation for a vacancy that still has a `PENDING` one is allowed; the newest is what the user
  polls, the old one finishes or times out on its own.

**Rejected:** a `generation` field on `Vacancy`. It would make the vacancy aggregate carry a half-done suggestion and
its lifecycle, and it would make "replace the draft while the user edits" a vacancy change. The two things have
different owners (the AI vs. the user) and different lifetimes.

### 2. Asynchronous in-process, state in the database, no queue

- `StartVacancyDescriptionGenerationService` validates the vacancy exists, saves the generation as `PENDING` and
  returns the id. After the transaction commits, a `@TransactionalEventListener(AFTER_COMMIT)`
  hands the id to an `@Async` method on a virtual-thread executor (`spring.threads.virtual.enabled=true`) that runs
  `CompleteVacancyDescriptionGenerationService`: call the generator port, validate the output, save `COMPLETED` or
  `FAILED`.
- The `AFTER_COMMIT` boundary guarantees the `PENDING` row exists before the worker starts, so a poll can never see a
  `404` for a job that was just started.
- The completion service runs with `Propagation.REQUIRES_NEW`. An `AFTER_COMMIT` listener still sees the original,
  already committed transaction as current; a plain `@Transactional` would join it and its writes would never be
  flushed. This also makes the flow correct when the executor is synchronous, which is how the integration tests run
  it (no `Thread.sleep`, no polling in tests).
- State lives in the database because the poll may come from another instance and may come minutes later.

**Rejected:**

- *In-memory cache (Caffeine) for the generation.* Simplest, but a restart loses every running and finished job and
  two instances cannot see each other's jobs. That contradicts the robustness requirement and makes the poll
  unreliable.
- *Redis.* Solves sharing between instances but is new infrastructure with recurring cost, and it does not solve
  durability either without extra configuration. Not justified before there is a production load.
- *A queue / outbox with a worker.* Durable and scalable, but a second moving part to operate for a feature whose
  load is a few calls per vacancy. Revisit when generation volume or multiple consumers justify it; the
  generation is behind a port, so the worker can be moved without touching the use cases.

### 3. Retention: a scheduled clean-up, nothing else

A `@Scheduled` job deletes generations older than `talmar.generation.retention` (default `PT32H`). It touches only
the generation table. "Cache" in the product sense is therefore a retention rule on an ordinary table, not a cache
technology.

### 4. LLM behind a port: Spring AI with Mistral

- Port: `VacancyDescriptionGenerator` (`application/port/out`) with its own input record (`GenerationInput`: job
  title, language, the three answers) and a result type (`GeneratedDraft` or a typed rejection). The port never sees
  inbound command types or Spring AI types.
- Adapter: `adapter/out/ai/mistral/`, using Spring AI's `ChatClient` with the Mistral starter. Model
  `mistral-small-latest`, temperature 0.5, max 1000 output tokens. Spring AI is a maintainer decision; the adapter is
  the only place that knows it.
- **Structured output**: the response is forced into a JSON schema (`summary`, `jobDescription`, `tasks`,
  `inputRelevant`, `rejectionReason`). The model refuses off-topic input by setting `inputRelevant=false`, which
  becomes `FAILED` / `INPUT_REJECTED`.
- **Timing**: per attempt a 15-second timeout, 3 attempts with short backoff (Spring AI retry configured explicitly,
  not the default which can run for minutes), so the worst case stays under one minute.
- **Prompts** are versioned classpath resources (`prompt/vacancy-description/v1/system.st`, `user.st`); each generation
  records the version it used. A prompt change is a reviewed PR, not a runtime setting. Templates in the database
  were rejected: more flexible, but unreviewable and hard to audit.
- **Prompt injection defences**, in order of weight:
  1. There is no conversation: three bounded fields, one fixed task, one call.
  2. Input limits: `maxLength` 1000 from the contract, `VacancyDescriptionContentValidator` (no HTML, no control
     characters), and the answers are wrapped in fixed delimiters that are stripped from the input itself. The system
     prompt states that delimited content is data, never instruction.
  3. Output limits: the JSON schema, the same content validator and the contract length limits (1000 / 5000 / 5000)
     applied to the draft, so a draft can always be saved through the manual endpoint. A canary token in the system
     prompt that shows up in the output marks the generation `FAILED` / `INVALID_OUTPUT`.
  4. The system prompt is kept short and contains nothing confidential; we assume it can leak.
  A separate moderation call before generation was rejected for now: extra latency and cost for every request. Revisit
  if the injection test set shows the schema and prompt are not enough.
- **Tests** never call Mistral. The adapter is tested against WireMock with recorded response shapes; use cases are
  tested with a mocked port.

### 5. Contract changes

- `language` (ISO 639-1 enum `nl`, `en`, `de`, `fr`) is **required** on `VacancyCoreRequest` and added to
  `VacancyUpdateRequest` and the `Vacancy` response. Making it required is a deliberate breaking change, agreed with
  the frontend team while they are still building; `oasdiff` will flag it and the maintainer accepts it.
- `GET …/generate-description/{generationId}`: `200` with `PENDING` or `COMPLETED`; a failed generation is a problem
  response: `500` for `PROVIDER_ERROR`, `TIMEOUT` and `INVALID_OUTPUT` (something is broken on our side, the user
  can retry), `422` for `INPUT_REJECTED` (retrying the same input will not help). `404` when the generation or
  vacancy does not exist. The `description` in a `COMPLETED` response carries only the generated fields.
- The text that says the draft is submitted via `PATCH /vacancy/{id}` is corrected to `POST /vacancy/{id}/description`.
- **Paths stay under `/vacancy/{id}/…`.** A REST resource follows how the client thinks: a generation only exists
  for one vacancy, so it is a sub-resource. An aggregate follows consistency boundaries, which the client does not
  care about. The inbound adapter maps between the two; that is what the hexagon is for. Moving the path to
  `/generation/{id}` was rejected: a breaking change for the frontend with no benefit to the user.
- The two generation operations get their own OpenAPI tag, `Generation`. Tags do not change the wire format, but the
  generator creates one interface per tag, so this yields a `GenerationApi` next to `VacancyApi` and therefore its
  own `VacancyDescriptionGenerationController` in `adapter/in/rest/generation/` instead of growing `VacancyController`.
  The generator currently groups by first path segment (`useTags` is off), so this needs `useTags=true` in the
  `openapi-generator-maven-plugin` configuration. That is a `<build>` change and therefore a guardrail change
  (`guardrail-change-ok`), called out in the PR. The `Vacancy` tag keeps producing `VacancyApi`, so nothing else moves.

## Consequences

- One new table (`vacancy_description_generation`) and a `NOT NULL` `language` column on
  `vacancy`. With `ddl-auto: update` the latter fails on a table with existing rows; locally that is a database reset.
- The frontend must send `language` on create and must handle `422` and `500` on the poll.
- `spring.ai.openai` configuration is removed; `MISTRAL_API_KEY` becomes the only AI secret.
- The application now has `@Async` and `@Scheduled` behaviour; tests for those use the synchronous executor and call
  the clean-up directly.
- Delivered in two PRs: (1) contract, domain, persistence, use cases, async flow and a stub generator so the frontend
  can integrate; (2) the Mistral adapter, prompts and output validation.

## Revision: review of PR #96 (2026-10-11)

The maintainer's review changed the following. The original text above is kept; where it conflicts, this section wins.

1. **Classification: input and draft are process data.** §1 classified the answers and the draft as `@PersonalData`
   because "free text about a team routinely names people". Rejected by the maintainer: the answers and the draft
   describe a job and a team, and do not identify a specific person. Every column of the generation is
   `@ProcessData`.
2. **No `REQUIRES_NEW` in the completion.** §2 required `Propagation.REQUIRES_NEW` because the integration tests ran
   `@Async` work on the calling thread, where the already committed transaction is still bound inside an
   `AFTER_COMMIT` callback. That put transaction plumbing (a `TransactionTemplate` per step) into the use case only
   for the sake of the test setup. Superseded: the test executor now runs each task on its own virtual thread and waits
   for it, so the task never sees the request's transaction, exactly as in production. The completion service has no
   transaction of its own; loading and saving are each the repository's own short transaction, so still no connection
   is held while waiting on the provider.
3. **Output validation in the adapter, not in the domain.** §4 applied the content and length rules to the draft in
   the domain (`GeneratedDraft.isSaveableAsDescription`). Superseded by the maintainer's "fail fast at the edge" rule:
   data is validated once, where it enters the application. The Mistral adapter now validates the draft
   (`GeneratedDraftValidator`); the domain no longer re-checks it, nor does it null-check its value objects.
4. **Retryable vs. non-retryable provider failures.** §4 reported every provider failure as `PROVIDER_ERROR` (500).
   Following Mistral's error guidance, 429 and 5xx are retried (429 explicitly, via `spring.ai.retry.on-http-codes`),
   and become `PROVIDER_ERROR` (500) when the retries run out. Every other 4xx (bad request, key, access, unknown
   model) is not retried and becomes the new `PROVIDER_UNAVAILABLE`, answered with a `503` (new `ServiceUnavailable`
   response in the contract, an additive change): retrying cannot help until we fix our side, so the user hears at
   once that AI generation is unavailable.
5. **One exception per response.** The poll no longer builds a "timed-out view" of a `PENDING` generation for the
   controller to inspect. The get use case throws a specific exception per kind of failure (rejected → 422, unavailable
   → 503, failed or timed out → 500), and the exception handler only maps it, with a fixed detail per status; the exact
   reason is logged, never returned.
6. **Temperature 1.1** instead of 0.5, for livelier texts. This is above Mistral's recommended 0.0–0.7; the strict JSON
   schema and the output validation still bound the output.
7. **Prompt versions are semver** (`vacancy-description/V1.0.0`) instead of a `v1` directory. The templates live in
   `prompt/vacancy-description/` and start with a template comment holding the version and a changelog, which is
   removed when rendering. Git keeps the old versions; a prompt change bumps the version and adds a changelog line.
8. **`language` is upper case** in the contract (`NL`, `EN`, `DE`, `FR`), like the country code. An unknown or
   lower-case value is a `400`.
9. **Tests**: integration tests go through the API only and replace Mistral with WireMock instead of mocking the
   generator port; the vacancies they need are seeded with an SQL script.
