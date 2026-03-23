package com.healthtracker.jellyfin

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class WorkoutMediaAutofillCoordinatorTest {

    @Test
    fun `buildSoundtrackForSession returns null when jellyfin is not configured`() = runBlocking {
        val coordinator = WorkoutMediaAutofillCoordinator(
            settings = FakeSettings(configured = false),
            client = FakeClient()
        )

        val result = coordinator.buildSoundtrackForSession(
            startTime = LocalDateTime.of(2026, 3, 23, 10, 0),
            durationMinutes = 60
        )

        assertNull(result)
    }

    @Test
    fun `buildSoundtrackForSession formats jellyfin media within workout window`() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val startTime = LocalDateTime.of(2026, 3, 23, 10, 0)
        val media = listOf(
            WatchedMediaDuringSession(
                title = "Heat",
                type = JellyfinMediaType.MOVIE,
                releaseYear = 1995,
                playedAt = startTime.plusMinutes(10).atZone(zoneId).toInstant()
            ),
            WatchedMediaDuringSession(
                title = "Lost episode",
                type = JellyfinMediaType.EPISODE,
                seriesName = "Lost",
                seasonNumber = 2,
                playedAt = startTime.plusMinutes(20).atZone(zoneId).toInstant()
            )
        )
        val coordinator = WorkoutMediaAutofillCoordinator(
            settings = FakeSettings(),
            client = FakeClient(media = media)
        )

        val result = coordinator.buildSoundtrackForSession(
            startTime = startTime,
            durationMinutes = 60
        )

        assertEquals("Heat (1995) + Lost S02", result)
    }

    private class FakeSettings(
        private val configured: Boolean = true
    ) : JellyfinSettings {
        override fun getServerUrl(): String? = if (configured) "http://192.168.0.107:8096" else null
        override fun setServerUrl(value: String?) = Unit
        override fun getUsername(): String? = if (configured) "vincent" else null
        override fun setUsername(value: String?) = Unit
        override fun getApiKey(): String? = if (configured) "secret" else null
        override fun setApiKey(value: String?) = Unit
        override fun isConfigured(): Boolean = configured
    }

    private class FakeClient(
        private val media: List<WatchedMediaDuringSession> = emptyList()
    ) : JellyfinClient {
        override suspend fun testServerConnection(serverUrl: String, apiKey: String) = Unit
        override suspend fun resolveUserId(serverUrl: String, apiKey: String, username: String): String = "user-1"
        override suspend fun fetchPlayedMedia(
            serverUrl: String,
            apiKey: String,
            userId: String
        ): List<WatchedMediaDuringSession> = media
    }
}
