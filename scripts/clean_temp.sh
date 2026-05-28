#!/usr/bin/env bash
set -euo pipefail

# Remove fixtures that were copied into <repo>/regression and <repo>/performance
# by make_examples.sh / make_perf.sh. Useful between local test runs.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

DIRS=()
if [ "$#" -gt 0 ]; then
  DIRS=("$@")
else
  DIRS=(
    "${ROOT_DIR}/regression"
    "${ROOT_DIR}/performance"
  )
fi

for dir in "${DIRS[@]}"; do
  [ -d "${dir}" ] || continue
  rm -f "${dir}"/*.lama "${dir}"/*.input "${dir}"/*.t
done
