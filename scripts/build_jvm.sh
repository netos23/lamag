#!/usr/bin/env bash
set -euo pipefail

# Build the lamag shaded jar (target/lamag-<version>-all.jar) so tests can run
# under a JVM without producing a native-image binary.
#
# Environment overrides:
#   MVN        - path to the mvn binary (default: mvn)
#   SKIP_TESTS - if "true", surefire tests are skipped during package (default: true)

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT_DIR}"

MVN_BIN="${MVN:-mvn}"
SKIP_TESTS="${SKIP_TESTS:-true}"

if ! command -v "${MVN_BIN}" >/dev/null 2>&1; then
  echo "::error::mvn executable not found in PATH" >&2
  exit 1
fi

echo "==> Packaging shaded jar via maven 'package'" >&2
"${MVN_BIN}" -B -DskipTests="${SKIP_TESTS}" package

JAR="$(find "${ROOT_DIR}/target" -maxdepth 1 -type f -name 'lamag-*-all.jar' | head -n1)"
if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
  echo "::error::shaded jar (lamag-*-all.jar) was not produced under target/" >&2
  exit 1
fi

echo "==> Shaded jar: ${JAR}" >&2
