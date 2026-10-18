# Talmar backend agent team — learnings

Retro notes appended by talmar-scrummaster after each completed task/epic. Every subagent
on this team reads this file before starting work — keep entries concrete and actionable.

## 2026-09-05 — GitHub issue #79, "Write vacancytext manually"

**What worked**
- Single trailing `[review-gate]` task (plan Task 12) instead of per-task review kept the developer
  moving TDD through all 11 tasks back-to-back without interruption, and the reviewer still caught a
  real bug: they specifically checked that the upsert-by-`vacancyId` logic in
  `VacancyRepositoryAdapter` doesn't silently accumulate duplicate `VacancyDescriptionJpaEntity` rows
  on a second write — the exact failure mode the plan's own risk #5 called out in advance. Naming a
  known-risky spot in the plan up front, then having the reviewer verify it specifically rather than
  trusting the diff, is worth repeating.
- Reviewer verified a new pom dependency by running `mvn dependency:tree` rather than trusting the
  developer's description of it — don't take a teammate's claim about dependency shape on faith when a
  one-command check is cheap.
- The architect's plan surfaced two real pre-existing bugs while researching the feature, unrelated to
  #79 itself (`Vacancy.Builder` had no way to reconstruct an existing id, blocking any future
  `findById`; `GlobalExceptionHandler` had no 404/generic-500 handling, which also silently affected
  the existing `CreateVacancyService`'s DB-failure path) — both were fixed as explicit, separately
  justified prerequisite tasks (ADR 0004 / plan Task 1, Task 7) rather than folded silently into the
  main change. Surfacing "found while researching, fixing as a named prerequisite" is a good pattern:
  it keeps the fix visible and reviewable instead of being scope creep buried in an unrelated diff.
- ADR 0004 kept both the architect's original recommendation and the human's overriding decision, with
  the rejected alternative's reasoning still written out (Decisions 1 and 3). When the human later
  needs to revisit the trade-off, the "why we didn't do the textbook version" is still on record instead
  of having been silently deleted. Worth reinforcing as the standard: a revised ADR should read as
  "decision + superseded alternative + why," never as a quiet rewrite.

**What wasted tokens or time**
- Two architects (`talmar-architect-79` and `talmar-architect-79-2`, the latter on a different model)
  were run on the same task at the same time. The team lead saw the first architect go idle
  mid–`mvn test` and, on a user request to switch models, spawned a fresh duplicate agent with the full
  original prompt instead of resuming/checking the first one — the first agent wasn't actually stuck,
  it was just idle-notified at a normal tool-call boundary. The second agent found the plan/ADR files
  already on disk from the first and re-derived the same conclusions before being told to stand down.
  Net effect: a full duplicate planning pass burned for no benefit (the same plan and ADR were produced
  twice).
- **Fix**: before spawning a replacement agent for one that reports idle/interrupted, send it a message
  first and check whether it responds or is genuinely blocked (e.g. on a permission prompt). Only spawn
  a fresh duplicate if that check confirms it's actually stuck — being idle right after a tool call is
  the normal state between turns, not evidence of being blocked. If a model switch is genuinely needed
  for an agent that's fine, prefer resuming the same agent under the new model/config where the harness
  allows it, rather than a full duplicate from scratch.

