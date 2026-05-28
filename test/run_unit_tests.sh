#!/usr/bin/env bash
set -euo pipefail

# Run Maven surefire unit tests (JVM-backed, no native image required).
#
# Environment overrides:
#   MVN - path to the mvn binary (default: mvn)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVN_BIN="${MVN:-mvn}"

cd "${ROOT_DIR}"

if ! command -v "${MVN_BIN}" >/dev/null 2>&1; then
  echo "::error::mvn executable not found in PATH" >&2
  exit 1
fi

"${MVN_BIN}" -B test
