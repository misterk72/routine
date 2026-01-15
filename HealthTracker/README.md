# HealthTracker - Application de Suivi de Sante Personnelle

## Description
HealthTracker est une application Android concue pour suivre les donnees de sante personnelles (poids, tour de taille, masse graisseuse). Les donnees sont stockees localement puis synchronisees avec un serveur distant pour visualisation et analyse via Grafana.

## Fonctionnalites
- Saisie de donnees simple et rapide
- Synchronisation automatique en arriere-plan (upload + download)
- Support multi-utilisateurs
- Stockage local hors ligne via Room (SQLite)
- Visualisation des donnees via Grafana (cote serveur)
- Gestion des suppressions (suppression logique) et detection de doublons
- Export des donnees (CSV, JSON, Excel)

## Architecture Technique

### Application Android
- **Langage** : Kotlin
- **Architecture** : MVVM (Model-View-ViewModel)
- **UI** : Jetpack Compose
- **Base de donnees locale** : Room (SQLite)
- **Asynchronisme** : Coroutines et Flow
- **Injection de dependances** : Hilt
- **Taches en arriere-plan** : WorkManager
- **Communication reseau** : OkHttp et Gson

### Backend
- **Base de donnees** : MariaDB
- **API** : PHP
- **Conteneurisation** : Docker
- **Visualisation** : Grafana

## Variantes de Build
- **dev** : Se connecte au serveur de developpement (192.168.0.13:5001)
- **prod** : Se connecte au serveur de production (192.168.0.103:5001)

## Synchronisation des Donnees
L'application utilise WorkManager pour synchroniser periodiquement les donnees entre la base de donnees locale et le serveur. La synchronisation comprend :

1. **Upload** : Envoi des entrees non synchronisees au serveur
2. **Download** : Telechargement des nouvelles entrees depuis le serveur
3. **Gestion des utilisateurs** : Creation intelligente d'utilisateurs temporaires si necessaire
4. **Gestion des suppressions** : Synchronisation des entrees supprimees (suppression logique)

## Modele de Donnees

### Entites Principales
- **User** : Represente un utilisateur avec un identifiant unique et un nom
- **HealthEntry** : Entree de donnees de sante avec reference a l'utilisateur

