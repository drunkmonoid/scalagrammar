#!/usr/bin/env bash
# Inventory check script for scalagrammar.
#
# Checks:
# 1. Every *.scala file under lessons/ and ecosystem/, except paths containing
#    /sbt-project/, /mill-project/, /macros/, or /.scala-build/, must contain
#    '//> using scala' in its first 15 lines and must contain a line starting with 'package '.
# 2. Every backtick span in README.md containing 'lessons/' or 'ecosystem/' and
#    ending with '.scala' must exist as a file relative to the repository root.
#
# Exits 1 on the first problem encountered.

set -euo pipefail

cd "$(dirname "$0")/.."

echo "Checking inventory..."

# Check 1: File contract check for lesson and ecosystem scala files
while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  if ! head -n 15 "$f" | grep -q '//> using scala'; then
    echo "Error: $f does not contain '//> using scala' in the first 15 lines" >&2
    exit 1
  fi
  if ! grep -q '^package ' "$f"; then
    echo "Error: $f does not contain a line starting with 'package '" >&2
    exit 1
  fi
done < <(find lessons ecosystem -name '*.scala' 2>/dev/null \
  | grep -v -E '/sbt-project/|/mill-project/|/macros/|/\.scala-build/' \
  | sort)

# Check 2: Backtick spans in README.md referencing lessons/ or ecosystem/ *.scala files
matches=$(awk -v b="\x60" 'BEGIN{FS=b} { for(i=2; i<=NF; i+=2) print $i }' README.md \
  | grep -E '(lessons/|ecosystem/).*\.scala$' || true)

if [[ -n "$matches" ]]; then
  while IFS= read -r match; do
    [[ -z "$match" ]] && continue
    if [[ ! -f "$match" ]]; then
      echo "Error: README.md references '$match' which does not exist" >&2
      exit 1
    fi
  done <<< "$matches"
fi

echo "Inventory checks passed."
