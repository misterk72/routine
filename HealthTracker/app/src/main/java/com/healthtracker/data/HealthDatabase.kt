package com.healthtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.healthtracker.data.converters.DateTimeConverters

@Database(
    entities = [
        HealthEntry::class,
        MetricValue::class,
        MetricType::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthEntryDao(): HealthEntryDao
    abstract fun metricValueDao(): MetricValueDao
    abstract fun metricTypeDao(): MetricTypeDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                .fallbackToDestructiveMigration() // Add this line to handle schema changes
                .allowMainThreadQueries() // Allow main thread queries for testing
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
