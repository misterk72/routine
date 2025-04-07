package com.healthtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.healthtracker.data.converters.DateTimeConverters

@Database(
    entities = [
        HealthEntry::class,
        MetricValue::class,
        MetricType::class
    ],
    version = 3,
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

        // Migration de la version 2 à 3 (ajout du champ bodyFat)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajouter la colonne bodyFat à la table HealthEntry
                database.execSQL("ALTER TABLE HealthEntry ADD COLUMN bodyFat REAL")
            }
        }

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                // Appliquer la migration de 2 à 3
                .addMigrations(MIGRATION_2_3)
                // Fallback en cas d'autres migrations
                .fallbackToDestructiveMigration()
                // Allow main thread queries for testing
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
