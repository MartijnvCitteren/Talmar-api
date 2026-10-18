#!/usr/bin/env bash
# Fails when an OpenAPI contract under talmar-contracts/ has a breaking change compared to a base revision,
# or when a contract that exists on the base revision was deleted.
# Usage: check-contract.sh <base-revision>   (e.g. origin/main)
set -euo pipefail

base=$1
status=0
tmp=$(mktemp -d)

list_specs() { grep -E '\.ya?ml$' | grep -v '/target/' || true; }

while IFS= read -r spec; do
  [ -z "$spec" ] && continue
  if [ ! -f "$spec" ]; then
    echo "::error file=$spec::Contract $spec exists on $base but was deleted."
    status=1
  fi
done < <(git ls-tree -r --name-only "$base" -- talmar-contracts | list_specs)

while IFS= read -r spec; do
  [ -z "$spec" ] && continue
  if ! git cat-file -e "$base:$spec" 2>/dev/null; then
    echo "New contract, nothing to compare: $spec"
    continue
  fi
  git show "$base:$spec" >"$tmp/base.yml"
  echo "Checking $spec against $base"
  if ! oasdiff breaking "$tmp/base.yml" "$spec" --fail-on ERR --format singleline; then
    echo "::error file=$spec::Breaking API change in $spec. Make the change backwards compatible, or deprecate first."
    status=1
  fi
done < <(git ls-files -- talmar-contracts | list_specs)

exit "$status"
