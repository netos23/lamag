#!/usr/bin/env bash
set -euo pipefail

# Measure runtime of lamag / lamac (-i, -s) on each *.lama file under the
# performance fixtures directory. Prints a sorted table with delta-to-previous
# percentages.
#
# Environment overrides:
#   FIXTURE_DIR - directory to scan for *.lama (default: <repo>/performance)
#   LAMAG_BIN   - path to lamag executable (default: <repo>/target/lamag)
#   LAMAC_BIN   - path to lamac executable or name resolvable via PATH (default: lamac)
#   SKIP_LAMAC  - if "true", lamac comparison is skipped (default: false)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURE_DIR="${FIXTURE_DIR:-${ROOT_DIR}/performance}"
LAMAG_BIN="${LAMAG_BIN:-${ROOT_DIR}/target/lamag}"
LAMAC_BIN="${LAMAC_BIN:-lamac}"
SKIP_LAMAC="${SKIP_LAMAC:-false}"

if [[ ! -x "${LAMAG_BIN}" ]]; then
  echo "::error file=${LAMAG_BIN}::lamag executable not found or not executable" >&2
  exit 1
fi

if [[ "${SKIP_LAMAC}" != "true" ]]; then
  if [[ "${LAMAC_BIN}" == */* ]]; then
    if [[ ! -x "${LAMAC_BIN}" ]]; then
      echo "::warning file=${LAMAC_BIN}::lamac executable not found, skipping lamac comparison" >&2
      SKIP_LAMAC="true"
    fi
  else
    if ! command -v "${LAMAC_BIN}" >/dev/null 2>&1; then
      echo "::warning file=${LAMAC_BIN}::lamac executable not in PATH, skipping lamac comparison" >&2
      SKIP_LAMAC="true"
    else
      LAMAC_BIN="$(command -v "${LAMAC_BIN}")"
    fi
  fi
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

RESULTS=()
FAILURES=()
MEASURE_ID=0

measure_time() {
  local label="$1" stdin_src="$2"
  shift 2

  local id=${MEASURE_ID}
  ((++MEASURE_ID))

  local err_file="${TMP_DIR}/err_${id}.log"
  local status=0 duration

  TIMEFORMAT=%R
  if [[ -n "${stdin_src}" ]]; then
    duration="$({ time "$@" <"${stdin_src}" >/dev/null 2>"${err_file}"; } 2>&1)" || status=$?
  else
    duration="$({ time "$@" </dev/null >/dev/null 2>"${err_file}"; } 2>&1)" || status=$?
  fi

  if (( status != 0 )); then
    echo "::error file=${label}::Command failed (exit ${status})" >&2
    FAILURES+=("${label}: exit ${status} (stderr: ${err_file})")
    return 1
  fi

  RESULTS+=("${label}|${duration}")
  return 0
}

echo "Running performance measurements for ${#LAMA_FILES[@]} files..." >&2

for LAMA_FILE in "${LAMA_FILES[@]}"; do
  DIRNAME="$(dirname "${LAMA_FILE}")"
  BASENAME="$(basename "${LAMA_FILE}" .lama)"
  INPUT_FILE="${DIRNAME}/${BASENAME}.input"
  STDIN_SRC="/dev/null"
  [[ -f "${INPUT_FILE}" ]] && STDIN_SRC="${INPUT_FILE}"

  measure_time "lamag ${LAMA_FILE}" "${STDIN_SRC}" "${LAMAG_BIN}" "${LAMA_FILE}" || true
  if [[ "${SKIP_LAMAC}" != "true" ]]; then
    measure_time "lamac -i ${LAMA_FILE}" "${STDIN_SRC}" "${LAMAC_BIN}" -i "${LAMA_FILE}" || true
    measure_time "lamac -s ${LAMA_FILE}" "${STDIN_SRC}" "${LAMAC_BIN}" -s "${LAMA_FILE}" || true
  fi

done

if [[ ${#RESULTS[@]} -eq 0 ]]; then
  echo "::error::No successful measurements recorded" >&2
  exit 1
fi

SORTED=$(printf '%s\n' "${RESULTS[@]}" | sort -t'|' -k2,2n)

printf "%-60s %12s %12s\n" "Program" "Seconds" "% prev"
PREV_TIME=""
while IFS='|' read -r label duration; do
  [[ -z "${duration}" ]] && continue
  if [[ -z "${PREV_TIME}" ]]; then
    delta="n/a"
  else
    delta=$(awk -v c="${duration}" -v p="${PREV_TIME}" 'BEGIN { if (p == 0) { print "n/a" } else { printf "%.2f%%", ((c - p) / p) * 100 } }')
  fi
  printf "%-60s %12.4f %12s\n" "${label}" "${duration}" "${delta}"
  PREV_TIME="${duration}"
done <<< "${SORTED}"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## Performance"
    echo ""
    echo '```'
    printf "%-60s %12s %12s\n" "Program" "Seconds" "% prev"
    PREV_TIME=""
    while IFS='|' read -r label duration; do
      [[ -z "${duration}" ]] && continue
      if [[ -z "${PREV_TIME}" ]]; then
        delta="n/a"
      else
        delta=$(awk -v c="${duration}" -v p="${PREV_TIME}" 'BEGIN { if (p == 0) { print "n/a" } else { printf "%.2f%%", ((c - p) / p) * 100 } }')
      fi
      printf "%-60s %12.4f %12s\n" "${label}" "${duration}" "${delta}"
      PREV_TIME="${duration}"
    done <<< "${SORTED}"
    echo '```'
  } >>"${GITHUB_STEP_SUMMARY}"
fi

if (( ${#FAILURES[@]} > 0 )); then
  echo "" >&2
  echo "Warnings/Failures:" >&2
  for ITEM in "${FAILURES[@]}"; do
    echo "- ${ITEM}" >&2
  done
fi
