#!/usr/bin/env bash
# Compares a PR with its base and fails on changes a human must explicitly accept: new suppressions, removed tests,
# guardrail changes and JPA schema changes. Also fails when CLAUDE.md names a path that does not
# exist. Each finding names its override label; only the human maintainer applies labels (ADR 0001).
# Usage: guardrail-diff.sh <base-revision> [<head-revision>]   with the PR's labels in $PR_LABELS (comma-separated)
# Reads both revisions through git only; nothing from the PR is executed.
set -euo pipefail

base=$1
pr_head=${2:-HEAD}
mb=$(git merge-base "$base" "$pr_head")
labels=",${PR_LABELS:-},"

failures=0
report=""

# finding <override-label|-> <message>: an error, unless the override label is set.
finding() {
  if [ "$1" != "-" ] && [[ "$labels" == *",$1,"* ]]; then
    echo "::notice::$2 Accepted via label '$1'."
    report+="| accepted (\`$1\`) | $2 |"$'\n'
    return
  fi
  local fix=""
  [ "$1" != "-" ] && fix=" Needs a fix, or label '$1' from the maintainer."
  echo "::error::$2$fix"
  report+="| **failed** | $2$fix |"$'\n'
  failures=$((failures + 1))
}

changed() { git diff --name-only --no-renames "$mb" "$pr_head" -- "$@"; }

count() { # <rev> <ERE> <pathspec...>: number of matches
  local out rc
  out=$(git grep -h -o -E -e "$2" "$1" -- "${@:3}") || { rc=$?; [ "$rc" -eq 1 ] || return "$rc"; }
  if [ -z "$out" ]; then echo 0; else wc -l <<<"$out" | tr -d ' '; fi
}

contains() { git grep -q -E -e "$2" "$1" -- "$3" 2>/dev/null; } # <rev> <ERE> <path>

joined() { paste -sd, - | sed 's/,/, /g'; }

filter() { grep "$@" || [ $? -eq 1 ]; } # grep that does not fail on zero matches

# --- Suppressions: anything that silences a test, check or warning ------------------------------------------------
SUPPRESSIONS=(
  '@SuppressWarnings'
  '@SuppressFBWarnings'
  '@Disabled[A-Za-z]*'
  '@Ignore([^A-Za-z]|$)'
  '(assume|assuming)(True|False|That)[[:space:]]*\('
  'NOSONAR|NOPMD|CHECKSTYLE:OFF|spotless:off|@formatter:off'
  'FreezingArchRule'
  '<skip[A-Za-z]*>[[:space:]]*true'
  '<testFailureIgnore>[[:space:]]*true'
  '<failOn[A-Za-z]*>[[:space:]]*false'
  '[A-Za-z.]*skip[A-Za-z]*=true'
  'continue-on-error:[[:space:]]*true'
)
SOURCES=('*.java' '*.xml' '*.yml' '*.yaml' '*.properties' '.mvn/*.config')
for pattern in "${SUPPRESSIONS[@]}"; do
  before=$(count "$mb" "$pattern" "${SOURCES[@]}")
  after=$(count "$pr_head" "$pattern" "${SOURCES[@]}")
  if [ "$after" -gt "$before" ]; then
    finding suppression-ok "Suppressions matching \`$pattern\` went from $before to $after."
  fi
done

# --- Tests: fewer test methods, or a deleted test class -----------------------------------------------------------
TEST_METHOD='@(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)([^A-Za-z]|$)'
tests_before=$(count "$mb" "$TEST_METHOD" '*src/test/*.java')
tests_after=$(count "$pr_head" "$TEST_METHOD" '*src/test/*.java')
echo "Test methods: $tests_before -> $tests_after"
if [ "$tests_after" -lt "$tests_before" ]; then
  finding test-removal-ok "Test methods went from $tests_before to $tests_after."
fi
deleted_tests=$(git diff --name-only --diff-filter=D -M "$mb" "$pr_head" -- '*src/test/*.java' |
  while IFS= read -r f; do if contains "$mb" "$TEST_METHOD" "$f"; then echo "$f"; fi; done | joined)
if [ -n "$deleted_tests" ]; then
  finding test-removal-ok "Test classes deleted: $deleted_tests."
fi

