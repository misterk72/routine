package com.healthtracker.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager

/**
 * Récepteur de broadcast pour déclencher la synchronisation
 */
class SyncBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "HT_SYNC_RECEIVER"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast reçu: ${intent.action}")
        
        if (intent.action == "com.healthtracker.SYNC_NOW") {
            try {
                Log.d(TAG, "Déclenchement de la synchronisation via broadcast")
                
                // Obtenir une instance de WorkManager directement
                val workManager = WorkManager.getInstance(context)
                val syncManager = SyncManager(context, workManager)
                
                // Déclencher la synchronisation
                val syncId = syncManager.syncNow()
                Log.d(TAG, "Synchronisation déclenchée avec l'ID: $syncId")
                
                // Tester directement la connexion au serveur
                testServerConnection(context)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du déclenchement de la synchronisation: ${e.message}", e)
            }
        }
    }
    
    /**
     * Teste directement la connexion au serveur pour déboguer
     */
    private fun testServerConnection(context: Context) {
        Thread {
            try {
                val url = "http://192.168.0.13:5001/sync.php?since=1746936175"
                Log.d(TAG, "Test de connexion au serveur: $url")
                
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Code de réponse: $responseCode")
                
                if (responseCode == 200) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Réponse du serveur: $response")
                    
                    // Analyser la réponse JSON pour voir combien d'entrées sont disponibles
                    if (response.contains("entries")) {
                        val entriesCount = response.split("\"id\":").size - 1
                        Log.d(TAG, "Nombre d'entrées disponibles sur le serveur: $entriesCount")
                    }
                } else {
                    Log.e(TAG, "Erreur lors de la connexion au serveur: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors du test de connexion: ${e.message}", e)
            }
        }.start()
    }
}
