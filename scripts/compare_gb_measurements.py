import csv
import datetime as dt
import sqlite3
import struct
from collections import defaultdict

DB_PATH = "/home/kassabji/workspace/routine/samples/Gadgetbridge.db"
MEASUREMENTS_PATH = "/home/kassabji/workspace/routine/samples/measurements.csv"
DEVICE_ID = 9

HUAMI_TYPE_LIGHT_SLEEP = 9
HUAMI_TYPE_DEEP_SLEEP = 11
HUAMI_TYPE_NO_CHANGE = 0
HUAMI_TYPE_IGNORE = 10
HUAMI_TYPE_UNSET = -1
MI_BAND_RAW_KIND_MASK = 0x0F
HR_MIN_VALID = 10
HR_MAX_VALID = 250

MIN_SLEEP_SESSION_SECONDS = 5 * 60
MAX_WAKE_PHASE_SECONDS = 2 * 60 * 60


def parse_float(value):
    if value is None:
        return None
    value = value.strip()
    if not value:
        return None
    value = value.replace("%", "").replace(",", ".")
    try:
        return float(value)
    except ValueError:
        return None


def parse_int(value):
    f = parse_float(value)
    if f is None:
        return None
    return int(round(f))


def parse_datetime(value):
    value = value.strip()
    if not value:
        return None
    date_part, time_part = value.split(" ")
    h, m, s = time_part.split(":")
    time_part = f"{int(h):02d}:{int(m):02d}:{int(s):02d}"
    return dt.datetime.strptime(f"{date_part} {time_part}", "%d/%m/%Y %H:%M:%S")


def load_measurements(path):
    measurements = {}
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter=";")
        for row in reader:
            ts = parse_datetime(row["Date et Heure"])
            if ts is None:
                continue
            key = ts.date().isoformat()
            measurements[key] = {
                "date": ts,
                "duration_min": parse_int(row["Duree (min)"]),
                "hr_avg": parse_int(row["Moyenne pulsations/min"]),
                "hr_min": parse_int(row["Min pulsations/min"]),
                "hr_max": parse_int(row["Max pulsations/min"]),
                "sleep_hr": parse_int(row["FC Repos pulsations/min"]),
                "vo2": parse_int(row["VO2"]),
            }
    return measurements


def parse_huami_raw_summary(raw_bytes):
    if not raw_bytes:
        return {}
    data = memoryview(raw_bytes)
    pos = 0

    def read_u16():
        nonlocal pos
        if pos + 2 > len(data):
            return None
        value = struct.unpack_from("<H", data, pos)[0]
        pos += 2
        return value

    def read_i16():
        nonlocal pos
        if pos + 2 > len(data):
            return None
        value = struct.unpack_from("<h", data, pos)[0]
        pos += 2
        return value

    def read_i32():
        nonlocal pos
        if pos + 4 > len(data):
            return None
        value = struct.unpack_from("<i", data, pos)[0]
        pos += 4
        return value

    def read_f32():
        nonlocal pos
        if pos + 4 > len(data):
            return None
        value = struct.unpack_from("<f", data, pos)[0]
        pos += 4
        return value

    def skip(count):
        nonlocal pos
        pos = min(pos + count, len(data))

    version = read_u16()
    if version is None:
        return {}
    _raw_kind = read_u16()
    skip(8 + 12)  # timestamps + base coords

    if version >= 512:
        min_hr = None
        if version == 519:
            skip(1)
            min_hr = read_u16()
            if len(data) >= 0x8C:
                pos = 0x8C
            else:
                return {}
        elif version == 516:
            skip(4)

        _steps = read_i32()
        active_seconds = read_i32()
        skip(16)  # lat/long bounds
        calories = read_f32()
        distance = read_f32()
        skip(20)  # ascent/descent/altitudes
        skip(12)  # speeds
        skip(12)  # pace
        skip(12)  # cadence
        skip(12)  # stride
        _distance2 = read_f32()
        skip(4)  # unknown
        avg_hr = read_u16()
        skip(4)  # pace + stride
        max_hr = read_u16()

        return {
            "active_seconds": active_seconds,
            "distance_m": distance,
            "calories": calories,
            "avg_hr": avg_hr,
            "min_hr": min_hr,
            "max_hr": max_hr,
        }

    distance = read_f32()
    skip(4 * 4)  # ascent/descent/altitudes
    skip(16)  # lat/long bounds
    _steps = read_i32()
    active_seconds = read_i32()
    calories = read_f32()
    skip(4 * 4)  # speed/pace/stride
    skip(4)  # unknown
    skip(28)  # swimming/other
    avg_hr = read_i16()

    return {
        "active_seconds": active_seconds,
        "distance_m": distance,
        "calories": calories,
        "avg_hr": avg_hr,
        "min_hr": None,
        "max_hr": None,
    }


