#!/usr/bin/env bash
set -euo pipefail

# Run lamag for all .lama files in a fixtures directory, feed matching .input,
# and compare stdout with expected .t files (ignoring shell/prompt prefixes from
# the reference output).
#
# Environment overrides:
#   FIXTURE_DIR - directory to scan for *.lama (default: <repo>/regression)
#   LAMAG_BIN   - path to lamag executable (default: <repo>/target/lamag)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURE_DIR="${FIXTURE_DIR:-${ROOT_DIR}/regression}"
LAMAG_BIN="${LAMAG_BIN:-${ROOT_DIR}/target/lamag}"

if [[ ! -x "${LAMAG_BIN}" ]]; then
  echo "::error file=${LAMAG_BIN}::lamag executable not found or not executable" >&2
  exit 1
fi

if [[ ! -d "${FIXTURE_DIR}" ]]; then
  echo "::error file=${FIXTURE_DIR}::fixture directory does not exist" >&2
  exit 1
fi

LAMA_FILES=()
while IFS= read -r lama_file; do
  LAMA_FILES+=("${lama_file}")
done < <(find "${FIXTURE_DIR}" -type f -name '*.lama' | sort)

if [[ ${#LAMA_FILES[@]} -eq 0 ]]; then
  echo "::warning::No .lama files found under ${FIXTURE_DIR}" >&2
  exit 0
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

normalize_expected() {
  local src="$1" dst="$2"
  sed -E \
    -e '/^[[:space:]]*\$/{d;}' \
    -e 's/>[[:space:]]+/>/g' \
    -e 's/[[:space:]]+/ /g' \
    -e 's/^[[:space:]]+//' \
    -e 's/[[:space:]]+$//' \
    "$src" | sed '/^[[:space:]]*$/d' >"$dst"
}

normalize_actual() {
  local src="$1" dst="$2"
  # Drop Truffle/polyglot diagnostics that occasionally leak into the captured
  # stream before normalizing whitespace.
  sed -E \
    -e '/^\[(To redirect|engine|\*).*$/d' \
    -e '/^\* .*polyglot.*$/d' \
    -e '/^Execution without runtime compilation/d' \
    -e '/^The following cause was found/d' \
    -e '/^For more information see/d' \
    -e '/^To disable this warning/d' \
    -e 's/>[[:space:]]+/>/g' \
    -e 's/[[:space:]]+/ /g' \
    -e 's/^[[:space:]]+//' \
    -e 's/[[:space:]]+$//' \
    "$src" | sed '/^[[:space:]]*$/d' >"$dst"
}

filter_noise() {
  local src="$1"
  grep -Ev '^\[(To redirect|engine|\*)|^\* .*polyglot|^Execution without runtime compilation|^The following cause was found|^For more information see|^To disable this warning' "$src" 2>/dev/null || true
}

TOTAL=0
PASS=0
FAIL=0
FAILURES=()

echo "Running ${#LAMA_FILES[@]} IO checks..."

for LAMA_FILE in "${LAMA_FILES[@]}"; do
  ((++TOTAL))
  echo "[${TOTAL}] ${LAMA_FILE}" >&2

  DIRNAME="$(dirname "${LAMA_FILE}")"
  BASENAME="$(basename "${LAMA_FILE}" .lama)"
  INPUT_FILE="${DIRNAME}/${BASENAME}.input"
  EXPECT_FILE="${DIRNAME}/${BASENAME}.t"

  STDIN_SRC="/dev/null"
  [[ -f "${INPUT_FILE}" ]] && STDIN_SRC="${INPUT_FILE}"

  if [[ ! -f "${EXPECT_FILE}" ]]; then
    ((++FAIL))
    echo "::error file=${EXPECT_FILE}::expected .t file not found" >&2
    FAILURES+=("${LAMA_FILE}: missing expected")
    continue
  fi

  EXPECT_FATAL=0
  if grep -qi 'Fatal' "${EXPECT_FILE}"; then
    EXPECT_FATAL=1
  fi

  LM_RAW_OUT="${TMP_DIR}/lamag_${TOTAL}.out"
  LM_ERR="${TMP_DIR}/lamag_${TOTAL}.err"
  EXP_NORM="${TMP_DIR}/expected_${TOTAL}.txt"
  ACT_NORM="${TMP_DIR}/actual_${TOTAL}.txt"
  DIFF_FILE="${TMP_DIR}/diff_${TOTAL}.txt"

  STATUS=0
  "${LAMAG_BIN}" "${LAMA_FILE}" <"${STDIN_SRC}" >"${LM_RAW_OUT}" 2>"${LM_ERR}" || STATUS=$?

  if (( EXPECT_FATAL )); then
    if (( STATUS == 0 )); then
      ((++FAIL))
      echo "::error file=${LAMA_FILE}::Expected non-zero exit (fatal expected in ${EXPECT_FILE}). Got code ${STATUS}" >&2
      FAILURES+=("${LAMA_FILE}: expected fatal exit")
    else
      ((++PASS))
      echo "::notice file=${LAMA_FILE}::PASS (fatal expected, exit ${STATUS})" >&2
    fi
    continue
  fi

  if (( STATUS != 0 )); then
    ((++FAIL))
    ERR_MSG=$(filter_noise "${LM_ERR}" | tr -d '\r' | head -c 400)
    echo "::error file=${LAMA_FILE}::lamag failed (exit ${STATUS}) ${ERR_MSG}" >&2
    FAILURES+=("${LAMA_FILE}: lamag failed (exit ${STATUS})")
    continue
  fi

  normalize_expected "${EXPECT_FILE}" "${EXP_NORM}"
  normalize_actual "${LM_RAW_OUT}" "${ACT_NORM}"

  if ! diff -u "${EXP_NORM}" "${ACT_NORM}" >"${DIFF_FILE}"; then
    ((++FAIL))
    echo "::error file=${LAMA_FILE}::Output mismatch against ${EXPECT_FILE}" >&2
    cat "${DIFF_FILE}"
    FAILURES+=("${LAMA_FILE}: output mismatch")
    continue
  fi

  ((++PASS))
  echo "::notice file=${LAMA_FILE}::PASS" >&2

done

echo "Total: ${TOTAL}, Pass: ${PASS}, Fail: ${FAIL}"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## IO regression"
    echo "- total: ${TOTAL}"
    echo "- pass: ${PASS}"
    echo "- fail: ${FAIL}"
    if (( FAIL > 0 )); then
      echo ""
      echo "### Failures"
      for ITEM in "${FAILURES[@]}"; do
        echo "- ${ITEM}"
      done
    fi
  } >>"${GITHUB_STEP_SUMMARY}"
fi

exit $(( FAIL > 0 ? 1 : 0 ))
