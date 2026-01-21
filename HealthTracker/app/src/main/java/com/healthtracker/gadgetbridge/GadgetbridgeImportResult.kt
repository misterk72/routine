package com.healthtracker.gadgetbridge

import java.time.LocalDateTime

data class GadgetbridgeImportResult(
    val startTime: LocalDateTime,
    val durationMinutes: Int?,
    val distanceKm: Float?,
    val calories: Int?,
    val heartRateAvg: Int?,
    val heartRateMin: Int?,
    val heartRateMax: Int?,
    val sleepHeartRateAvg: Int?,
    val vo2Max: Float?
)
