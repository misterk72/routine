#!/usr/bin/env python3
"""Import manual workout entries from XLSX into MariaDB workouts."""

import argparse
import datetime as dt
import sqlite3
import subprocess
import zipfile
import xml.etree.ElementTree as ET
from collections import defaultdict
import unicodedata

NS = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}

EXCLUDE_SHEETS = {"FC700", "Graphiques 2024", "Graphiques 2023"}


def _cell_value(cell: ET.Element, shared_strings: list[str]) -> str:
    cell_type = cell.attrib.get("t")
    if cell_type == "inlineStr":
        is_elem = cell.find("a:is", NS)
        if is_elem is None:
            return ""
        texts = [t.text or "" for t in is_elem.findall(".//a:t", NS)]
        return "".join(texts)
    if cell_type == "s":
        v = cell.find("a:v", NS)
        if v is None or v.text is None:
            return ""
        try:
            return shared_strings[int(v.text)]
        except (ValueError, IndexError):
            return v.text or ""
    v = cell.find("a:v", NS)
    if v is None:
        return ""
    return v.text or ""


def _row_values(row: ET.Element, shared_strings: list[str]) -> list[str]:
    vals: dict[int, str] = defaultdict(str)
    for cell in row.findall("a:c", NS):
        ref = cell.attrib.get("r", "")
        col = "".join([ch for ch in ref if ch.isalpha()])
        idx = 0
        for ch in col:
            idx = idx * 26 + (ord(ch.upper()) - ord("A") + 1)
        vals[idx] = _cell_value(cell, shared_strings)
    if not vals:
        return []
    max_idx = max(vals)
    return [vals[i] for i in range(1, max_idx + 1)]


