package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Insert
    suspend fun insertEntry(entry: HealthEntry): Long

    @Update
    suspend fun updateEntry(entry: HealthEntry)

    @Delete
    suspend fun deleteEntry(entry: HealthEntry)

    @Query("SELECT * FROM health_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HealthEntry>>

    @Query("SELECT * FROM health_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<HealthEntry?>
    
    @Query("SELECT * FROM health_entries ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentEntry(): Flow<HealthEntry?>
    
    @Query("SELECT COUNT(*) FROM health_entries")
    suspend fun getEntryCount(): Int
}
