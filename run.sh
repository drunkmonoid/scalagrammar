#!/usr/bin/env bash
# Helper for running lessons.
#
#   ./run.sh <path>              run a single file
#   ./run.sh <fragment>          run the first lesson whose path contains <fragment>
#   ./run.sh --list              list all lessons (use --fast to run without heavy targets)
#   ./run.sh --all [dir]         run every lesson under dir (default: lessons), report failures
#   ./run.sh --fast [dir]        like --all, but skips spark, scala-js, and scala-native
#
# Files ending in .test.scala are run with `scala-cli test`.
set -euo pipefail
cd "$(dirname "$0")"

all_files() {
  { find "${1:-lessons}" ecosystem -name '*.scala' 2>/dev/null \
      | grep -v '/sbt-project/\|/mill-project/\|/macros/'
    find "${1:-lessons}" ecosystem -type d -name macros -not -path '*/.scala-build/*' 2>/dev/null; } | sort
}

run_one() {
  local f="$1"
  echo -e "\n\033[1;34m▶ $f\033[0m"
  if [[ -d "$f" ]]; then
    scala-cli run --server=false "$f"   # multi-file lessons (macros/): see macros/project.scala
  elif [[ "$f" == *.test.scala ]]; then
    scala-cli test "$f"
  else
    scala-cli run "$f"
  fi
}

case "${1:-}" in
  "" | -h | --help)
    sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
    ;;
  --list)
    all_files lessons
    echo "# Note: use --fast to skip spark, scala-js, and scala-native" >&2
    ;;
  --all | --fast)
    is_fast=0
    [[ "$1" == "--fast" ]] && is_fast=1
    dir="${2:-lessons}"
    failed=()
    while read -r f; do
      if ((is_fast)) && [[ "$f" =~ 02_spark\.scala|01_scala_js\.scala|02_scala_native\.scala ]]; then
        continue
      fi
      run_one "$f" || failed+=("$f")
    done < <({ find "$dir" -name '*.scala' | grep -v '/sbt-project/\|/mill-project/\|/macros/'
               find "$dir" -type d -name macros -not -path '*/.scala-build/*'; } | sort)
    echo
    if ((${#failed[@]})); then
      echo -e "\033[1;31m${#failed[@]} failed:\033[0m"; printf '  %s\n' "${failed[@]}"; exit 1
    else
      echo -e "\033[1;32mall passed\033[0m"
    fi
    ;;
  *)
    if [[ -e "$1" ]]; then
      run_one "$1"
    else
      match=$(all_files lessons | grep -i -- "$1" | head -1 || true)
      [[ -z "$match" ]] && { echo "no lesson matches '$1'"; exit 1; }
      run_one "$match"
    fi
    ;;
esac
