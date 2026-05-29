#!/usr/bin/env bash
set -euo pipefail

# Build everything needed to run the Lama interpreter on the JVM without
# producing a native-image binary: the compiled language module (target/classes)
# and its runtime dependencies copied to target/modules. scripts/lamag_jvm.sh
# launches the interpreter from that module-path.
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

echo "==> Packaging via maven 'package'" >&2
"${MVN_BIN}" -B -DskipTests="${SKIP_TESTS}" package

if [[ ! -d "${ROOT_DIR}/target/modules" || ! -d "${ROOT_DIR}/target/classes" ]]; then
  echo "::error::module-path (target/modules, target/classes) was not produced" >&2
  exit 1
fi

echo "==> Module-path ready: ${ROOT_DIR}/target/modules + ${ROOT_DIR}/target/classes" >&2
