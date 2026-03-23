package com.healthtracker.jellyfin

interface JellyfinClient {
    suspend fun testServerConnection(serverUrl: String, apiKey: String)
    suspend fun resolveUserId(serverUrl: String, apiKey: String, username: String): String
    suspend fun fetchPlayedMedia(serverUrl: String, apiKey: String, userId: String): List<WatchedMediaDuringSession>
}
