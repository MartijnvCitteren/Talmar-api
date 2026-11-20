# ADR 0008 — Package by business module, not by AI capability

- **Status:** Accepted
- **Date:** 2026-11-20
- **Supersedes:** the package location of the description generation in ADR 0002 section 1 (`domain/generation/`).

## Context

Hand-written code is layer-first (`domain`, `application`, `adapter`) and, within each layer, was grouped per
aggregate. AI vacancy description generation (ADR 0002) got its own top-level group, `generation`, in every layer:
`domain/generation/`, `application/service/generation/`, `adapter/in/rest/generation/` and so on.

But `generation` names a technique (the text is made by an LLM), not a business area. Everything in it is about the
vacancy: the API paths are `/vacancy/{id}/generate-description…` in `VacancyApi.yml`, the generated `GenerationApi`
already lands in the vacancy package, and starting a generation loads the `Vacancy`. The roadmap adds more generated
artefacts: vacancy images, marketing text, e-mails to candidates. Under the old layout they would all end up in
`generation`, grouping unrelated business areas only because they call an LLM, and splitting each area's code over two
places.

## Decision

1. **Within each layer, code is grouped per business module** (`vacancy`, `identity`), not per technique. A module
   holds one or more aggregates.
2. **AI capabilities live in the module whose concept they produce**: vacancy text and images in `vacancy`, a
   candidate e-mail with the candidate. There is no top-level `generation` or `ai` package outside `adapter/out`.
3. **A sub-area with its own lifecycle gets a sub-package, mirrored in every layer.** The description generation moves
   to `domain/vacancy/generation/`, `application/{port/in,port/out,service}/vacancy/generation/`,
   `adapter/in/rest/vacancy/generation/` and `adapter/out/persistence/vacancy/generation/`. Small, tightly linked
   aggregates may share the module package instead (`User` and `Company` in `domain/identity/`).
4. **`VacancyDescriptionGeneration` stays a separate aggregate from `Vacancy`.** Only its package moves; the reasons
   of ADR 0002 (different owner, lifetime and transaction boundary) still hold.
5. **Outbound adapters stay grouped by technology** (`adapter/out/ai/mistral/`, `adapter/out/keycloak/`). That is
   what an adapter is. When a second AI capability arrives, its prompts and response types get a sub-package there;
   the shared client configuration stays at the top.
6. Nothing outside the Java packages changes: API paths, the contract and its `Generation` tag, generated packages,
   class names, the table `vacancy_description_generation` (named explicitly in `@Table`) and the
   `taldo.generation.*` properties.

## Alternatives considered

- **Keep `generation` as a top-level module.** Rejected: it groups by technique, so every future generated artefact
  would land there regardless of the business area it belongs to.
- **Flatten the generation into the vacancy packages** (`VacancyDescriptionGeneration` next to `Vacancy`). Rejected:
  it blurs the aggregate boundary ADR 0002 drew on purpose, and puts two status enums and two sets of exceptions side
  by side.
- **A generic `ai` module now**, holding the asynchronous job lifecycle (`PENDING` → `COMPLETED`/`FAILED`, timeout,
  retention) and the AI provenance (model, model version, prompt version) that every AI feature needs. Deferred, not
  rejected: with one AI feature there is nothing to share yet. Extract these mechanisms into a small shared package
  when the second AI feature needs them; the audit trail of recommendations (`CLAUDE.md`) is the likely trigger.

## Consequences

- Package names change for about 30 production classes and their tests; behaviour does not.
- `guardrail-diff` reports a `schema-change` because the JPA entity file moved, although the table and its columns are
  unchanged.
- `CLAUDE.md` describes the layout per module instead of per aggregate.
