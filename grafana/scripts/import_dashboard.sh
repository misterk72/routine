#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <GRAFANA_URL> <API_TOKEN> <DASHBOARD_JSON>"
  echo "Example: $0 http://localhost:3000 eyJr... grafana/dashboards/workouts.json"
  exit 1
fi

GRAFANA_URL="$1"
API_TOKEN="$2"
DASHBOARD_JSON="$3"

payload=$(jq -n --argjson dash "$(cat "${DASHBOARD_JSON}")" '{dashboard: $dash, overwrite: true}')

tmp_response=$(mktemp)
http_code=$(
  curl -sS -o "${tmp_response}" -w "%{http_code}" \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -H "Content-Type: application/json" \
    -X POST "${GRAFANA_URL}/api/dashboards/db" \
    -d "${payload}"
)

if [[ "${http_code}" != "200" ]]; then
  echo "Import failed (HTTP ${http_code}). Response:" >&2
  cat "${tmp_response}" >&2
  rm -f "${tmp_response}"
  exit 1
fi

cat "${tmp_response}"
rm -f "${tmp_response}"
echo
echo "Imported: ${DASHBOARD_JSON}"
