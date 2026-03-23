package com.healthtracker.jellyfin

import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutMediaAutofillCoordinator @Inject constructor(
    private val settings: JellyfinSettings,
    private val client: JellyfinClient
) {

    fun isConfigured(): Boolean = settings.isConfigured()

    suspend fun testConnection(): JellyfinConnectionTestResult {
        val serverUrl = settings.getServerUrl()
        val username = settings.getUsername()
        val apiKey = settings.getApiKey()
        if (serverUrl.isNullOrBlank() || username.isNullOrBlank() || apiKey.isNullOrBlank()) {
            return JellyfinConnectionTestResult(false, "Configuration Jellyfin incomplète")
        }

        return try {
            client.testServerConnection(serverUrl, apiKey)
            val userId = client.resolveUserId(serverUrl, apiKey, username)
            val media = client.fetchPlayedMedia(serverUrl, apiKey, userId)
            val preview = media.take(5).joinToString("\n") { item ->
                "- ${item.playedAt.toDisplayString()} | ${item.toDisplayLabel()}"
            }.ifBlank { "- aucun media trouve" }
            JellyfinConnectionTestResult(
                true,
                "Connexion Jellyfin OK\nuser=$username\nuserId=$userId\n$preview"
            )
        } catch (e: IOException) {
            JellyfinConnectionTestResult(false, e.message ?: "Erreur Jellyfin")
        }
    }

    suspend fun buildSoundtrackForSession(
        startTime: LocalDateTime,
        durationMinutes: Int
    ): String? {
        if (!settings.isConfigured() || durationMinutes <= 0) {
            return null
        }

        val serverUrl = settings.getServerUrl() ?: return null
        val username = settings.getUsername() ?: return null
        val apiKey = settings.getApiKey() ?: return null

        val userId = client.resolveUserId(serverUrl, apiKey, username)
        val media = client.fetchPlayedMedia(serverUrl, apiKey, userId)
        val zoneId = ZoneId.systemDefault()
        val windowStart = startTime.atZone(zoneId).toInstant()
        val windowEnd = startTime.plusMinutes(durationMinutes.toLong()).atZone(zoneId).toInstant()

        return JellyfinPlaybackFormatter.buildSoundtrack(media, windowStart, windowEnd)
    }

    private fun WatchedMediaDuringSession.toDisplayLabel(): String {
        return when (type) {
            JellyfinMediaType.MOVIE -> releaseYear?.let { "$title ($it)" } ?: title
            JellyfinMediaType.EPISODE -> {
                val series = seriesName ?: title
                seasonNumber?.let { "%s S%02d".format(series, it) } ?: series
            }
        }
    }

    private fun java.time.Instant.toDisplayString(): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .format(atZone(ZoneId.systemDefault()))
    }
}
