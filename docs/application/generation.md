# Vacancy description generation

A recruiter can let an LLM draft the description of an existing vacancy from three short answers (the most important
tasks, the team, why the job is nice). The draft is a suggestion: the frontend shows it for editing and the user saves
it through the manual endpoint (`POST /vacancy/{id}/description`, see `vacancy.md`). Nothing generated is ever written
to the vacancy automatically. The design and its rejected alternatives are in ADR 0002.

## Domain model

`VacancyDescriptionGeneration` (`domain/generation`) is its own aggregate, not part of `Vacancy`. It references the
vacancy by id only and never loads it.

| Field | Notes |
|-------|-------|
| `id` | UUIDv7; the `generationId` in the contract. |
| `vacancyId` | The vacancy it was started for. |
| `input` | `GenerationInput`: snapshot of the vacancy's job title and language plus the three answers. |
| `status` | `PENDING` → `COMPLETED` or `FAILED`. |
| `failureReason` | `PROVIDER_ERROR`, `PROVIDER_UNAVAILABLE`, `TIMEOUT`, `INPUT_REJECTED`, `INVALID_OUTPUT`; only when `FAILED`. |
| `draft` | `GeneratedDraft(summary, jobDescription, tasks)`; only when `COMPLETED`. `whatWeOffer` and `aboutUs` are never generated: there are no facts for them until company data exists. |
| `model`, `promptVersion` | What produced the draft. |
| `requestedAt`, `completedAt` | `completedAt` is set in either final state. |

`VacancyDescriptionGeneration.newPending(...)` only builds a new `PENDING` generation; the work starts elsewhere once it
is saved.

Rules the aggregate owns:

- It ends exactly once; a second `acceptDraft` or `fail` throws.
- **90-second timeout.** A `PENDING` generation older than 90 seconds has timed out (`isTimedOutAt(now)`). This covers
  a crash or restart during generation; the stored row stays `PENDING` until the retention clean-up removes it. A
  result that arrives after the deadline is recorded as `TIMEOUT` too, so the outcome never flips after a poll reported
  it.
- Starting a new generation while another is `PENDING` is allowed; the user polls the newest.

The aggregate and its value objects do not validate their input again: the answers are validated at the REST edge and
the draft in the Mistral adapter, where the data enters the application.

## Flow

```
POST /vacancy/{id}/generate-description
  VacancyDescriptionGenerationController
    1. bean validation (maxLength 1000, all three required)
    2. GenerateVacancyDescriptionRequestValidator (TextContentRules) → 400 naming the field
  StartVacancyDescriptionGenerationService  (@Transactional)
    loads the vacancy (404), snapshots job title + language, saves PENDING,
    publishes VacancyDescriptionGenerationStarted
  ← 202 {generationId, status: PENDING}, Location: …/generate-description/{generationId}

after commit:
  VacancyDescriptionGenerationStartedListener  (@TransactionalEventListener AFTER_COMMIT + @Async, virtual thread)
  CompleteVacancyDescriptionGenerationService  (no @Transactional)
    load the generation          (the repository's own short transaction)
    VacancyDescriptionGenerator  (no transaction: no DB connection held while waiting on the provider)
    acceptDraft / fail, save     (the repository's own short transaction)

GET /vacancy/{id}/generate-description/{generationId}
  GetVacancyDescriptionGenerationService: find by id and vacancy (404 otherwise);
  a failed or timed-out generation is thrown as the exception for its kind of failure
  ← 200 PENDING | 200 COMPLETED with description
  | 422 INPUT_REJECTED | 503 PROVIDER_UNAVAILABLE | 500 PROVIDER_ERROR, TIMEOUT, INVALID_OUTPUT
```

- `AFTER_COMMIT` guarantees the `PENDING` row exists before work starts, so a poll never sees a `404` for a generation
  that was just started.
- If the generator throws despite its contract, the generation is recorded as `PROVIDER_ERROR` rather than left
  `PENDING`.
- A generation is only readable through the vacancy it was started for.
- One exception per response (`VacancyDescriptionGenerationRejectedException` → 422,
  `…UnavailableException` → 503, `…FailedException` → 500, `…NotFoundException` → 404).
  `GenerationExceptionHandler` only maps them, with a fixed title and detail per status from `Constants`; the failure
  reason is logged, never returned.
- State lives in the database, so a poll may hit another instance. The background work itself is in-process: a crash
  loses it, and the 90-second rule turns that into a `TIMEOUT`.

## The generator port and the Mistral adapter

`VacancyDescriptionGenerator` (`application/port/out`) takes a `GenerationInput` and returns a `GenerationResult`,
a sealed type that permits `Drafted(draft, model, promptVersion)` and `Rejected(reason)`. It never throws for a failed
generation and never sees Spring AI or contract types.

`MistralVacancyDescriptionGenerator` (`adapter/out/ai/mistral`) is the only implementation, using a Spring AI
`ChatClient` (`MistralChatClientConfig`; Spring Boot only auto-configures its builder) with the Mistral starter:

- **Configuration** (`application.yml`): `mistral-small-latest`, temperature 1.1 (above Mistral's recommended 0–0.7,
  for livelier texts; the schema and the draft validation bound the output), max 1000 output tokens; API key from
  `MISTRAL_API_KEY`. The recorded `model` is the version Mistral reports in the response (a dated snapshot).
- **Prompts** are classpath templates in `prompt/vacancy-description/` (`system.st`, `user.st`). The version is semver
  (`VacancyDescriptionPrompt.VERSION`, now `vacancy-description/V1.0.0`) and is stored on every generation. Each template
  starts with a template comment (`{! … !}`) holding the version and a changelog; the comment is removed when rendering,
  so the model never sees it. A prompt change bumps the version, adds a changelog line and is a reviewed PR;
  `VacancyDescriptionPromptTest` checks that both headers carry the version in the code.
