# ADR 0001 — Deterministic guardrails for agentic development

- **Status:** Accepted
- **Date:** 2026-10-06

## Context

Most code in this repository is written by AI coding agents and reviewed and merged by a single human maintainer. Until
now there was no CI and no automated gate: the only checks were the tests and ArchUnit rules an agent chose to run. The
formatter (`spotless:apply`) silently rewrote code instead of failing, and the agent instructions referenced files that
no longer existed.

Agents fail in recognisable ways that ordinary code-quality tooling does not target:

- making a build green by weakening it (skipping tests, `@Disabled`, suppressions, loosened assertions);
- writing code for older framework versions (Spring Boot 3, Jackson 2, `javax.*`) because those dominate training data;
- changing observable API behaviour (field names, null handling, date formats) as a side effect of a refactor;
- silently changing the database schema (`ddl-auto: update` applies entity changes to production);
- producing diffs too large for one human to review properly;
- following stale or third-party instructions (outdated `CLAUDE.md`, installed skills).

Constraints:

- Solo developer, startup pace: guardrails must be cheap to set up and maintain.
- The repository is public now but **may become private**. On a private repo under a personal account, GitHub Code
  Security (CodeQL, dependency review) and Secret Protection (push protection) are not available at all — they are sold
  only to organisation plans. GitHub Actions minutes drop from unlimited to 2,000 (Free) / 3,000 (Pro) per month.

## Decision

### Principles

1. **Every guardrail that must survive going private runs inside Maven or as a plain CLI** (not as a GitHub-only feature).
   GitHub-native features are an optional bonus layer that can be removed without losing coverage.
2. **Two layers.** Claude Code hooks give the agent fast feedback inside its own loop; CI is the backstop the agent cannot
   bypass. Hooks and permission rules are *not* a sandbox (an agent can reach a file via Bash), so CI is authoritative.
3. **Checks fail the build; they don't silently fix things.**
4. **Override labels are human-only.** CI cannot tell whether a label was added by the maintainer or by an agent using the
   maintainer's `gh` credentials, so the agent-side hook blocks agents from adding labels.
5. **Each PR that adds a guardrail updates `CLAUDE.md` in the same PR**, so instructions never lag behind enforcement.

### Rollout (one small PR each)

| PR | Guardrail | Layer |
|----|-----------|-------|
| 1 | Clean-up: remove dead hook and Speckit references, rewrite `CLAUDE.md`, Maven Wrapper with checksum pin, `spotless:check` instead of `apply`, deny `gh pr merge`, narrow `gh` allowlist, this ADR | repo |
| 2 | Claude Code hooks: block bypass commands (skip flags, `--no-verify`, force push, `reset --hard`, `gh pr merge`, adding labels); block writes to `target/` and generated sources; ask before editing guardrail files and skills; Stop hook formats the code and requires green `./mvnw verify`; Gitleaks pre-commit | agent |
| 3 | GitHub Actions in two jobs (each job is billed rounded up to a minute on private repos): `build` (`./mvnw -B verify`, CycloneDX SBOM, OSV-Scanner on the SBOM) and `checks` (Gitleaks, actionlint, hook regression tests, `oasdiff` breaking-change check); actions pinned by commit SHA, tools by SHA-256; Dependabot (Maven + Actions); branch protection on `main` | CI |
| 4 | `guardrail-diff` job: suppression count vs `main`; deleted tests / lower test count; PR size limit (~400 lines production + config, ~800 tests); skills and guardrail-config changes; `schema-change` label on JPA entity changes; paths named in `CLAUDE.md` must exist | CI |
| 5 | Error Prone + NullAway (JSpecify mode, adopted per package via `@NullMarked`), Maven Enforcer, SpotBugs + FindSecBugs, random test order | build |
| 6 | ArchUnit ban list for typical agent mistakes (Jackson 2, `javax`, `java.util.Date`, `System.out`, field injection, `@Transactional` outside services, `now()` without `Clock`, `Thread.sleep` in tests); GDPR rule: every persistence-entity field is `@PersonalData` or `@ProcessData` | build |
| 7 | Integration-test responses validated against the OpenAPI spec (RestAssured filter) | build |
| 8 | Schema snapshot test: generated SQL Server DDL compared to a committed `schema-snapshot.sql` | build |
| 9 | Startup smoke test in CI: jar with `dev` profile against a SQL Server service container, `/actuator/health` must be UP | CI |

### Deferred, with explicit triggers

| Guardrail | Trigger |
|-----------|---------|
| PIT mutation testing on `domain` and `application.service` (`withHistory`) | Epic 2 brings real domain logic (matching, scoring) |
| WireMock (`wiremock-spring-boot` 4.x) | First outbound adapter (aggregator, LLM provider) |
| Flyway (`spring-boot-starter-flyway` + `flyway-sqlserver`), Testcontainers SQL Server, `ddl-auto: validate` | Before real candidate data reaches production |
| CodeQL | Optional while the repo is public, as a separate workflow to delete when going private |

## Alternatives considered and rejected

