package com.healthtracker.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.User
import com.healthtracker.data.WorkoutEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
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
            uploadUnsyncedWorkouts(database)

            // Step 2: Clear local data
            Log.d(TAG, "Étape 2: Suppression des données locales...")
            database.healthEntryDao().deleteAllEntries()
            database.workoutEntryDao().deleteAllEntries()
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
    }

    private suspend fun uploadUnsyncedWorkouts(database: HealthDatabase) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val unsyncedEntries = database.workoutEntryDao().getUnsyncedEntries()
        val deletedEntries = database.workoutEntryDao().getDeletedUnsyncedEntries()

        if (unsyncedEntries.isEmpty() && deletedEntries.isEmpty()) {
            Log.d(TAG, "Aucune séance à synchroniser")
            return
        }

        val allEntries = unsyncedEntries + deletedEntries
        val workoutsJson = gson.toJson(mapOf("workouts" to allEntries.map { entry ->
            mapOf(
                "id" to entry.id,
                "userId" to entry.userId,
                "startTime" to entry.startTime.format(dateFormatter),
                "durationMinutes" to entry.durationMinutes,
                "distanceKm" to entry.distanceKm,
                "calories" to entry.calories,
                "program" to entry.program,
                "notes" to entry.notes,
                "deleted" to entry.deleted
            )
        }))

        val requestBody = workoutsJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Erreur lors de l'envoi des séances: ${response.code}")
            }
            val responseBody = response.body?.string()
            Log.d(TAG, "DEBUG - Réponse du serveur (workouts upload): $responseBody")

            val idsToMarkSynced = allEntries.map { it.id }
            database.workoutEntryDao().markAllAsSynced(idsToMarkSynced)
            Log.d(TAG, "${idsToMarkSynced.size} séances marquées comme synchronisées")
        }
    }

    private suspend fun downloadNewEntries(database: HealthDatabase, fetchAll: Boolean = false) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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

            // Étape 1: Synchroniser les utilisateurs
            val serverUsersMap = jsonResponse["users"]
            if (serverUsersMap != null) {
                val usersToInsert = serverUsersMap.mapNotNull { userMap ->
                    val userId = (userMap["id"] as? Double)?.toLong()
                    val userName = userMap["name"] as? String
                    if (userId != null && userName != null) {
                        User(id = userId, name = userName)
                    } else {
                        Log.w(TAG, "Utilisateur serveur ignoré, données invalides: $userMap")
                        null
                    }
                }
                if (usersToInsert.isNotEmpty()) {
                    database.userDao().insertOrUpdateUsers(usersToInsert)
                    Log.d(TAG, "${usersToInsert.size} utilisateurs insérés/mis à jour depuis le serveur.")
                }
            }

            // Étape 2: Gérer les utilisateurs manquants de manière plus intelligente
            val serverEntriesMap = jsonResponse["entries"] ?: emptyList()
            val userDao = database.userDao()
            val existingUsers = userDao.getAllUsers().first()
            val existingUserIds = existingUsers.map { it.id }.toSet()
            
            // Récupérer les IDs d'utilisateurs manquants
            val missingUserIds = serverEntriesMap
                .mapNotNull { (it["userId"] as? Double)?.toLong() }
                .filter { it !in existingUserIds }
                .distinct()
                
            // Si nous avons des utilisateurs manquants et qu'il existe au moins un utilisateur par défaut
            if (missingUserIds.isNotEmpty()) {
                // Trouver l'utilisateur par défaut ou le premier utilisateur disponible
                val defaultUser = existingUsers.find { it.isDefault } ?: existingUsers.firstOrNull()
                
                if (defaultUser != null) {
                    // Créer une entrée temporaire dans la table des utilisateurs pour chaque ID manquant
                    // en utilisant l'ID du serveur mais en préservant le nom de l'utilisateur par défaut
                    val temporaryUsers = missingUserIds.map { userId ->
                        User(id = userId, name = defaultUser.name, isDefault = false)
                    }
                    
                    userDao.insertOrUpdateUsers(temporaryUsers)
                    Log.d(TAG, "${temporaryUsers.size} utilisateurs temporaires créés avec le nom '${defaultUser.name}'")
                } else {
                    // Cas de secours: créer un utilisateur par défaut si aucun n'existe
                    val defaultUserName = "Utilisateur par défaut"
                    val newDefaultUser = User(id = 1, name = defaultUserName, isDefault = true)
                    userDao.insert(newDefaultUser)
                    
                    // Puis créer les utilisateurs temporaires
                    val temporaryUsers = missingUserIds.map { userId ->
                        User(id = userId, name = defaultUserName, isDefault = false)
                    }
                    
                    userDao.insertOrUpdateUsers(temporaryUsers)
                    Log.d(TAG, "Utilisateur par défaut créé et ${temporaryUsers.size} utilisateurs temporaires ajoutés")
                }
            }

            // Étape 3: Synchroniser les entrées

            if (serverEntriesMap.isEmpty()) {
                Log.d(TAG, "Aucune nouvelle entrée sur le serveur.")
                // Ne pas retourner ici, car il pourrait y avoir eu des utilisateurs à synchroniser
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
                    id = 0, // Room auto-génère
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
                if (!newEntry.deleted) {
                    newEntriesToInsert.add(newEntry)
                }
            }

            if (newEntriesToInsert.isNotEmpty()) {
                dao.insertOrUpdateEntries(newEntriesToInsert)
                Log.d(TAG, "${newEntriesToInsert.size} nouvelles entrées insérées depuis le serveur.")
            }

            val serverWorkoutsMap = jsonResponse["workouts"] ?: emptyList()
            if (serverWorkoutsMap.isEmpty()) {
                Log.d(TAG, "Aucune nouvelle séance sur le serveur.")
                return@use
            }

            val workoutDao = database.workoutEntryDao()
            val newWorkoutsToInsert = mutableListOf<WorkoutEntry>()

            for (workoutMap in serverWorkoutsMap) {
                val serverId = (workoutMap["id"] as? Double)?.toLong()
                val clientId = (workoutMap["clientId"] as? Double)?.toLong()

                if (serverId == null) {
                    Log.w(TAG, "Séance serveur ignorée, ID manquant: $workoutMap")
                    continue
                }

                if (clientId != null && !fetchAll) {
                    val localEntry = workoutDao.getEntryByIdSuspend(clientId)
                    if (localEntry != null && localEntry.serverEntryId == null) {
                        Log.d(TAG, "Correspondance séance par clientId: id local ${localEntry.id} -> id serveur $serverId")
                        workoutDao.markAsSynced(localEntry.id, serverId)
                        continue
                    }
                }

                val existingEntry = workoutDao.getEntriesByServerIds(listOf(serverId))
                if (existingEntry.isNotEmpty()) {
                    Log.d(TAG, "Séance avec id serveur $serverId déjà présente localement. Ignorée.")
                    continue
                }

                val startTime = LocalDateTime.parse(workoutMap["startTime"] as String, dateFormatter)
                val newEntry = WorkoutEntry(
                    id = 0,
                    serverEntryId = serverId,
                    userId = (workoutMap["userId"] as Double).toLong(),
                    startTime = startTime,
                    durationMinutes = (workoutMap["durationMinutes"] as? Double)?.toInt(),
                    distanceKm = (workoutMap["distanceKm"] as? Double)?.toFloat(),
                    calories = (workoutMap["calories"] as? Double)?.toInt(),
                    program = workoutMap["program"] as? String,
                    notes = workoutMap["notes"] as? String,
                    synced = true,
                    deleted = (workoutMap["deleted"] as? Double)?.toInt() == 1
                )
                if (!newEntry.deleted) {
                    newWorkoutsToInsert.add(newEntry)
                }
            }

            if (newWorkoutsToInsert.isNotEmpty()) {
                workoutDao.insertOrUpdateEntries(newWorkoutsToInsert)
                Log.d(TAG, "${newWorkoutsToInsert.size} nouvelles séances insérées depuis le serveur.")
            }
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
                    syncManager.uploadUnsyncedWorkouts(database)
                    syncManager.downloadNewEntries(database)
                    
                    syncManager.saveLastSyncTimestamp(System.currentTimeMillis())
                    Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la synchronisation", e)
                    Result.failure()
                }
            }
        }
    }
}
