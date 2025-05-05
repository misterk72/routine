# Suivi Santé - Application Android

*Note: L'application est entièrement en français et optimisée pour une utilisation en France*

## Aperçu du Projet
Une application Android pour suivre les métriques de santé personnelles, notamment :
- Masse corporelle (kg)
- Masse graisseuse (kg)
- Tour de taille (cm)
- Remarques/notes de santé
- Horodatage pour toutes les entrées
- Gestion multi-utilisateurs
- Base de données SQLite locale avec synchronisation vers MariaDB
- Conteneur Docker pour le serveur MariaDB et l'API PHP
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
    
    // WorkManager pour les tâches en arrière-plan
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    
    // Bibliothèques pour la synchronisation HTTP
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Hilt pour l'injection de dépendances
    implementation 'com.google.dagger:hilt-android:2.50'
    kapt 'com.google.dagger:hilt-android-compiler:2.50'
}
```

## Architecture Backend

### Structure de la Base de Données
- Base de données SQLite locale sur l'appareil Android
- Base de données MariaDB sur serveur pour la synchronisation
- API PHP pour la communication entre l'application et MariaDB
- Structured schema for workouts with flexible fields
- JSON support for custom data
- Tag-based categorization
- Free-form notes
- Suivi de l'état de synchronisation des entrées

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
2. Les données sont stockées localement dans la base de données SQLite
3. L'application synchronise les données avec le serveur MariaDB via l'API PHP
   - Les entrées non synchronisées sont envoyées au serveur
   - Les nouvelles entrées du serveur sont récupérées
   - Les entrées sont marquées comme synchronisées dans la base locale
4. Grafana se connecte à MariaDB pour la visualisation
5. Les données peuvent être exportées pour analyse

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
   - Liste des entrées de santé avec date, utilisateur et métriques
   - Affichage conditionnel des métriques disponibles (masse, masse graisseuse, tour de taille)
   - Bouton d'ajout de nouvelle entrée

2. Écran d'Ajout/Modification d'Entrée
   - Sélecteur de date/heure
     - Initialisation automatique avec la date et l'heure actuelles à l'ouverture
     - Sélection en deux étapes : d'abord la date, puis l'heure
     - Format d'affichage localisé en français
   - Champs de saisie des mesures de base (dans l'ordre) :
     - Masse (kg)
     - Masse graisseuse (kg)
     - Tour de taille (cm)
   - Saisie de note
   - Sélection de tags

### 4. Visualisation
- Grafana dashboards for:
  - Workout history
  - Progress tracking
  - Statistical analysis
  - Trend visualization

## Instructions d'Installation

1. Cloner le dépôt
2. Construire et démarrer les conteneurs Docker :
   ```bash
   cd docker-sqlite
   docker-compose build
   docker-compose up -d
   ```
   Cela démarrera :
   - Le serveur MariaDB sur le port 3306
   - phpMyAdmin sur le port 8080
   - L'API PHP sur le port 5001

3. Vérifier que l'API est accessible :
   ```bash
   curl http://localhost:5001/sync.php
   ```
   Vous devriez voir une réponse JSON comme `{"entries":[]}`

4. Importer des données existantes (si nécessaire) :
   ```bash
   python import_data.py
   ```

5. Ouvrir le projet dans Android Studio

6. Mettre à jour l'adresse IP du serveur dans `SyncManager.kt` avec l'adresse IP de votre machine

7. Exécuter l'application sur votre appareil ou émulateur

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
