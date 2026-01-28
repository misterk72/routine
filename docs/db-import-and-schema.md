# MariaDB Structure and Import Notes

This project uses MariaDB for centralized storage. The schema lives in
`docker-sqlite/schema-mariadb.sql`, while the sync API creates extra tables
at runtime in `docker-sqlite/api/sync.php`.

## Connection

- Host: `192.168.0.13`
- Port: `3306`
- DB: `healthtracker`
- User: `healthuser`
- Password: `healthpassword`
- Client note: SSL is required by the client defaults but the server does not
  support it. Always use `--ssl=OFF`.

Example:
```bash
mariadb --protocol=TCP --ssl=OFF -h 192.168.0.13 -P 3306 \
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
