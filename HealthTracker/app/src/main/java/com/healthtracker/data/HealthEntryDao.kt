package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Insert
    suspend fun insertEntry(entry: HealthEntry): Long

    /**
     * Met à jour une entrée existante
     * Note: N'utilise pas l'annotation @Update standard car nous devons aussi mettre à jour le flag synced
     */
    @Query("UPDATE health_entries SET timestamp = :timestamp, weight = :weight, waistMeasurement = :waistMeasurement, bodyFat = :bodyFat, notes = :notes, synced = 0 WHERE id = :id")
    suspend fun updateEntry(id: Long, timestamp: String, weight: Float?, waistMeasurement: Float?, bodyFat: Float?, notes: String?)
    
    /**
     * Met à jour une entrée existante et la marque comme non synchronisée
     */
    suspend fun updateEntry(entry: HealthEntry) {
        updateEntry(
            id = entry.id,
            timestamp = entry.timestamp.toString(),
            weight = entry.weight,
            waistMeasurement = entry.waistMeasurement,
            bodyFat = entry.bodyFat,
            notes = entry.notes
        )
    }

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

    @Query("SELECT * FROM health_entries WHERE id = :id AND deleted = 0")
    suspend fun getEntryByIdSuspend(id: Long): HealthEntry?
    
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

    /**
     * Récupère les entrées qui ont été marquées comme synchronisées mais n'ont pas encore de serverEntryId.
     * C'est un état transitoire qui se produit après un téléversement réussi mais avant le téléchargement de confirmation.
     */
    @Query("SELECT * FROM health_entries WHERE synced = 1 AND serverEntryId IS NULL AND deleted = 0")
    suspend fun getSyncedEntriesWithoutServerId(): List<HealthEntry>
    
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

    @Query("DELETE FROM health_entries")
    suspend fun deleteAllEntries()
}
