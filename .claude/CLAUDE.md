# CLAUDE.md

Instructions for Claude (Code) when working in the talmar-backend repository. This file describes the *code* context — for the
product/market "why" and the roadmap, see project knowledge (product/market vision, H2 2026 epics).

**Before you start a task, read `docs/application/`** (start with `overview.md`): it describes what is built, how it
fits together and its known gaps. Keeping it current is part of every change — see "Application docs" below.

## What Talmar is
Talmar is a recruitment hub — ATS, channel-advice engine, and later a marketplace — replacing fragmented point tools. The
advice engine depends entirely on ATS pipeline data (channel source, rejection reason, stage transitions), so those must be
captured accurately from day one. Build order is dependency-driven, ATS core first:

1. Manually created vacancies.
2. AI-generated vacancy texts.
3. Accounts and login, with company information fed into generated vacancy texts.
4. Multiposting to job boards (LinkedIn, Indeed, …) via an aggregator.
5. AI candidate matching and a channel dashboard where AI advises. Matching may move ahead of multiposting: it holds the
   most complex domain logic and the most value.

Marketplace and payment mediation are explicitly phase 2. Don't build marketplace, payment mediation, or a full
flow-builder now — out of scope until the ATS core and advice engine prove out.

This repository is the backend only. A separate frontend team builds the frontend against the OpenAPI contracts in
`talmar-contracts`.

## AI and human oversight

Talmar is AI-native: AI capabilities (vacancy text generation, candidate matching, advice) are first-class concepts in the
domain model, not a layer bolted onto a finished ATS. That does not change the build order above.

- **AI advises, a human decides.** No use case lets AI decide about a candidate (invite, reject, advance a stage). AI
  produces a recommendation (e.g. a match score with a rationale); a decision is always a separate, explicit action by an
  identified user. CV screening is high-risk under the EU AI Act — this is a hard rule, not a UX preference.
- **A decision always carries the user's own reason**, also when it follows the AI's advice. Bulk decisions (e.g.
  rejecting all remaining candidates because the vacancy is nearly filled) are allowed, but still require a reason and
  record one decision per candidate. The detailed rules for bulk decisions get designed when that feature is built.
- **Every AI-assisted decision is auditable**: who decided, when, with which reason, and which recommendation they saw
  (model, model version, prompt version, score). A recommendation is immutable once shown.
- **The audit trail holds no personal data** — it references candidates and users by ID only, so anonymization leaves it
  intact. A match score is process data and survives anonymization; a rationale that quotes or summarizes the CV is
  personal data and gets wiped with it.
- **Explainable output**: every recommendation carries a human-readable rationale that the API exposes, so the frontend
  can show why a candidate ranks where they do.
- **LLMs**: start with Mistral (European), behind a port. Deviating to a non-European model is a case-by-case decision for
  the human — flag it, never switch silently.

**This drives how you work here:**

- Momentum > perfection. Pick the smallest change that advances the current epic; don't let scope creep beyond what was
  asked.
- Deliberate over-engineering is fine when it serves an explicit learning goal (DDD/Hexagonal, OCP 21 track). Without a
  learning goal: take the pragmatic route.
- Keep recurring costs (cloud hosting, external APIs like the aggregator in epic 4, LLM calls) low — flag it if a change
  affects that.
- Clean, simple-to-read code, also where the logic is complex (matching, scoring): name things after the domain, keep
  methods small, and explain the *why* of non-obvious domain rules in Javadoc.

## Tech stack

- **Java 25**, **Spring Boot 4.1.x** (Spring Framework 7, Jackson 3 under `tools.jackson.*`). Boot 4 has Java 17 as its minimum baseline but first-class support
  for 25 (JSpecify null-safety, modular jars) — use those features where it makes sense, but check library compatibility (not
  every Spring dependency is equally far along with Java 25/virtual threads).
- **Maven** as build tool, multi-module reactor, pinned via the Maven Wrapper (`./mvnw`, version and checksum in
  `.mvn/wrapper/maven-wrapper.properties`).
- **SQL Server** as database (`mssql-jdbc`); tests run on in-memory H2 (`test` profile).
- **Lombok** and **MapStruct** as annotation processors; **ArchUnit** for architecture tests; **RestAssured** for
  integration tests.
