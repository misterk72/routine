#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <GRAFANA_URL> <API_TOKEN> <DASHBOARD_UID>"
  echo "Example: $0 http://localhost:3000 eyJr... abcDEF123"
  exit 1
fi

GRAFANA_URL="$1"
API_TOKEN="$2"
DASHBOARD_UID="$3"

curl -sS -H "Authorization: Bearer ${API_TOKEN}" \
  "${GRAFANA_URL}/api/dashboards/uid/${DASHBOARD_UID}" \
  | jq '.dashboard' > "grafana/dashboards/${DASHBOARD_UID}.json"

echo "Saved: grafana/dashboards/${DASHBOARD_UID}.json"
