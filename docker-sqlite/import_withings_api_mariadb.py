#!/usr/bin/env python3
"""Import Withings API measurements directly into MariaDB weight_measurements."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

import requests

TOKEN_URL = "https://wbsapi.withings.net/v2/oauth2"
DATA_URL = "https://wbsapi.withings.net/measure"
DEFAULT_WITHINGS_DIR = Path(__file__).resolve().parents[2] / "withings"


@dataclass
class Measurement:
    source_uid: str
    measured_at: str
    weight_kg: float | None
    fat_mass_kg: float | None
    fat_percentage: float | None
    device_id: str
    model: str


def _sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _sql_value(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return f"'{_sql_escape(str(value))}'"


def _read_json(path: str) -> dict:
    if not os.path.exists(path):
        raise RuntimeError(f"Missing tokens file: {path}")
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _write_json(path: str, data: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f)


def _is_token_valid(tokens: dict) -> bool:
    expires_at = float(tokens.get("expires_at", 0))
    return dt.datetime.now().timestamp() < expires_at - 30


def _extract_withings_creds(withings_py: str) -> tuple[str, str]:
    if not os.path.exists(withings_py):
        raise RuntimeError(f"Missing withings.py file: {withings_py}")
    with open(withings_py, "r", encoding="utf-8") as f:
        content = f.read()
    id_match = re.search(r"CLIENT_ID\s*=\s*'([^']+)'", content)
    secret_match = re.search(r"CLIENT_SECRET\s*=\s*'([^']+)'", content)
    if not id_match or not secret_match:
        raise RuntimeError(
            "Cannot extract CLIENT_ID/CLIENT_SECRET from withings.py. "
            "Set WITHINGS_CLIENT_ID and WITHINGS_CLIENT_SECRET."
        )
    return id_match.group(1), secret_match.group(1)


def _refresh_access_token(tokens: dict, client_id: str, client_secret: str) -> dict:
    refresh_token = tokens.get("refresh_token")
    if not refresh_token:
        raise RuntimeError("No refresh_token available in tokens.json")

    response = requests.post(
        TOKEN_URL,
        data={
            "action": "requesttoken",
            "grant_type": "refresh_token",
            "client_id": client_id,
            "client_secret": client_secret,
            "refresh_token": refresh_token,
        },
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    if "error" in payload:
        raise RuntimeError(f"Withings token refresh failed: {payload['error']}")
    body = payload["body"]
    return {
        "access_token": body["access_token"],
        "refresh_token": body["refresh_token"],
        "expires_at": (dt.datetime.now() + dt.timedelta(seconds=body["expires_in"])).timestamp(),
    }


def _run_mariadb_capture(
    db_host: str, db_user: str, db_pass: str, db_name: str, sql: str
) -> str:
    cmd = [
        "mariadb",
        "--protocol=TCP",
        "--ssl=OFF",
        "-N",
        "-B",
        "-h",
        db_host,
        "-u",
        db_user,
        f"-p{db_pass}",
        "-D",
        db_name,
        "-e",
        sql,
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return res.stdout.strip()


def _max_measured_at(
    db_host: str, db_user: str, db_pass: str, db_name: str, user_id: int, source_id: int
) -> dt.datetime | None:
    out = _run_mariadb_capture(
        db_host,
        db_user,
        db_pass,
        db_name,
        (
            "SELECT DATE_FORMAT(MAX(measured_at), '%Y-%m-%d %H:%i:%s') "
            f"FROM weight_measurements WHERE user_id={user_id} AND source_id={source_id}"
        ),
    )
    if not out:
        return None
    return dt.datetime.strptime(out, "%Y-%m-%d %H:%M:%S")


def _fetch_measurements(access_token: str, start_dt: dt.datetime, end_dt: dt.datetime) -> list[Measurement]:
    response = requests.get(
        DATA_URL,
        params={
            "action": "getmeas",
            "meastype": "1,8",
            "category": 1,
            "startdate": int(start_dt.timestamp()),
            "enddate": int(end_dt.timestamp()),
        },
        headers={"Authorization": f"Bearer {access_token}"},
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    if "error" in payload:
        raise RuntimeError(f"Withings fetch failed: {payload['error']}")

    groups = payload.get("body", {}).get("measuregrps", [])
    rows: list[Measurement] = []
    for grp in groups:
        ts = int(grp["date"])
        measured_at = dt.datetime.fromtimestamp(ts, dt.timezone.utc).astimezone().replace(
            tzinfo=None
        )
        measured_at_str = measured_at.strftime("%Y-%m-%d %H:%M:%S")
        device_id = grp.get("deviceid") or "unknown_device"
        model = grp.get("model") or ""
        weight_kg = None
        fat_mass_kg = None

        for measure in grp.get("measures", []):
            m_type = int(measure.get("type", -1))
            value = float(measure["value"]) * (10 ** int(measure["unit"]))
            if m_type == 1:
                weight_kg = value
            elif m_type == 8:
                fat_mass_kg = value

        if weight_kg is None and fat_mass_kg is None:
            continue

        fat_percentage = None
        if weight_kg and fat_mass_kg is not None and weight_kg > 0:
            fat_percentage = round((fat_mass_kg / weight_kg) * 100.0, 2)

        source_uid = f"withings:{device_id}:{ts}"
        rows.append(
            Measurement(
                source_uid=source_uid,
                measured_at=measured_at_str,
                weight_kg=round(weight_kg, 2) if weight_kg is not None else None,
                fat_mass_kg=round(fat_mass_kg, 2) if fat_mass_kg is not None else None,
                fat_percentage=fat_percentage,
                device_id=device_id,
                model=model,
            )
        )
    return rows


def _build_sql(rows: list[Measurement], user_id: int, source_id: int) -> str:
    stmts = []
    for row in rows:
        notes = f"withings_model={row.model}" if row.model else None
        stmts.append(
            "INSERT INTO weight_measurements "
            "(user_id, source_id, source_uid, measured_at, weight_kg, fat_mass_kg, fat_percentage, notes) VALUES ("
            f"{_sql_value(user_id)}, {source_id}, {_sql_value(row.source_uid)}, {_sql_value(row.measured_at)}, "
            f"{_sql_value(row.weight_kg)}, {_sql_value(row.fat_mass_kg)}, {_sql_value(row.fat_percentage)}, "
            f"{_sql_value(notes)}) "
            "ON DUPLICATE KEY UPDATE "
            "measured_at=VALUES(measured_at), "
            "weight_kg=VALUES(weight_kg), "
            "fat_mass_kg=VALUES(fat_mass_kg), "
            "fat_percentage=VALUES(fat_percentage), "
            "notes=VALUES(notes);"
        )
    return "\n".join(stmts)


def _apply_sql(sql_path: str, db_host: str, db_user: str, db_pass: str, db_name: str) -> int:
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
    with open(sql_path, "rb") as f:
        res = subprocess.run(cmd, stdin=f)
    return res.returncode


def _write_optional_xlsx(path: str, rows: list[Measurement]) -> None:
    try:
        from openpyxl import Workbook
    except ImportError as exc:
        raise RuntimeError("openpyxl is required for --export-xlsx") from exc

    wb = Workbook()
    ws = wb.active
    ws.title = "Weight Data"
    ws.append(["Date", "Weight (kg)", "Fat Mass (kg)", "Fat Percentage", "Device ID", "Model"])
    for row in sorted(rows, key=lambda item: item.measured_at, reverse=True):
        ws.append(
            [
                row.measured_at,
                row.weight_kg,
                row.fat_mass_kg,
                row.fat_percentage,
                row.device_id,
                row.model,
            ]
        )
    wb.save(path)


def main() -> int:
    parser = argparse.ArgumentParser(description="Import Withings API data directly into MariaDB.")
    parser.add_argument("--user-id", type=int, required=True)
    parser.add_argument("--source-id", type=int, default=2)
    parser.add_argument("--backfill-days", type=int, default=30)
    parser.add_argument(
        "--tokens-json", default=str(DEFAULT_WITHINGS_DIR / "tokens.json")
    )
    parser.add_argument(
        "--withings-py", default=str(DEFAULT_WITHINGS_DIR / "withings.py")
    )
    parser.add_argument("--export-xlsx", default=None)
    parser.add_argument("--out-sql", default="/tmp/withings_api_import.sql")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--db-host", default="192.168.0.103")
    parser.add_argument("--db-user", default="healthuser")
    parser.add_argument("--db-pass", default="healthpassword")
    parser.add_argument("--db-name", default="healthtracker")
    args = parser.parse_args()

    tokens = _read_json(args.tokens_json)
    if not _is_token_valid(tokens):
        client_id = os.environ.get("WITHINGS_CLIENT_ID")
        client_secret = os.environ.get("WITHINGS_CLIENT_SECRET")
        if not client_id or not client_secret:
            client_id, client_secret = _extract_withings_creds(args.withings_py)
        tokens = _refresh_access_token(tokens, client_id, client_secret)
        _write_json(args.tokens_json, tokens)
        print("Withings access token refreshed.")

    db_max = _max_measured_at(
        args.db_host, args.db_user, args.db_pass, args.db_name, args.user_id, args.source_id
    )
    end_dt = dt.datetime.now()
    if db_max is None:
        start_dt = end_dt - dt.timedelta(days=args.backfill_days)
    else:
        start_dt = db_max - dt.timedelta(days=args.backfill_days)

    rows = _fetch_measurements(tokens["access_token"], start_dt, end_dt)
    print(
        f"Fetched {len(rows)} measurement groups from Withings between "
        f"{start_dt.strftime('%Y-%m-%d %H:%M:%S')} and {end_dt.strftime('%Y-%m-%d %H:%M:%S')}."
    )
    if not rows:
        return 0

    sql = _build_sql(rows, args.user_id, args.source_id)
    with open(args.out_sql, "w", encoding="utf-8") as f:
        f.write(sql)
    print(f"Wrote SQL file: {args.out_sql}")

    if args.export_xlsx:
        _write_optional_xlsx(args.export_xlsx, rows)
        print(f"Wrote debug XLSX: {args.export_xlsx}")

    if args.dry_run:
        return 0
    if args.apply:
        return _apply_sql(args.out_sql, args.db_host, args.db_user, args.db_pass, args.db_name)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as exc:
        print(f"Network/API error: {exc}", file=sys.stderr)
        raise SystemExit(2)
    except subprocess.CalledProcessError as exc:
        print(f"MariaDB command failed: {exc}", file=sys.stderr)
        raise SystemExit(3)
    except Exception as exc:  # noqa: BLE001
        print(f"Error: {exc}", file=sys.stderr)
        raise SystemExit(1)
