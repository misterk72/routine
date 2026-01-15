#!/usr/bin/env python3
"""Extract a workout session and plot heart rate over time from a Gadgetbridge DB."""

import argparse
import csv
import datetime as dt
import sqlite3
import sys
from dataclasses import dataclass

import matplotlib.pyplot as plt


@dataclass
class Device:
    device_id: int
    name: str
    model: str
    alias: str


@dataclass
class Session:
    session_id: int
    device_id: int
    start_ms: int
    end_ms: int
    activity_kind: int


def _local_tz():
    return dt.datetime.now().astimezone().tzinfo


def _day_bounds_ms(day: dt.date) -> tuple[int, int]:
    tz = _local_tz()
    start = dt.datetime(day.year, day.month, day.day, tzinfo=tz)
    end = start + dt.timedelta(days=1)
    return int(start.timestamp() * 1000), int(end.timestamp() * 1000)


def list_devices(conn: sqlite3.Connection) -> list[Device]:
    cur = conn.cursor()
    cur.execute("select _id, NAME, MODEL, ALIAS from DEVICE order by _id")
    return [Device(*row) for row in cur.fetchall()]


def list_sessions_for_day(
    conn: sqlite3.Connection, day: dt.date, device_id: int | None
) -> list[Session]:
    start_ms, end_ms = _day_bounds_ms(day)
    cur = conn.cursor()
    if device_id is None:
        cur.execute(
            "select _id, DEVICE_ID, START_TIME, END_TIME, ACTIVITY_KIND "
            "from BASE_ACTIVITY_SUMMARY "
            "where START_TIME >= ? and START_TIME < ? "
            "order by START_TIME",
            (start_ms, end_ms),
        )
    else:
        cur.execute(
            "select _id, DEVICE_ID, START_TIME, END_TIME, ACTIVITY_KIND "
            "from BASE_ACTIVITY_SUMMARY "
            "where DEVICE_ID = ? and START_TIME >= ? and START_TIME < ? "
            "order by START_TIME",
            (device_id, start_ms, end_ms),
        )
    return [Session(*row) for row in cur.fetchall()]


def list_sessions_all(conn: sqlite3.Connection, device_id: int | None) -> list[Session]:
    cur = conn.cursor()
    if device_id is None:
        cur.execute(
            "select _id, DEVICE_ID, START_TIME, END_TIME, ACTIVITY_KIND "
            "from BASE_ACTIVITY_SUMMARY "
            "order by START_TIME"
        )
    else:
        cur.execute(
            "select _id, DEVICE_ID, START_TIME, END_TIME, ACTIVITY_KIND "
            "from BASE_ACTIVITY_SUMMARY "
            "where DEVICE_ID = ? "
            "order by START_TIME",
            (device_id,),
        )
    return [Session(*row) for row in cur.fetchall()]


def select_session(sessions: list[Session], index: int) -> Session:
    if index < 0 or index >= len(sessions):
        raise ValueError("Invalid session index")
    return sessions[index]


def fetch_heart_rate(
    conn: sqlite3.Connection, device_id: int, start_ms: int, end_ms: int
) -> list[tuple[int, int]]:
    start_sec = start_ms // 1000
    end_sec = end_ms // 1000
    cur = conn.cursor()
    cur.execute(
        "select TIMESTAMP, HEART_RATE "
        "from MI_BAND_ACTIVITY_SAMPLE "
        "where DEVICE_ID = ? and TIMESTAMP >= ? and TIMESTAMP <= ? "
        "and HEART_RATE > 0 "
        "order by TIMESTAMP",
        (device_id, start_sec, end_sec),
    )
    return cur.fetchall()


def write_csv(rows: list[tuple[int, int]], csv_path: str) -> None:
    with open(csv_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["timestamp_iso", "heart_rate"])
        for ts_sec, hr in rows:
            iso = dt.datetime.fromtimestamp(ts_sec, tz=dt.timezone.utc).isoformat()
            writer.writerow([iso, hr])


