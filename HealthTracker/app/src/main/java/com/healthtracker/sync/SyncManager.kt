package com.healthtracker.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.converters.DateTimeConverters
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de synchronisation pour synchroniser les données entre la base de données locale
 * et le serveur via HTTP.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        const val SYNC_WORK_NAME = "com.healthtracker.sync.SyncWorker"
        private const val PREFS_NAME = "sync_prefs"
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
        
        // Configuration de la connexion au serveur API
        private const val API_URL = "http://192.168.0.103:5001/sync.php"
        private const val TAG = "SyncManager"
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
            
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Garder le travail existant s'il y en a un
            syncRequest
        )
    }
    
    /**
     * Déclenche une synchronisation immédiate
     * @return WorkInfo.Id pour suivre l'état de la synchronisation
     */
    fun syncNow(): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
            
        workManager.enqueue(syncRequest)
        return syncRequest.id
    }
    
    /**
     * Sauvegarde le timestamp de la dernière synchronisation
     */
    fun saveLastSyncTimestamp(timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_SYNC_KEY, timestamp)
            .apply()
    }
    
    /**
     * Récupère le timestamp de la dernière synchronisation
     */
    fun getLastSyncTimestamp(): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_SYNC_KEY, 0)
    }
    
    /**
     * Worker pour effectuer la synchronisation en arrière-plan
     */
    class SyncWorker(appContext: Context, workerParams: WorkerParameters) : 
        CoroutineWorker(appContext, workerParams) {
        
        private val httpClient = OkHttpClient()
        private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        override suspend fun doWork(): Result {
            return withContext(Dispatchers.IO) {
                try {
                    // Obtenir la base de données locale
                    val database = HealthDatabase.getDatabase(applicationContext)
                    
                    // Synchroniser les données
                    synchronizeData(database)
                    
                    // Mettre à jour le timestamp de dernière synchronisation
                    val syncManager = SyncManager(applicationContext, WorkManager.getInstance(applicationContext))
                    syncManager.saveLastSyncTimestamp(System.currentTimeMillis())
                    
                    Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la synchronisation", e)
                    Result.retry()
                }
            }
        }
        
        /**
         * Synchronise les données entre la base de données locale et le serveur
         */
        private suspend fun synchronizeData(database: HealthDatabase) {
            // 1. Envoyer les entrées non synchronisées au serveur
            uploadUnsyncedEntries(database)
            
            // 2. Récupérer les nouvelles entrées du serveur
            downloadNewEntries(database)
        }
        
        /**
         * Envoie les entrées non synchronisées au serveur
         */
        private suspend fun uploadUnsyncedEntries(database: HealthDatabase) {
            // Récupérer les entrées non synchronisées (modifiées et supprimées)
            val unsyncedEntries = database.healthEntryDao().getUnsyncedEntries()
            val deletedEntries = database.healthEntryDao().getDeletedUnsyncedEntries()
            
            Log.d(TAG, "DEBUG - Entrées non synchronisées: ${unsyncedEntries.size}")
            for (entry in unsyncedEntries) {
                Log.d(TAG, "DEBUG - Entrée non synchronisée: id=${entry.id}, timestamp=${entry.timestamp}, deleted=${entry.deleted}, synced=${entry.synced}")
            }
            
            Log.d(TAG, "DEBUG - Entrées supprimées non synchronisées: ${deletedEntries.size}")
            for (entry in deletedEntries) {
                Log.d(TAG, "DEBUG - Entrée supprimée: id=${entry.id}, timestamp=${entry.timestamp}, deleted=${entry.deleted}, synced=${entry.synced}")
            }
            
            if (unsyncedEntries.isEmpty() && deletedEntries.isEmpty()) {
                Log.d(TAG, "Aucune entrée à synchroniser")
                return
            }
            
            val totalEntries = unsyncedEntries.size + deletedEntries.size
            Log.d(TAG, "Envoi de $totalEntries entrées au serveur (${deletedEntries.size} supprimées)")
            
            // Convertir les entrées en JSON
            // Inclure toutes les entrées (modifiées et supprimées) dans un seul tableau
            val allEntries = unsyncedEntries + deletedEntries
            val entriesJson = gson.toJson(mapOf("entries" to allEntries.map { entry ->
                mapOf(
                    "id" to entry.id,
                    "userId" to entry.userId,
                    "timestamp" to entry.timestamp.format(dateFormatter),
                    "weight" to entry.weight,
                    "waistMeasurement" to entry.waistMeasurement,
                    "bodyFat" to entry.bodyFat,
                    "notes" to entry.notes,
                    "deleted" to entry.deleted
                )
            }))
            
            // Créer la requête HTTP
            val requestBody = entriesJson.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build()
            
            // Exécuter la requête
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Erreur lors de l'envoi des données: ${response.code}")
                }
                
                // Analyser la réponse
                val responseBody = response.body?.string()
                Log.d(TAG, "DEBUG - Réponse du serveur: $responseBody")
                
                val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                
                if (jsonResponse.get("success")?.asBoolean == true) {
                    // Marquer toutes les entrées comme synchronisées
                    val allEntryIds = allEntries.map { it.id }
                    Log.d(TAG, "DEBUG - Marquage de ${allEntryIds.size} entrées comme synchronisées: $allEntryIds")
                    database.healthEntryDao().markAllAsSynced(allEntryIds)
                    
                    // Supprimer physiquement les entrées qui ont été marquées comme supprimées et synchronisées
                    // Cette étape est optionnelle, mais permet de nettoyer la base de données locale
                    if (deletedEntries.isNotEmpty()) {
                        Log.d(TAG, "DEBUG - Suppression physique de ${deletedEntries.size} entrées")
                        for (entry in deletedEntries) {
                            Log.d(TAG, "DEBUG - Suppression physique de l'entrée id=${entry.id}")
                            database.healthEntryDao().hardDeleteEntry(entry)
                        }
                    }
                    
                    Log.d(TAG, "${jsonResponse.get("processed")?.asInt ?: 0} entrées synchronisées avec succès (dont ${deletedEntries.size} supprimées)")
                } else {
                    Log.e(TAG, "Erreur lors de la synchronisation: ${jsonResponse.get("error")?.asString}")
                }
            }
        }
        
        /**
         * Télécharge les nouvelles entrées du serveur
         */
        private suspend fun downloadNewEntries(database: HealthDatabase) {
        // Récupérer le timestamp de la dernière synchronisation
        val syncManager = SyncManager(applicationContext, WorkManager.getInstance(applicationContext))
        val lastSyncTimestamp = syncManager.getLastSyncTimestamp()
        
        Log.d(TAG, "DEBUG - Récupération des entrées depuis $lastSyncTimestamp")
        Log.d(TAG, "DEBUG - URL API: $API_URL")
            
            // Créer la requête HTTP
            val request = Request.Builder()
                .url("${API_URL}?since=$lastSyncTimestamp")
                .get()
                .build()
            
            // Exécuter la requête
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Erreur lors de la récupération des données: ${response.code}")
                }
                
                // Analyser la réponse
                val responseBody = response.body?.string()
                val responseType = object : TypeToken<Map<String, List<Map<String, Any?>>>>() {}.type
                val jsonResponse = gson.fromJson<Map<String, List<Map<String, Any?>>>>(responseBody, responseType)
                
                val entries = jsonResponse["entries"] ?: emptyList()
                
                if (entries.isNotEmpty()) {
                    Log.d(TAG, "${entries.size} nouvelles entrées récupérées du serveur")
                    
                    // Filtrer les entrées qui ont déjà un serverEntryId correspondant
                    val serverIds = entries.mapNotNull { (it["id"] as? Double)?.toLong() }
                    val existingEntries = database.healthEntryDao().getEntriesByServerIds(serverIds)
                    val existingServerIds = existingEntries.mapNotNull { it.serverEntryId }.toSet()
                    
                    // Ne conserver que les entrées qui n'existent pas déjà localement
                    val newServerEntries = entries.filter { entryMap -> 
                        val serverId = (entryMap["id"] as? Double)?.toLong()
                        serverId == null || serverId !in existingServerIds
                    }
                    
                    if (newServerEntries.isNotEmpty()) {
                        Log.d(TAG, "${newServerEntries.size} nouvelles entrées à ajouter localement")
                        
                        // Convertir les entrées JSON en objets HealthEntry
                        val newEntries = newServerEntries.map { entryMap ->
                            createHealthEntryFromMap(entryMap)
                        }
                        
                        // Insérer les nouvelles entrées dans la base de données locale
                        database.healthEntryDao().insertOrUpdateEntries(newEntries)
                    } else {
                        Log.d(TAG, "Toutes les entrées du serveur existent déjà localement")
                    }
                } else {
                    Log.d(TAG, "Aucune nouvelle entrée sur le serveur")
                }
            }
        }
        
        /**
         * Crée un objet HealthEntry à partir d'une Map
         */
        private fun createHealthEntryFromMap(entryMap: Map<String, Any?>): HealthEntry {
            val id = (entryMap["id"] as? Double)?.toLong() ?: 0L
            val userId = (entryMap["userId"] as? Double)?.toLong() ?: 1L
            
            // Convertir la chaîne de date en LocalDateTime
            val timestampStr = entryMap["timestamp"] as String
            val timestamp = LocalDateTime.parse(timestampStr, dateFormatter)
            
            val weight = (entryMap["weight"] as? Double)?.toFloat()
            val waistMeasurement = (entryMap["waistMeasurement"] as? Double)?.toFloat()
            val bodyFat = (entryMap["bodyFat"] as? Double)?.toFloat()
            val notes = entryMap["notes"] as? String
            
            return HealthEntry(
                id = 0, // ID local généré automatiquement
                userId = userId,
                timestamp = timestamp,
                weight = weight,
                waistMeasurement = waistMeasurement,
                bodyFat = bodyFat,
                notes = notes,
                synced = true,
                serverEntryId = id
            )
        }
    }
}
