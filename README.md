# Suivi Santé - Application Android

*Note: L'application est entièrement en français*

## Aperçu du Projet
Une application Android pour suivre les métriques de santé personnelles, notamment :
- Poids corporel
- Tour de taille
- Remarques/notes de santé
- Horodatage pour toutes les entrées
- Base de données SQLite avec conteneur Docker pour le stockage des données
- Intégration Grafana pour la visualisation des données

## Exigences Techniques

### Environnement de Développement
- Android Studio (latest version)
- Minimum Android SDK: API 24 (Android 7.0)
- Target Android SDK: API 34 (Android 14)
- Kotlin programming language

### Dépendances
```gradle
dependencies {
    // Android Core Libraries
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    
    // Room Database for local storage
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // Coroutines for async operations
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // ViewModel and LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
}
```

## Architecture Backend

### Structure de la Base de Données
- SQLite database with Docker container
- Structured schema for workouts with flexible fields
- JSON support for custom data
- Tag-based categorization
- Free-form notes

### Schéma de la Base de Données
```sql
CREATE TABLE workouts (
    id INTEGER PRIMARY KEY,
    date_time DATETIME NOT NULL,
    program TEXT,
    duration_minutes INTEGER,
    average_speed REAL,
    distance_km REAL,
    calories INTEGER,
    calories_per_km REAL,
    average_heart_rate INTEGER,
    max_heart_rate INTEGER,
    min_heart_rate INTEGER,
    weight_kg REAL,
    fat_mass_kg REAL,
    fat_percentage REAL,
    waist_circumference_cm REAL,
    background_music TEXT,
    observations TEXT,
    custom_data JSON,
    tags TEXT,
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Flux de Données
1. L'application Android collecte les données de santé
2. Les données sont synchronisées avec la base de données SQLite
3. Grafana se connecte à SQLite pour la visualisation
4. Les données peuvent être exportées pour analyse

## Structure du Projet

### 1. Modèles de Données
- Create flexible data classes for health metrics:
  ```kotlin
  // Main entry that stores timestamp and metadata
  data class HealthEntry(
      val id: Long,
      val timestamp: Long,
      val syncStatus: Boolean,
      val note: String? = null
  )

  // Flexible metric value storage
  data class MetricValue(
      val id: Long,
      val entryId: Long,  // References HealthEntry
      val metricType: String,  // e.g., "weight", "waist", "blood_pressure", etc.
      val value: String,  // Store as string to support various formats
      val unit: String?  // e.g., "kg", "cm", "mmHg", etc.
  )

  // Metric type definition for UI and validation
  data class MetricType(
      val name: String,  // e.g., "weight", "waist", etc.
      val displayName: String,  // e.g., "Body Weight", "Waist Circumference"
      val valueType: ValueType,  // NUMBER, TEXT, BOOLEAN
      val defaultUnit: String?,
      val validationRules: List<ValidationRule>? = null
  )

  enum class ValueType {
      NUMBER,
      TEXT,
      BOOLEAN
  }
  ```

### 2. Couche Base de Données
- Use Room database for local storage
- Create DAOs (Data Access Objects) for CRUD operations
- Implement repository pattern for data management
- Sync with Dockerized SQLite database

### 3. Composants UI
1. Activité Principale
   - Liste des entrées de santé
   - Bouton d'ajout de nouvelle entrée
   - Indicateur de statut de synchronisation

2. Écran d'Ajout/Modification d'Entrée
   - Sélecteur de date/heure
   - Champ de saisie du poids
   - Sélection de métrique
   - Saisie de note
   - Sélection de tags

### 4. Visualisation
- Grafana dashboards for:
  - Workout history
  - Progress tracking
  - Statistical analysis
  - Trend visualization

## Instructions d'Installation

1. Clone the repository
2. Build the Docker container:
   ```bash
   cd docker-sqlite
   docker-compose build
   docker-compose up -d
   ```
3. Import existing data (if any):
   ```bash
   python import_data.py
   ```
4. Open the project in Android Studio
5. Run the app on your device or emulator

## Tests

### Tests Unitaires
The project includes comprehensive unit tests for:
- Data models (HealthEntry, MetricValue, MetricType)
- ViewModels with coroutines and Flow
- Business logic and data transformations

Run unit tests:
```bash
./gradlew test
```

### Tests d'Instrumentation
Android instrumentation tests cover:
- Room database operations
- DAO implementations
- UI interactions with Espresso

Run instrumentation tests:
```bash
./gradlew connectedAndroidTest
```

### Dépendances de Test
```gradle
// Tests Unitaires
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.3.1'
testImplementation 'org.mockito.kotlin:mockito-kotlin:5.1.0'
testImplementation 'androidx.arch.core:core-testing:2.2.0'
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
testImplementation 'app.cash.turbine:turbine:1.0.0'

// Tests d'Instrumentation
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
androidTestImplementation 'androidx.test:runner:1.5.2'
androidTestImplementation 'androidx.test:rules:1.5.0'
androidTestImplementation 'androidx.room:room-testing:2.6.1'
androidTestImplementation 'com.google.dagger:hilt-android-testing:2.50'
```

## Data Export
- Export to Excel format
- Export to JSON format
- Export to CSV format
- Custom export options available

## Visualization Features
- Interactive charts
- Trend analysis
- Statistical summaries
- Custom dashboard creation
- Data filtering and sorting