- **SonarQube Cloud as the quality gate.** Free only for public repos, configuration lives in its UI rather than in the
  repo, and agents only see results after a push. Error Prone + NullAway + SpotBugs fail `./mvnw compile`/`verify`
  locally with the same result for agent, human and CI.
- **OWASP dependency-check.** Needs an NVD API key, is slow and noisy, and overlaps with OSV-Scanner and Dependabot.
- **Trivy.** Its GitHub Action and binary were compromised in a supply-chain attack in March 2026.
- **JaCoCo coverage gate.** Line coverage rewards tests that execute code without asserting on it — the typical agent
  failure. Mutation testing (PIT) measures what matters and is deferred until there is domain logic worth it.
- **Flyway + Testcontainers now.** Rejected for now by the maintainer: the SQL Server image is amd64-only and slow on
  Apple Silicon, and there is no production data yet. Mitigated by the schema snapshot (PR 8), the `schema-change`
  label (PR 4) and the SQL Server smoke test (PR 9). The snapshot becomes the first Flyway migration later.
- **Requiring approval for every `pom.xml` edit.** Rejected by the maintainer as too much friction; dependency risk is
  covered by OSV-Scanner, Dependabot and Maven Enforcer.
- **LLM-as-judge review bots in CI.** Useful as advice, but not deterministic and they cost tokens on every PR.

## Consequences

- Five new mechanisms to maintain: hooks, CI workflows, the `guardrail-diff` script, Maven plugins and ArchUnit rules.
  All of them work on a private repository without paid features.
- Estimated CI time ~5–6 minutes per PR including the smoke test (~350 PR runs per month within 2,000 private minutes).
- Formatting is no longer applied automatically by the build; IDE formatting, the agent hook (PR 2) or
  `./mvnw spotless:apply` must be used.
- OSV-Scanner must scan an SBOM produced by Maven, not `pom.xml`: on this multi-module build it cannot resolve the
  internal `talmar-contracts` module and silently reports zero packages and zero vulnerabilities. The first real scan
  found 14 known vulnerabilities (4 critical), fixed by upgrading Spring Boot to 4.1.1 and pinning patched Jackson and
  Tomcat versions.
- The `guardrail-diff` job (PR 4) runs as its own workflow on `pull_request_target`, with the PR head checked out as
  data only: a PR cannot weaken the script that judges it, and adding a label re-runs only this job. The cost is that a
  change to the script is not exercised by that job until it is merged; `test-guardrail-diff.sh` in `ci.yml` covers the
  PR's own copy instead. Rejected: running it on `pull_request` in `ci.yml` (the PR's version of the script would judge
  the PR, and every label change would re-run the full build).
- Branch protection (and rulesets) on a private repository requires a paid plan (GitHub Pro for a personal account).
  Without it, CI still runs but no longer blocks merges.
- Static analysis (PR 5) runs inside `./mvnw verify`: Error Prone and NullAway as javac plugins with `-Werror`, so
  any compiler warning fails the build; SpotBugs + FindSecBugs and Maven Enforcer as plugins. Error Prone works
  together with Lombok and MapStruct; it needs `--add-exports`/`--add-opens` for `jdk.compiler` in `.mvn/jvm.config`.
  NullAway runs in JSpecify mode on `@NullMarked` packages only (`application` first), so adoption can grow package
  by package without a big-bang annotation pass. `domain` is deliberately not null-marked: it keeps zero external
  dependencies, JSpecify included, and the maintainer accepts the null risk there in exchange (domain code checks for
  null itself). Rejected: allowing JSpecify in the domain as an annotation-only dependency. The SpotBugs exclude filter lives in `config/` and is a
  guardrail file. Tests run in random class and method order with a per-build seed that is printed and can be
  replayed with `-Dtest.order.seed`; rejected: Surefire's `runOrder=random`, which only shuffles classes.
- The ArchUnit ban list and the GDPR rule (PR 6) live in `ArchitectureTest`, which is already a guardrail file, so
  the hooks and `guardrail-diff` protect them without new patterns. Rejected: a separate rules class, which would have
  needed new guard patterns to get the same protection. The `javax` ban names the Java EE packages that moved to
  `jakarta` instead of all of `javax..`, because `javax.annotation.processing`, `javax.crypto` and `javax.sql` are
  still part of the JDK. The GDPR rule exempts `@Id`, `@Version`, `@Transient` and associations, because they are keys
  and links whose target entity classifies its own columns. To meet the rules, the domain now receives `createdAt`
  from the application service, which takes it from an injected `Clock` bean. Rejected: passing the `Clock` into the
  domain, because MapStruct cannot supply it when it rebuilds an aggregate from the database. MapStruct mappers that
  use other mappers switched to constructor injection, set per mapper. Rejected: the global
  `mapstruct.defaultInjectionStrategy` compiler option, which would change the build config for two mappers. Every
  custom rule was checked against a temporary violation to make sure it actually fails.
- Open risks to verify during rollout: Jackson 3 support in `swagger-request-validator` (PR 7), offline SQL Server DDL
  generation with Hibernate (PR 8), PIT on JUnit 6 (deferred).
