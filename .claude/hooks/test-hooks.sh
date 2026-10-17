#!/usr/bin/env bash
# Regression tests for guard-bash.sh and guard-paths.sh. Run: .claude/hooks/test-hooks.sh
set -uo pipefail

dir="$(cd "$(dirname "$0")" && pwd)"
export CLAUDE_PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$dir/../.." && pwd)}"
failures=0

decision() { # <hook> <json> -> allow | ask | deny
  local out code
  out=$("$dir/$1" <<<"$2" 2>/dev/null)
  code=$?
  if [ "$code" -eq 2 ]; then echo deny
  elif grep -q '"ask"' <<<"$out"; then echo ask
  elif [ "$code" -eq 0 ]; then echo allow
  else echo "error($code)"; fi
}

expect_bash() { # <expected> <command>
  local got
  got=$(decision guard-bash.sh "$(jq -n --arg c "$2" '{tool_name: "Bash", tool_input: {command: $c}}')")
  if [ "$got" != "$1" ]; then echo "FAIL bash  expected=$1 got=$got :: $2"; failures=$((failures + 1)); fi
}

expect_path() { # <expected> <file_path>
  local got
  got=$(decision guard-paths.sh "$(jq -n --arg p "$2" '{tool_name: "Edit", tool_input: {file_path: $p}}')")
  if [ "$got" != "$1" ]; then echo "FAIL path  expected=$1 got=$got :: $2"; failures=$((failures + 1)); fi
}

# --- guard-bash: must be blocked
expect_bash deny './mvnw verify -DskipTests'
expect_bash deny 'mvn -Dmaven.test.skip=true install'
expect_bash deny './mvnw verify -Dspotless.check.skip=true'
expect_bash deny 'MAVEN_OPTS="-Dmaven.test.failure.ignore=true" ./mvnw test'
expect_bash deny './mvnw --fail-never verify'
expect_bash deny 'git commit --no-verify -m "wip"'
expect_bash deny 'git commit -nm "wip"'
expect_bash deny 'git commit -a -n -m wip'
expect_bash deny 'git -c core.hooksPath=/dev/null commit -m x'
expect_bash deny 'git push --force origin feature/x'
expect_bash deny 'git push -f'
expect_bash deny 'git push --force-with-lease'
expect_bash deny 'git push origin +feature/x'
expect_bash deny 'git push origin main'
expect_bash deny 'git push origin HEAD:main'
expect_bash deny 'git reset --hard HEAD~1'
expect_bash deny 'git clean -fd'
expect_bash deny 'git checkout -- .'
expect_bash deny 'git restore .'
expect_bash deny 'gh pr merge 12 --squash'
expect_bash deny 'gh pr create --title x --body y --label large-pr-ok'
expect_bash deny 'gh pr create --title x -l suppression-ok'
expect_bash deny 'gh pr edit 12 --add-label guardrail-change-ok'
expect_bash deny 'gh label create foo'
expect_bash deny 'gh api repos/o/r/pulls/1/merge -X PUT'
expect_bash deny 'gh api repos/o/r/branches/main/protection -X DELETE'
expect_bash deny 'gh workflow disable ci.yml'
expect_bash deny 'gh repo edit --visibility private'

# --- guard-bash: shell writes to guardrail files need approval
expect_bash ask "sed -i '' 's/x/y/' .claude/settings.json"
expect_bash ask 'echo "{}" > .claude/settings.json'
expect_bash ask 'rm .claude/hooks/guard-bash.sh'
expect_bash ask 'cp /tmp/ci.yml .github/workflows/ci.yml'
expect_bash ask 'python3 - <<EOF
open(".claude/CLAUDE.md", "w").write("x")
EOF'
expect_bash ask 'chmod -x .githooks/pre-commit'
expect_bash ask 'cd talmar-api && rm ../.claude/hooks/guard-bash.sh'
expect_bash ask 'ls && mv .github/workflows/ci.yml /tmp/'
expect_bash ask "sed -i '' 's/assert/x/' talmar-api/src/test/java/eu/talmar/api/archunit/ArchitectureTest.java"
expect_bash ask 'cat > config/spotbugs-exclude.xml <<EOF
<FindBugsFilter/>
EOF'
expect_bash allow "sed -i '' 's/x/y/' talmar-api/src/main/java/eu/talmar/api/shared/config/WebConfig.java"
expect_bash ask "rm \"$CLAUDE_PROJECT_DIR/.claude/hooks/verify-on-stop.sh\""

# --- guard-bash: running the Maven Wrapper is not a write to it
expect_bash allow './mvnw -pl talmar-api test > /tmp/test.log'
expect_bash allow './mvnw verify 2>&1 | tee /tmp/verify.log'
expect_bash allow "sed -i '' 's/a/b/' talmar-api/src/test/java/eu/talmar/api/FooTest.java && ./mvnw -pl talmar-api test"
expect_bash allow 'git checkout -b fix/x && ./mvnw verify'
expect_bash allow "cd talmar-api && \"$CLAUDE_PROJECT_DIR/mvnw\" test > /tmp/test.log"
expect_bash ask 'cp /tmp/mvnw mvnw'
expect_bash ask 'echo x > mvnw'
expect_bash ask './mvnw verify && chmod -x mvnw'
expect_bash ask './mvnw verify > .mvn/wrapper/maven-wrapper.properties'

