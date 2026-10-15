#!/usr/bin/env bash
# PreToolUse hook (Edit/Write/MultiEdit/NotebookEdit): blocks writes to build output and generated code,
# and asks the human before an agent edits guardrail files. See docs/adr/0001-agentic-development-guardrails.md.
set -euo pipefail

path=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // ""')
[ -z "$path" ] && exit 0

proj="${CLAUDE_PROJECT_DIR:-$PWD}"

deny() {
  echo "Blocked by .claude/hooks/guard-paths.sh: $1" >&2
  exit 2
}

ask() {
  jq -n --arg r "guard-paths.sh: $1" \
    '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $r}}'
  exit 0
}

# The deepest worktree of this repository that contains $path, or nothing when the path lies outside all of them.
# Judging a path relative to its own worktree keeps a test class in a worktree under .claude/worktrees/ from being
# mistaken for a .claude/ file, and protects the guardrail files of sibling worktrees as well.
worktree_root() {
  local best="" root
  while IFS= read -r root; do
    case "$path" in "$root"/*) [ "${#root}" -gt "${#best}" ] && best="$root" ;; esac
  done < <(
    echo "$proj"
    git -C "$proj" worktree list --porcelain 2>/dev/null | sed -n 's/^worktree //p'
  )
  echo "$best"
}

case "/$path" in
  */../*) ask "path contains '..', cannot verify it stays outside guardrail files: $path" ;;
esac

case "$path" in
  /*)
    root=$(worktree_root)
    [ -z "$root" ] && exit 0 # outside this repository: normal permission rules apply
    rel="${path#"$root"/}"
    ;;
  *) rel="$path" ;;
esac

case "/$rel" in
  */target/*) deny "$rel is build output. Change the source (e.g. the OpenAPI YAML) and rebuild instead." ;;
  */generated-sources/*) deny "$rel is generated code. Never hand-edit it; change the OpenAPI YAML instead." ;;
  /.claude/* | /.github/* | /.githooks/* | /.agents/* | /.mvn/* | /config/* | /skills-lock.json | /mvnw | /mvnw.cmd | */ArchitectureTest.java)
    ask "$rel is a guardrail or agent-instruction file. Changes to the safety net need explicit human approval." ;;
esac

exit 0
