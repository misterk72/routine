package com.healthtracker.jellyfin

import java.time.Instant

enum class JellyfinMediaType {
    MOVIE,
    EPISODE
}

data class WatchedMediaDuringSession(
    val title: String,
    val type: JellyfinMediaType,
    val releaseYear: Int? = null,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val playedAt: Instant,
    val itemId: String? = null
)

data class JellyfinConnectionTestResult(
    val success: Boolean,
    val message: String
)
