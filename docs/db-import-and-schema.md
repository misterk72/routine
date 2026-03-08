# MariaDB Structure and Import Notes

This project uses MariaDB for centralized storage. The schema lives in
`docker-sqlite/schema-mariadb.sql`, while the sync API creates extra tables
at runtime in `docker-sqlite/api/sync.php`.

## Connection

- Prod Host: `192.168.0.103`
- Dev Host: `192.168.0.13`
- Port: `3306`
- DB: `healthtracker`
- User: `healthuser`
- Password: `healthpassword`
- Client note: SSL is required by the client defaults but the server does not
  support it. Always use `--ssl=OFF`.

Example:
```bash
mariadb --protocol=TCP --ssl=OFF -h 192.168.0.103 -P 3306 \
  -u healthuser -phealthpassword -D healthtracker
```

## Core tables (schema-mariadb.sql)

- `sources`: source registry (`healthtracker`, `gadgetbridge`, `withings`, `manual`)
- `users`: human profiles (shared with app sync)
- `user_source_map`: mapping source device/user to user
- `workouts`: canonical workouts (all sources, created by sync.php)
- `weight_measurements`: canonical body measurements
- `gadgetbridge_samples`: HR/steps samples

## API tables (sync.php)

The app sync layer uses:
- `users`
- `health_entries`
- `workouts`
- `locations`

If you are migrating from `user_profiles`, use the SQL helper in
`docs/migrations/2026-01-28-merge-users.sql` (review and adjust FK names).

`workouts` is the single canonical table. External imports now write
there too, using `source_id` and `source_uid` for dedupe.

`workouts` columns include:
- `start_time`, `end_time`, `duration_minutes`, `program`
- `distance_km`, `avg_speed_kmh`, `calories`, `calories_per_km`
- `avg_heart_rate`, `min_heart_rate`, `max_heart_rate`
- `sleep_heart_rate_avg`, `vo2_max`
- `soundtrack`, `notes`

`health_entries` also stores:
- `location_id` (FK to `locations.id`)

`locations` columns include:
- `name`, `latitude`, `longitude`, `radius`, `is_default`, `client_id`

Notes:
- The mobile app sends locations with their **client-side IDs**.
- The API stores those in `locations.client_id` and maps `health_entries.location_id`
  to the **server-side** `locations.id`.
- On download, `locationId` is translated back to the client-side ID.

## Import scripts

All import scripts live in `docker-sqlite/`:

- `import_manual_workouts_mariadb.py`
  - Imports XLSX workouts into `workouts` (source_id=4 by default).
  - Supports upserts with `--ignore-duplicates`.
  - Skips ghost rows where all workout metrics are empty.
  - Example:
    ```bash
    python docker-sqlite/import_manual_workouts_mariadb.py \
      --xlsx "samples/Entrainement vélo elliptique3.xlsx" \
      --user-id 1 --source-id 4 --ignore-duplicates --apply
    ```

- `import_gadgetbridge_mariadb.py`
  - Imports Gadgetbridge summaries into `workouts` (source_id=3 by default).
  - Optional sample import into `gadgetbridge_samples`.
  - Example:
    ```bash
    python docker-sqlite/import_gadgetbridge_mariadb.py \
      --db samples/Gadgetbridge.db \
      --include-samples --apply
    ```

- `import_withings_mariadb.py`
  - Imports Withings XLSX into `weight_measurements`.
- `import_withings_api_mariadb.py`
  - Imports Withings API data directly into `weight_measurements` (no mandatory XLSX step).
  - Uses backfill window (`--backfill-days`, default `30`) and upserts by `source_uid`.
  - Optional debug export with `--export-xlsx`.
  - Example:
    ```bash
    python docker-sqlite/import_withings_api_mariadb.py \
      --user-id 1 --backfill-days 30 --apply
    ```

