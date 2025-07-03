package com.healthtracker.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.HealthEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        const val SYNC_WORK_NAME = "com.healthtracker.sync.SyncWorker"
        private const val PREFS_NAME = "sync_prefs"
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
        private val API_URL = com.healthtracker.BuildConfig.API_BASE_URL + "sync.php"
        private const val TAG = "HT_SYNC_MANAGER"
    }

    private val httpClient = OkHttpClient()
    private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()

    fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun syncNow(): UUID {
        Log.d(TAG, "Déclenchement d'une synchronisation immédiate")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueue(syncRequest)
        return syncRequest.id
    }

    fun saveLastSyncTimestamp(timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_SYNC_KEY, timestamp)
            .apply()
    }

    fun getLastSyncTimestamp(): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_SYNC_KEY, 0)
    }

    suspend fun inspectServerDataForDuplicates_FIXED() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "--- DÉBUT DE L'INSPECTION DES DONNÉES SERVEUR (FIXED) ---")

            val url = API_URL
            val request = Request.Builder().url(url).get().build()
            val serverEntriesMap: List<Map<String, Any?>> = try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Erreur lors de la récupération de toutes les données: ${response.code}")
                        return@withContext
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.d(TAG, "Réponse vide du serveur.")
                        return@withContext
                    }
                    val serverData = gson.fromJson<Map<String, List<Map<String, Any?>>>>(responseBody, object : TypeToken<Map<String, List<Map<String, Any?>>>>() {}.type)
                    serverData["entries"] ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors de la récupération de toutes les données: ${e.message}", e)
                return@withContext
            }

            if (serverEntriesMap.isEmpty()) {
                Log.d(TAG, "Aucune entrée reçue du serveur pour l'inspection.")
                Log.d(TAG, "--- FIN DE L'INSPECTION ---")
                return@withContext
            }

            Log.d(TAG, "${serverEntriesMap.size} entrées totales reçues du serveur.")

            // Check 1: Group by clientId
            val duplicatesByClientId = serverEntriesMap
                .filter { (it["clientId"] as? Double) != null }
                .groupBy { (it["clientId"] as Double).toLong() }
                .filter { it.value.size > 1 }

            if (duplicatesByClientId.isNotEmpty()) {
                Log.w(TAG, "ATTENTION: Doublons trouvés basés sur le clientId!")
                duplicatesByClientId.forEach { (clientId, entries) ->
                    val serverIds = entries.map { (it["id"] as? Double)?.toLong() }
                    Log.w(TAG, "  ClientId ${clientId} est utilisé par ${entries.size} entrées serveur: ${serverIds}")
                }
            } else {
                Log.d(TAG, "Vérification par clientId: Aucune duplication trouvée.")
            }

            // Check 2: Group by content
            val duplicatesByContent = serverEntriesMap.groupBy {
                val timestamp = it["timestamp"] as? String
                val weight = it["weight"] as? Double
                val waist = it["waistMeasurement"] as? Double
                val bodyFat = it["bodyFat"] as? Double
                val notes = (it["notes"] as? String)?.trim()
                // Using a list for grouping
                listOf(timestamp, weight, waist, bodyFat, notes)
            }.filter { it.value.size > 1 }

            if (duplicatesByContent.isNotEmpty()) {
                Log.w(TAG, "ATTENTION: Doublons trouvés basés sur le contenu identique!")
                duplicatesByContent.forEach { (content, entries) ->
                    val serverIds = entries.map { (it["id"] as? Double)?.toLong() }
                    Log.w(TAG, "  Le contenu suivant est dupliqué ${entries.size} fois: ${content}")
                    Log.w(TAG, "    IDs serveur concernés: ${serverIds}")
                }
            } else {
                Log.d(TAG, "Vérification par contenu: Aucune duplication trouvée.")
            }

            Log.d(TAG, "--- FIN DE L'INSPECTION ---")
        }
    }

    suspend fun performFullResynchronization() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "--- DÉBUT DE LA RESYNCHRONISATION COMPLÈTE ---")
            val database = HealthDatabase.getDatabase(context)

            // Step 1: Upload any pending changes
            Log.d(TAG, "Étape 1: Envoi des modifications non synchronisées...")
            uploadUnsyncedEntries(database)

            // Step 2: Clear local data
            Log.d(TAG, "Étape 2: Suppression des données locales...")
            database.healthEntryDao().deleteAllEntries()
            Log.d(TAG, "Toutes les entrées locales ont été supprimées.")

            // Step 3: Download all data from server
            Log.d(TAG, "Étape 3: Téléchargement de toutes les données du serveur...")
            downloadNewEntries(database, fetchAll = true)

            // Step 4: Update last sync timestamp
            saveLastSyncTimestamp(System.currentTimeMillis())
            Log.d(TAG, "--- FIN DE LA RESYNCHRONISATION COMPLÈTE ---")
        }
    }

    private suspend fun uploadUnsyncedEntries(database: HealthDatabase) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val unsyncedEntries = database.healthEntryDao().getUnsyncedEntries()
        val deletedEntries = database.healthEntryDao().getDeletedUnsyncedEntries()

        if (unsyncedEntries.isEmpty() && deletedEntries.isEmpty()) {
            Log.d(TAG, "Aucune entrée à synchroniser")
            return
        }

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

        val requestBody = entriesJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Erreur lors de l'envoi des données: ${response.code}")
                }
                val responseBody = response.body?.string()
                Log.d(TAG, "DEBUG - Réponse du serveur (upload): $responseBody")

                val idsToMarkSynced = allEntries.map { it.id }
                database.healthEntryDao().markAllAsSynced(idsToMarkSynced)
                Log.d(TAG, "${idsToMarkSynced.size} entrées marquées comme synchronisées")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'envoi des données: ${e.message}", e)
        }
    }

    private suspend fun downloadNewEntries(database: HealthDatabase, fetchAll: Boolean = false) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        try {
            val urlBuilder = API_URL.toHttpUrlOrNull()?.newBuilder() ?: run {
                Log.e(TAG, "URL API invalide: $API_URL")
                return
            }
            if (!fetchAll) {
                val lastSyncTimestamp = getLastSyncTimestamp()
                val timestampInSeconds = lastSyncTimestamp / 1000
                urlBuilder.addQueryParameter("since", timestampInSeconds.toString())
            }
            val url = urlBuilder.build().toString()
            val request = Request.Builder().url(url).get().build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Erreur HTTP: ${response.code}")

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    Log.d(TAG, "Réponse du serveur vide.")
                    return@use
                }

                val responseType = object : TypeToken<Map<String, List<Map<String, Any?>>>>() {}.type
                val jsonResponse = gson.fromJson<Map<String, List<Map<String, Any?>>>>(responseBody, responseType)
                val serverEntriesMap = jsonResponse["entries"] ?: emptyList()

                if (serverEntriesMap.isEmpty()) {
                    Log.d(TAG, "Aucune nouvelle entrée sur le serveur.")
                    return@use
                }

                val newEntriesToInsert = mutableListOf<HealthEntry>()
                val dao = database.healthEntryDao()

                for (entryMap in serverEntriesMap) {
                    val serverId = (entryMap["id"] as? Double)?.toLong()
                    val clientId = (entryMap["clientId"] as? Double)?.toLong()

                    if (serverId == null) {
                        Log.w(TAG, "Entrée serveur ignorée, ID manquant: $entryMap")
                        continue
                    }

                    if (clientId != null && !fetchAll) {
                        val localEntry = dao.getEntryByIdSuspend(clientId)
                        if (localEntry != null && localEntry.serverEntryId == null) {
                            Log.d(TAG, "Correspondance par clientId: id local ${localEntry.id} -> id serveur $serverId")
                            dao.markAsSynced(localEntry.id, serverId)
                            continue
                        }
                    }

                    val existingEntry = dao.getEntriesByServerIds(listOf(serverId))
                    if (existingEntry.isNotEmpty()) {
                        Log.d(TAG, "Entrée avec id serveur $serverId déjà présente localement. Ignorée.")
                        continue
                    }

                    val timestamp = LocalDateTime.parse(entryMap["timestamp"] as String, dateFormatter)
                    val newEntry = HealthEntry(
                        id = 0, // Room will auto-generate
                        serverEntryId = serverId,
                        userId = (entryMap["userId"] as Double).toLong(),
                        timestamp = timestamp,
                        weight = (entryMap["weight"] as? Double)?.toFloat(),
                        waistMeasurement = (entryMap["waistMeasurement"] as? Double)?.toFloat(),
                        bodyFat = (entryMap["bodyFat"] as? Double)?.toFloat(),
                        notes = entryMap["notes"] as? String,
                        synced = true,
                        deleted = (entryMap["deleted"] as? Double)?.toInt() == 1
                    )
                    if (!newEntry.deleted) { // Do not insert entries that are marked as deleted on the server
                        newEntriesToInsert.add(newEntry)
                    }
                }

                if (newEntriesToInsert.isNotEmpty()) {
                    dao.insertOrUpdateEntries(newEntriesToInsert)
                    Log.d(TAG, "${newEntriesToInsert.size} nouvelles entrées insérées depuis le serveur.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du téléchargement des nouvelles données: ${e.message}", e)
        }
    }

    class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

        override suspend fun doWork(): Result {
            return withContext(Dispatchers.IO) {
                try {
                    val database = HealthDatabase.getDatabase(applicationContext)
                    // Hilt does not inject into Workers, so we manually get the SyncManager.
                    // This is a simplification. For a production app, Hilt's Worker support should be configured.
                    val syncManager = SyncManager(applicationContext, WorkManager.getInstance(applicationContext))
                    
                    // Regular sync process
                    syncManager.uploadUnsyncedEntries(database)
                    syncManager.downloadNewEntries(database)
                    
                    syncManager.saveLastSyncTimestamp(System.currentTimeMillis())
                    Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la synchronisation", e)
                    Result.retry()
                }
            }
        }
    }
}
