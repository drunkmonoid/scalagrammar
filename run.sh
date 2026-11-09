#!/usr/bin/env bash
# Helper for running lessons.
#
#   ./run.sh <path>              run a single file
#   ./run.sh <fragment>          run the first lesson whose path contains <fragment>
#   ./run.sh --list              list all lessons, stubs, and solutions
#   ./run.sh --all [dir]         run every lesson under dir (default: lessons), report failures
#   ./run.sh --fast [dir]        like --all, but skips spark, scala-js, and scala-native
#   ./run.sh --exercises         compile stubs and test solutions under exercises/
#
# Files ending in .test.scala are run with `scala-cli test`.
set -euo pipefail
cd "$(dirname "$0")"

all_files() {
  { find "${1:-lessons}" ecosystem -name '*.scala' 2>/dev/null \
      | grep -v '/sbt-project/\|/mill-project/\|/cross-project/\|/macros/\|/exercises/'
    find "${1:-lessons}" ecosystem -type d -name macros -not -path '*/.scala-build/*' 2>/dev/null; } | sort
}

run_one() {
  local f="$1"
  echo -e "\n\033[1;34m▶ $f\033[0m"
  if [[ -d "$f" ]]; then
    scala-cli run --server=false "$f"   # multi-file lessons (macros/): see macros/project.scala
  elif [[ "$f" == *.test.scala ]]; then
    scala-cli test "$f"
  elif [[ "$f" == *.bench.scala ]]; then
    scala-cli --power run --jmh "$f" -- -wi 1 -i 2 -f 1 -w 1s -r 1s
  else
    scala-cli run "$f"
  fi
}

case "${1:-}" in
  "" | -h | --help)
    sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
    ;;
  --list)
    all_files lessons
    if [[ -d exercises ]]; then
      echo -e "\n# Exercises (stubs):"
      find exercises -name '*.test.scala' 2>/dev/null | grep -v '/solutions/' | sort || true
      echo -e "\n# Exercises (solutions):"
      find exercises/solutions -name '*.test.scala' 2>/dev/null | sort || true
    fi
    echo -e "\n# Note: use --fast to skip spark, scala-js, and scala-native" >&2
    echo "# Note: use --exercises to run exercises" >&2
    ;;
  --all | --fast)
    if [[ "${2:-}" =~ ^exercises/? ]]; then
      echo "Use ./run.sh --exercises to run exercises (stubs fail tests by design)" >&2
      exit 1
    fi
    is_fast=0
    [[ "$1" == "--fast" ]] && is_fast=1
    dir="${2:-lessons}"
    failed=()
    while read -r f; do
      if ((is_fast)) && [[ "$f" =~ 02_spark\.scala|01_scala_js\.scala|02_scala_native\.scala|03_laminar\.scala|\.bench\.scala$ ]]; then
        continue
      fi
      run_one "$f" || failed+=("$f")
    done < <({ find "$dir" -name '*.scala' | grep -v '/sbt-project/\|/mill-project/\|/cross-project/\|/macros/\|/exercises/'
               find "$dir" -type d -name macros -not -path '*/.scala-build/*'; } | sort)
    echo
    if ((${#failed[@]})); then
      echo -e "\033[1;31m${#failed[@]} failed:\033[0m"; printf '  %s\n' "${failed[@]}"; exit 1
    else
      echo -e "\033[1;32mall passed\033[0m"
    fi
    ;;
  --exercises)
    stubs=()
    if [[ -d exercises ]]; then
      while IFS= read -r f; do
        [[ -n "$f" ]] && stubs+=("$f")
      done < <(find exercises -name '*.test.scala' 2>/dev/null | grep -v '/solutions/' | sort)
    fi

    solutions=()
    if [[ -d exercises/solutions ]]; then
      while IFS= read -r f; do
        [[ -n "$f" ]] && solutions+=("$f")
      done < <(find exercises/solutions -name '*.test.scala' 2>/dev/null | sort)
    fi

    if ((${#stubs[@]} == 0)) && ((${#solutions[@]} == 0)); then
      echo "No exercises found under exercises/ (nothing to run)"
      exit 0
    fi

    failed=()
    for s in "${stubs[@]}"; do
      echo -e "\n\033[1;34m▶ compile stub: $s\033[0m"
      scala-cli compile --test "$s" || failed+=("$s (compile)")
    done

    for sol in "${solutions[@]}"; do
      echo -e "\n\033[1;34m▶ test solution: $sol\033[0m"
      scala-cli test "$sol" || failed+=("$sol (test)")
    done

    echo
    if ((${#failed[@]})); then
      echo -e "\033[1;31m${#failed[@]} failed:\033[0m"; printf '  %s\n' "${failed[@]}"; exit 1
    else
      echo -e "\033[1;32mall exercises passed\033[0m"
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
