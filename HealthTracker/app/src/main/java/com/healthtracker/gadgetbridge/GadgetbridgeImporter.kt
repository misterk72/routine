package com.healthtracker.gadgetbridge

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.zip.ZipInputStream
import org.json.JSONObject

class GadgetbridgeImporter(
    private val context: Context,
    private val config: GadgetbridgeImportConfig = GadgetbridgeImportConfig(context)
) {
    companion object {
        private const val TAG = "GadgetbridgeImporter"
        private const val HUAMI_TYPE_LIGHT_SLEEP = 9
        private const val HUAMI_TYPE_DEEP_SLEEP = 11
        private const val HUAMI_EXT_TYPE_SLEEP = 120
        private const val HUAMI_EXT_TYPE_DEEP_SLEEP = 121
        private const val HUAMI_EXT_TYPE_REM_SLEEP = 122
        private const val HUAMI_EXT_TYPE_AWAKE_SLEEP = 123
        private const val HUAMI_TYPE_NO_CHANGE = 0
        private const val HUAMI_TYPE_IGNORE = 10
        private const val HUAMI_TYPE_UNSET = -1
        private const val MI_BAND_RAW_KIND_MASK = 0x0f
        private const val MI_BAND_TYPE_EXCLUDE_1 = 16
        private const val MI_BAND_TYPE_EXCLUDE_2 = 80
        private const val MI_BAND_TYPE_EXCLUDE_3 = 96
        private const val MI_BAND_TYPE_EXCLUDE_4 = 112
        private const val HR_MIN_VALID = 10
        private const val HR_MAX_VALID = 250
        private const val SLEEP_KIND_NONE = 0
        private const val SLEEP_KIND_LIGHT = 1
        private const val SLEEP_KIND_DEEP = 2
        private const val SLEEP_KIND_REM = 3
        private const val SLEEP_KIND_AWAKE = 4
        private const val MIN_SLEEP_SESSION_SECONDS = 5 * 60
        private const val MAX_WAKE_PHASE_SECONDS = 2 * 60 * 60
    }

    fun importLatestWorkout(): GadgetbridgeImportResult? {
        val exportUri = config.getExportUri() ?: return null
        val deviceMatch = config.getDeviceMatch()
        val lastImportedStart = config.getLastImportedStartTime()

        val tempFile = copyUriToTemp(exportUri) ?: return null
        val dbFile = extractDbIfNeeded(tempFile) ?: return null

        try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val tables = loadTables(db)
                val deviceTable = findTable(tables, listOf("device")) ?: return null
                val deviceColumns = loadColumns(db, deviceTable)
                val deviceId = findDeviceId(db, deviceTable, deviceColumns, deviceMatch) ?: return null

                val summaryTable = findTable(tables, listOf("base_activity_summary")) ?: return null
                val summaryColumns = loadColumns(db, summaryTable)
                val summary = loadLatestSummary(
                    db,
                    summaryTable,
                    summaryColumns,
                    deviceId,
                    lastImportedStart
                ) ?: return null

                val (startMillis, endMillis, summaryData, rawSummaryData) = summary
                Log.d(
                    TAG,
                    "Summary payload summaryLen=${summaryData?.length ?: 0} rawLen=${rawSummaryData?.size ?: 0}"
                )

                val summaryJson = summaryData?.let { data ->
                    try {
                        JSONObject(data)
                    } catch (_: Exception) {
                        JSONObject()
                    }
                } ?: JSONObject()

                val rawValues = if (summaryData.isNullOrBlank()) {
                    parseHuamiRawSummary(rawSummaryData)
                } else {
                    null
                }
                if (rawValues != null) {
                    Log.d(
                        TAG,
                        "Raw summary parsed activeSeconds=${rawValues.activeSeconds} " +
                            "distanceMeters=${rawValues.distanceMeters} calories=${rawValues.calories} " +
                            "avgHr=${rawValues.averageHr} minHr=${rawValues.minHr} maxHr=${rawValues.maxHr}"
                    )
                }

                val durationSeconds = getDouble(summaryJson, "activeSeconds")
                    ?: rawValues?.activeSeconds?.toDouble()
                    ?: ((endMillis - startMillis) / 1000.0)
                val distanceMeters = getDouble(summaryJson, "distanceMeters")
                    ?: rawValues?.distanceMeters

                val workoutHeartRates = findWorkoutHeartRates(db, tables, deviceId, startMillis, endMillis)
                val hrAvg = rawValues?.averageHr
                    ?: getDouble(summaryJson, "averageHR")?.toInt()
                    ?: workoutHeartRates?.avg
                val hrMin = workoutHeartRates?.min
                    ?: getDouble(summaryJson, "minHR")?.toInt()
                    ?: rawValues?.minHr
                val hrMax = workoutHeartRates?.max
                    ?: getDouble(summaryJson, "maxHR")?.toInt()
                    ?: rawValues?.maxHr

                val sleepHrAvg = findSleepHeartRateAvg(db, tables, deviceId, startMillis)
                val vo2Max = if (hrMax != null && sleepHrAvg != null && sleepHrAvg > 0) {
                    15f * hrMax.toFloat() / sleepHrAvg.toFloat()
                } else {
                    null
                }

                val startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startMillis),
                    ZoneId.systemDefault()
                )
                val durationMinutes = (durationSeconds / 60.0).toInt().takeIf { it > 0 }
                val distanceKm = null // Distance stays manual, do not import.

                config.setLastImportedStartTime(startMillis)

                return GadgetbridgeImportResult(
                    startTime = startTime,
                    durationMinutes = durationMinutes,
                    distanceKm = distanceKm,
                    calories = null, // Calories stay manual, do not import.
                    heartRateAvg = hrAvg,
                    heartRateMin = hrMin,
                    heartRateMax = hrMax,
                    sleepHeartRateAvg = sleepHrAvg,
                    vo2Max = vo2Max
                )
            }
        } finally {
            if (dbFile != tempFile) {
                dbFile.delete()
            }
            tempFile.delete()
        }
    }

    private fun copyUriToTemp(uri: Uri): File? {
        val tempFile = File.createTempFile("gadgetbridge_export_", ".db", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            return tempFile
        }
        Log.w(TAG, "Unable to read export uri: $uri")
        tempFile.delete()
        return null
    }

    private fun extractDbIfNeeded(file: File): File? {
        if (!isZipFile(file)) {
            return file
        }
        ZipInputStream(file.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.lowercase(Locale.US).endsWith(".db")) {
                    val extracted = File.createTempFile("gadgetbridge_export_", ".db", context.cacheDir)
                    FileOutputStream(extracted).use { output ->
                        zip.copyTo(output)
                    }
                    return extracted
                }
                entry = zip.nextEntry
            }
        }
        Log.w(TAG, "Zip export did not contain a .db file")
        return null
    }

    private fun isZipFile(file: File): Boolean {
        file.inputStream().use { input ->
            val header = ByteArray(4)
            val read = input.read(header)
            if (read < 4) {
                return false
            }
            return header[0] == 'P'.code.toByte() &&
                header[1] == 'K'.code.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
        }
    }

    private fun loadTables(db: SQLiteDatabase): Map<String, String> {
        val tables = mutableMapOf<String, String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                tables[normalize(name)] = name
            }
        }
        return tables
    }

    private fun loadColumns(db: SQLiteDatabase, table: String): Map<String, String> {
        val columns = mutableMapOf<String, String>()
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                columns[normalize(name)] = name
            }
        }
        return columns
    }

    private fun findTable(tables: Map<String, String>, candidates: List<String>): String? {
        for (candidate in candidates) {
            val normalized = normalize(candidate)
            tables[normalized]?.let { return it }
        }
        return null
    }

    private fun findColumn(columns: Map<String, String>, candidates: List<String>): String? {
        for (candidate in candidates) {
            val normalized = normalize(candidate)
            columns[normalized]?.let { return it }
        }
        return null
    }

    private fun findDeviceId(
        db: SQLiteDatabase,
        table: String,
        columns: Map<String, String>,
        deviceMatch: String?
    ): Long? {
        val idCol = findColumn(columns, listOf("id")) ?: return null
        val nameCol = findColumn(columns, listOf("name"))
        val aliasCol = findColumn(columns, listOf("alias"))
        val identifierCol = findColumn(columns, listOf("identifier"))

        val cursor = db.query(
            table,
            listOfNotNull(idCol, nameCol, aliasCol, identifierCol).toTypedArray(),
            null,
            null,
            null,
            null,
            null
        )

        cursor.use {
            val match = deviceMatch?.trim()?.lowercase(Locale.US)
            var firstId: Long? = null
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(idCol))
                if (firstId == null) {
                    firstId = id
                }
                if (match.isNullOrEmpty()) {
                    continue
                }
                val name = nameCol?.let { col -> it.getString(it.getColumnIndexOrThrow(col)) }
                val alias = aliasCol?.let { col -> it.getString(it.getColumnIndexOrThrow(col)) }
                val identifier = identifierCol?.let { col -> it.getString(it.getColumnIndexOrThrow(col)) }
                if (listOfNotNull(name, alias, identifier).any { value ->
                        value.lowercase(Locale.US).contains(match)
                    }) {
                    Log.d(TAG, "Matched device id=$id name=$name alias=$alias identifier=$identifier")
                    return id
                }
            }
            if (!match.isNullOrEmpty()) {
                Log.w(TAG, "No device matched '$match', using none")
            }
            return if (match.isNullOrEmpty()) firstId else null
        }
    }

    private fun loadLatestSummary(
        db: SQLiteDatabase,
        table: String,
        columns: Map<String, String>,
        deviceId: Long,
        lastImportedStart: Long
    ): SummaryRow? {
        val deviceCol = findColumn(columns, listOf("deviceId", "device_id")) ?: return null
        val startCol = findColumn(columns, listOf("startTime", "start_time")) ?: return null
        val endCol = findColumn(columns, listOf("endTime", "end_time"))
        val summaryCol = findColumn(columns, listOf("summaryData", "summary_data"))
        val rawSummaryCol = findColumn(columns, listOf("rawSummaryData", "raw_summary_data"))

        Log.d(
            TAG,
            "Summary table=$table deviceCol=$deviceCol startCol=$startCol endCol=$endCol " +
                "summaryCol=$summaryCol rawSummaryCol=$rawSummaryCol lastImportedStart=$lastImportedStart"
        )
        logSummaryCandidates(db, table, deviceCol, startCol, deviceId, lastImportedStart)

        val selection = if (lastImportedStart > 0) {
            "$deviceCol = ? AND $startCol > ?"
        } else {
            "$deviceCol = ?"
        }
        val args = if (lastImportedStart > 0) {
            arrayOf(deviceId.toString(), lastImportedStart.toString())
        } else {
            arrayOf(deviceId.toString())
        }

        val first = querySummary(db, table, startCol, endCol, summaryCol, rawSummaryCol, selection, args)
        if (first != null) {
            return first
        }

        if (lastImportedStart > 0) {
            Log.w(TAG, "No summary after lastImportedStart, retrying without filter")
            val fallback = querySummary(
                db,
                table,
                startCol,
                endCol,
                summaryCol,
                rawSummaryCol,
                "$deviceCol = ?",
                arrayOf(deviceId.toString())
            )
            if (fallback != null) {
                return fallback
            }
        }

        val totalForDevice = countSummaries(db, table, deviceCol, deviceId)
        Log.w(TAG, "No activity summary found for deviceId=$deviceId (count=$totalForDevice)")
        return null
    }

    private fun logSummaryCandidates(
        db: SQLiteDatabase,
        table: String,
        deviceCol: String,
        startCol: String,
        deviceId: Long,
        lastImportedStart: Long
    ) {
        val cursor = db.query(
            table,
            arrayOf(startCol),
            "$deviceCol = ?",
            arrayOf(deviceId.toString()),
            null,
            null,
            "$startCol DESC",
            "3"
        )
        cursor.use {
            if (!it.moveToFirst()) {
                Log.d(TAG, "No summary candidates for deviceId=$deviceId")
                return
            }
            var index = 0
            do {
                val raw = when (it.getType(0)) {
                    Cursor.FIELD_TYPE_INTEGER -> it.getLong(0).toString()
                    Cursor.FIELD_TYPE_STRING -> it.getString(0)
                    else -> "null"
                }
                val parsed = parseDateValue(it, startCol)
                Log.d(
                    TAG,
                    "Summary candidate[$index] rawStart=$raw parsedStart=$parsed " +
                        "lastImportedStart=$lastImportedStart"
                )
                index++
            } while (it.moveToNext())
        }
    }

    private fun querySummary(
        db: SQLiteDatabase,
        table: String,
        startCol: String,
        endCol: String?,
        summaryCol: String?,
        rawSummaryCol: String?,
        selection: String,
        args: Array<String>
    ): SummaryRow? {
        val cursor = db.query(
            table,
            listOfNotNull(startCol, endCol, summaryCol, rawSummaryCol).toTypedArray(),
            selection,
            args,
            null,
            null,
            "$startCol DESC",
            "1"
        )

        cursor.use {
            if (!it.moveToFirst()) {
                return null
            }
            val startMillis = parseDateValue(it, startCol)
            val endMillis = endCol?.let { col -> parseDateValue(it, col) } ?: startMillis
            val summaryData = summaryCol?.let { col -> it.getString(it.getColumnIndexOrThrow(col)) }
            val rawSummaryData = rawSummaryCol?.let { col -> it.getBlob(it.getColumnIndexOrThrow(col)) }
            Log.d(TAG, "Loaded summary start=$startMillis end=$endMillis")
            return SummaryRow(startMillis, endMillis, summaryData, rawSummaryData)
        }
    }

    private fun countSummaries(
        db: SQLiteDatabase,
        table: String,
        deviceCol: String,
        deviceId: Long
    ): Long {
        val cursor = db.rawQuery(
            "SELECT COUNT(1) FROM $table WHERE $deviceCol = ?",
            arrayOf(deviceId.toString())
        )
        cursor.use {
            return if (it.moveToFirst()) it.getLong(0) else 0L
        }
    }

    private fun parseDateValue(cursor: Cursor, column: String): Long {
        val index = cursor.getColumnIndexOrThrow(column)
        return when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> {
                val value = cursor.getLong(index)
                if (value < 10_000_000_000L) value * 1000L else value
            }
            Cursor.FIELD_TYPE_STRING -> {
                val raw = cursor.getString(index)
                raw.toLongOrNull()?.let { value ->
                    if (value < 10_000_000_000L) value * 1000L else value
                } ?: 0L
            }
            else -> 0L
        }
    }

    private fun findSleepHeartRateAvg(
        db: SQLiteDatabase,
        tables: Map<String, String>,
        deviceId: Long,
        startMillis: Long
    ): Int? {
        val sleepSampleAvg = findSleepHeartRateAvgFromActivitySamples(db, tables, deviceId, startMillis)
        if (sleepSampleAvg != null) {
            return sleepSampleAvg
        }
        val sleepSessionAvg = findSleepSessionAvgHr(db, tables, deviceId, startMillis)
        if (sleepSessionAvg != null) {
            return sleepSessionAvg
        }
        return findRestingHeartRateAvg(db, tables, deviceId, startMillis)
    }

    private fun findSleepHeartRateAvgFromActivitySamples(
        db: SQLiteDatabase,
        tables: Map<String, String>,
        deviceId: Long,
        workoutStartMillis: Long
    ): Int? {
        val (windowStart, windowEnd) = computeSleepWindow(workoutStartMillis)
        val candidateTables = tables.entries.map { it.value }.filter { table ->
            normalize(table).contains("activitysample")
        }.sorted()

        Log.d(
            TAG,
            "Sleep HR window $windowStart..$windowEnd tables=${candidateTables.size}"
        )

        for (table in candidateTables) {
            val columns = loadColumns(db, table)
            val deviceCol = findColumn(columns, listOf("deviceId", "device_id")) ?: continue
            val timestampCol = findColumn(columns, listOf("timestamp")) ?: continue
            val hrCol = findColumn(columns, listOf("heartRate", "heart_rate")) ?: continue
            val kindCol = findColumn(columns, listOf("rawKind", "raw_kind", "kind")) ?: continue

            val (rangeStart, rangeEnd) = adjustWindowForTimestamp(
                db,
                table,
                deviceCol,
                timestampCol,
                deviceId,
                windowStart,
                windowEnd
            )

            val useMiBandPostProcess = normalize(table) == "mibandactivitysample"
            val lastValidKind = if (useMiBandPostProcess) {
                findLastValidMiBandKind(db, table, deviceCol, timestampCol, kindCol, deviceId, rangeStart)
            } else {
                HUAMI_TYPE_UNSET
            }

            val samples = loadActivitySamples(
                db,
                table,
                deviceCol,
                timestampCol,
                kindCol,
                hrCol,
                deviceId,
                rangeStart,
                rangeEnd,
                useMiBandPostProcess,
                lastValidKind
            )

            val avg = computeSleepAverageHr(samples)
            if (avg != null) {
                Log.d(TAG, "Found sleep HR avg from $table")
                return avg
            }
        }

        Log.d(TAG, "No sleep HR avg found from activity samples")
        return null
    }

    private fun findSleepSessionAvgHr(
        db: SQLiteDatabase,
        tables: Map<String, String>,
        deviceId: Long,
        startMillis: Long
    ): Int? {
        val sessionTable = findTable(tables, listOf("huami_sleep_session_sample", "sleep_session_sample"))
            ?: return null
        val columns = loadColumns(db, sessionTable)
        val deviceCol = findColumn(columns, listOf("deviceId", "device_id")) ?: return null
        val timestampCol = findColumn(columns, listOf("timestamp")) ?: return null
        val dataCol = findColumn(columns, listOf("data")) ?: return null

        val cursor = db.query(
            sessionTable,
            arrayOf(timestampCol, dataCol),
            "$deviceCol = ?",
            arrayOf(deviceId.toString()),
            null,
            null,
            "$timestampCol DESC",
            "20"
        )

        cursor.use {
            while (it.moveToNext()) {
                val timestampMillis = parseDateValue(it, timestampCol)
                if (timestampMillis > startMillis) {
                    continue
                }
                val data = it.getBlob(it.getColumnIndexOrThrow(dataCol)) ?: continue
                val avgHr = readUnsignedByte(data, 0x15)
                if (avgHr != null && avgHr > 0) {
                    Log.d(TAG, "Found sleep session avg HR from $sessionTable")
                    return avgHr
                }
            }
        }
        Log.d(TAG, "No sleep session avg HR found")
        return null
    }

    private fun findRestingHeartRateAvg(
        db: SQLiteDatabase,
        tables: Map<String, String>,
        deviceId: Long,
        startMillis: Long
    ): Int? {
        val restingTable = tables.entries.firstOrNull { entry ->
            val name = entry.key
            name.contains("heartrateresting") || (name.contains("resting") && name.contains("heartrate"))
        }?.value

        val candidateTables = mutableListOf<String>()
        if (restingTable != null) {
            candidateTables.add(restingTable)
        }
        candidateTables.addAll(
            tables.entries.filter { entry ->
                val name = entry.key
                name.contains("heartratesample") && !name.contains("resting")
            }.map { it.value }
        )

        val windowStart = startMillis - 12L * 60L * 60L * 1000L
        val windowEnd = startMillis

        Log.d(TAG, "Resting HR window $windowStart..$windowEnd tables=${candidateTables.size}")

        for (table in candidateTables) {
            val columns = loadColumns(db, table)
            val deviceCol = findColumn(columns, listOf("deviceId", "device_id")) ?: continue
            val timestampCol = findColumn(columns, listOf("timestamp")) ?: continue
            val hrCol = findColumn(columns, listOf("heartRate", "heart_rate")) ?: continue

            val (rangeStart, rangeEnd) = adjustWindowForTimestamp(
                db,
                table,
                deviceCol,
                timestampCol,
                deviceId,
                windowStart,
                windowEnd
            )
            val cursor = db.rawQuery(
                "SELECT AVG($hrCol) FROM $table WHERE $deviceCol = ? AND $timestampCol BETWEEN ? AND ?",
                arrayOf(deviceId.toString(), rangeStart.toString(), rangeEnd.toString())
            )
            cursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    Log.d(TAG, "Found resting HR avg from $table")
                    return it.getDouble(0).toInt()
                }
            }
        }
        Log.d(TAG, "No resting HR avg found")
        return null
    }

    private fun findWorkoutHeartRates(
        db: SQLiteDatabase,
        tables: Map<String, String>,
        deviceId: Long,
        startMillis: Long,
        endMillis: Long
    ): WorkoutHeartRates? {
        if (endMillis <= startMillis) {
            return null
        }
        val candidateTables = tables.entries.map { it.value }.filter { table ->
            val name = normalize(table)
            val isHeartRateSample =
                (name.contains("heartrate") || name.contains("heart_rate") || name.contains("heartpulse")) &&
                    name.contains("sample")
            val isActivitySample = name.contains("activitysample") && name.contains("sample")
            isHeartRateSample || isActivitySample
        }.sorted()

        Log.d(
            TAG,
            "Workout HR window $startMillis..$endMillis tables=${candidateTables.size}"
        )

        for (table in candidateTables) {
            val columns = loadColumns(db, table)
            val deviceCol = findColumn(columns, listOf("deviceId", "device_id")) ?: continue
            val timestampCol = findColumn(columns, listOf("timestamp")) ?: continue
            val hrCol = findColumn(
                columns,
                listOf("heartRate", "heart_rate", "pulse", "heartPulse", "heartrate")
            ) ?: continue

            val (rangeStart, rangeEnd) = adjustWindowForTimestamp(
                db,
                table,
                deviceCol,
                timestampCol,
                deviceId,
                startMillis,
                endMillis
            )
            val cursor = db.rawQuery(
                "SELECT AVG($hrCol), MIN($hrCol), MAX($hrCol) FROM $table " +
                    "WHERE $deviceCol = ? AND $timestampCol BETWEEN ? AND ?",
                arrayOf(deviceId.toString(), rangeStart.toString(), rangeEnd.toString())
            )
            cursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    Log.d(TAG, "Found workout HRs from $table")
                    return WorkoutHeartRates(
                        avg = it.getDouble(0).toInt(),
                        min = if (!it.isNull(1)) it.getInt(1) else null,
                        max = if (!it.isNull(2)) it.getInt(2) else null
                    )
                }
            }
        }
        Log.d(TAG, "No workout HRs found")
        return null
    }

    private fun parseHuamiRawSummary(raw: ByteArray?): RawSummaryValues? {
        if (raw == null || raw.isEmpty()) {
            return null
        }
        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val version = readUnsignedShort(buffer) ?: return null
        // raw activity kind (unused for now)
        readUnsignedShort(buffer) ?: return null
        if (!skipBytes(buffer, 8 + 12)) {
            return null
        }

        return if (version >= 512) {
            var minHr: Int? = null
            if (version == 519) {
                if (!skipBytes(buffer, 1)) return null
                minHr = readUnsignedShort(buffer)
                if (buffer.capacity() >= 0x8c) {
                    buffer.position(0x8c)
                } else {
                    return null
                }
            } else if (version == 516) {
                if (!skipBytes(buffer, 4)) return null
            }

            readInt(buffer) ?: return null // steps
            val activeSeconds = readInt(buffer) ?: return null
            if (!skipBytes(buffer, 16)) return null // lat/long bounds

            val calories = readFloat(buffer) ?: return null
            val distance = readFloat(buffer) ?: return null

            if (!skipBytes(buffer, 20)) return null // ascent/descent/altitudes
            if (!skipBytes(buffer, 12)) return null // speeds
            if (!skipBytes(buffer, 12)) return null // pace
            if (!skipBytes(buffer, 12)) return null // cadence
            if (!skipBytes(buffer, 12)) return null // stride

            readFloat(buffer) ?: return null // distance2
            if (!skipBytes(buffer, 4)) return null // unknown
            val avgHr = readUnsignedShort(buffer)
            if (!skipBytes(buffer, 4)) return null // pace + stride
            val maxHr = readUnsignedShort(buffer)

            RawSummaryValues(
                activeSeconds = activeSeconds,
                distanceMeters = distance.toDouble(),
                calories = calories.toDouble(),
                averageHr = avgHr,
                minHr = minHr,
                maxHr = maxHr
            )
        } else {
            val distance = readFloat(buffer) ?: return null
            if (!skipBytes(buffer, 4 * 4)) return null // ascent, descent, altitudes
            if (!skipBytes(buffer, 16)) return null // lat/long bounds
            readInt(buffer) ?: return null // steps
            val activeSeconds = readInt(buffer) ?: return null
            val calories = readFloat(buffer) ?: return null

            if (!skipBytes(buffer, 4 * 4)) return null // speed/pace/stride
            if (!skipBytes(buffer, 4)) return null // unknown
            if (!skipBytes(buffer, 28)) return null // swimming or other details
            val avgHr = readUnsignedShort(buffer)

            RawSummaryValues(
                activeSeconds = activeSeconds,
                distanceMeters = distance.toDouble(),
                calories = calories.toDouble(),
                averageHr = avgHr,
                minHr = null,
                maxHr = null
            )
        }
    }

    private fun adjustWindowForTimestamp(
        db: SQLiteDatabase,
        table: String,
        deviceCol: String,
        timestampCol: String,
        deviceId: Long,
        startMillis: Long,
        endMillis: Long
    ): Pair<Long, Long> {
        val cursor = db.rawQuery(
            "SELECT MAX($timestampCol) FROM $table WHERE $deviceCol = ?",
            arrayOf(deviceId.toString())
        )
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                val maxTs = it.getLong(0)
                val isSeconds = maxTs in 1..9_999_999_999L
                return if (isSeconds) {
                    Pair(startMillis / 1000L, endMillis / 1000L)
                } else {
                    Pair(startMillis, endMillis)
                }
            }
        }
        return Pair(startMillis, endMillis)
    }

    private fun loadActivitySamples(
        db: SQLiteDatabase,
        table: String,
        deviceCol: String,
        timestampCol: String,
        kindCol: String,
        hrCol: String,
        deviceId: Long,
        rangeStart: Long,
        rangeEnd: Long,
        useMiBandPostProcess: Boolean,
        initialLastKind: Int
    ): List<ActivitySampleRow> {
        val samples = mutableListOf<ActivitySampleRow>()
        val cursor = db.rawQuery(
            "SELECT $timestampCol, $kindCol, $hrCol FROM $table " +
                "WHERE $deviceCol = ? AND $timestampCol BETWEEN ? AND ? " +
                "ORDER BY $timestampCol ASC",
            arrayOf(deviceId.toString(), rangeStart.toString(), rangeEnd.toString())
        )
        cursor.use {
            var lastKind = initialLastKind
            while (it.moveToNext()) {
                val timestampValue = it.getLong(0)
                val rawKind = it.getInt(1)
                val heartRate = it.getInt(2)
                val normalizedKind = if (useMiBandPostProcess) {
                    val normalized = normalizeMiBandRawKind(rawKind, lastKind)
                    lastKind = normalized.lastValidKind
                    normalized.rawKind
                } else {
                    rawKind
                }
                val sleepKind = mapHuamiRawKindToSleepKind(normalizedKind)
                val timestampSec = if (timestampValue > 9_999_999_999L) {
                    timestampValue / 1000L
                } else {
                    timestampValue
                }
                samples.add(ActivitySampleRow(timestampSec, sleepKind, heartRate))
            }
        }
        return samples
    }

    private fun computeSleepAverageHr(samples: List<ActivitySampleRow>): Int? {
        if (samples.isEmpty()) {
            return null
        }

        val sessions = computeSleepSessions(samples)
        val range = if (sessions.isNotEmpty()) {
            val first = sessions.first()
            val last = sessions.last()
            Pair(first.startSec, last.endSec)
        } else {
            null
        }

        var sum = 0L
        var count = 0L
        for (sample in samples) {
            if (sample.sleepKind == SLEEP_KIND_NONE) {
                continue
            }
            if (sample.heartRate < HR_MIN_VALID || sample.heartRate > HR_MAX_VALID) {
                continue
            }
            if (range != null) {
                if (sample.timestampSec < range.first || sample.timestampSec > range.second) {
                    continue
                }
            }
            sum += sample.heartRate.toLong()
            count++
        }

        if (count == 0L) {
            return null
        }
        return kotlin.math.round(sum.toDouble() / count.toDouble()).toInt()
    }

    private fun computeSleepSessions(samples: List<ActivitySampleRow>): List<SleepSessionRow> {
        val sessions = mutableListOf<SleepSessionRow>()
        var previous: ActivitySampleRow? = null
        var sleepStart: Long? = null
        var sleepEnd: Long? = null
        var light = 0L
        var deep = 0L
        var rem = 0L
        var awake = 0L
        var durationSinceLastSleep = 0L

        fun finalizeSessionIfValid() {
            if (sleepStart == null || sleepEnd == null) {
                return
            }
            val duration = light + deep + rem + awake
            if (sleepEnd!! - sleepStart!! > MIN_SLEEP_SESSION_SECONDS &&
                duration > MIN_SLEEP_SESSION_SECONDS
            ) {
                sessions.add(SleepSessionRow(sleepStart!!, sleepEnd!!))
            }
        }

        for (sample in samples) {
            if (sample.sleepKind != SLEEP_KIND_NONE) {
                if (sleepStart == null) {
                    sleepStart = sample.timestampSec
                }
                sleepEnd = sample.timestampSec
                durationSinceLastSleep = 0
            } else {
                finalizeSessionIfValid()
                sleepStart = null
                sleepEnd = null
                light = 0
                deep = 0
                rem = 0
                awake = 0
            }

            if (previous != null) {
                val delta = sample.timestampSec - previous.timestampSec
                when (sample.sleepKind) {
                    SLEEP_KIND_LIGHT -> light += delta
                    SLEEP_KIND_DEEP -> deep += delta
                    SLEEP_KIND_REM -> rem += delta
                    SLEEP_KIND_AWAKE -> awake += delta
                    else -> {
                        durationSinceLastSleep += delta
                        if (sleepStart != null && durationSinceLastSleep > MAX_WAKE_PHASE_SECONDS) {
                            finalizeSessionIfValid()
                            sleepStart = null
                            sleepEnd = null
                            light = 0
                            deep = 0
                            rem = 0
                            awake = 0
                        }
                    }
                }
            }
            previous = sample
        }

        if (sleepStart != null && sleepEnd != null) {
            val duration = light + deep + rem + awake
            if (duration > MIN_SLEEP_SESSION_SECONDS) {
                sessions.add(SleepSessionRow(sleepStart, sleepEnd))
            }
        }
        return sessions
    }

    private fun mapHuamiRawKindToSleepKind(rawKind: Int): Int {
        return when (rawKind) {
            HUAMI_TYPE_LIGHT_SLEEP -> SLEEP_KIND_LIGHT
            HUAMI_TYPE_DEEP_SLEEP -> SLEEP_KIND_DEEP
            HUAMI_EXT_TYPE_SLEEP -> SLEEP_KIND_LIGHT
            HUAMI_EXT_TYPE_DEEP_SLEEP -> SLEEP_KIND_DEEP
            HUAMI_EXT_TYPE_REM_SLEEP -> SLEEP_KIND_REM
            HUAMI_EXT_TYPE_AWAKE_SLEEP -> SLEEP_KIND_AWAKE
            else -> SLEEP_KIND_NONE
        }
    }

    private fun findLastValidMiBandKind(
        db: SQLiteDatabase,
        table: String,
        deviceCol: String,
        timestampCol: String,
        kindCol: String,
        deviceId: Long,
        rangeStart: Long
    ): Int {
        val cursor = db.rawQuery(
            "SELECT $kindCol FROM $table WHERE $deviceCol = ? AND $timestampCol < ? " +
                "AND $kindCol NOT IN (?, ?, ?, ?, ?, ?, ?) " +
                "ORDER BY $timestampCol DESC LIMIT 1",
            arrayOf(
                deviceId.toString(),
                rangeStart.toString(),
                HUAMI_TYPE_NO_CHANGE.toString(),
                HUAMI_TYPE_IGNORE.toString(),
                HUAMI_TYPE_UNSET.toString(),
                MI_BAND_TYPE_EXCLUDE_1.toString(),
                MI_BAND_TYPE_EXCLUDE_2.toString(),
                MI_BAND_TYPE_EXCLUDE_3.toString(),
                MI_BAND_TYPE_EXCLUDE_4.toString()
            )
        )
        cursor.use {
            if (it.moveToFirst()) {
                val rawKind = it.getInt(0)
                return rawKind and MI_BAND_RAW_KIND_MASK
            }
        }
        return HUAMI_TYPE_UNSET
    }

    private data class MiBandKindResult(
        val rawKind: Int,
        val lastValidKind: Int
    )

    private fun normalizeMiBandRawKind(rawKind: Int, lastValidKind: Int): MiBandKindResult {
        var normalized = rawKind
        var lastKind = lastValidKind
        if (normalized != HUAMI_TYPE_UNSET) {
            normalized = normalized and MI_BAND_RAW_KIND_MASK
        }
        if (normalized == HUAMI_TYPE_IGNORE || normalized == HUAMI_TYPE_NO_CHANGE) {
            if (lastKind != HUAMI_TYPE_UNSET) {
                normalized = lastKind
            }
        } else {
            lastKind = normalized
        }
        return MiBandKindResult(normalized, lastKind)
    }

    private fun computeSleepWindow(workoutStartMillis: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(workoutStartMillis), zone)
        val noon = startTime.toLocalDate().atTime(12, 0)
        val windowStart = if (startTime.isBefore(noon)) {
            noon.minusDays(1)
        } else {
            noon
        }
        val windowEnd = windowStart.plusDays(1)
        return Pair(
            windowStart.atZone(zone).toInstant().toEpochMilli(),
            windowEnd.atZone(zone).toInstant().toEpochMilli()
        )
    }

    private fun getDouble(json: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            if (json.has(key) && !json.isNull(key)) {
                return json.optDouble(key)
            }
        }
        return null
    }

    private fun normalize(value: String): String = value.replace("_", "").lowercase(Locale.US)

    private data class SummaryRow(
        val startMillis: Long,
        val endMillis: Long,
        val summaryData: String?,
        val rawSummaryData: ByteArray?
    )

    private data class ActivitySampleRow(
        val timestampSec: Long,
        val sleepKind: Int,
        val heartRate: Int
    )

    private data class SleepSessionRow(
        val startSec: Long,
        val endSec: Long
    )

    private data class WorkoutHeartRates(
        val avg: Int?,
        val min: Int?,
        val max: Int?
    )

    private data class RawSummaryValues(
        val activeSeconds: Int?,
        val distanceMeters: Double?,
        val calories: Double?,
        val averageHr: Int?,
        val minHr: Int?,
        val maxHr: Int?
    )

    private fun readUnsignedShort(buffer: ByteBuffer): Int? {
        if (buffer.remaining() < 2) return null
        return buffer.short.toInt() and 0xFFFF
    }

    private fun readInt(buffer: ByteBuffer): Int? {
        if (buffer.remaining() < 4) return null
        return buffer.int
    }

    private fun readFloat(buffer: ByteBuffer): Float? {
        if (buffer.remaining() < 4) return null
        return buffer.float
    }

    private fun skipBytes(buffer: ByteBuffer, count: Int): Boolean {
        if (buffer.remaining() < count) return false
        buffer.position(buffer.position() + count)
        return true
    }

    private fun readUnsignedByte(data: ByteArray, offset: Int): Int? {
        if (offset < 0 || offset >= data.size) return null
        return data[offset].toInt() and 0xFF
    }
}
