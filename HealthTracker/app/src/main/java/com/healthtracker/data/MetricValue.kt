package com.healthtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "metric_values",
    foreignKeys = [
        ForeignKey(
            entity = HealthEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MetricValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val metricType: String,
    val value: Double,
    val unit: String? = null,
    val notes: String? = null
)
