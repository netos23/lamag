#!/usr/bin/env bash
set -euo pipefail

# Shared helper that copies test-suite fixtures from a source directory into a
# destination directory. Used by make_examples.sh and make_perf.sh.
#
# Usage: make_suite.sh <source_dir> <destination_dir> <copy_glob>...

usage() {
  echo "Usage: $0 <source_dir> <destination_dir> <copy_glob>..." >&2
  exit 1
}

if [ "$#" -lt 3 ]; then
  usage
fi

SOURCE_DIR="$1"
DEST_DIR="$2"
shift 2
COPY_PATTERNS=("$@")

if [ ! -d "${SOURCE_DIR}" ]; then
  echo "Source directory not found: ${SOURCE_DIR}" >&2
  echo "Did you run 'git submodule update --init --recursive'?" >&2
  exit 1
fi

mkdir -p "${DEST_DIR}"

shopt -s nullglob
files=()
for pattern in "${COPY_PATTERNS[@]}"; do
  files+=("${SOURCE_DIR}"/${pattern})
done
if [ ${#files[@]} -gt 0 ]; then
  cp "${files[@]}" "${DEST_DIR}/"
fi
shopt -u nullglob

echo "Copied ${#files[@]} fixtures from ${SOURCE_DIR} to ${DEST_DIR}" >&2
