#!/usr/bin/env python3
"""Import Withings weight data from XLSX into MariaDB weight_measurements."""

import argparse
import datetime as dt
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET
from collections import defaultdict

NS = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}


def _cell_value(cell: ET.Element) -> str:
    cell_type = cell.attrib.get("t")
    if cell_type == "inlineStr":
        is_elem = cell.find("a:is", NS)
        if is_elem is None:
            return ""
        texts = [t.text or "" for t in is_elem.findall(".//a:t", NS)]
        return "".join(texts)
    value = cell.find("a:v", NS)
    if value is None:
        return ""
    return value.text or ""


def _row_values(row: ET.Element) -> list[str]:
    vals: dict[int, str] = defaultdict(str)
    for cell in row.findall("a:c", NS):
        ref = cell.attrib.get("r", "")
        col = "".join([ch for ch in ref if ch.isalpha()])
        idx = 0
        for ch in col:
            idx = idx * 26 + (ord(ch.upper()) - ord("A") + 1)
        vals[idx] = _cell_value(cell)
    if not vals:
        return []
    max_idx = max(vals)
    return [vals[i] for i in range(1, max_idx + 1)]


def _parse_xlsx_rows(path: str) -> list[list[str]]:
    with zipfile.ZipFile(path) as z:
        data = z.read("xl/worksheets/sheet1.xml")
        tree = ET.fromstring(data)
        rows = tree.findall("a:sheetData/a:row", NS)
        return [_row_values(r) for r in rows if _row_values(r)]


def _parse_datetime(value: str) -> str:
    # Format: DD/MM/YYYY HH:MM:SS
    try:
        dt_obj = dt.datetime.strptime(value, "%d/%m/%Y %H:%M:%S")
    except ValueError:
        # Fallback: date only
        dt_obj = dt.datetime.strptime(value, "%d/%m/%Y")
    return dt_obj.strftime("%Y-%m-%d %H:%M:%S")


def _sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _sql_value(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return f"'{_sql_escape(str(value))}'"


def build_inserts(rows: list[list[str]], user_profile_id: int, source_id: int) -> list[str]:
    if not rows:
        return []
    header = rows[0]
    col_index = {name: idx for idx, name in enumerate(header)}
    required = ["Date", "Weight (kg)", "Fat Mass (kg)", "Device ID", "Model"]
    for name in required:
        if name not in col_index:
            raise ValueError(f"Missing required column: {name}")

    statements = []
    for row in rows[1:]:
        if len(row) < len(header):
            row = row + [""] * (len(header) - len(row))
        date_raw = row[col_index["Date"]]
        weight_raw = row[col_index["Weight (kg)"]]
        fat_raw = row[col_index["Fat Mass (kg)"]]
        device_id = row[col_index["Device ID"]]
        model = row[col_index["Model"]]

        if not date_raw:
            continue

        measured_at = _parse_datetime(date_raw)
        weight = float(weight_raw) if weight_raw else None
        fat_mass = float(fat_raw) if fat_raw else None
        fat_pct = None
        if weight and fat_mass is not None:
            fat_pct = round((fat_mass / weight) * 100, 2)

        source_uid = f"{device_id}:{measured_at}"
        stmt = (
            "INSERT INTO weight_measurements "
            "(user_profile_id, source_id, source_uid, measured_at, "
            "weight_kg, fat_mass_kg, fat_percentage) VALUES ("
            f"{_sql_value(user_profile_id)}, {source_id}, {_sql_value(source_uid)}, "
            f"{_sql_value(measured_at)}, {_sql_value(weight)}, {_sql_value(fat_mass)}, "
            f"{_sql_value(fat_pct)});"
        )
        statements.append(stmt)
    return statements


def main() -> int:
    parser = argparse.ArgumentParser(description="Import Withings XLSX into MariaDB.")
    parser.add_argument("--xlsx", default="samples/Withings_Weight_Data.xlsx")
    parser.add_argument("--user-profile-id", type=int, required=True)
    parser.add_argument("--source-id", type=int, default=2)
    parser.add_argument("--out-sql", default="/tmp/withings_import.sql")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--db-host", default="192.168.0.13")
    parser.add_argument("--db-user", default="healthuser")
    parser.add_argument("--db-pass", default="healthpassword")
    parser.add_argument("--db-name", default="healthtracker")

    args = parser.parse_args()

    rows = _parse_xlsx_rows(args.xlsx)
    statements = build_inserts(rows, args.user_profile_id, args.source_id)
    if not statements:
        print("No rows to import.")
        return 1

    with open(args.out_sql, "w") as f:
        f.write("\n".join(statements))

    print(f"Wrote SQL: {args.out_sql} ({len(statements)} rows)")

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
        with open(args.out_sql, "rb") as f:
            res = subprocess.run(cmd, stdin=f)
        return res.returncode

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
