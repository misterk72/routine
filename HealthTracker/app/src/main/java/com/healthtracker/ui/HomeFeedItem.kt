package com.healthtracker.ui

import com.healthtracker.data.HealthEntryWithUser
import com.healthtracker.data.WorkoutEntryWithUser
import java.time.LocalDateTime

sealed class HomeFeedItem {
    abstract val timestamp: LocalDateTime
    abstract val stableId: Long

    data class Health(val entryWithUser: HealthEntryWithUser) : HomeFeedItem() {
        override val timestamp: LocalDateTime = entryWithUser.entry.timestamp
        override val stableId: Long = entryWithUser.entry.id
    }

    data class Workout(val entryWithUser: WorkoutEntryWithUser) : HomeFeedItem() {
        override val timestamp: LocalDateTime = entryWithUser.entry.startTime
        override val stableId: Long = entryWithUser.entry.id
    }
}
