#!/usr/bin/env bash
set -euo pipefail

# Build the lamag interpreter as a GraalVM native-image binary.
# The resulting executable is written to target/lamag and runs .lama sources
# directly without a JVM.
#
# Requirements:
#   - GraalVM JDK (with native-image installed, e.g. `gu install native-image`)
#   - Maven 3.8+
#
# Environment overrides:
#   MVN          - path to the mvn binary (default: mvn)
#   SKIP_TESTS   - if set to "true", surefire tests are skipped (default: true)

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT_DIR}"

# When JAVA_HOME points to a GraalVM, make sure its bin/ is on PATH so that
# native-image and the GraalVM java are found by `command -v` and by mvn.
if [[ -n "${JAVA_HOME:-}" ]] && [[ -d "${JAVA_HOME}/bin" ]]; then
  PATH="${JAVA_HOME}/bin:${PATH}"
fi

MVN_BIN="${MVN:-mvn}"
SKIP_TESTS="${SKIP_TESTS:-true}"

if ! command -v "${MVN_BIN}" >/dev/null 2>&1; then
  echo "::error::mvn executable not found in PATH (PATH=${PATH})" >&2
  exit 1
fi

if ! command -v native-image >/dev/null 2>&1; then
  echo "::error::native-image executable not found in PATH" >&2
  echo "Install GraalVM with native-image and ensure \$JAVA_HOME/bin is on PATH." >&2
  echo "Tip: re-run as 'JAVA_HOME=/path/to/graalvm ./scripts/build_native.sh' (this script auto-prepends \$JAVA_HOME/bin)." >&2
  exit 1
fi

echo "==> JAVA_HOME=${JAVA_HOME:-unset}" >&2
echo "==> java: $(command -v java)" >&2
echo "==> native-image: $(command -v native-image)" >&2
echo "==> mvn: $(command -v "${MVN_BIN}")" >&2

echo "==> Building native image via maven 'native' profile" >&2
"${MVN_BIN}" -Pnative -DskipTests="${SKIP_TESTS}" package

BIN="${ROOT_DIR}/target/lamag"
if [ ! -x "${BIN}" ]; then
  echo "::error file=${BIN}::native image was not produced" >&2
  exit 1
fi

echo "==> Native binary: ${BIN}" >&2
"${BIN}" --version 2>/dev/null || true