- **Error Prone + NullAway** (JSpecify mode) in the compiler, **SpotBugs + FindSecBugs** and **Maven Enforcer** in
  `verify` — see "Static analysis" below.
- **Hosting: European provider, EU only — a hard requirement.** Not AWS or Azure, not even their EU regions: they remain
  under US jurisdiction. The provider itself is not decided yet; custom domain talmar.eu. Keep the application
  cloud-agnostic: configuration through Spring properties / environment variables, a standard container image, and no
  cloud SDK outside an adapter behind a port. Flag any change that would tie the code to one provider or send data
  outside the EU.


## Repository structure (multi-module)

Root modules (`pom.xml` `<modules>`): **`talmar-contracts`** and **`talmar-api`**. Root package for both:
`eu.talmar`.

- **`talmar-contracts`** — the API contract as source of truth. Contains the OpenAPI/YAML spec(s) (e.g.
  `src/main/java/eu/talmar/contracts/VacancyApi.yml`); shared models and API interfaces are generated from these via
  `openapi-generator-maven-plugin`.
    - **Never** hand-edit generated classes. Changes always go through the YAML.
    - Contract changes are breaking-change-sensitive: the frontend team consumes them today, and the multiposting
      aggregator and future integrations will later — treat the YAML as a published API.
    - Codegen is configured in `talmar-api/pom.xml` (`openapi-generator-maven-plugin`, bound to `generate-sources`, output
      in `talmar-api/target/generated-sources/openapi`). Generated code lands at the adapter edge, never under `domain`:
      models in `eu.talmar.api.vacancy.adapter.in.web.contract`, API interfaces in `eu.talmar.api.vacancy.adapter.in.rest`.
      A generated model landing under a `domain` package is a bug, not a style nit.
    - Note: generated packages are aggregate-first (`eu.talmar.api.vacancy.adapter...`), hand-written code is layer-first
      (see below). Don't "fix" this inconsistency as a side effect of another change.
- **`talmar-api`** — the application core, built following **DDD + Hexagonal (Ports & Adapters)**. Hand-written code is
  organised layer-first under `eu.talmar.api`:
    - `domain/<aggregate>/` — entities, value objects (`domain/<aggregate>/valueobject/`), domain services, domain
      events. Shared domain base types (`BaseObject`, `UuidV7Generator`) live directly in `domain/`. **Zero** framework
      dependencies, no Spring annotations.
    - `application/service/` — use-case orchestration and transactions.
    - `application/port/in/` (with `application/port/in/command/` for inbound command DTOs) and `application/port/out/`
      — ports owned by the core.
    - `adapter/in/rest/` — inbound REST adapter: cross-aggregate `GlobalExceptionHandler` at the root,
      `adapter/in/rest/<aggregate>/` per aggregate with `mapper/request/`, `mapper/response/` and `validation/`
      subpackages for explicit contract-DTO ↔ domain mapping.
    - `adapter/out/persistence/` — outbound JPA adapter (SQL Server): `BaseJpaEntity` and JPA auditing config at the root,
      `adapter/out/persistence/<aggregate>/` per aggregate with its own `mapper/` subpackage.
    - `shared/` — cross-cutting code: `shared/config/` (e.g. `WebConfig`, `ClockConfig`), `shared/exception/` (e.g.
      `BaseException`), `shared/gdpr/` (`@PersonalData`, `@ProcessData`).
    - Adapters know the domain, never the reverse. Generated contract models belong at the `adapter/in` edge, not in the
      domain model — map explicitly between contract DTOs and domain models, never leak contract types into `domain` or
      `application`. `ArchitectureTest` enforces the layer rules and the ban list below.

## Architecture principles for changes

- Apply tactical DDD where the domain is interesting (matching, scoring, screening, retention/anonymization — see epic 2, an
  explicit learning hook for domain modeling). For CRUD-ish edges: no dogma, just be pragmatic.
- New external integrations (aggregator, LLM providers) always go behind a port with an adapter — never inject the SDK/client
  directly into a use case.
