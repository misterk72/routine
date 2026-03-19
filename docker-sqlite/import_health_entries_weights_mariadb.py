#!/usr/bin/env python3
"""Sync HealthTracker health_entries body metrics into weight_measurements."""

from __future__ import annotations

import argparse
import subprocess
import sys

MIN_WAIST_CM = 30
MAX_WAIST_CM = 300


def _build_sql(source_id: int) -> str:
    return f"""
INSERT INTO weight_measurements (
    user_id,
    source_id,
    source_uid,
    measured_at,
    weight_kg,
    fat_mass_kg,
    fat_percentage,
    waist_cm,
    notes
)
SELECT
    he.user_id,
    {source_id} AS source_id,
    CONCAT('healthentry:', he.id) AS source_uid,
    he.timestamp AS measured_at,
    he.weight AS weight_kg,
    NULL AS fat_mass_kg,
    he.body_fat AS fat_percentage,
    CASE
        WHEN he.waist_measurement BETWEEN {MIN_WAIST_CM} AND {MAX_WAIST_CM}
            THEN he.waist_measurement
        ELSE NULL
    END AS waist_cm,
    he.notes
FROM health_entries he
WHERE he.deleted = 0
  AND he.user_id IS NOT NULL
  AND (
      he.weight IS NOT NULL
      OR he.body_fat IS NOT NULL
      OR he.waist_measurement IS NOT NULL
      OR (he.notes IS NOT NULL AND he.notes <> '')
  )
ON DUPLICATE KEY UPDATE
    measured_at = VALUES(measured_at),
    weight_kg = VALUES(weight_kg),
    fat_percentage = VALUES(fat_percentage),
    waist_cm = VALUES(waist_cm),
    notes = VALUES(notes);
""".strip()


def _apply_sql(sql: str, db_host: str, db_user: str, db_pass: str, db_name: str) -> int:
    cmd = [
        "mariadb",
        "--protocol=TCP",
        "--ssl=OFF",
        "-h",
        db_host,
        "-u",
        db_user,
        f"-p{db_pass}",
        "-D",
        db_name,
    ]
    res = subprocess.run(cmd, input=sql.encode("utf-8"))
    return res.returncode


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync health_entries metrics into weight_measurements.")
    parser.add_argument("--source-id", type=int, default=1)
    parser.add_argument("--db-host", default="192.168.0.103")
    parser.add_argument("--db-user", default="healthuser")
    parser.add_argument("--db-pass", default="healthpassword")
    parser.add_argument("--db-name", default="healthtracker")
    parser.add_argument("--out-sql", default="/tmp/health_entries_weight_sync.sql")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    sql = _build_sql(args.source_id)
    with open(args.out_sql, "w", encoding="utf-8") as f:
        f.write(sql)
    print(f"Wrote SQL file: {args.out_sql}")

    if args.apply:
        return _apply_sql(sql, args.db_host, args.db_user, args.db_pass, args.db_name)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as exc:
        print(f"MariaDB command failed: {exc}", file=sys.stderr)
        raise SystemExit(2)
