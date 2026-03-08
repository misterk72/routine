#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PYTHON_BIN="${PYTHON_BIN:-python3}"
LOG_FILE="${ROOT_DIR}/scripts/sync_weights_pipeline.log"
USER_ID="${USER_ID:-1}"
DB_HOST="${DB_HOST:-192.168.0.103}"
DB_USER="${DB_USER:-healthuser}"
DB_PASS="${DB_PASS:-healthpassword}"
DB_NAME="${DB_NAME:-healthtracker}"
BACKFILL_DAYS="${BACKFILL_DAYS:-30}"
STALE_DAYS="${STALE_DAYS:-3}"
WITHINGS_TOKENS_JSON="${WITHINGS_TOKENS_JSON:-${ROOT_DIR}/../withings/tokens.json}"
WITHINGS_PY="${WITHINGS_PY:-${ROOT_DIR}/../withings/withings.py}"

log() {
  mkdir -p "$(dirname "${LOG_FILE}")"
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1" | tee -a "${LOG_FILE}"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "ERROR: missing command '$1'"
    exit 2
  fi
}

log "Starting weights sync pipeline"
require_cmd "${PYTHON_BIN}"
require_cmd mariadb

if ! "${PYTHON_BIN}" -c "import requests" >/dev/null 2>&1; then
  log "ERROR: python dependency 'requests' is missing"
  exit 3
fi

set +e
"${PYTHON_BIN}" "${ROOT_DIR}/docker-sqlite/import_withings_api_mariadb.py" \
  --user-id "${USER_ID}" \
  --backfill-days "${BACKFILL_DAYS}" \
  --tokens-json "${WITHINGS_TOKENS_JSON}" \
  --withings-py "${WITHINGS_PY}" \
  --db-host "${DB_HOST}" \
  --db-user "${DB_USER}" \
  --db-pass "${DB_PASS}" \
  --db-name "${DB_NAME}" \
  --apply >>"${LOG_FILE}" 2>&1
status_withings=$?
set -e
if [[ ${status_withings} -ne 0 ]]; then
  log "ERROR: withings direct import failed (exit=${status_withings})"
  tail -n 40 "${LOG_FILE}" >&2 || true
  exit 10
fi
log "Withings direct import completed"

set +e
"${PYTHON_BIN}" "${ROOT_DIR}/docker-sqlite/import_health_entries_weights_mariadb.py" \
  --db-host "${DB_HOST}" \
  --db-user "${DB_USER}" \
  --db-pass "${DB_PASS}" \
  --db-name "${DB_NAME}" \
  --apply >>"${LOG_FILE}" 2>&1
status_app=$?
set -e
if [[ ${status_app} -ne 0 ]]; then
  log "ERROR: health_entries sync failed (exit=${status_app})"
  exit 11
fi
log "HealthTracker weight sync completed"

last_withings="$(
  mariadb --protocol=TCP --ssl=OFF -N -B \
    -h "${DB_HOST}" -u "${DB_USER}" "-p${DB_PASS}" -D "${DB_NAME}" \
    -e "SELECT DATE_FORMAT(MAX(measured_at), '%Y-%m-%d %H:%i:%s') FROM weight_measurements WHERE source_id=2;"
)"
if [[ -z "${last_withings}" ]]; then
  log "ERROR: no withings rows found in weight_measurements"
  exit 12
fi

age_days="$(
  mariadb --protocol=TCP --ssl=OFF -N -B \
    -h "${DB_HOST}" -u "${DB_USER}" "-p${DB_PASS}" -D "${DB_NAME}" \
    -e "SELECT TIMESTAMPDIFF(DAY, MAX(measured_at), NOW()) FROM weight_measurements WHERE source_id=2;"
)"
if [[ -n "${age_days}" ]] && (( age_days > STALE_DAYS )); then
  log "ERROR: latest Withings measurement is stale (${age_days} days, last=${last_withings})"
  exit 13
fi

log "Pipeline succeeded (last withings=${last_withings}, age_days=${age_days})"