def plot_hr(rows: list[tuple[int, int]], png_path: str, title: str) -> None:
    if not rows:
        raise ValueError("No heart rate samples found in that window.")
    times = [dt.datetime.fromtimestamp(ts, tz=dt.timezone.utc) for ts, _ in rows]
    hrs = [hr for _, hr in rows]

    plt.figure(figsize=(10, 4))
    plt.plot(times, hrs, linewidth=1.2)
    plt.xlabel("Time (UTC)")
    plt.ylabel("Heart rate (bpm)")
    plt.title(title)
    plt.tight_layout()
    plt.savefig(png_path, dpi=150)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Extract a workout session and plot heart rate over time."
    )
    parser.add_argument("--db", default="Gadgetbridge/Gadgetbridge.db")
    parser.add_argument("--date", help="YYYY-MM-DD (default: today)")
    parser.add_argument("--device-id", type=int, help="Device id from DEVICE table")
    parser.add_argument(
        "--session-index",
        type=int,
        default=0,
        help="Index in today's session list (default: 0)",
    )
    parser.add_argument(
        "--out",
        default=None,
        help="Output PNG path (default: workout_hr_<date>_device_<id>.png)",
    )
    parser.add_argument(
        "--csv-out",
        default=None,
        help="Optional CSV output path",
    )
    parser.add_argument(
        "--list-sessions",
        action="store_true",
        help="List sessions for the day and exit",
    )
    parser.add_argument(
        "--list-all-sessions",
        action="store_true",
        help="List all sessions across the database and exit",
    )

    args = parser.parse_args()

    day = dt.date.today()
    if args.date:
        try:
            day = dt.date.fromisoformat(args.date)
        except ValueError:
            print("Invalid --date, expected YYYY-MM-DD", file=sys.stderr)
            return 2

    conn = sqlite3.connect(args.db)
    try:
        if args.list_all_sessions:
            sessions = list_sessions_all(conn, args.device_id)
            if not sessions:
                print("No sessions found in the database.")
                return 0
            for i, s in enumerate(sessions):
                start_iso = dt.datetime.fromtimestamp(
                    s.start_ms / 1000, tz=dt.timezone.utc
                ).isoformat()
                end_iso = dt.datetime.fromtimestamp(
                    s.end_ms / 1000, tz=dt.timezone.utc
                ).isoformat()
                print(
                    f"[{i}] device={s.device_id} activity_kind={s.activity_kind} "
                    f"start={start_iso} end={end_iso}"
                )
            return 0

        sessions = list_sessions_for_day(conn, day, args.device_id)

        if args.list_sessions:
            if not sessions:
                print("No sessions found for that day.")
                return 0
            for i, s in enumerate(sessions):
                start_iso = dt.datetime.fromtimestamp(
                    s.start_ms / 1000, tz=dt.timezone.utc
                ).isoformat()
                end_iso = dt.datetime.fromtimestamp(
                    s.end_ms / 1000, tz=dt.timezone.utc
                ).isoformat()
                print(
                    f"[{i}] device={s.device_id} activity_kind={s.activity_kind} "
                    f"start={start_iso} end={end_iso}"
                )
            return 0

        if not sessions:
            devices = list_devices(conn)
            print("No sessions found for that day.")
            if devices:
                print("Available devices:")
                for d in devices:
                    alias = f" ({d.alias})" if d.alias else ""
                    print(f"  {d.device_id}: {d.name} {d.model}{alias}")
            print("Tip: try --list-sessions or pass --date / --device-id.")
            return 1

        session = select_session(sessions, args.session_index)

        rows = fetch_heart_rate(conn, session.device_id, session.start_ms, session.end_ms)
        if not rows:
            print("No heart rate samples found for that session.")
            return 1

        date_str = day.isoformat()
        out_path = args.out or f"workout_hr_{date_str}_device_{session.device_id}.png"
        title = f"Heart rate - {date_str} (device {session.device_id})"
        plot_hr(rows, out_path, title)

        if args.csv_out:
            write_csv(rows, args.csv_out)

        print(f"Saved chart: {out_path}")
        if args.csv_out:
            print(f"Saved CSV: {args.csv_out}")
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
