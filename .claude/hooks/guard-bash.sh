#!/usr/bin/env bash
# PreToolUse hook (Bash): blocks commands that bypass or weaken the guardrails, and asks the human
# before a shell command writes to guardrail files. See docs/adr/0001-agentic-development-guardrails.md.
#
# Not a sandbox: a script file executed by the agent is opaque to this check. CI is the authoritative gate.
set -euo pipefail

cmd=$(jq -r '.tool_input.command // ""')
[ -z "$cmd" ] && exit 0

deny() {
  echo "Blocked by .claude/hooks/guard-bash.sh: $1" >&2
  echo "Fix the underlying problem instead. If this check is wrong, stop and tell the human." >&2
  exit 2
}

ask() {
  jq -n --arg r "guard-bash.sh: $1" \
    '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $r}}'
  exit 0
}

has() { grep -Eq -- "$1" <<<"$cmd"; }

S='[[:space:]]'
END='([[:space:]=;&|)]|$)'
# Rest of the same command segment: patterns must not match across `&&`, `;` or `|`
# (e.g. pushing a branch and then running `gh pr create --base main` is fine).
ARGS='[^;&|]*'
TOKEN='[^[:space:];&|]+'
GIT="(^|[^[:alnum:]_./-])git${S}+"
GH="(^|[^[:alnum:]_./-])gh${S}+"

# --- Weakening the build -------------------------------------------------------------------------
has "-D[^[:space:]=]*([sS]kip|[iI]gnore)" &&
  deny "skipping or ignoring tests/checks via -D...skip / -D...ignore is not allowed."
has "${S}(--fail-never|-fn)${END}" &&
  deny "--fail-never hides build failures."

# --- Bypassing git hooks -------------------------------------------------------------------------
has "--no-verify" && deny "--no-verify bypasses the git hooks."
has "${GIT}commit(${S}+${TOKEN})*${S}+-[[:alpha:]]*n[[:alpha:]]*${END}" &&
  deny "git commit -n bypasses the git hooks."
has "core\.hooksPath" && deny "changing core.hooksPath disables the git hooks."

# --- Destructive or review-bypassing git operations ----------------------------------------------
has "${GIT}push(${S}${ARGS})?${S}(--force|--force-with-lease|-f|--mirror|--delete|-d)${END}" &&
  deny "force/mirror/delete pushes are not allowed."
has "${GIT}push(${S}${ARGS})?${S}\+[^[:space:]]" && deny "force-pushing via a +refspec is not allowed."
has "${GIT}push(${S}${ARGS})?${S}([^[:space:]]*:)?(main|master)${END}" &&
  deny "pushing to main is not allowed; push a branch and open a PR."
has "${GIT}reset(${S}${ARGS})?${S}--hard" && deny "git reset --hard discards work."
has "${GIT}clean(${S}${ARGS})?${S}-[[:alpha:]]*f" && deny "git clean -f deletes untracked files."
has "${GIT}(checkout|restore)(${S}+--)?${S}+\.${END}" && deny "discarding the whole working tree is not allowed."

# --- GitHub: merging, labels and repository settings are human-only ------------------------------
has "${GH}pr${S}+merge" && deny "only the human maintainer merges PRs."
has "${GH}${ARGS}(--label|--add-label|--remove-label)${END}" &&
  deny "override labels are applied only by the human maintainer."
has "${GH}(pr|issue)${S}+create(${S}${ARGS})?${S}-l${END}" &&
  deny "override labels are applied only by the human maintainer."
has "${GH}label${S}" && deny "managing labels is human-only."
has "${GH}api${S}${ARGS}(/merge|/labels|/protection|/rulesets)" &&
  deny "merging, labels and branch protection via the API are human-only."
has "${GH}(workflow${S}+disable|repo${S}+(edit|delete|rename|archive))" &&
  deny "changing workflows or repository settings is human-only."

# --- Shell writes to guardrail files: ask the human ----------------------------------------------
proj="${CLAUDE_PROJECT_DIR:-$PWD}"
# Normalise the command before looking for guardrail paths, so normal work does not need approval:
# - a path in a worktree under .claude/worktrees/<name>/ is judged relative to that worktree (a name starting with
#   '.' is kept, so '.claude/worktrees/../hooks' still counts as .claude/);
# - running the Maven Wrapper (mvnw in command position) is not a write to it; `cp x mvnw` or `> mvnw` still is.
scan=$(sed -E \
  -e "s#(${proj}/)?\.claude/worktrees/[^./[:space:]'\"][^/[:space:]'\"]*/?##g" \
  -e "s#(^|[;&|(])([[:space:]]*)[\"']?(\./|${proj}/)?mvnw(\.cmd)?[\"']?([[:space:]]|\$)#\1\2\5#g" <<<"$cmd")
guard_paths="(^|[[:space:]'\"=(:])((\.\.?/)+|${proj}/)?(\.claude/|\.githooks/|\.git/hooks|\.github/|\.agents/|\.mvn/|config/|skills-lock\.json|mvnw|[^[:space:]]*ArchitectureTest\.java)"
# Ignore harmless redirections like 2>/dev/null or 2>&1 before looking for writes.
stripped=$(sed -E 's#[0-9]*>{1,2}[[:space:]]*/dev/null##g; s#[0-9]*>&[0-9]##g' <<<"$scan")
writes="(sed${S}+(-[[:alpha:]]*${S}+)*-i|perl${S}+-[[:alpha:]]*i|>|tee${S}|(^|[;&|([:space:]])(mv|cp|rm|chmod|ln|truncate|touch)${S}|python|node${S}|git${S}+(checkout|restore|rm|mv|apply))"
if grep -Eq -- "$guard_paths" <<<"$scan" &&grep -Eq -- "$writes" <<<"$stripped"; then
  ask "this shell command may modify guardrail files (.claude/, .github/, .githooks/, .agents/, .mvn/, config/, mvnw, skills-lock.json or ArchitectureTest)."
fi

exit 0
