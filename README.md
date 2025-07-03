
fatal : cannot exec '/mnt/nas_Media/Download/W
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

Le schéma de la base de données locale (Room) est défini par l'entité `HealthEntry`.

```kotlin
@Entity(tableName = "health_entries")
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val timestamp: LocalDateTime,
    val weight: Float? = null,
    val waistMeasurement: Float? = null,
    val bodyFat: Float? = null,
    val notes: String? = null,
    val synced: Boolean = false,
    val serverEntryId: Long? = null,
    val deleted: Boolean = false
)
```

### Flux de Données
1. L'application Android collecte les données de santé
2. Les données sont stockées localement dans la base de données SQLite
3. L'application synchronise les données avec le serveur MariaDB via l'API PHP
   - Les entrées non synchronisées sont envoyées au serveur
   - Les entrées modifiées sont automatiquement marquées pour synchronisation
   - Les entrées supprimées sont synchronisées avec le serveur (suppression logique)
   - Les nouvelles entrées du serveur sont récupérées
   - Les entrées sont marquées comme synchronisées dans la base locale
   - Support pour serveur de développement local et serveur de production
4. Grafana se connecte à MariaDB pour la visualisation
5. Les données peuvent être exportées pour analyse

## Structure du Projet

### 1. Modèle de Données
- L'application utilise un modèle de données simple et robuste centré sur la classe `HealthEntry`.

  ```kotlin
  data class HealthEntry(
      @PrimaryKey(autoGenerate = true)
      val id: Long = 0,
      val userId: Long, // Référence à l'utilisateur
      val timestamp: LocalDateTime,
      val weight: Float? = null,
      val waistMeasurement: Float? = null,
      val bodyFat: Float? = null,
      val notes: String? = null,
      val synced: Boolean = false, // Indique si l'entrée a été synchronisée
      val serverEntryId: Long? = null, // ID de l'entrée sur le serveur
      val deleted: Boolean = false // Pour la suppression logique (soft delete)
  )
  ```

### 5. Synchronisation et Correction des Données
- **Correction des Doublons** : Implémentation d'une logique de synchronisation avancée pour éliminer les doublons de données en se basant sur un `clientId` unique.
- **Resynchronisation Complète** : Ajout d'une fonctionnalité de resynchronisation complète qui permet de :
  1. Envoyer les entrées locales non synchronisées au serveur.
  2. Supprimer toutes les données de la base de données locale.
  3. Télécharger à nouveau toutes les entrées depuis le serveur.
  Cette fonctionnalité est accessible via un bouton temporaire dans l'interface pour les tests en `dev`.

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
