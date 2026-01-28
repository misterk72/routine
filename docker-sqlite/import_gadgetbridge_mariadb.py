#!/usr/bin/env python3
"""Import Gadgetbridge activity summaries into MariaDB workouts."""

import argparse
import datetime as dt
import sqlite3
import subprocess
from typing import Dict, Iterable, Optional


def _sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _sql_value(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return f"'{_sql_escape(str(value))}'"


def _ts_ms_to_dt_str(ts_ms: int) -> str:
    return dt.datetime.utcfromtimestamp(ts_ms / 1000).strftime("%Y-%m-%d %H:%M:%S")


def load_sessions(conn: sqlite3.Connection):
    cur = conn.cursor()
    cur.execute(
        "select _id, DEVICE_ID, START_TIME, END_TIME, ACTIVITY_KIND "
        "from BASE_ACTIVITY_SUMMARY order by START_TIME"
    )
    return cur.fetchall()


def heart_rate_stats(conn: sqlite3.Connection, device_id: int, start_ms: int, end_ms: int):
    cur = conn.cursor()
    start_sec = start_ms // 1000
    end_sec = end_ms // 1000
    cur.execute(
        "select HEART_RATE from MI_BAND_ACTIVITY_SAMPLE "
        "where DEVICE_ID = ? and TIMESTAMP >= ? and TIMESTAMP <= ? and HEART_RATE > 0",
        (device_id, start_sec, end_sec),
    )
    rows = [r[0] for r in cur.fetchall()]
    if not rows:
        return None, None, None
    return int(sum(rows) / len(rows)), min(rows), max(rows)


def _parse_date_bound(value: Optional[str], is_end: bool) -> Optional[int]:
    if not value:
        return None
    if len(value) == 10:
        fmt = "%Y-%m-%d"
        dt_obj = dt.datetime.strptime(value, fmt)
        if is_end:
            dt_obj = dt_obj.replace(hour=23, minute=59, second=59)
    else:
        fmt = "%Y-%m-%d %H:%M:%S"
        dt_obj = dt.datetime.strptime(value, fmt)
    return int(dt_obj.replace(tzinfo=dt.timezone.utc).timestamp())


def _parse_device_ids(value: Optional[str]) -> Optional[list[int]]:
    if not value:
        return None
    return [int(v.strip()) for v in value.split(",") if v.strip()]


def _sample_rows(
    conn: sqlite3.Connection,
    device_ids: Optional[list[int]],
    since_ts: Optional[int],
    until_ts: Optional[int],
) -> Iterable[tuple[int, int, int, int]]:
    cur = conn.cursor()
    base = "select DEVICE_ID, TIMESTAMP, HEART_RATE, STEPS from MI_BAND_ACTIVITY_SAMPLE"
    clauses = []
    params = []
    if device_ids:
        placeholders = ",".join(["?"] * len(device_ids))
        clauses.append(f"DEVICE_ID in ({placeholders})")
        params.extend(device_ids)
    if since_ts is not None:
        clauses.append("TIMESTAMP >= ?")
        params.append(since_ts)
    if until_ts is not None:
        clauses.append("TIMESTAMP <= ?")
        params.append(until_ts)
    if clauses:
        base += " where " + " and ".join(clauses)
    cur.execute(base, params)
    return cur.fetchall()


def build_sample_inserts(
    conn: sqlite3.Connection,
    mapping: Dict[int, int],
    source_id: int,
    device_ids: Optional[list[int]],
    since_ts: Optional[int],
    until_ts: Optional[int],
    batch_size: int,
):
    statements = []
    total = 0
    rows = _sample_rows(conn, device_ids, since_ts, until_ts)
    batch = []
    for device_id, ts, heart_rate, steps in rows:
        user_profile_id = mapping.get(device_id)
        if not user_profile_id:
            continue
        sample_time = dt.datetime.utcfromtimestamp(ts).strftime("%Y-%m-%d %H:%M:%S")
        batch.append(
            f"({_sql_value(user_profile_id)}, {source_id}, {_sql_value(device_id)}, "
            f"{_sql_value(sample_time)}, {_sql_value(heart_rate)}, {_sql_value(steps)})"
        )
        total += 1
        if len(batch) >= batch_size:
            statements.append(
                "INSERT INTO gadgetbridge_samples "
                "(user_profile_id, source_id, device_id, sample_time, heart_rate, steps) VALUES "
                + ",".join(batch)
                + ";"
            )
            batch = []
    if batch:
        statements.append(
            "INSERT INTO gadgetbridge_samples "
            "(user_profile_id, source_id, device_id, sample_time, heart_rate, steps) VALUES "
            + ",".join(batch)
            + ";"
        )
    return statements, total


def build_inserts(
    sessions,
    mapping: Dict[int, int],
    source_id: int,
    conn: sqlite3.Connection,
):
    statements = []
    skipped = 0
    for session_id, device_id, start_ms, end_ms, activity_kind in sessions:
        user_id = mapping.get(device_id)
        if not user_id:
            skipped += 1
            continue

        avg_hr, min_hr, max_hr = heart_rate_stats(conn, device_id, start_ms, end_ms)
        start_time = _ts_ms_to_dt_str(start_ms)
        end_time = _ts_ms_to_dt_str(end_ms) if end_ms else None
        duration_minutes = None
        if start_ms and end_ms and end_ms >= start_ms:
            duration_minutes = int((end_ms - start_ms) / 60000)

        source_uid = f"{device_id}:{start_ms}"
        stmt = (
            "INSERT INTO workouts "
            "(user_id, source_id, source_uid, start_time, end_time, "
            "duration_minutes, avg_heart_rate, min_heart_rate, max_heart_rate) VALUES ("
            f"{_sql_value(user_id)}, {source_id}, {_sql_value(source_uid)}, "
            f"{_sql_value(start_time)}, {_sql_value(end_time)}, {_sql_value(duration_minutes)}, "
            f"{_sql_value(avg_hr)}, {_sql_value(min_hr)}, {_sql_value(max_hr)});"
        )
        statements.append(stmt)
    return statements, skipped


def parse_mapping(mapping_str: Optional[str]) -> Dict[int, int]:
    if not mapping_str:
        return {}
    result = {}
    for pair in mapping_str.split(","):
        pair = pair.strip()
        if not pair:
            continue
        device, profile = pair.split(":", 1)
        result[int(device)] = int(profile)
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description="Import Gadgetbridge data into MariaDB.")
    parser.add_argument("--db", default="samples/Gadgetbridge.db")
    parser.add_argument("--source-id", type=int, default=3)
    parser.add_argument(
        "--mapping",
        default=None,
        help="Comma-separated device_id:user_id pairs (e.g. 1:1,2:1,3:2).",
    )
    parser.add_argument("--out-sql", default="/tmp/gadgetbridge_workouts.sql")
    parser.add_argument("--samples-out", default="/tmp/gadgetbridge_samples.sql")
    parser.add_argument(
        "--include-samples",
        action="store_true",
        help="Also export gadgetbridge_samples; mapping must match user_profile_id for samples.",
    )
    parser.add_argument("--samples-only", action="store_true")
    parser.add_argument("--samples-device-ids", default=None)
    parser.add_argument("--samples-since", default=None, help="YYYY-MM-DD or YYYY-MM-DD HH:MM:SS (UTC)")
    parser.add_argument("--samples-until", default=None, help="YYYY-MM-DD or YYYY-MM-DD HH:MM:SS (UTC)")
    parser.add_argument("--samples-batch-size", type=int, default=1000)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--db-host", default="192.168.0.13")
    parser.add_argument("--db-user", default="healthuser")
    parser.add_argument("--db-pass", default="healthpassword")
    parser.add_argument("--db-name", default="healthtracker")

    args = parser.parse_args()

    mapping = parse_mapping(args.mapping)
    if not mapping:
        # Default mapping (device_id -> user_id).
        mapping = {
            1: 1,
            2: 1,
            3: 2,
            4: 2,
            5: 3,
            7: 2,
            8: 3,
            9: 1,
            10: 2,
            11: 2,
            12: 2,
        }

    conn = sqlite3.connect(args.db)
    try:
        statements = []
        skipped = 0
        if not args.samples_only:
            sessions = load_sessions(conn)
            statements, skipped = build_inserts(sessions, mapping, args.source_id, conn)
        sample_statements = []
        sample_total = 0
        if args.include_samples:
            device_ids = _parse_device_ids(args.samples_device_ids)
            since_ts = _parse_date_bound(args.samples_since, is_end=False)
            until_ts = _parse_date_bound(args.samples_until, is_end=True)
            sample_statements, sample_total = build_sample_inserts(
                conn,
                mapping,
                args.source_id,
                device_ids,
                since_ts,
                until_ts,
                args.samples_batch_size,
            )
    finally:
        conn.close()

    if not args.samples_only:
        with open(args.out_sql, "w") as f:
            f.write("\n".join(statements))
        print(f"Wrote SQL: {args.out_sql} ({len(statements)} rows, skipped {skipped})")
    if args.include_samples:
        with open(args.samples_out, "w") as f:
            f.write("\n".join(sample_statements))
        print(f"Wrote SQL: {args.samples_out} ({sample_total} samples)")

    if args.apply:
        cmd = [
            "mariadb",
            "-h",
            args.db_host,
            "-u",
            args.db_user,
            f"-p{args.db_pass}",
            "--ssl=0",
            args.db_name,
        ]
        if not args.samples_only:
            with open(args.out_sql, "rb") as f:
                res = subprocess.run(cmd, stdin=f)
            if res.returncode != 0:
                return res.returncode
        if args.include_samples:
            with open(args.samples_out, "rb") as f:
                res = subprocess.run(cmd, stdin=f)
            return res.returncode
        return 0

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
