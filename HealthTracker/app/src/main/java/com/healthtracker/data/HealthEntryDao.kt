package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Insert
    suspend fun insertEntry(entry: HealthEntry): Long

    @Update
    suspend fun updateEntry(entry: HealthEntry)

    /**
     * Suppression physique d'une entrée (utilisé uniquement en interne)
     */
    @Delete
    suspend fun hardDeleteEntry(entry: HealthEntry)
    
    /**
     * Marque une entrée comme supprimée (soft delete)
     */
    @Query("UPDATE health_entries SET deleted = 1, synced = 0 WHERE id = :id")
    suspend fun markEntryAsDeleted(id: Long)

    @Query("SELECT * FROM health_entries WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HealthEntry>>

    @Query("SELECT * FROM health_entries WHERE id = :id AND deleted = 0")
    fun getEntryById(id: Long): Flow<HealthEntry?>
    
    @Query("SELECT * FROM health_entries WHERE deleted = 0 ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentEntry(): Flow<HealthEntry?>
    
    @Query("SELECT COUNT(*) FROM health_entries WHERE deleted = 0")
    suspend fun getEntryCount(): Int
    
    @Transaction
    @Query("SELECT * FROM health_entries WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllEntriesWithUser(): Flow<List<HealthEntryWithUser>>
    
    @Transaction
    @Query("SELECT * FROM health_entries WHERE id = :id AND deleted = 0")
    fun getEntryWithUserById(id: Long): Flow<HealthEntryWithUser?>
    
    // Méthodes pour la synchronisation
    @Query("SELECT * FROM health_entries WHERE synced = 0")
    suspend fun getUnsyncedEntries(): List<HealthEntry>
    
    /**
     * Récupère les entrées marquées comme supprimées mais non encore synchronisées
     */
    @Query("SELECT * FROM health_entries WHERE deleted = 1 AND synced = 0")
    suspend fun getDeletedUnsyncedEntries(): List<HealthEntry>
    
    @Query("UPDATE health_entries SET synced = 1, serverEntryId = :serverEntryId WHERE id = :id")
    suspend fun markAsSynced(id: Long, serverEntryId: Long)
    
    @Query("UPDATE health_entries SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAllAsSynced(ids: List<Long>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEntries(entries: List<HealthEntry>)
    
    @Query("SELECT * FROM health_entries WHERE timestamp > :timestamp")
    suspend fun getEntriesNewerThan(timestamp: String): List<HealthEntry>
    
    @Query("SELECT * FROM health_entries WHERE serverEntryId IN (:serverIds)")
    suspend fun getEntriesByServerIds(serverIds: List<Long>): List<HealthEntry>
}
