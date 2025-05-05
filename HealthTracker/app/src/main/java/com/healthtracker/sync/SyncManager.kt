package com.healthtracker.sync

import android.content.Context
import androidx.work.*
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de synchronisation entre la base de données locale SQLite et la base de données distante MariaDB
 */
@Singleton
class SyncManager @Inject constructor(private val context: Context) {
    
    companion object {
        const val SYNC_WORK_NAME = "health_data_sync"
        private const val SERVER_URL = "jdbc:mariadb://192.168.0.103:3306/healthtracker"
        private const val USERNAME = "healthuser"
        private const val PASSWORD = "healthpassword"
        
        // Clé pour les préférences partagées
        private const val PREF_NAME = "sync_preferences"
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
    }
    
    /**
     * Planifie une synchronisation périodique des données
     */
    fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,  // Synchroniser toutes les 15 minutes
            5, TimeUnit.MINUTES    // Flexibilité de 5 minutes
        )
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Garder le travail existant s'il y en a un
            syncRequest
        )
    }
    
    /**
     * Déclenche une synchronisation immédiate
     */
    fun syncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
    
    /**
     * Sauvegarde le timestamp de la dernière synchronisation
     */
    fun saveLastSyncTimestamp(timestamp: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_SYNC_KEY, timestamp)
            .apply()
    }
    
    /**
     * Récupère le timestamp de la dernière synchronisation
     */
    fun getLastSyncTimestamp(): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_SYNC_KEY, 0)
    }
    
    /**
     * Convertit un LocalDateTime en Timestamp pour SQL
     */
    private fun LocalDateTime.toSqlTimestamp(): Timestamp {
        return Timestamp.valueOf(this.toString())
    }
    
    /**
     * Convertit un Timestamp SQL en LocalDateTime
     */
    private fun Timestamp.toLocalDateTime(): LocalDateTime {
        return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    
    /**
     * Worker pour effectuer la synchronisation en arrière-plan
     */
    class SyncWorker(appContext: Context, workerParams: WorkerParameters) : 
        CoroutineWorker(appContext, workerParams) {
        
        private val syncManager = SyncManager(applicationContext)
        
        override suspend fun doWork(): Result {
            return withContext(Dispatchers.IO) {
                try {
                    // Obtenir la base de données locale
                    val database = HealthDatabase.getDatabase(applicationContext)
                    
                    // Établir une connexion à MariaDB
                    Class.forName("org.mariadb.jdbc.Driver")
                    val connection = DriverManager.getConnection(
                        SERVER_URL, 
                        USERNAME, 
                        PASSWORD
                    )
                    
                    // Synchroniser les données
                    synchronizeData(connection, database)
                    
                    // Mettre à jour le timestamp de dernière synchronisation
                    syncManager.saveLastSyncTimestamp(System.currentTimeMillis())
                    
                    // Fermer la connexion
                    connection.close()
                    
                    Result.success()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.retry()
                }
            }
        }
        
        /**
         * Synchronise les données entre la base de données locale et le serveur
         */
        private suspend fun synchronizeData(connection: Connection, database: HealthDatabase) {
            // 1. Envoyer les entrées non synchronisées au serveur
            uploadUnsyncedEntries(connection, database)
            
            // 2. Récupérer les nouvelles entrées du serveur
            downloadNewEntries(connection, database)
        }
        
        /**
         * Envoie les entrées non synchronisées au serveur
         */
        private suspend fun uploadUnsyncedEntries(connection: Connection, database: HealthDatabase) {
            // Récupérer les entrées non synchronisées
            val unsyncedEntries = database.healthEntryDao().getUnsyncedEntries()
            
            if (unsyncedEntries.isEmpty()) return
            
            // Préparer la requête d'insertion
            val insertStatement = connection.prepareStatement("""
                INSERT INTO health_entries (user_id, timestamp, weight, waist_measurement, body_fat, notes, client_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                weight = VALUES(weight),
                waist_measurement = VALUES(waist_measurement),
                body_fat = VALUES(body_fat),
                notes = VALUES(notes)
            """, PreparedStatement.RETURN_GENERATED_KEYS)
            
            // Insérer chaque entrée
            for (entry in unsyncedEntries) {
                insertStatement.setLong(1, entry.userId)
                insertStatement.setTimestamp(2, Timestamp.valueOf(entry.timestamp.toString()))
                entry.weight?.let { insertStatement.setFloat(3, it) } ?: insertStatement.setNull(3, java.sql.Types.FLOAT)
                entry.waistMeasurement?.let { insertStatement.setFloat(4, it) } ?: insertStatement.setNull(4, java.sql.Types.FLOAT)
                entry.bodyFat?.let { insertStatement.setFloat(5, it) } ?: insertStatement.setNull(5, java.sql.Types.FLOAT)
                entry.notes?.let { insertStatement.setString(6, it) } ?: insertStatement.setNull(6, java.sql.Types.VARCHAR)
                insertStatement.setLong(7, entry.id)
                
                insertStatement.addBatch()
            }
            
            // Exécuter le batch
            insertStatement.executeBatch()
            
            // Récupérer les IDs générés
            val generatedKeys = insertStatement.generatedKeys
            val entryIds = unsyncedEntries.map { it.id }
            
            // Mettre à jour les entrées locales avec les IDs du serveur
            var i = 0
            while (generatedKeys.next() && i < entryIds.size) {
                val serverEntryId = generatedKeys.getLong(1)
                database.healthEntryDao().markAsSynced(entryIds[i], serverEntryId)
                i++
            }
            
            insertStatement.close()
        }
        
        /**
         * Télécharge les nouvelles entrées du serveur
         */
        private suspend fun downloadNewEntries(connection: Connection, database: HealthDatabase) {
            // Récupérer le timestamp de la dernière synchronisation
            val lastSyncTimestamp = syncManager.getLastSyncTimestamp()
            val lastSyncDateTime = if (lastSyncTimestamp > 0) {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(lastSyncTimestamp),
                    ZoneId.systemDefault()
                )
            } else {
                LocalDateTime.of(2000, 1, 1, 0, 0) // Date par défaut si jamais synchronisé
            }
            
            // Convertir en String pour la requête SQL
            val lastSyncDateTimeString = lastSyncDateTime.toString()
            
            // Préparer la requête pour récupérer les nouvelles entrées
            val selectStatement = connection.prepareStatement("""
                SELECT id, user_id, timestamp, weight, waist_measurement, body_fat, notes, client_id
                FROM health_entries
                WHERE last_modified > ? AND (client_id IS NULL OR client_id NOT IN 
                    (SELECT id FROM health_entries WHERE synced = 1))
            """)
            
            selectStatement.setTimestamp(1, Timestamp.valueOf(lastSyncDateTime.toString()))
            
            // Exécuter la requête
            val resultSet = selectStatement.executeQuery()
            
            // Convertir les résultats en objets HealthEntry
            val newEntries = mutableListOf<HealthEntry>()
            
            while (resultSet.next()) {
                newEntries.add(createHealthEntryFromResultSet(resultSet))
            }
            
            // Insérer les nouvelles entrées dans la base de données locale
            if (newEntries.isNotEmpty()) {
                database.healthEntryDao().insertOrUpdateEntries(newEntries)
            }
            
            selectStatement.close()
        }
        
        /**
         * Crée un objet HealthEntry à partir d'un ResultSet
         */
        private fun createHealthEntryFromResultSet(resultSet: ResultSet): HealthEntry {
            val serverEntryId = resultSet.getLong("id")
            val userId = resultSet.getLong("user_id")
            val timestamp = LocalDateTime.ofInstant(
                resultSet.getTimestamp("timestamp").toInstant(),
                ZoneId.systemDefault()
            )
            
            val weight = if (resultSet.getObject("weight") != null) resultSet.getFloat("weight") else null
            val waistMeasurement = if (resultSet.getObject("waist_measurement") != null) resultSet.getFloat("waist_measurement") else null
            val bodyFat = if (resultSet.getObject("body_fat") != null) resultSet.getFloat("body_fat") else null
            val notes = resultSet.getString("notes")
            
            // Récupérer l'ID client s'il existe
            val clientId = if (!resultSet.getObject("client_id").equals(null)) {
                resultSet.getLong("client_id")
            } else {
                0L // Nouvel ID local sera généré
            }
            
            return HealthEntry(
                id = clientId,
                userId = userId,
                timestamp = timestamp,
                weight = weight,
                waistMeasurement = waistMeasurement,
                bodyFat = bodyFat,
                notes = notes,
                synced = true,
                serverEntryId = serverEntryId
            )
        }
    }
}