**What to change**
- **Environment/infra gap, not a teammate behavior fix**: the team playbook
  (`.claude/commands/backend-team.md`) and the `require-review-signoff.sh` hook assume a real shared
  task-tracking tool (create/claim/complete task objects with `task_subject`/`task_description`) is
  available to the team lead, backend developer, and code reviewer. In this run no such tool was ever
  available — the "task list" was only prose in `.claude/planning/79-write-vacancytext-manually.md`,
  and the review sign-off ended up being recorded by hand (team lead directly edited the plan file to
  add "Reviewed-by: talmar-code-reviewer" under Task 12, as seen in this file's Task 12 status line)
  because there was nothing to formally mark complete. This doesn't need a workaround from future
  teammates — it needs whoever configures agent teams/hooks in this environment to either provision the
  expected task-tracking tool or update the playbook and hook to match the prose-plan-file reality.
  Flagging here so it isn't silently rediscovered and hand-patched the same way on every future run.
- When an idle-notification comes in for a teammate mid-verification-run (e.g. mid `mvn test`), treat it
  as expected rather than reflexively escalating to a duplicate agent spawn — see the fix above.

## 2026-09-06 — PR #80 review-feedback fix cycle

**What worked**
- Skipping Speckit for a "fix reviewer feedback" cycle and scoping directly from the review comments
  (`.claude/planning/pr-80-feedback.md`) worked fine because the comments (RC-1 through RC-8) were
  bounded and explicit enough to plan from without a full spec. Not every change needs the full
  spec→plan→tasks pipeline — a review-comment list can be a sufficient planning input on its own when
  it's this concrete. Don't default to Speckit reflexively for feedback-fix cycles; ask.
- The architect verified which review comments were already resolved by *reading current file state*
  (RC-1 through RC-6 turned out to already be fixed by commit `2d7f7a2`) instead of trusting the
  review-comment list at face value. This avoided planning six no-op tasks. Always re-verify a review
  comment against the live file before turning it into a task — comment lists go stale the moment any
  commit lands after they were written.
- RC-7 (introduce `@OneToOne` between `Vacancy`/`VacancyDescription`) directly reversed a prior explicit
  decision (ADR 0004 Decision 1, which had rejected `@OneToOne`). The architect wrote ADR 0005 with a
  design (shared-PK relation) that answered ADR 0004's original lazy-loading/cascade objection, but
  did **not** silently override the earlier ADR — it flagged the reversal, and a second scope item
  (JPA auditing as a prerequisite), for explicit human sign-off via the team lead. Both were approved
  as proposed. Reinforcing the pattern from the 2026-09-05 entry: a decision that reverses a prior ADR
  must be flagged for explicit re-confirmation, never quietly overwritten just because a new reviewer
  disagreed with the old one.
- Mid-implementation, the developer discovered the plan/ADR 0005 was wrong about an implementation
  mechanic: `orphanRemoval` doesn't fire when Hibernate merges a freshly-built transient parent graph
  (no prior loaded snapshot to diff against) — verified empirically via SQL logging with an explicit
  flush showing no `DELETE` was emitted. Rather than either blindly following the wrong plan or silently
  deviating, the developer kept the plan's overall shape, added a narrowly-scoped `deleteById()` call
  on just the null-description branch, and surfaced the finding to the architect, who documented it as
  an ADR 0005 addendum instead of leaving the design doc wrong. Good pattern to repeat: a plan/ADR can
  be correct on architecture but wrong on framework-level mechanics that only surface once you actually
  try it — the fix is an addendum to the doc plus a proactive heads-up, not silent deviation and not
  dogmatic adherence to a plan proven wrong by evidence.

**What wasted tokens or time**
- **Near-miss**: the documentation-writer started its pass, ran `git status`/diff-equivalent checks,
  and found all of the feature's implementation work still uncommitted (this team doesn't commit
  mid-feature — see prior entry's note that even review sign-off gets hand-recorded in the plan file
  rather than as a commit). It misread "not committed" as "not actually implemented," and self-reported
  a false "critical issue" — claiming credit/blame for having itself implemented production code (the
  `@OneToOne` redesign, the adapter rewrite) when its actual edits were only Javadoc and one clarifying
  test comment, exactly as scoped. The team lead had to stop and diff the actual files by hand to
  confirm no revert was needed, costing a few round-trips (doc-writer alarm → team lead verification →
  all-clear) even though no actual harm occurred, since the doc-writer correctly stopped and asked
  rather than trying to unilaterally "fix" what it thought was wrong.
- **Root cause**: the doc-writer's mental model assumed "uncommitted at the point I start my pass" is
  anomalous, when on this team it's the norm — nothing is committed until the final review-gate task
  completes. It also had no earlier commit to diff its own edits against, so it couldn't tell "already
  there before I touched it" apart from "my own edit" just by staring at working-tree state.

**What to change**
- **Doc-writer (and any late-stage teammate that reads working-tree state at task start)**: before
  concluding that uncommitted code implies a process violation or missing implementation, check whether
  this team's convention is "commit only at the final review-gate" (it is, per this and the prior
  entry) — uncommitted-but-present code mid-feature is expected, not evidence of anything wrong. If a
  late-stage agent needs to distinguish "pre-existing work" from "my own edit," it should diff against
  its own tool-call history/edits in this conversation (what *it* wrote), not against git history that
  doesn't exist yet — and if genuinely unsure, ask the team lead directly ("is X already implemented or
  did I just write it?") rather than self-reporting a critical issue as fact.
- Consider whether task-list checkpoints (e.g. after each numbered task in a plan like
  `pr-80-feedback.md`) warrant a lightweight intermediate commit specifically so any teammate joining
  later in the cycle has something concrete to diff against — this is a real, recurring cost (two
  retros in a row have now hit friction from the no-commit-until-the-end convention) and is worth
  weighing against this team's evident preference for a single clean final commit per feature.