```kotlin
@Entity(tableName = "health_entries")
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,  // Cle etrangere vers User.id
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

## Dependances Principales (build.gradle.kts)

```kotlin
dependencies {
    // Core & UI
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Room pour la base de donnees locale
    implementation 'androidx.room:room-runtime:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'

    // Coroutines pour la gestion des threads
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // ViewModel and LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'

    // WorkManager pour les taches en arriere-plan
    implementation 'androidx.work:work-runtime-ktx:2.9.0'

    // Bibliotheques pour la synchronisation HTTP
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'
    implementation 'com.google.code.gson:gson:2.10.1'

    // Hilt pour l'injection de dependances
    implementation 'com.google.dagger:hilt-android:2.50'
    kapt 'com.google.dagger:hilt-android-compiler:2.50'
}
```

## Dernieres Ameliorations (Juillet 2025)
- Correction des problemes de contrainte de cle etrangere lors de la synchronisation
- Amelioration de la gestion des utilisateurs pour preserver les noms personnalises
- Resolution du probleme de selecteur d'utilisateur manquant dans l'interface

## Installation
L'application peut etre installee via les fichiers APK generes pour les variantes dev et prod :
```
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
adb install -r app/build/outputs/apk/prod/debug/app-prod-debug.apk
```

## Documentation
Consultez le fichier `DEVLOG.md` pour un historique detaille du developpement du projet.

## Architecture Backend

### Structure de la Base de Donnees
- Base de donnees SQLite locale sur l'appareil Android
- Base de donnees MariaDB sur serveur pour la synchronisation
- API PHP pour la communication entre l'application et MariaDB
- Structured schema for workouts with flexible fields
- JSON support for custom data
- Tag-based categorization
- Free-form notes
- Suivi de l'etat de synchronisation des entrees

### Schema de la Base de Donnees

Le schema de la base de donnees locale (Room) est defini par l'entite `HealthEntry`.

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

### Flux de Donnees
1. L'application Android collecte les donnees de sante
2. Les donnees sont stockees localement dans la base de donnees SQLite
3. L'application synchronise les donnees avec le serveur MariaDB via l'API PHP
   - Les entrees non synchronisees sont envoyees au serveur
   - Les entrees modifiees sont automatiquement marquees pour synchronisation
   - Les entrees supprimees sont synchronisees avec le serveur (suppression logique)
   - Les nouvelles entrees du serveur sont recuperees
   - Les entrees sont marquees comme synchronisees dans la base locale
   - Support pour serveur de developpement local et serveur de production
4. Grafana se connecte a MariaDB pour la visualisation
5. Les donnees peuvent etre exportees pour analyse

## Structure du Projet

### Modele de Donnees
- L'application utilise un modele de donnees simple et robuste centre sur la classe `HealthEntry`.

```kotlin
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long, // Reference a l'utilisateur
    val timestamp: LocalDateTime,
    val weight: Float? = null,
    val waistMeasurement: Float? = null,
    val bodyFat: Float? = null,
    val notes: String? = null,
    val synced: Boolean = false, // Indique si l'entree a ete synchronisee
    val serverEntryId: Long? = null, // ID de l'entree sur le serveur
    val deleted: Boolean = false // Pour la suppression logique (soft delete)
)
```

### Synchronisation et Correction des Donnees
- **Correction des Doublons** : implementation d'une logique de synchronisation avancee pour eliminer les doublons de donnees en se basant sur un `clientId` unique.
- **Resynchronisation Complete** : ajout d'une fonctionnalite de resynchronisation complete qui permet de :
  1. Envoyer les entrees locales non synchronisees au serveur.
  2. Supprimer toutes les donnees de la base de donnees locale.
  3. Telecharger a nouveau toutes les entrees depuis le serveur.
  Cette fonctionnalite est accessible via un bouton temporaire dans l'interface pour les tests en `dev`.

### Couche Base de Donnees
- Use Room database for local storage
- Create DAOs (Data Access Objects) for CRUD operations
- Implement repository pattern for data management
- Sync with Dockerized SQLite database

### Composants UI
1. Activite Principale
   - Liste des entrees de sante avec date, utilisateur et metriques
   - Affichage conditionnel des metriques disponibles (masse, masse graisseuse, tour de taille)
   - Bouton d'ajout de nouvelle entree

2. Ecran d'Ajout/Modification d'Entree
   - Selecteur de date/heure
     - Initialisation automatique avec la date et l'heure actuelles a l'ouverture
     - Selection en deux etapes : d'abord la date, puis l'heure
     - Format d'affichage localise en francais
   - Champs de saisie des mesures de base (dans l'ordre) :
     - Masse (kg)
     - Masse graisseuse (kg)
     - Tour de taille (cm)
   - Saisie de note
   - Selection de tags

### Visualisation
- Grafana dashboards for:
  - Workout history
  - Progress tracking
  - Statistical analysis
  - Trend visualization

## Instructions d'Installation

1. Cloner le depot
2. Construire et demarrer les conteneurs Docker :
   ```bash
   cd docker-sqlite
   docker-compose build
   docker-compose up -d
   ```
   Cela demarrera :
   - Le serveur MariaDB sur le port 3306
   - phpMyAdmin sur le port 8080
   - L'API PHP sur le port 5001

3. Verifier que l'API est accessible :
   ```bash
   curl http://localhost:5001/sync.php
   ```
   Vous devriez voir une reponse JSON comme `{"entries":[]}`

4. Importer des donnees existantes (si necessaire) :
   ```bash
   python import_data.py
   ```

5. Ouvrir le projet dans Android Studio

6. Mettre a jour l'adresse IP du serveur dans `SyncManager.kt` avec l'adresse IP de votre machine

7. Executer l'application sur votre appareil ou emulateur

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

### Dependances de Test
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