- `import_health_entries_weights_mariadb.py`
  - Syncs mobile app entries from `health_entries` into canonical `weight_measurements`.
  - Source UID format: `healthentry:{id}`.
  - Example:
    ```bash
    python docker-sqlite/import_health_entries_weights_mariadb.py --apply
    ```

- `scripts/sync_weights_pipeline.sh`
  - Runs direct Withings import then HealthTracker sync.
  - Includes dependency checks and stale-data alert (Withings older than 3 days).
  - Example:
    ```bash
    bash scripts/sync_weights_pipeline.sh
    ```

## Recommended scheduled job

Replace old cron job (`withings.py` only) by the direct DB pipeline.
Recommended on the **prod server**:

```bash
0 8 * * * PYTHON_BIN=/home/kassabji/workspace/routine/.venv/bin/python \
  DB_HOST=192.168.0.103 \
  /home/kassabji/workspace/routine/scripts/sync_weights_pipeline.sh
```

This writes directly to MariaDB and keeps optional XLSX export for debugging only.

## Validation queries

Check per-source body measurement counts:
```bash
mariadb --protocol=TCP --ssl=OFF -h 192.168.0.13 -P 3306 \
  -u healthuser -phealthpassword -D healthtracker \
  -e "SELECT s.name, COUNT(*) AS rows_total, \
      SUM(wm.weight_kg IS NOT NULL) AS rows_weight, \
      MIN(wm.measured_at) AS first_at, MAX(wm.measured_at) AS last_at \
      FROM weight_measurements wm \
      JOIN sources s ON s.id = wm.source_id \
      GROUP BY s.name ORDER BY rows_total DESC;"
```

Check that dashboard day-level join has coverage:
```bash
mariadb --protocol=TCP --ssl=OFF -h 192.168.0.13 -P 3306 \
  -u healthuser -phealthpassword -D healthtracker \
  -e "SELECT COUNT(*) AS workouts_total, \
      SUM(wm.id IS NOT NULL) AS workouts_with_weight_same_day \
      FROM workouts w \
      LEFT JOIN weight_measurements wm \
        ON wm.user_id = w.user_id AND DATE(wm.measured_at) = DATE(w.start_time) \
      WHERE w.deleted = 0;"
```

## Troubleshooting

- `ModuleNotFoundError: requests`
  - Install dependencies in the Python environment used by cron.
  - The pipeline script checks this at startup and exits non-zero when missing.

- Withings token expired
  - Ensure `tokens.json` contains a valid `refresh_token`.
  - Set `WITHINGS_CLIENT_ID` and `WITHINGS_CLIENT_SECRET` environment variables,
    or keep them available in `withings.py` for token refresh fallback.

- No recent Withings rows in DB
  - Inspect `scripts/sync_weights_pipeline.log`.
  - Run direct import manually in dry-run mode:
    ```bash
    python docker-sqlite/import_withings_api_mariadb.py \
      --user-id 1 --backfill-days 30 --dry-run
    ```

## Cleanup helpers

Remove ghost rows created by manual XLSX import:
```bash
mariadb --protocol=TCP --ssl=OFF -h 192.168.0.13 -P 3306 \
  -u healthuser -phealthpassword -D healthtracker \
  -e "DELETE FROM workouts WHERE source_id=4 \
      AND distance_km IS NULL AND calories IS NULL \
      AND avg_heart_rate IS NULL AND min_heart_rate IS NULL \
      AND max_heart_rate IS NULL;"
```

Verify recent manual workouts:
```bash
mariadb --protocol=TCP --ssl=OFF -h 192.168.0.13 -P 3306 \
  -u healthuser -phealthpassword -D healthtracker \
  -e "SELECT start_time, distance_km, calories, calories_per_km, \
      avg_heart_rate, min_heart_rate, max_heart_rate, \
      sleep_heart_rate_avg, vo2_max \
      FROM workouts WHERE source_id=4 ORDER BY start_time DESC LIMIT 10;"
```
