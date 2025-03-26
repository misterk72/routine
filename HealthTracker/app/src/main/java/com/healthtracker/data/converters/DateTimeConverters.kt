package com.healthtracker.data.converters

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeConverters {
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
