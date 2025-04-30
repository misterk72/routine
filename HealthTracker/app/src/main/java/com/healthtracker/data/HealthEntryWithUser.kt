package com.healthtracker.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe qui combine une entrée de santé avec les informations de l'utilisateur associé
 */
data class HealthEntryWithUser(
    @Embedded
    val entry: HealthEntry,
    
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: User
)
