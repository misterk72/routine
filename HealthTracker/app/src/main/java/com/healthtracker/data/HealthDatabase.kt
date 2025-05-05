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
        MetricType::class,
        User::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthEntryDao(): HealthEntryDao
    abstract fun metricValueDao(): MetricValueDao
    abstract fun metricTypeDao(): MetricTypeDao
    abstract fun userDao(): UserDao

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
        
        // Migration de la version 3 à 4 (ajout de la table User et modification de HealthEntry)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Créer la table User
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
                
                // Insérer un utilisateur par défaut
                database.execSQL(
                    """
                    INSERT INTO users (name, isDefault) 
                    VALUES ('Utilisateur par défaut', 1)
                    """
                )
                
                // Créer une table temporaire pour HealthEntry
                database.execSQL(
                    """
                    CREATE TABLE health_entries_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        timestamp TEXT NOT NULL,
                        weight REAL,
                        waistMeasurement REAL,
                        bodyFat REAL,
                        notes TEXT,
                        FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE
                    )
                    """
                )
                
                // Copier les données existantes avec l'utilisateur par défaut (id=1)
                database.execSQL(
                    """
                    INSERT INTO health_entries_temp (id, userId, timestamp, weight, waistMeasurement, bodyFat, notes)
                    SELECT id, 1, timestamp, weight, waistMeasurement, bodyFat, notes FROM health_entries
                    """
                )
                
                // Supprimer l'ancienne table
                database.execSQL("DROP TABLE health_entries")
                
                // Renommer la table temporaire
                database.execSQL("ALTER TABLE health_entries_temp RENAME TO health_entries")
                
                // Créer un index pour améliorer les performances
                database.execSQL("CREATE INDEX index_health_entries_userId ON health_entries (userId)")
            }
        }
        
        // Migration de la version 4 à 5 (ajout des champs synced et serverEntryId pour la synchronisation)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajouter la colonne synced à la table health_entries
                database.execSQL("ALTER TABLE health_entries ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
                
                // Ajouter la colonne serverEntryId à la table health_entries
                database.execSQL("ALTER TABLE health_entries ADD COLUMN serverEntryId INTEGER")
            }
        }

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                // Appliquer les migrations
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
