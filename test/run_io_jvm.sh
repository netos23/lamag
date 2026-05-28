#!/usr/bin/env bash
set -euo pipefail

# JVM variant of run_io.sh: drives the IO regression suite through the shaded
# jar instead of the native-image binary. Reuses test/run_io.sh by overriding
# LAMAG_BIN with the JVM wrapper.
#
# Environment overrides (passed through to run_io.sh):
#   FIXTURE_DIR  - directory to scan for *.lama (default: <repo>/regression)
#   LAMAG_JAR    - path to the shaded jar (default: latest target/lamag-*-all.jar)
#   JAVA         - path to the java binary
#   JAVA_OPTS    - extra JVM options

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LAMAG_BIN="${ROOT_DIR}/scripts/lamag_jvm.sh"

if [[ ! -x "${LAMAG_BIN}" ]]; then
  echo "::error file=${LAMAG_BIN}::JVM wrapper not found or not executable" >&2
  exit 1
fi

export LAMAG_BIN
exec "${ROOT_DIR}/test/run_io.sh" "$@"