- For an architecture decision with real trade-offs: capture it as a short ADR in `docs/adr/` (`NNNN-kebab-title.md`)
  instead of deciding it in code alone. A revised ADR keeps the superseded alternative and why it was rejected.
- GDPR/personal data: process data (events, channel source, rejection reason) and personal data (name, CV, email) are
  deliberately separated in the data model so anonymization can wipe the latter while leaving the former intact. New
  candidate-related fields: decide explicitly which category they belong to before adding them. Every field of a JPA
  entity, mapped superclass or embeddable carries exactly one of `@PersonalData` or `@ProcessData`; `@Id`, `@Version`,
  `@Transient` and associations are exempt (the entity they point to classifies its own columns).

## Code conventions (maintainer preferences)

These come from the maintainer's PR reviews. Follow them from the first commit; a reviewer should not have to repeat
them. When a review adds a new preference, add it here in the same PR.

**Validation: fail fast at the edge**
- Input validation (presence, length, format, allowed characters, enums) is part of the API contract and happens in
  the adapter, before the application layer runs: as constraints in the OpenAPI YAML (bean validation on the generated
  model), or in an adapter validator (`adapter/in/rest/<aggregate>/validation/`) when YAML cannot express it. The same
  goes for data from an outbound adapter (e.g. an LLM response): validate it in that adapter.
- The domain and application layers do not validate again. No null, blank, length or format checks in value objects
  or services for values the adapter has already checked. A null that slips through anyway is our bug and may surface
  as a 500.
- A rule the API promises is a contract rule, also when it needs a list the YAML cannot hold (e.g. "work e-mail
  addresses only" against a block list): it goes in an adapter validator, so a change to the API requirement only
  touches the adapter.
- The domain holds the business rules only: state transitions, expiry, derived values, membership. A business rule
  that needs a port lives in the use case.
- Contract fields get tight, realistic limits and a whitelist of allowed characters where free text is not needed
  (names: 35 characters of letters, spaces, `-`, `'`, `.`; e-mail: 75 characters). Every request body gets realistic
  `examples`, so Swagger UI does not generate gibberish from the patterns.
- Don't ask the user for what can be derived (e.g. the company name from the e-mail domain). Let them correct it later.

**Errors and exceptions**
- Exception handlers stay dumb: no logic, no branching on message or cause. Throw a specific exception for each
  response from the start, so changing a response never means changing handler logic. One exception handler per
  aggregate, next to its controller; shared constants live in a separate class (e.g. `shared.Constants`).
- Error responses only contain what the contract promises. Never show the user what exactly failed inside; log the
  detail at `error` level.
- Use the HTTP status for what it means. `401` is only for missing or expired authentication, never for an invalid
  token in a request body.
- No `serialVersionUID`/`@Serial` on exceptions: we never Java-serialize them.
- External calls distinguish retryable from non-retryable failures. When retrying cannot help, tell the user at once
  that the service is unavailable.

**Naming and structure**
- A name says exactly what happens. A method named `start…`, `generate…` or `send…` must do that. A method that only
  builds and returns an object is named like a factory or mapper. Name use-case fields after the use case
  (`startGenerationUseCase`, not `starter`).
- Keep methods small: extract each step into a private method named after the step (`getGeneration`,
  `validateResponse`, `processResponse`).
- Make visibility as narrow as possible. Fields that never change, such as ids, are `final`.
- Outside `domain`, use Lombok: `@Getter` and `@RequiredArgsConstructor`, no hand-written getters or constructors.
  Inside `domain`: no external dependencies at all (no Lombok, no JSpecify), so getters are written by hand.
- A sealed type lists its subtypes explicitly with `permits`.
- Prefer `application.yml` over a Java `@Configuration` class when Spring Boot can configure it through properties.
- `toString` contains ids and process data only: no personal data, and no large text fields that flood the logs.
- Fix static-analysis and SonarQube hints instead of ignoring them (e.g. use a record pattern in a `switch`).
- Versions of prompts and similar artifacts follow semver: `V1.0.0`. A prompt file starts with a template comment
  holding its version and a changelog (newest first) that the model never sees; bump the version with every change.
- Name query aliases and lambda parameters after what they are (`generation`, not `g`).

**Security and data**
- Don't settle for the minimum on security. Choose the strong recommended setting, name the cost (memory, latency,
  money) and record it (e.g. Argon2id with RFC 9106 parameters, not the OWASP baseline).