def _sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _sql_value(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return f"'{_sql_escape(str(value))}'"


def _excel_serial_to_datetime(value: float) -> dt.datetime:
    # Excel epoch: 1899-12-30
    base = dt.datetime(1899, 12, 30)
    return base + dt.timedelta(days=value)


def _parse_datetime(value: str) -> dt.datetime:
    value = value.strip()
    if not value:
        raise ValueError("empty date")
    # numeric serial
    try:
        num = float(value)
        return _excel_serial_to_datetime(num)
    except ValueError:
        pass
    # common string formats
    for fmt in ("%d/%m/%Y %H:%M:%S", "%d/%m/%Y %H:%M", "%d/%m/%Y"):
        try:
            return dt.datetime.strptime(value, fmt)
        except ValueError:
            continue
    raise ValueError(f"unsupported date format: {value}")


def _sheet_names(path: str) -> tuple[list[str], list[tuple[str, str]]]:
    with zipfile.ZipFile(path) as z:
        shared_strings = []
        if "xl/sharedStrings.xml" in z.namelist():
            tree = ET.fromstring(z.read("xl/sharedStrings.xml"))
            for si in tree.findall("a:si", NS):
                texts = [t.text or "" for t in si.findall(".//a:t", NS)]
                shared_strings.append("".join(texts))
        wb = ET.fromstring(z.read("xl/workbook.xml"))
        sheets = []
        for sheet in wb.findall("a:sheets/a:sheet", NS):
            sheets.append(
                (
                    sheet.attrib["name"],
                    sheet.attrib.get(
                        "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
                    ),
                )
            )
        rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        rel_map = {
            rel.attrib["Id"]: rel.attrib["Target"]
            for rel in rels.findall(
                "{http://schemas.openxmlformats.org/package/2006/relationships}Relationship"
            )
        }
        result = []
        for name, rel_id in sheets:
            target = rel_map.get(rel_id)
            if not target:
                continue
            result.append((name, "xl/" + target.lstrip("/")))
        return shared_strings, result


def _load_sheet_rows(path: str, sheet_path: str, shared_strings: list[str]) -> list[list[str]]:
    with zipfile.ZipFile(path) as z:
        data = z.read(sheet_path)
    tree = ET.fromstring(data)
    rows = tree.findall("a:sheetData/a:row", NS)
    values = []
    for r in rows:
        row_vals = _row_values(r, shared_strings)
        if row_vals:
            values.append(row_vals)
    return values


def _normalize_text(value: str) -> str:
    value = value.replace("\ufeff", "").strip()
    value = unicodedata.normalize("NFKD", value)
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    value = " ".join(value.split())
    return value.lower()


def _normalize_header(rows: list[list[str]]) -> tuple[list[str], list[list[str]]]:
    if not rows:
        return [], []
    header = rows[0]
    data = rows[1:]
    # Find the first row that looks like a header (case-insensitive "date").
    for idx, row in enumerate(rows[:50]):
        joined = _normalize_text(" ".join(row))
        if "date" in joined:
            header = row
            data = rows[idx + 1 :]
            break
    return header, data


def _header_index(header: list[str]) -> dict[str, int]:
    mapping: dict[str, int] = {}
    for idx, name in enumerate(header):
        if not name:
            continue
        norm = _normalize_text(str(name))
        if not norm:
            continue
        mapping[norm] = idx

    def find(*keys: str) -> int | None:
        for key in keys:
            key_norm = _normalize_text(key)
            if key_norm in mapping:
                return mapping[key_norm]
        return None

    return {
        "date": find("Date et Heure", "Date", "Date Heure", "Date/Heure"),
        "program": find("Programme", "Program"),
        "duration": find("Durée (min)", "Duree (min)", "Duree", "Durée", "Duration"),
        "avg_speed": find("Vitesse Moyenne (km/h)", "Vitesse Moyenne", "Avg Speed (km/h)"),
        "distance": find("Distance parcourue (km)", "Distance (km)", "Distance"),
        "calories": find("Calories"),
        "calories_per_km": find("Calories/km", "Calories par km", "Calories par Km"),
        "avg_hr": find("Moyenne pulsations/min", "Moyenne pulsations", "FC moyenne", "Avg heart rate"),
        "max_hr": find("Max pulsations/min", "Max pulsations", "FC max", "Max heart rate"),
        "min_hr": find("Min pulsations/min", "Min pulsations", "FC min", "Min heart rate"),
        "sleep_hr": find("FC Repos pulsations/min", "FC Repos", "Resting HR"),
        "vo2_max": find("VO2", "VO2 max", "VO2_Max"),
        "soundtrack": find("Fond sonore", "Soundtrack"),
        "notes": find("Observations", "Notes"),
    }


def _float_or_none(value: str) -> float | None:
    value = value.strip()
    if not value:
        return None
    try:
        return float(value)
    except ValueError:
        return None


def build_inserts(
    xlsx_path: str,
    user_id: int,
    source_id: int,
    ignore_duplicates: bool,
    max_date: dt.date | None,
) -> list[str]:
    statements = []
    seen = set()
    shared_strings, sheet_list = _sheet_names(xlsx_path)
    for sheet_name, sheet_path in sheet_list:
        if sheet_name in EXCLUDE_SHEETS:
            continue
        rows = _load_sheet_rows(xlsx_path, sheet_path, shared_strings)
        header, data = _normalize_header(rows)
        if not header:
            continue
        col_index = _header_index(header)
        if col_index.get("date") is None:
            continue

        for row in data:
            if len(row) < len(header):
                row = row + [""] * (len(header) - len(row))

            date_raw = row[col_index["date"]]
            if not date_raw:
                continue
            try:
                dt_obj = _parse_datetime(date_raw)
            except ValueError:
                continue
            if max_date is not None and dt_obj.date() > max_date:
                continue
            start_time = dt_obj.strftime("%Y-%m-%d %H:%M:%S")

            program = row[col_index["program"]] if col_index.get("program") is not None else ""
            duration = _float_or_none(row[col_index["duration"]]) if col_index.get("duration") is not None else None
            avg_speed = _float_or_none(row[col_index["avg_speed"]]) if col_index.get("avg_speed") is not None else None
            distance = _float_or_none(row[col_index["distance"]]) if col_index.get("distance") is not None else None
            calories = _float_or_none(row[col_index["calories"]]) if col_index.get("calories") is not None else None
            calories_per_km = (
                _float_or_none(row[col_index["calories_per_km"]])
                if col_index.get("calories_per_km") is not None
                else None
            )
            avg_hr = _float_or_none(row[col_index["avg_hr"]]) if col_index.get("avg_hr") is not None else None
            max_hr = _float_or_none(row[col_index["max_hr"]]) if col_index.get("max_hr") is not None else None
            min_hr = _float_or_none(row[col_index["min_hr"]]) if col_index.get("min_hr") is not None else None
            sleep_hr_avg = (
                _float_or_none(row[col_index["sleep_hr"]]) if col_index.get("sleep_hr") is not None else None
            )
            vo2_max = _float_or_none(row[col_index["vo2_max"]]) if col_index.get("vo2_max") is not None else None
            notes = row[col_index["notes"]] if col_index.get("notes") is not None else ""
            soundtrack = row[col_index["soundtrack"]] if col_index.get("soundtrack") is not None else ""

            # Skip ghost rows with no workout metrics, even if program/notes are filled.
            if (
                distance is None
                and calories is None
                and avg_hr is None
                and min_hr is None
                and max_hr is None
            ):
                continue

            source_uid = f"{sheet_name}:{start_time}"
            if source_uid in seen:
                continue
            seen.add(source_uid)

            stmt = (
                "INSERT INTO workouts "
                "(user_id, source_id, source_uid, start_time, "
                "duration_minutes, program, distance_km, avg_speed_kmh, calories, calories_per_km, "
                "avg_heart_rate, min_heart_rate, max_heart_rate, sleep_heart_rate_avg, vo2_max, "
                "soundtrack, notes) VALUES ("
                f"{_sql_value(user_id)}, {source_id}, {_sql_value(source_uid)}, "
                f"{_sql_value(start_time)}, {_sql_value(duration)}, {_sql_value(program)}, "
                f"{_sql_value(distance)}, {_sql_value(avg_speed)}, {_sql_value(calories)}, "
                f"{_sql_value(calories_per_km)}, {_sql_value(avg_hr)}, {_sql_value(min_hr)}, "
                f"{_sql_value(max_hr)}, {_sql_value(sleep_hr_avg)}, {_sql_value(vo2_max)}, "
                f"{_sql_value(soundtrack)}, {_sql_value(notes)})"
            )
            if ignore_duplicates:
                stmt += (
                    " ON DUPLICATE KEY UPDATE "
                    "start_time=VALUES(start_time), duration_minutes=VALUES(duration_minutes), "
                    "program=VALUES(program), distance_km=VALUES(distance_km), "
                    "avg_speed_kmh=VALUES(avg_speed_kmh), calories=VALUES(calories), "
                    "calories_per_km=VALUES(calories_per_km), avg_heart_rate=VALUES(avg_heart_rate), "
                    "min_heart_rate=VALUES(min_heart_rate), max_heart_rate=VALUES(max_heart_rate), "
                    "sleep_heart_rate_avg=VALUES(sleep_heart_rate_avg), vo2_max=VALUES(vo2_max), "
                    "soundtrack=VALUES(soundtrack), notes=VALUES(notes)"
                )
            stmt += ";"
            statements.append(stmt)
    return statements


def main() -> int:
    parser = argparse.ArgumentParser(description="Import manual workouts XLSX into MariaDB.")
    parser.add_argument("--xlsx", default="samples/Entrainement vélo elliptique3.xlsx")
    parser.add_argument("--user-id", type=int, required=False)
    parser.add_argument("--user-profile-id", type=int, dest="user_id", required=False)
    parser.add_argument("--source-id", type=int, default=4)
    parser.add_argument("--ignore-duplicates", action="store_true")
    parser.add_argument("--max-date", default=None, help="Skip rows after this date (YYYY-MM-DD).")
    parser.add_argument("--allow-future", action="store_true", help="Allow dates after today.")
    parser.add_argument("--out-sql", default="/tmp/manual_workouts.sql")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--db-host", default="192.168.0.13")
    parser.add_argument("--db-user", default="healthuser")
    parser.add_argument("--db-pass", default="healthpassword")
    parser.add_argument("--db-name", default="healthtracker")

    args = parser.parse_args()

    if args.user_id is None:
        parser.error("--user-id is required")

    max_date = None
    if args.max_date:
        try:
            max_date = dt.datetime.strptime(args.max_date, "%Y-%m-%d").date()
        except ValueError as exc:
            parser.error(f"--max-date must be YYYY-MM-DD (got {args.max_date})")
    elif not args.allow_future:
        max_date = dt.date.today()

    statements = build_inserts(
        args.xlsx, args.user_id, args.source_id, args.ignore_duplicates, max_date
    )
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
