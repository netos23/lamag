#!/usr/bin/env bash
set -euo pipefail

# Thin wrapper that lets `java -jar target/lamag-*-all.jar <file.lama>` be used
# anywhere a `LAMAG_BIN` executable is expected (run_io.sh, run_performance.sh).
#
# Environment overrides:
#   JAVA       - path to the java binary (default: $JAVA_HOME/bin/java or PATH)
#   JAVA_OPTS  - extra JVM options
#   LAMAG_JAR  - path to the shaded jar (default: latest target/lamag-*-all.jar)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

JAR="${LAMAG_JAR:-$(find "${ROOT_DIR}/target" -maxdepth 1 -type f -name 'lamag-*-all.jar' 2>/dev/null | head -n1)}"
if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
  echo "::error::shaded jar not found. Run scripts/build_jvm.sh first." >&2
  exit 1
fi

if [[ -n "${JAVA:-}" ]]; then
  JAVA_BIN="${JAVA}"
elif [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
else
  JAVA_BIN="java"
fi

# truffle.class.path.append makes the bundled Truffle module in a GraalVM JDK
# pick up our language provider from the shaded jar. On a plain JDK the
# property is a harmless no-op.
exec "${JAVA_BIN}" \
  -Dpolyglot.engine.WarnInterpreterOnly=false \
  -Dtruffle.class.path.append="${JAR}" \
  ${JAVA_OPTS:-} \
  -jar "${JAR}" \
  "$@"
