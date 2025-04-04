package com.healthtracker.data.converters

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Converts between LocalDateTime and String for Room database storage
 * Uses ISO format for storage to ensure proper sorting and compatibility
 */
class DateTimeConverters {
    // Keep using ISO format for database storage to ensure proper sorting
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { formatter.parse(it, LocalDateTime::from) }
    }

    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }
}
