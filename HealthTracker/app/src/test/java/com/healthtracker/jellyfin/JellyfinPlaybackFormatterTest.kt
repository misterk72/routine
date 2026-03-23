package com.healthtracker.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class JellyfinPlaybackFormatterTest {

    @Test
    fun `buildSoundtrack keeps chronological order and deduplicates seasons`() {
        val media = listOf(
            movie("Heat", "2026-03-23T10:10:00Z", 1995),
            episode("Lost", 2, "2026-03-23T10:20:00Z"),
            episode("Lost", 2, "2026-03-23T10:25:00Z"),
            movie("Collateral", "2026-03-23T10:30:00Z", 2004)
        )

        val result = JellyfinPlaybackFormatter.buildSoundtrack(
            media = media,
            windowStart = Instant.parse("2026-03-23T10:00:00Z"),
            windowEnd = Instant.parse("2026-03-23T11:00:00Z")
        )

        assertEquals("Heat (1995) + Lost S02 + Collateral (2004)", result)
    }

    @Test
    fun `buildSoundtrack returns null when nothing is in the session window`() {
        val media = listOf(movie("Heat", "2026-03-23T12:10:00Z", 1995))

        val result = JellyfinPlaybackFormatter.buildSoundtrack(
            media = media,
            windowStart = Instant.parse("2026-03-23T10:00:00Z"),
            windowEnd = Instant.parse("2026-03-23T11:00:00Z")
        )

        assertNull(result)
    }

    private fun movie(title: String, playedAt: String, releaseYear: Int? = null) = WatchedMediaDuringSession(
        title = title,
        type = JellyfinMediaType.MOVIE,
        releaseYear = releaseYear,
        playedAt = Instant.parse(playedAt)
    )

    private fun episode(seriesName: String, seasonNumber: Int, playedAt: String) = WatchedMediaDuringSession(
        title = "$seriesName episode",
        type = JellyfinMediaType.EPISODE,
        seriesName = seriesName,
        seasonNumber = seasonNumber,
        playedAt = Instant.parse(playedAt)
    )
}