- Audit columns reference the user by `app_user` id (`lastModifiedBy`), never by name or e-mail address.
- Relations between tables are real foreign keys in JPA (e.g. `@ManyToOne`), even when the domain references another
  aggregate by id only.
- Classify a field as personal data only when it can identify a specific person. Free text that describes a job or a
  team (e.g. the answers for AI vacancy text) is process data.

**Tests**
- Every assertion and every `verify` checks behaviour that matters. Remove checks that test nothing; they confuse
  more than they protect.
- Assert against the input you passed in (`assertEquals(request.summary(), result.summary())`), not against literals
  typed out a second time.
- Contract validation is tested where it happens: through the REST adapter (integration test), not in domain tests.
- More test rules (mocks, integration tests, Sonar hints) are in the `unit-testing` skill
  (`.claude/skills/unit-testing/SKILL.md`); read it before writing or changing tests.

**Processing a review**
- Reply to every review comment in its thread: what changed and in which commit, or why it did not change. Answer a
  question with reasoning. If you disagree, say so and explain why; the maintainer decides.

## Application docs

`docs/application/` describes how the application works *as it is on `main`*: `overview.md` for the module layout,
request flow and cross-cutting behaviour, and one page per aggregate (e.g. `vacancy.md`) for its model, use cases,
persistence and known gaps. It is written for the next agent or developer: what is built, how it fits together, and
what a change there affects.

- **Update the docs in the same PR as the code.** A PR that adds or changes behaviour, an endpoint, a domain rule, a
  persistence mapping or configuration updates the matching page; a new aggregate gets a new page. The docs are
  reviewed together with the code — never generate or rewrite them after the merge.
- Describe the current state only. Plans belong in this file or an ADR, decisions and their rationale in `docs/adr/`.
- Keep it short and factual: explain the *why* and the impact, don't restate the code line by line.
- When you find a bug or inconsistency you are not fixing, add it under "Known gaps" on the matching page. Remove the
  entry in the PR that fixes it.

## Build & test

Always use the wrapper (`./mvnw`), never a globally installed `mvn`.

```
./mvnw clean install                           # full build incl. contract codegen, tests and checks
./mvnw verify                                  # compile, all tests, format check — the definition of "green"
./mvnw -pl talmar-contracts -am generate-sources   # regenerate contract only
./mvnw -pl talmar-api -am test                  # tests for the api module only (fast inner loop)
./mvnw spotless:apply                          # fix formatting (google-java-format)
```

### Static analysis

- **Compiler warnings fail the build** (`-Werror`): Error Prone bug patterns, MapStruct unmapped properties, javac
  warnings. Fix the cause; for a MapStruct target that is deliberately not mapped, add `@Mapping(target = …, ignore = true)`.
- **NullAway** checks only packages annotated with `@NullMarked` (JSpecify) in `package-info.java`; currently
  `application..`. Everything there is non-null unless annotated `org.jspecify.annotations.@Nullable`. A new package
  under `application` gets a `package-info.java` with `@NullMarked`; adopt it in adapters package by package. Never fix
  a NullAway error with a blanket `@Nullable` — decide whether the value can really be absent. **`domain` stays free of
  JSpecify** (maintainer decision: zero external dependencies there, `ArchitectureTest` enforces it). The maintainer
  accepts the risk of a null there instead: the domain does not re-check request input the adapter has validated (see
  "Code conventions"). `Objects.requireNonNull` in an aggregate builder is only for state the aggregate itself must
  always have.
- **SpotBugs + FindSecBugs** run in `verify`. Generated code is excluded in `config/spotbugs-exclude.xml`, which is a
  guardrail file: fix a finding instead of adding an exclusion.
- **Maven Enforcer** requires Maven 3.9+ and Java 25+, dependency convergence, and bans Jackson 2
  (`com.fasterxml.jackson.core:jackson-databind`; `jackson-annotations` is fine, Jackson 3 uses it), `javax.*`, JUnit 4
  and log4j 1. Don't exclude a banned artifact to make it pass; find the dependency that pulls it in.
