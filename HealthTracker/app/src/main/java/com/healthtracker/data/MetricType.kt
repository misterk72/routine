package com.healthtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metric_types")
data class MetricType(
    @PrimaryKey
    val id: String,
    val name: String,
    val unit: String? = null,
    val description: String? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val stepSize: Double? = null
)
