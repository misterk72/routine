package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Insert
    suspend fun insert(entry: HealthEntry)

    @Update
    suspend fun update(entry: HealthEntry)

    @Delete
    suspend fun delete(entry: HealthEntry)

    @Query("SELECT * FROM health_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HealthEntry>>

    @Query("SELECT * FROM health_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): HealthEntry?
}
