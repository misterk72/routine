package com.healthtracker.data.repository

import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.HealthEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for managing HealthEntry objects in the database.
 * This class serves as a single source of truth for accessing health entry data.
 */
@Singleton
class HealthEntryRepository @Inject constructor(
    private val database: HealthDatabase
) {
    /**
     * Get all health entries as a Flow
     * @return Flow of List of HealthEntry objects
     */
    fun getAllEntries(): Flow<List<HealthEntry>> {
        return database.healthEntryDao().getAllEntries()
    }
    
    /**
     * Get a specific health entry by ID
     * @param id The ID of the entry to retrieve
     * @return The HealthEntry object wrapped in a Flow
     */
    fun getEntryById(id: Long): Flow<HealthEntry?> {
        return database.healthEntryDao().getEntryById(id)
    }
    
    /**
     * Insert a new health entry
     * @param entry The HealthEntry to insert
     * @return The ID of the inserted entry
     */
    suspend fun insertEntry(entry: HealthEntry): Long {
        return database.healthEntryDao().insertEntry(entry)
    }
    
    /**
     * Update an existing health entry
     * @param entry The HealthEntry to update
     */
    suspend fun updateEntry(entry: HealthEntry) {
        database.healthEntryDao().updateEntry(entry)
    }
    
    /**
     * Delete a health entry
     * @param entry The HealthEntry to delete
     */
    suspend fun deleteEntry(entry: HealthEntry) {
        database.healthEntryDao().deleteEntry(entry)
    }
    
    /**
     * Get the most recent health entry
     * @return The most recent HealthEntry wrapped in a Flow
     */
    fun getMostRecentEntry(): Flow<HealthEntry?> {
        return database.healthEntryDao().getMostRecentEntry()
    }
    
    /**
     * Get the count of health entries in the database
     * @return The number of entries
     */
    suspend fun getEntryCount(): Int {
        return database.healthEntryDao().getEntryCount()
    }
}
