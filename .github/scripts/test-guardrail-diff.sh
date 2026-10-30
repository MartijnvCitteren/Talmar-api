#!/usr/bin/env bash
# Regression tests for guardrail-diff.sh: builds a throwaway repository per case and checks pass/fail.
# Run: .github/scripts/test-guardrail-diff.sh
set -uo pipefail

script="$(cd "$(dirname "$0")" && pwd)/guardrail-diff.sh"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

git_q() { git -c user.name=t -c user.email=t@t -c commit.gpgsign=false "$@" >/dev/null; }

new_repo() { # a base commit that resembles this repository
  rm -rf "$work/repo" && mkdir -p "$work/repo" && cd "$work/repo" || exit 1
  git_q init -q -b main
  mkdir -p .claude api/src/main/java/app api/src/test/java/app
  cat >.claude/CLAUDE.md <<'EOF'
See `docs/` and `api/pom.xml`, packages like `src/main/java/<pkg>/`.
EOF
  mkdir -p docs && echo "# docs" >docs/README.md
  printf '<project>\n<dependencies/>\n<build>\n<plugins/>\n</build>\n</project>\n' >api/pom.xml
  printf '@Entity\nclass VacancyJpaEntity {}\n' >api/src/main/java/app/VacancyJpaEntity.java
  printf 'class Vacancy {}\n' >api/src/main/java/app/Vacancy.java
  printf 'class VacancyTest {\n@Test void a() {}\n@Test void b() {}\n}\n' >api/src/test/java/app/VacancyTest.java
  printf 'class OtherTest {\n@ParameterizedTest void c() {}\n}\n' >api/src/test/java/app/OtherTest.java
  git_q add -A && git_q commit -q -m base
  git_q switch -q -c pr
}

commit() { git_q add -A && git_q commit -q -m change; }

expect() { # <pass|fail> <description> [labels]
  local got=pass
  PR_LABELS="${3:-}" "$script" main pr >"$work/out" 2>&1 || got=fail
  if [ "$got" != "$1" ]; then
    echo "FAIL expected=$1 got=$got :: $2"
    sed 's/^/    /' "$work/out"
    failures=$((failures + 1))
  fi
}

new_repo
echo 'class Vacancy { int x; }' >api/src/main/java/app/Vacancy.java && commit
expect pass "small production change"

new_repo
printf 'class VacancyTest {\n@Disabled @Test void a() {}\n@Test void b() {}\n}\n' >api/src/test/java/app/VacancyTest.java && commit
expect fail "new @Disabled"
expect pass "new @Disabled with override label" "other,suppression-ok"

new_repo
echo '@SuppressWarnings("unused") class Vacancy {}' >api/src/main/java/app/Vacancy.java && commit
expect fail "new @SuppressWarnings"

new_repo
printf '<project>\n<properties><skipTests>true</skipTests></properties>\n<build>\n<plugins/>\n</build>\n</project>\n' >api/pom.xml && commit
expect fail "skipTests in pom properties"

new_repo
printf 'class VacancyTest {\n@Test void a() {}\n}\n' >api/src/test/java/app/VacancyTest.java && commit
expect fail "test method removed"
expect pass "test method removed with override label" "test-removal-ok"

new_repo
git_q rm -q api/src/test/java/app/OtherTest.java
printf 'class VacancyTest {\n@Test void a() {}\n@Test void b() {}\n@Test void c() {}\n}\n' >api/src/test/java/app/VacancyTest.java && commit
expect fail "test class deleted while the count stays equal"

new_repo
git_q mv api/src/test/java/app/OtherTest.java api/src/test/java/app/MovedTest.java && commit
expect pass "test class renamed"

new_repo
seq 1 801 >api/src/test/java/app/data.txt && seq 1 401 >api/src/main/resources.txt && commit
expect pass "large PR: size is reported, not a finding"

new_repo
mkdir -p .github/workflows && echo "name: x" >.github/workflows/ci.yml && commit
expect fail "workflow added"
expect pass "workflow added with override label" "guardrail-change-ok"

new_repo
mkdir -p config && echo '<FindBugsFilter/>' >config/spotbugs-exclude.xml && commit
expect fail "SpotBugs exclude filter added"

new_repo
printf '<project>\n<dependencies/>\n<build>\n<plugins><plugin/></plugins>\n</build>\n</project>\n' >api/pom.xml && commit
expect fail "pom <build> changed"

new_repo
printf '<project>\n<dependencies><dependency/></dependencies>\n<build>\n<plugins/>\n</build>\n</project>\n' >api/pom.xml && commit
expect pass "pom dependency changed"

new_repo
printf '@Entity\nclass VacancyJpaEntity { String title; }\n' >api/src/main/java/app/VacancyJpaEntity.java && commit
expect fail "entity changed"
expect pass "entity changed with schema-change label" "schema-change"

new_repo
mkdir -p api/src/main/resources && echo "ddl-auto: update" >api/src/main/resources/application.yml && commit
expect fail "ddl-auto changed"

new_repo
cat >.claude/CLAUDE.md <<'EOF'
See `docs/` and `api/pom.xml` and `scripts/gone.sh`.
EOF
commit
expect fail "CLAUDE.md names a missing path" "guardrail-change-ok"

new_repo
git_q rm -q -r docs && commit
expect fail "path named in CLAUDE.md deleted"

if [ "$failures" -eq 0 ]; then echo "All guardrail-diff tests passed."; else echo "$failures guardrail-diff test(s) failed."; exit 1; fi
