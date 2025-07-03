package com.healthtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "health_entries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Location::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("locationId")]
)
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long, // Référence à l'utilisateur
    val timestamp: LocalDateTime,
    val weight: Float? = null,
    val waistMeasurement: Float? = null,
    val bodyFat: Float? = null,
    val notes: String? = null,
    val locationId: Long? = null, // Référence à la localisation
    val synced: Boolean = false, // Indique si l'entrée a été synchronisée avec le serveur
    val serverEntryId: Long? = null, // ID de l'entrée sur le serveur
    val deleted: Boolean = false // Indique si l'entrée a été supprimée (soft delete)
) {

}
