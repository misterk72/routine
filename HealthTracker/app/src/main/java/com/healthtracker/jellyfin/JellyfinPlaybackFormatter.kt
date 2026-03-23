package com.healthtracker.jellyfin

import java.time.Instant

object JellyfinPlaybackFormatter {

    fun buildSoundtrack(
        media: List<WatchedMediaDuringSession>,
        windowStart: Instant,
        windowEnd: Instant
    ): String? {
        val labels = linkedSetOf<String>()
        media.asSequence()
            .filter { !it.playedAt.isBefore(windowStart) && !it.playedAt.isAfter(windowEnd) }
            .sortedBy { it.playedAt }
            .mapNotNull(::toDisplayLabel)
            .forEach { label -> labels.add(label) }

        return labels.takeIf { it.isNotEmpty() }?.joinToString(" + ")
    }

    private fun toDisplayLabel(item: WatchedMediaDuringSession): String? {
        return when (item.type) {
            JellyfinMediaType.MOVIE -> item.title.takeIf { it.isNotBlank() }?.let { title ->
                item.releaseYear?.let { "$title ($it)" } ?: title
            }
            JellyfinMediaType.EPISODE -> {
                val seriesName = item.seriesName?.takeIf { it.isNotBlank() } ?: return null
                val seasonNumber = item.seasonNumber ?: return seriesName
                "%s S%02d".format(seriesName, seasonNumber)
            }
        }
    }
}