- **Structured output**: the JSON schema of `MistralDraftResponse` (`summary`, `jobDescription`, `tasks`,
  `inputRelevant`, `rejectionReason`) is sent as a strict `json_schema` response format. `inputRelevant=false` becomes
  `INPUT_REJECTED`. The model's `rejectionReason` is neither stored nor returned.
- **Output validation** happens in the adapter: `validateResponse` (an answer is there, has text, does not contain the
  canary, reports a model), parsing against the schema, and `GeneratedDraftValidator` — every section present,
  non-blank, within the contract limits of `POST /vacancy/{id}/description` (1000 / 5000 / 5000) and passing
  `TextContentRules` (no raw tags, no control characters other than newline, carriage return and tab). So a draft can
  always be saved unchanged. Any failure here is `INVALID_OUTPUT`.
- **Prompt-injection defences**: one fixed task in one call without conversation; the answers only in the user message,
  each inside `[[[name]]] … [[[/name]]]` blocks that the system prompt declares to be data, with the brackets stripped
  from the answers first; a random canary token per call in the system prompt, and output containing it is
  `INVALID_OUTPUT`; the output validation above.
- **Timing and errors**: per attempt 15 s read timeout and 5 s connect timeout (`spring.http.clients.*`, JDK HTTP client
  so a timeout is recognisable as `HttpTimeoutException`); 3 attempts in total with 1–2 s backoff (`spring.ai.retry.*`).
  Worst case stays under one minute. Which failures are retried follows Mistral's error guidance:

  | Mistral answer | Retried | Outcome | Poll |
  |----------------|---------|---------|------|
  | 429 (rate limit), 500, 502, 503, 504 | yes (`on-http-codes: 429` makes 429 retryable) | `PROVIDER_ERROR` after the last attempt | 500 |
  | timeout, connection refused | yes | `TIMEOUT` / `PROVIDER_ERROR` after the last attempt | 500 |
  | 400, 401, 403, 404, 422 (bad request, key, access, model) | no | `PROVIDER_UNAVAILABLE` at once | 503 |
  | unusable answer (see output validation) | no | `INVALID_OUTPUT` | 500 |

  A retry cannot fix a 4xx other than 429: it fails the same way until we fix our request or configuration, so the user
  hears right away that AI generation is unavailable and can write the text themselves.
- Nothing logs the prompt, the answers or the draft.

## Retention

`GenerationRetentionJob` (`adapter/in/scheduling`) calls `PurgeExpiredGenerationsService` every
`talmar.generation.purge-interval` (default `PT1H`, first run one minute after start). It bulk-deletes generations
requested longer ago than `talmar.generation.retention` (default `PT32H`). It touches only the generation table. Every
instance runs it; the delete is idempotent. `talmar.scheduling.enabled=false` switches all scheduled jobs off (tests do
this).

## Persistence

One table, `vacancy_description_generation` (`VacancyDescriptionGenerationJpaEntity`), with the input and draft
flattened into columns and an index on `requested_at` for the clean-up. `vacancy_id` is a plain column, not a foreign
key association.

Every column is `@ProcessData`: the answers and the draft describe a job and a team, not an identifiable person, and the
job title and language are copies of vacancy fields.

## Testing

- Integration tests only go through the API. `StartVacancyDescriptionGenerationIntegrationTest` covers the `POST`,
  `PollVacancyDescriptionGenerationIntegrationTest` covers the `GET`, each with happy and unhappy flows. They start from
  vacancies seeded by `sql/generation-vacancies.sql` (`@Sql`, before every test).
- Mistral is the only thing replaced: `MistralStub` runs one WireMock server for the whole test run (one port, so cached
  Spring contexts stay valid), and `BaseIntegrationTest` points `spring.ai.mistralai.chat.base-url` at it. Our own
  generator port is never mocked in integration tests.
- `SynchronousAsyncConfig` replaces the application task executor with one that runs each task on its own virtual
  thread and waits for it, so a generation has finished when the `POST` returns, without sleeping or polling, and the
  task starts without the request's transaction bound to it, as in production.
- `MistralVacancyDescriptionGeneratorTest` runs the adapter with the real Spring AI configuration against the same
  WireMock server: the request it sends, which failures are retried, how answers are classified. Only the read
  timeout is shortened; the test profile shortens the retry backoff.
- The test profile points `spring.ai.mistralai.chat.base-url` at an unreachable address for every other context.
  Override the **chat** base URL: it defaults to `api.mistral.ai` and wins over `spring.ai.mistralai.base-url`.
- The `PENDING` and timeout states cannot be reached through the API in a test (the work finishes during the `POST`);
  they are covered by `GetVacancyDescriptionGenerationServiceTest` and the domain test.

## Known gaps

- No rate limiting or authentication on the endpoint (waits for login), so the application must not be exposed
  publicly with a real `MISTRAL_API_KEY`.
- The application does not start without `MISTRAL_API_KEY`; there is no stub generator for running without a key.
- No audit trail of generations beyond the 32-hour retention. Vacancy text is not a candidate decision; the audit trail
  is designed with candidate matching (epic 5).
- A crash during generation is only noticed through the 90-second rule; there is no retry of lost work.
- Mistral answers both a rate limit and an exhausted monthly quota with `429`. Both are retried and end as a `500`,
  although a quota will not recover by retrying; telling them apart needs the error body.
- `vacancy_id` is not a foreign key, while the code conventions ask for real foreign keys between tables. Not changed in
  the review of PR #96; to decide with the maintainer.
