#!/usr/bin/env bash
# Inventory check script for scalagrammar.
#
# Checks:
# 1. Every *.scala file under lessons/ and ecosystem/, except paths containing
#    /sbt-project/, /mill-project/, /cross-project/, /macros/, or /.scala-build/, must contain
#    '//> using scala' in its first 15 lines and must contain a line starting with 'package '.
# 2. Every backtick span in README.md containing 'lessons/', 'ecosystem/', or 'exercises/'
#    and ending with '.scala' must exist as a file relative to the repository root.
# 3. Every stub under exercises/ has a solution at the mirrored path under exercises/solutions/,
#    and every solution has a stub.
# 4. Every stub contains '???'. No solution contains '???'.
# 5. Every stub and solution contains '//> using scala' in its first 15 lines and a line
#    starting with 'package '. The stub's package starts with 'exercises.' and the solution's
#    with 'solutions.'.
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
  | grep -v -E '/sbt-project/|/mill-project/|/cross-project/|/macros/|/\.scala-build/' \
  | sort)

# Check 2: Backtick spans in README.md referencing lessons/, ecosystem/, or exercises/ *.scala files
matches=$(awk -v b="\x60" 'BEGIN{FS=b} { for(i=2; i<=NF; i+=2) print $i }' README.md \
  | grep -E '(lessons/|ecosystem/|exercises/).*\.scala$' || true)

if [[ -n "$matches" ]]; then
  while IFS= read -r match; do
    [[ -z "$match" ]] && continue
    if [[ ! -f "$match" ]]; then
      echo "Error: README.md references '$match' which does not exist" >&2
      exit 1
    fi
  done <<< "$matches"
fi

# Exercise checks (only if exercises directory exists)
if [[ -d exercises ]]; then
  # Check 3: Every stub has a solution at the mirrored path, and every solution has a stub
  while IFS= read -r stub; do
    [[ -z "$stub" ]] && continue
    rel="${stub#exercises/}"
    sol="exercises/solutions/$rel"
    if [[ ! -f "$sol" ]]; then
      echo "Error: Stub '$stub' has no corresponding solution at '$sol'" >&2
      exit 1
    fi
  done < <(find exercises -name '*.test.scala' 2>/dev/null | grep -v '/solutions/' | sort)

  while IFS= read -r sol; do
    [[ -z "$sol" ]] && continue
    rel="${sol#exercises/solutions/}"
    stub="exercises/$rel"
    if [[ ! -f "$stub" ]]; then
      echo "Error: Solution '$sol' has no corresponding stub at '$stub'" >&2
      exit 1
    fi
  done < <(find exercises/solutions -name '*.test.scala' 2>/dev/null | sort)

  # Check 4: Every stub contains ???; no solution contains ???
  while IFS= read -r stub; do
    [[ -z "$stub" ]] && continue
    if ! grep -q '\?\?\?' "$stub"; then
      echo "Error: Stub '$stub' does not contain '???'" >&2
      exit 1
    fi
  done < <(find exercises -name '*.test.scala' 2>/dev/null | grep -v '/solutions/' | sort)

  while IFS= read -r sol; do
    [[ -z "$sol" ]] && continue
    if grep -q '\?\?\?' "$sol"; then
      echo "Error: Solution '$sol' unexpectedly contains '???'" >&2
      exit 1
    fi
  done < <(find exercises/solutions -name '*.test.scala' 2>/dev/null | sort)

  # Check 5: Directives and packages for stubs and solutions
  while IFS= read -r stub; do
    [[ -z "$stub" ]] && continue
    if ! head -n 15 "$stub" | grep -q '//> using scala'; then
      echo "Error: Stub '$stub' does not contain '//> using scala' in the first 15 lines" >&2
      exit 1
    fi
    if ! grep -E '^package exercises\.' "$stub" >/dev/null; then
      echo "Error: Stub '$stub' does not have package starting with 'exercises.'" >&2
      exit 1
    fi
  done < <(find exercises -name '*.test.scala' 2>/dev/null | grep -v '/solutions/' | sort)

  while IFS= read -r sol; do
    [[ -z "$sol" ]] && continue
    if ! head -n 15 "$sol" | grep -q '//> using scala'; then
      echo "Error: Solution '$sol' does not contain '//> using scala' in the first 15 lines" >&2
      exit 1
    fi
    if ! grep -E '^package solutions\.' "$sol" >/dev/null; then
      echo "Error: Solution '$sol' does not have package starting with 'solutions.'" >&2
      exit 1
    fi
  done < <(find exercises/solutions -name '*.test.scala' 2>/dev/null | sort)
fi

echo "Inventory checks passed."
