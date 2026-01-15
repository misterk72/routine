package com.healthtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "workout_entries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val startTime: LocalDateTime,
    val durationMinutes: Int? = null,
    val distanceKm: Float? = null,
    val calories: Int? = null,
    val program: String? = null,
    val notes: String? = null,
    val synced: Boolean = false,
    val serverEntryId: Long? = null,
    val deleted: Boolean = false
)
