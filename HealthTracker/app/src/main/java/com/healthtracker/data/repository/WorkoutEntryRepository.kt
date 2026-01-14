package com.healthtracker.data.repository

import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.WorkoutEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutEntryRepository @Inject constructor(
    private val database: HealthDatabase
) {
    fun getAllEntries(): Flow<List<WorkoutEntry>> {
        return database.workoutEntryDao().getAllEntries()
    }

    suspend fun insertEntry(entry: WorkoutEntry): Long {
        return database.workoutEntryDao().insertEntry(entry)
    }

    suspend fun updateEntry(entry: WorkoutEntry) {
        database.workoutEntryDao().updateEntry(entry)
    }

    suspend fun deleteEntry(entry: WorkoutEntry) {
        if (entry.id > 0) {
            database.workoutEntryDao().markEntryAsDeleted(entry.id)
        }
    }
}