# --- PR size: reported for the reviewer, never a finding (the maintainer declines a PR that is too large) ----------
prod_lines=0
test_lines=0
while IFS=$'\t' read -r added deleted path; do
  [ "$added" = "-" ] && continue # binary
  case "$path" in
    *.md | docs/* | .agents/* | skills-lock.json | mvnw | mvnw.cmd) ;; # docs and vendored files
    *src/test/*) test_lines=$((test_lines + added + deleted)) ;;
    *) prod_lines=$((prod_lines + added + deleted)) ;;
  esac
done < <(git diff --numstat -M "$mb" "$pr_head")
echo "Changed lines: $prod_lines production/config, $test_lines test"

# --- Guardrail changes: agent config, skills, CI, build wrapper, ArchUnit rules and Maven <build> sections --------
guardrail_files=$(changed '.github/*' '.claude/*' '.githooks/*' '.agents/*' '.mvn/*' 'config/*' mvnw mvnw.cmd skills-lock.json \
  '*ArchitectureTest.java')
build_section() { git show "$1:$2" 2>/dev/null | sed -n '/<build>/,/<\/build>/p'; }
while IFS= read -r pom; do
  [ -z "$pom" ] && continue
  if [ "$(build_section "$mb" "$pom")" != "$(build_section "$pr_head" "$pom")" ]; then
    guardrail_files+=$'\n'"$pom (<build>)"
  fi
done < <(changed pom.xml '*/pom.xml')
guardrail_files=$(sed '/^$/d' <<<"$guardrail_files" | joined)
if [ -n "$guardrail_files" ]; then
  finding guardrail-change-ok "Guardrail files changed: $guardrail_files. Call this out in the PR description."
fi

# --- Schema changes: JPA entities and ddl-auto --------------------------------------------------------------------
ENTITY='@(Entity|Embeddable|MappedSuperclass)([^A-Za-z]|$)'
entities=$(changed '*src/main/*.java' | while IFS= read -r f; do
  if contains "$mb" "$ENTITY" "$f" || contains "$pr_head" "$ENTITY" "$f"; then echo "$f"; fi
done)
if git diff "$mb" "$pr_head" -- '*application*.yml' '*application*.yaml' '*application*.properties' |
  grep -Eq '^[+-].*ddl-auto'; then
  entities+=$'\n'"ddl-auto setting"
fi
entities=$(sed '/^$/d' <<<"$entities" | joined)
if [ -n "$entities" ]; then
  finding schema-change "Database schema may change: $entities."
fi

# --- CLAUDE.md must not name paths that do not exist ---------------------------------------------------------------
if git cat-file -e "$pr_head:.claude/CLAUDE.md" 2>/dev/null; then
  files=$(git ls-tree -r --name-only "$pr_head")
  bt=$'\x60' # backtick: inline code spans in Markdown
  missing=$(git show "$pr_head:.claude/CLAUDE.md" | filter -o "${bt}[^${bt}]*${bt}" | tr -d "$bt" |
    filter -E '^[A-Za-z0-9._/<>-]+$' | filter -E '/|\.(md|ya?ml|json|java|sh|xml|properties|cmd)$' |
    filter -vE '(^|/)(target|\.git)/|NNNN' | sort -u |
    while IFS= read -r ref; do
      # <placeholder> matches one path segment; a reference may be relative to a module or package root.
      regex=$(sed -E 's#^\./##; s#/$##; s#\.#\\.#g; s#<[^>]*>#[^/]+#g' <<<"$ref")
      grep -Eq "(^|/)$regex(/|$)" <<<"$files" || echo "$ref"
    done | joined)
  if [ -n "$missing" ]; then
    finding - "CLAUDE.md names paths that do not exist: $missing. Update CLAUDE.md."
  fi
fi

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "### Guardrail diff"
    echo
    echo "Test methods: $tests_before → $tests_after · changed lines: $prod_lines production/config, $test_lines test"
    if [ -n "$report" ]; then printf '\n| Result | Finding |\n|---|---|\n%s' "$report"; fi
  } >>"$GITHUB_STEP_SUMMARY"
fi

if [ "$failures" -gt 0 ]; then
  echo "$failures guardrail finding(s) need a fix or an override label from the maintainer."
  exit 1
fi
echo "No unaccepted guardrail findings."
