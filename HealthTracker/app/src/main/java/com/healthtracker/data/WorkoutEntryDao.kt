package com.healthtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutEntryDao {
    @Insert
    suspend fun insertEntry(entry: WorkoutEntry): Long

    @Query(
        "UPDATE workout_entries SET startTime = :startTime, durationMinutes = :durationMinutes, " +
            "distanceKm = :distanceKm, calories = :calories, heartRateAvg = :heartRateAvg, " +
            "heartRateMin = :heartRateMin, heartRateMax = :heartRateMax, " +
            "sleepHeartRateAvg = :sleepHeartRateAvg, vo2Max = :vo2Max, " +
            "program = :program, soundtrack = :soundtrack, " +
            "notes = :notes, synced = 0 WHERE id = :id"
    )
    suspend fun updateEntry(
        id: Long,
        startTime: String,
        durationMinutes: Int?,
        distanceKm: Float?,
        calories: Int?,
        heartRateAvg: Int?,
        heartRateMin: Int?,
        heartRateMax: Int?,
        sleepHeartRateAvg: Int?,
        vo2Max: Float?,
        program: String?,
        soundtrack: String?,
        notes: String?
    )

    suspend fun updateEntry(entry: WorkoutEntry) {
        updateEntry(
            id = entry.id,
            startTime = entry.startTime.toString(),
            durationMinutes = entry.durationMinutes,
            distanceKm = entry.distanceKm,
            calories = entry.calories,
            heartRateAvg = entry.heartRateAvg,
            heartRateMin = entry.heartRateMin,
            heartRateMax = entry.heartRateMax,
            sleepHeartRateAvg = entry.sleepHeartRateAvg,
            vo2Max = entry.vo2Max,
            program = entry.program,
            soundtrack = entry.soundtrack,
            notes = entry.notes
        )
    }

    @Delete
    suspend fun hardDeleteEntry(entry: WorkoutEntry)

    @Query("UPDATE workout_entries SET deleted = 1, synced = 0 WHERE id = :id")
    suspend fun markEntryAsDeleted(id: Long)

    @Query("SELECT * FROM workout_entries WHERE deleted = 0 ORDER BY startTime DESC")
    fun getAllEntries(): Flow<List<WorkoutEntry>>

    @Transaction
    @Query("SELECT * FROM workout_entries WHERE deleted = 0 ORDER BY startTime DESC")
    fun getAllEntriesWithUser(): Flow<List<WorkoutEntryWithUser>>

    @Transaction
    @Query("SELECT * FROM workout_entries WHERE id = :id AND deleted = 0")
    fun getEntryWithUserById(id: Long): Flow<WorkoutEntryWithUser?>

    @Query("SELECT * FROM workout_entries WHERE id = :id AND deleted = 0")
    suspend fun getEntryByIdSuspend(id: Long): WorkoutEntry?

    @Query("SELECT * FROM workout_entries WHERE synced = 0")
    suspend fun getUnsyncedEntries(): List<WorkoutEntry>

    @Query("SELECT * FROM workout_entries WHERE deleted = 1 AND synced = 0")
    suspend fun getDeletedUnsyncedEntries(): List<WorkoutEntry>

    @Query("SELECT * FROM workout_entries WHERE synced = 1 AND serverEntryId IS NULL AND deleted = 0")
    suspend fun getSyncedEntriesWithoutServerId(): List<WorkoutEntry>

    @Query("UPDATE workout_entries SET synced = 1, serverEntryId = :serverEntryId WHERE id = :id")
    suspend fun markAsSynced(id: Long, serverEntryId: Long)

    @Query("UPDATE workout_entries SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAllAsSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEntries(entries: List<WorkoutEntry>)

    @Query("SELECT * FROM workout_entries WHERE startTime > :timestamp")
    suspend fun getEntriesNewerThan(timestamp: String): List<WorkoutEntry>

    @Query("SELECT * FROM workout_entries WHERE serverEntryId IN (:serverIds)")
    suspend fun getEntriesByServerIds(serverIds: List<Long>): List<WorkoutEntry>

    @Query("DELETE FROM workout_entries")
    suspend fun deleteAllEntries()
}
