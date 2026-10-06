#!/usr/bin/env bash
set -euo pipefail


PATH=/usr/local/bin:/usr/bin:/bin

ROUTINE_DIR="${ROUTINE_DIR:-/home/media/routine}"
ENV_FILE="${ENV_FILE:-$ROUTINE_DIR/docker-sqlite/.env}"
# Secrets hors du depot : ils viennent du .env (jamais commite)
if [ -f "${ENV_FILE:-}" ]; then set -a; . "$ENV_FILE"; set +a; fi

BACKUP_ROOT="${GADGETBRIDGE_BACKUP_ROOT:-/mnt/nas_documents/Sauvegardes/Santé connectée/Miband}"
IMPORT_SCRIPT="${IMPORT_SCRIPT:-$ROUTINE_DIR/docker-sqlite/import_gadgetbridge_mariadb.py}"
BACKFILL_SCRIPT="${BACKFILL_SCRIPT:-$ROUTINE_DIR/scripts/backfill_fc700_recovery.php}"
STATE_DIR="${STATE_DIR:-$ROUTINE_DIR/.state}"
STATE_FILE="${STATE_FILE:-$STATE_DIR/gadgetbridge_samples_last_import}"
SQL_FILE="${SQL_FILE:-/tmp/gadgetbridge_samples_auto.sql}"
LOCK_FILE="${LOCK_FILE:-/tmp/sync_gadgetbridge_samples.lock}"
SAMPLES_SINCE_DAYS="${SAMPLES_SINCE_DAYS:-14}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:?DB_USER manquant (voir docker-sqlite/.env)}"
DB_PASS="${DB_PASS:?DB_PASS manquant (voir docker-sqlite/.env)}"
DB_NAME="${DB_NAME:-healthtracker}"
# Le nom du conteneur change lors d une recreation par compose v2 (_1 -> -1) :
# on le retrouve par son etiquette de service, jamais par un nom en dur.
DOCKER_CONTAINER="${DOCKER_CONTAINER:-$(docker ps --filter 'label=com.docker.compose.service=api' --format '{{.Names}}' | head -1)}"
DOCKER_CONTAINER="${DOCKER_CONTAINER:?conteneur api introuvable}"

force="${1:-}"

log() {
    printf '%s %s\n' "$(date -Is)" "$*"
}

mkdir -p "$STATE_DIR"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    log "sync already running"
    exit 0
fi

latest_line="$(find "$BACKUP_ROOT" -type f -name 'Gadgetbridge.db' -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -1 || true)"
if [[ -z "$latest_line" ]]; then
    log "no Gadgetbridge.db found under $BACKUP_ROOT"
    exit 0
fi

previous_line="$(cat "$STATE_FILE" 2>/dev/null || true)"
if [[ "$force" != "--force" && "$latest_line" == "$previous_line" ]]; then
    exit 0
fi

since="$(date -u -d "$SAMPLES_SINCE_DAYS days ago" +%F)"

log "detected Gadgetbridge DB change: $latest_line"
log "importing gadgetbridge_samples since $since"
python3 "$IMPORT_SCRIPT" \
    --db "$BACKUP_ROOT" \
    --samples-only \
    --include-samples \
    --samples-since "$since" \
    --samples-out "$SQL_FILE" \
    --apply \
    --db-host "$DB_HOST" \
    --db-port "$DB_PORT" \
    --db-user "$DB_USER" \
    --db-pass "$DB_PASS" \
    --db-name "$DB_NAME"

log "backfilling recent FC700 recovery metrics"
docker exec -i "$DOCKER_CONTAINER" php < "$BACKFILL_SCRIPT"

printf '%s\n' "$latest_line" > "$STATE_FILE"
log "done"