# --- guard-bash: a worktree under .claude/worktrees/ is judged relative to that worktree
expect_bash allow "sed -i '' 's/a/b/' $CLAUDE_PROJECT_DIR/.claude/worktrees/agent-1/talmar-api/src/test/java/FooTest.java"
expect_bash allow "cd .claude/worktrees/agent-1 && sed -i '' 's/a/b/' talmar-api/src/test/java/FooTest.java"
expect_bash ask "sed -i '' 's/x/y/' .claude/worktrees/agent-1/.claude/settings.json"
expect_bash ask 'rm -rf .claude/worktrees/../hooks'

# --- guard-bash: patterns stay within one command segment
expect_bash allow 'git push -u origin fix/add-harness && gh pr create --base main --title x --body-file /tmp/b.md'
expect_bash allow 'git commit -m "wip" && ls -n'
expect_bash allow 'git log --oneline main..HEAD'
expect_bash allow 'echo "think it through --label is human-only"'
expect_bash deny 'git push -u origin feature/x && git push origin main'
expect_bash deny 'cd repo && gh pr create --title x --label large-pr-ok'

# --- guard-bash: normal work stays allowed
expect_bash allow './mvnw verify'
expect_bash allow './mvnw -pl talmar-api test -Dtest=VacancyTest'
expect_bash allow './mvnw spotless:apply'
expect_bash allow 'git commit -m "Add vacancy endpoint"'
expect_bash allow 'git commit --amend -m "Fix typo"'
expect_bash allow 'git push -u origin feature/79-main-description'
expect_bash allow 'git checkout -- talmar-api/src/main/java/Foo.java'
expect_bash allow 'gh pr create --title "Add x" --body "y"'
expect_bash allow 'gh pr view 12'
expect_bash allow 'cat .claude/settings.json 2>/dev/null'
expect_bash allow 'grep -rn foo .claude/hooks >/dev/null 2>&1'
expect_bash allow 'ls -la .github/workflows'
expect_bash allow 'git diff -- .claude/CLAUDE.md'
expect_bash allow 'echo hello > /tmp/out.txt'
expect_bash allow "cat >> $HOME/.claude/projects/x/memory/MEMORY.md <<'EOF'
note
EOF"

# --- guard-paths
p="$CLAUDE_PROJECT_DIR"
expect_path deny "$p/talmar-api/target/generated-sources/openapi/src/main/java/Foo.java"
expect_path deny "$p/talmar-api/target/classes/application.yml"
expect_path ask "$p/.claude/settings.json"
expect_path ask "$p/.claude/CLAUDE.md"
expect_path ask "$p/.claude/hooks/guard-bash.sh"
expect_path ask "$p/.github/workflows/ci.yml"
expect_path ask "$p/.githooks/pre-commit"
expect_path ask "$p/.agents/skills/grilling/SKILL.md"
expect_path ask "$p/.mvn/wrapper/maven-wrapper.properties"
expect_path ask "$p/config/spotbugs-exclude.xml"
expect_path ask "$p/skills-lock.json"
expect_path ask "$p/mvnw"
expect_path ask "$p/talmar-api/src/test/java/eu/talmar/api/archunit/ArchitectureTest.java"
expect_path ask "$p/talmar-api/../.claude/settings.json"
expect_path allow "$p/pom.xml"
expect_path allow "$p/talmar-api/src/main/java/eu/talmar/api/shared/config/WebConfig.java"
expect_path allow "$p/talmar-api/src/main/java/eu/talmar/api/domain/vacancy/Vacancy.java"
expect_path allow "$p/talmar-contracts/src/main/java/eu/talmar/contracts/VacancyApi.yml"
expect_path allow "$p/docs/adr/0002-something.md"
expect_path allow "$HOME/.claude/projects/x/memory/MEMORY.md"

# --- guard-paths: files are judged relative to the worktree of this repository they belong to
nested="$p/.claude/worktrees/hook-test-$$"
sibling_parent=$(cd "$(mktemp -d)" && pwd -P)
sibling="$sibling_parent/sibling"
cleanup() {
  git -C "$p" worktree remove --force "$nested" 2>/dev/null
  git -C "$p" worktree remove --force "$sibling" 2>/dev/null
  rmdir "$sibling_parent" "$p/.claude/worktrees" 2>/dev/null
}
trap cleanup EXIT
git -C "$p" worktree add -q --detach --no-checkout "$nested"
git -C "$p" worktree add -q --detach --no-checkout "$sibling"
expect_path allow "$nested/talmar-api/src/test/java/eu/talmar/api/FooTest.java"
expect_path ask "$nested/.claude/settings.json"
expect_path ask "$nested/talmar-api/src/test/java/eu/talmar/api/archunit/ArchitectureTest.java"
expect_path deny "$nested/talmar-api/target/classes/application.yml"
expect_path allow "$sibling/talmar-api/src/test/java/eu/talmar/api/FooTest.java"
expect_path ask "$sibling/.github/workflows/ci.yml"
expect_path ask "$p/.claude/worktrees/../hooks/guard-bash.sh"
expect_path allow "$sibling_parent/other-repo/.claude/settings.json"

if [ "$failures" -eq 0 ]; then echo "All hook tests passed."; else echo "$failures hook test(s) failed."; exit 1; fi
