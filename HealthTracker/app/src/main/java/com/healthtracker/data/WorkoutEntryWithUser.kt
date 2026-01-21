package com.healthtracker.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Combine une séance avec les informations de l'utilisateur associé.
 */
data class WorkoutEntryWithUser(
    @Embedded
    val entry: WorkoutEntry,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: User
)
