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
        User::class,
        Location::class,
        WorkoutEntry::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthEntryDao(): HealthEntryDao
    abstract fun metricValueDao(): MetricValueDao
    abstract fun metricTypeDao(): MetricTypeDao
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun workoutEntryDao(): WorkoutEntryDao

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
        
        // Migration de la version 5 à 6 (ajout du champ deleted pour la suppression logique)
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajouter la colonne deleted à la table health_entries
                database.execSQL("ALTER TABLE health_entries ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration de la version 6 à 7 (ajout de la table Location et du champ locationId dans HealthEntry)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Créer la table locations
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        radius REAL NOT NULL DEFAULT 100,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
                
                // 2. Ajouter quelques localisations par défaut
                database.execSQL(
                    """
                    INSERT INTO locations (name, latitude, longitude, isDefault) 
                    VALUES ('Domène', 45.2028, 5.8417, 0)
                    """
                )
                database.execSQL(
                    """
                    INSERT INTO locations (name, latitude, longitude, isDefault) 
                    VALUES ('Avon', 48.4167, 2.7333, 0)
                    """
                )
                database.execSQL(
                    """
                    INSERT INTO locations (name, latitude, longitude, isDefault) 
                    VALUES ('La Roche-de-Glun', 45.0167, 4.8333, 0)
                    """
                )
                
                // 3. Vérifier si la colonne locationId existe déjà dans health_entries
                try {
                    database.execSQL("SELECT locationId FROM health_entries LIMIT 1")
                } catch (e: Exception) {
                    // La colonne n'existe pas, on l'ajoute
                    database.execSQL("ALTER TABLE health_entries ADD COLUMN locationId INTEGER")
                }
                
                // 4. Obtenir la structure actuelle de la table health_entries
                val tableInfo = database.query("PRAGMA table_info(health_entries)")
                val columnNames = mutableListOf<String>()
                while (tableInfo.moveToNext()) {
                    columnNames.add(tableInfo.getString(1)) // Le nom de la colonne est à l'index 1
                }
                tableInfo.close()
                
                // 5. Créer une table temporaire avec la structure complète incluant la contrainte de clé étrangère
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
                        locationId INTEGER,
                        synced INTEGER NOT NULL DEFAULT 0,
                        serverEntryId INTEGER,
                        deleted INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE,
                        FOREIGN KEY (locationId) REFERENCES locations (id) ON DELETE SET NULL
                    )
                    """
                )
                
                // 6. Construire la requête d'insertion en fonction des colonnes existantes
                val selectColumns = columnNames.joinToString(", ")
                
                // 7. Copier les données de l'ancienne table vers la nouvelle
                database.execSQL(
                    """
                    INSERT INTO health_entries_temp (${selectColumns})
                    SELECT ${selectColumns} FROM health_entries
                    """
                )
                
                // 8. Supprimer l'ancienne table
                database.execSQL("DROP TABLE health_entries")
                
                // 9. Renommer la table temporaire
                database.execSQL("ALTER TABLE health_entries_temp RENAME TO health_entries")
                
                // 10. Recréer les index nécessaires
                database.execSQL("CREATE INDEX index_health_entries_userId ON health_entries (userId)")
                database.execSQL("CREATE INDEX index_health_entries_locationId ON health_entries (locationId)")
            }
        }

        // Migration de la version 7 à 8 (ajout de la table WorkoutEntry)
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        durationMinutes INTEGER,
                        distanceKm REAL,
                        calories INTEGER,
                        program TEXT,
                        notes TEXT,
                        synced INTEGER NOT NULL DEFAULT 0,
                        serverEntryId INTEGER,
                        deleted INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE
                    )
                    """
                )
                database.execSQL("CREATE INDEX index_workout_entries_userId ON workout_entries (userId)")
            }
        }

        // Migration de la version 8 à 9 (ajout des métriques FC/VO2 aux séances)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workout_entries ADD COLUMN heartRateAvg INTEGER")
                database.execSQL("ALTER TABLE workout_entries ADD COLUMN heartRateMin INTEGER")
                database.execSQL("ALTER TABLE workout_entries ADD COLUMN heartRateMax INTEGER")
                database.execSQL("ALTER TABLE workout_entries ADD COLUMN sleepHeartRateAvg INTEGER")
                database.execSQL("ALTER TABLE workout_entries ADD COLUMN vo2Max REAL")
            }
        }

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    com.healthtracker.BuildConfig.DATABASE_NAME
                )
                // Appliquer les migrations
                .addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                // Fallback en cas d'autres migrations non gérées
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