- **Tests run in random order** (classes and methods). The seed is printed at the start of the run ("Random test order
  seed"); reproduce with `./mvnw -pl talmar-api -am test -Dtest.order.seed=<seed>`. An order-dependent failure is a test
  isolation bug — fix the shared state, don't pin the order.
- **ArchUnit ban list** (`ArchitectureTest`, production code unless noted): no Jackson 2 (`com.fasterxml.jackson`
  outside `.annotation`), no Java EE `javax.*` (use `jakarta.*`), no `java.util.Date`/`Calendar`/`SimpleDateFormat`/
  `java.sql` time types, no `System.out`/`System.err`/`printStackTrace` (use `@Log4j2`), no field injection (MapStruct
  mappers with `uses` need `injectionStrategy = InjectionStrategy.CONSTRUCTOR`), `@Transactional` only in
  `application.service`, no `now()` on `java.time` types without a `Clock` (production and tests — inject the `Clock`
  bean; the domain receives timestamps, not a clock), no `Thread.sleep`/`TimeUnit.sleep` in tests.

Use Maven/the linter for style and compile errors — not Claude as a linter. Run existing tests/checks yourself via bash
rather than relying on your own judgment of correctness. `./mvnw clean install` is not cheap — run it once per
verification pass, not repeatedly "to be sure"; only re-run it if you have a concrete new reason to suspect flakiness (a
fresh change to test isolation/config), not because an area was flaky once before.

## Guardrails

Deterministic checks are the source of truth for "does it work" — not an agent's own judgment. The rationale and the
roadmap of guardrails are in `docs/adr/0001-agentic-development-guardrails.md`.

- **Done means `./mvnw verify` is green.** Never report a task as done on a red or unrun build.
- **Never weaken a check to get green.** No skipping tests or checks (`-DskipTests`, `-Dmaven.test.skip`,
  `-D*.skip=true`, `--no-verify`), no `@Disabled`, no new `@SuppressWarnings`, no deleting or loosening assertions, no
  editing `ArchitectureTest` to make a violation pass. If a check is wrong, say so and stop — the human decides.
- **Formatting is checked, not auto-applied** by the build (`spotless:check` in `verify`). Run `./mvnw spotless:apply`
  before finishing.
- **Agents never merge, force-push, or label PRs.** Agents push branches and open PRs; only the human maintainer merges
  and applies override labels. `gh pr merge` is denied in `.claude/settings.json`.
- **Keep PRs reviewable**: aim for at most ~400 changed lines of production code and config per PR. Split larger work
  into a sequence of PRs that each leave the build green.
- **Changes to guardrails are changes to the safety net** (`.github/`, `.claude/`, `.agents/skills/`,
  `skills-lock.json`, `ArchitectureTest`, build plugin config): call them out explicitly in the PR description, never
  bundle them silently with feature work.
- **Third-party skills** (`.agents/skills/`, pinned by hash in `skills-lock.json`) are instructions agents execute —
  review an update like a dependency bump.

### Agent hooks (`.claude/hooks/`, wired in `.claude/settings.json`)

| Hook | Effect |
|------|--------|
| `guard-bash.sh` (PreToolUse, Bash) | **Blocks** skip/ignore flags (`-D…skip`, `-D…ignore`, `--fail-never`), `--no-verify` / `commit -n` / `core.hooksPath`, force/mirror/delete pushes and pushes to `main`, `reset --hard`, `clean -f`, discarding the whole tree, `gh pr merge`, label changes, branch-protection and repo-setting changes. **Asks** before a shell command writes to guardrail files. |
| `guard-paths.sh` (PreToolUse, Edit/Write) | **Blocks** edits to `target/` and generated sources. **Asks** before editing `.claude/`, `.github/`, `.githooks/`, `.agents/`, `.mvn/`, `config/`, `mvnw`, `skills-lock.json` or `ArchitectureTest.java`. |
| `verify-on-stop.sh` (Stop) | Runs `./mvnw spotless:apply` + `./mvnw verify` when build-relevant files changed since the last green run and **blocks finishing while red** (3 attempts, then the human is warned). State and log in `.git/claude-guard/`. |

- When a hook blocks you, fix the cause — never work around a hook (e.g. by moving the command into a script file).
  If the hook is wrong, stop and tell the human.
- After changing a hook, run `.claude/hooks/test-hooks.sh` and extend it with a case for the change.
- `CLAUDE_SKIP_VERIFY_ON_STOP=1` (set by the human when starting Claude Code) disables the Stop hook for a session.

### CI (`.github/workflows/`) — the authoritative gate

| Job | Checks |
|-----|--------|
| `build` | `./mvnw verify` (compile, tests, ArchUnit, format check), CycloneDX SBOM, OSV-Scanner vulnerability scan of the SBOM |
| `checks` | gitleaks over the full history, actionlint and shellcheck, `.claude/hooks/test-hooks.sh` and `.github/scripts/test-guardrail-diff.sh`, `oasdiff` breaking-change check of the contracts against `main` (PRs only) |
| `guardrail-diff` (own workflow, PRs only) | `.github/scripts/guardrail-diff.sh` compares the PR with `main`; each finding fails unless the maintainer applies its override label (table below) |

- All three jobs must be green to merge into `main`. A red CI is never fixed by weakening a check.
- `guardrail-diff` runs on `pull_request_target`: the script from `main` judges the PR, so editing the script in a PR
  does not change the verdict on that PR. After changing it, run `.github/scripts/test-guardrail-diff.sh` and extend it
  with a case for the change.
- **Known vulnerability**: upgrade the dependency. If Spring Boot doesn't manage a fixed version yet, pin it under
  "security overrides" in the root `pom.xml` (ahead of the Spring Boot BOM), name the advisory IDs in the commit
  message (not in the pom), and remove the pin once Spring Boot catches up. A dependency Spring Boot does not manage
  at all gets a plain `<name>.version` property, not an override. Never ignore a vulnerability without the human.
- **Breaking contract change**: make it backwards compatible (e.g. add optional fields, deprecate before removing). An
  intended break is the human's decision.
- Actions are pinned by commit SHA, downloaded tools by SHA-256 (`env` in `ci.yml`). Bump version and hash together.
  Dependabot opens weekly grouped update PRs for Maven dependencies and GitHub Actions.

| `guardrail-diff` finding | Override label (human-only) |
|--------------------------|-----------------------------|
| More suppressions than `main` (`@SuppressWarnings`, `@Disabled`, assumptions, `NOSONAR`, `spotless:off`, `<skip…>true`, `skip…=true`, `continue-on-error: true`, …) | `suppression-ok` |
| Fewer test methods than `main`, or a test class deleted | `test-removal-ok` |
| More than 400 changed lines of production code and config, or 800 of tests (docs and `.agents/` excluded) | `large-pr-ok` |
| Guardrail files changed (`.github/`, `.claude/`, `.githooks/`, `.agents/`, `.mvn/`, `config/`, `mvnw`, `skills-lock.json`, `ArchitectureTest`, a `<build>` section in a `pom.xml`) | `guardrail-change-ok` |
| JPA entity (`@Entity`, `@Embeddable`, `@MappedSuperclass`) or `ddl-auto` changed | `schema-change` |
| `CLAUDE.md` names a path that does not exist | none — fix `CLAUDE.md` |

- An agent fixes the cause of a finding; when the finding is intended, it says so in the PR description and leaves the
  label to the maintainer.

### One-time setup per clone (human)

```
brew install gitleaks
ln -s ../../.githooks/pre-commit .git/hooks/pre-commit   # secret scan on every commit
```

## Language policy (strict)

**Everything in this codebase is in English.** No exceptions:

- **Code**: identifiers, method/class/variable names, log messages, exception messages.
- **Javadoc and comments**: English only.
- **Commit messages and PR descriptions**: English only.
- **ADRs and in-repo docs**: English only.

Dutch is fine in conversation with the human developer but never leaks into anything that ends up in the repository.

## Working style in this repo

- Be critical and direct in code review and proposals — no cheerleading. If an approach is weak or lets scope creep, say so.
- When torn between the "clean/academic variant" and the "pragmatic variant": name both with trade-offs, make the choice
  explicit.
