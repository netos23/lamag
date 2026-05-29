#!/usr/bin/env bash
set -euo pipefail

# Run the Lama interpreter on the JVM with the GraalVM optimizing Truffle
# runtime. Everything is launched from the module-path: the runtime
# dependencies (polyglot, truffle-api, the optimizing truffle-runtime +
# truffle-compiler, antlr) live in target/modules (produced by `mvn package` /
# scripts/build_jvm.sh) and the compiled language module is target/classes.
#
# Truffle ships Multi-Release JARs, so they must stay as individual jars on the
# module-path; merging them into a single uber-jar breaks Truffle init. Plug
# this script in anywhere a LAMAG_BIN executable is expected (run_io.sh,
# run_performance.sh).
#
# Environment overrides:
#   JAVA          - path to the java binary (default: $JAVA_HOME/bin/java or PATH)
#   JAVA_OPTS     - extra JVM options
#   LAMAG_MODULES - module directory (default: target/modules)
#   LAMAG_CLASSES - compiled language module (default: target/classes)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

MODULES="${LAMAG_MODULES:-${ROOT_DIR}/target/modules}"
CLASSES="${LAMAG_CLASSES:-${ROOT_DIR}/target/classes}"

if [[ ! -d "${MODULES}" || ! -d "${CLASSES}" ]]; then
  echo "::error::module-path not found (${MODULES}, ${CLASSES}). Run scripts/build_jvm.sh first." >&2
  exit 1
fi

if [[ -n "${JAVA:-}" ]]; then
  JAVA_BIN="${JAVA}"
elif [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
else
  JAVA_BIN="java"
fi

# WarnInterpreterOnly=false: stay quiet if launched on a JVM without the
# optimizing runtime. --enable-native-access silences the Truffle runtime's
# legitimate use of native memory. The guest stack size is handled inside Main
# (it runs the program on a large-stack thread), so no -Xss is needed here.
#
# -Xmx: list-heavy programs without tail-call elimination keep every
# intermediate list alive on the recursion stack (e.g. performance/Sort.lama
# retains ~4.7 GB), so a generous max heap is needed. Listed before JAVA_OPTS so
# callers can override it (e.g. JAVA_OPTS="-Xmx2g").
LAMAG_XMX="${LAMAG_XMX:--Xmx8g}"
exec "${JAVA_BIN}" \
  -Dpolyglot.engine.WarnInterpreterOnly=false \
  --enable-native-access=org.graalvm.truffle \
  ${LAMAG_XMX} \
  ${JAVA_OPTS:-} \
  --module-path "${MODULES}:${CLASSES}" \
  --module pro.fbtw.lamag/pro.fbtw.lamag.Main \
  "$@"