def get_timestamp_unit(conn, table):
    cur = conn.execute(f"SELECT MAX(TIMESTAMP) FROM {table} WHERE DEVICE_ID = ?", (DEVICE_ID,))
    max_ts = cur.fetchone()[0]
    if max_ts is None:
        return "ms"
    return "s" if 0 < max_ts <= 9_999_999_999 else "ms"


def find_last_valid_kind(conn, table, range_start):
    cur = conn.execute(
        f"SELECT RAW_KIND FROM {table} WHERE DEVICE_ID = ? AND TIMESTAMP < ? "
        f"AND RAW_KIND NOT IN ({','.join(['?'] * 7)}) "
        f"ORDER BY TIMESTAMP DESC LIMIT 1",
        (DEVICE_ID, range_start, HUAMI_TYPE_NO_CHANGE, HUAMI_TYPE_IGNORE, HUAMI_TYPE_UNSET,
         16, 80, 96, 112),
    )
    row = cur.fetchone()
    if row:
        return row[0] & MI_BAND_RAW_KIND_MASK
    return HUAMI_TYPE_UNSET


def normalize_miband_kind(raw_kind, last_valid):
    kind = raw_kind
    if kind != HUAMI_TYPE_UNSET:
        kind = kind & MI_BAND_RAW_KIND_MASK
    if kind in (HUAMI_TYPE_IGNORE, HUAMI_TYPE_NO_CHANGE):
        if last_valid != HUAMI_TYPE_UNSET:
            kind = last_valid
    else:
        last_valid = kind
    return kind, last_valid


def map_sleep_kind(raw_kind):
    if raw_kind == HUAMI_TYPE_LIGHT_SLEEP:
        return 1
    if raw_kind == HUAMI_TYPE_DEEP_SLEEP:
        return 2
    return 0


def compute_sleep_sessions(samples):
    sessions = []
    prev = None
    sleep_start = None
    sleep_end = None
    light = deep = 0
    duration_since_last_sleep = 0

    def finalize():
        if sleep_start is None or sleep_end is None:
            return
        duration = light + deep
        if sleep_end - sleep_start > MIN_SLEEP_SESSION_SECONDS and duration > MIN_SLEEP_SESSION_SECONDS:
            sessions.append((sleep_start, sleep_end))

    for sample in samples:
        if sample[1] != 0:
            if sleep_start is None:
                sleep_start = sample[0]
            sleep_end = sample[0]
            duration_since_last_sleep = 0
        else:
            finalize()
            sleep_start = sleep_end = None
            light = deep = 0

        if prev is not None:
            delta = sample[0] - prev[0]
            if sample[1] == 1:
                light += delta
            elif sample[1] == 2:
                deep += delta
            else:
                duration_since_last_sleep += delta
                if sleep_start is not None and duration_since_last_sleep > MAX_WAKE_PHASE_SECONDS:
                    finalize()
                    sleep_start = sleep_end = None
                    light = deep = 0
        prev = sample

    if sleep_start is not None and sleep_end is not None:
        duration = light + deep
        if duration > MIN_SLEEP_SESSION_SECONDS:
            sessions.append((sleep_start, sleep_end))
    return sessions


def compute_sleep_avg_hr(samples):
    if not samples:
        return None
    sessions = compute_sleep_sessions(samples)
    if sessions:
        start = sessions[0][0]
        end = sessions[-1][1]
    else:
        start = end = None
    total = 0
    count = 0
    for ts, kind, hr in samples:
        if kind == 0:
            continue
        if hr < HR_MIN_VALID or hr > HR_MAX_VALID:
            continue
        if start is not None and (ts < start or ts > end):
            continue
        total += hr
        count += 1
    if count == 0:
        return None
    return int(round(total / count))


def sleep_window_for(workout_start_ms):
    local = dt.datetime.fromtimestamp(workout_start_ms / 1000)
    noon = local.replace(hour=12, minute=0, second=0, microsecond=0)
    if local < noon:
        start = noon - dt.timedelta(days=1)
    else:
        start = noon
    end = start + dt.timedelta(days=1)
    return int(start.timestamp()), int(end.timestamp())


def main():
    measurements = load_measurements(MEASUREMENTS_PATH)

    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row

    summaries = conn.execute(
        "SELECT START_TIME, END_TIME, SUMMARY_DATA, RAW_SUMMARY_DATA "
        "FROM BASE_ACTIVITY_SUMMARY WHERE DEVICE_ID = ? ORDER BY START_TIME",
        (DEVICE_ID,),
    ).fetchall()

    ts_unit = get_timestamp_unit(conn, "MI_BAND_ACTIVITY_SAMPLE")

    by_date = defaultdict(dict)
    for row in summaries:
        start_ms = row["START_TIME"]
        end_ms = row["END_TIME"]
        if start_ms < 10_000_000_000:
            start_ms *= 1000
        if end_ms < 10_000_000_000:
            end_ms *= 1000
        date_key = dt.datetime.fromtimestamp(start_ms / 1000).date().isoformat()
        parsed = parse_huami_raw_summary(row["RAW_SUMMARY_DATA"])
        duration_min = None
        if parsed.get("active_seconds"):
            duration_min = int(round(parsed["active_seconds"] / 60))
        workout_hr_avg = parsed.get("avg_hr")

        range_start = start_ms // 1000 if ts_unit == "s" else start_ms
        range_end = end_ms // 1000 if ts_unit == "s" else end_ms
        last_valid = find_last_valid_kind(conn, "MI_BAND_ACTIVITY_SAMPLE", range_start)
        cur = conn.execute(
            "SELECT TIMESTAMP, RAW_KIND, HEART_RATE FROM MI_BAND_ACTIVITY_SAMPLE "
            "WHERE DEVICE_ID = ? AND TIMESTAMP BETWEEN ? AND ? ORDER BY TIMESTAMP ASC",
            (DEVICE_ID, range_start, range_end),
        )
        hr_vals = []
        for ts, raw_kind, hr in cur:
            kind, last_valid = normalize_miband_kind(raw_kind, last_valid)
            if HR_MIN_VALID <= hr <= HR_MAX_VALID:
                hr_vals.append(hr)
        hr_min = min(hr_vals) if hr_vals else None
        hr_max = max(hr_vals) if hr_vals else None

        sleep_start, sleep_end = sleep_window_for(start_ms)
        sleep_range_start = sleep_start
        sleep_range_end = sleep_end
        if ts_unit == "ms":
            sleep_range_start *= 1000
            sleep_range_end *= 1000

        last_valid_sleep = find_last_valid_kind(conn, "MI_BAND_ACTIVITY_SAMPLE", sleep_range_start)
        cur = conn.execute(
            "SELECT TIMESTAMP, RAW_KIND, HEART_RATE FROM MI_BAND_ACTIVITY_SAMPLE "
            "WHERE DEVICE_ID = ? AND TIMESTAMP BETWEEN ? AND ? ORDER BY TIMESTAMP ASC",
            (DEVICE_ID, sleep_range_start, sleep_range_end),
        )
        samples = []
        for ts, raw_kind, hr in cur:
            kind, last_valid_sleep = normalize_miband_kind(raw_kind, last_valid_sleep)
            sleep_kind = map_sleep_kind(kind)
            ts_sec = ts if ts_unit == "s" else ts // 1000
            samples.append((ts_sec, sleep_kind, hr))
        sleep_avg = compute_sleep_avg_hr(samples)

        by_date[date_key] = {
            "start": dt.datetime.fromtimestamp(start_ms / 1000),
            "duration_min": duration_min,
            "hr_avg": workout_hr_avg,
            "hr_min": hr_min,
            "hr_max": hr_max,
            "sleep_hr": sleep_avg,
        }

    print("date,start,meas_hr_avg,db_hr_avg,meas_hr_min,db_hr_min,meas_hr_max,db_hr_max,meas_sleep_hr,db_sleep_hr")
    for date_key in sorted(measurements.keys()):
        m = measurements[date_key]
        db = by_date.get(date_key)
        if not db:
            print(f"{date_key},{m['date']},,,,,,,,,")
            continue
        print(
            f"{date_key},{db['start']},"
            f"{m['hr_avg']},{db['hr_avg']},"
            f"{m['hr_min']},{db['hr_min']},"
            f"{m['hr_max']},{db['hr_max']},"
            f"{m['sleep_hr']},{db['sleep_hr']}"
        )


if __name__ == "__main__":
    main()
