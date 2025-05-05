# Development Log - Personal Health Tracker

## Project Start Date: March 24, 2025

### Initial Setup
- [x] Create new Android project in Android Studio
- [x] Configure Git repository
- [x] Add initial dependencies in build.gradle
- [x] Create project structure (packages for models, database, ui, network)

### Backend Architecture
- [x] Design SQLite database schema
- [x] Create Docker container for SQLite
- [x] Implement data import from Excel
- [x] Add flexible fields (JSON, tags, notes)
- [x] Create database indexes for performance

### Data Layer Implementation
- [x] Create data models
  - [x] HealthEntry class (core entry with timestamp)
  - [x] MetricValue class (flexible value storage)
  - [x] MetricType class (metric definitions)
  - [x] ValueType enum
- [x] Set up Room Database
  - [x] Create Database class
  - [x] Implement DAOs for each entity
  - [x] Set up relationships between entities
  - [x] Write database migrations
- [x] Create Repository classes
  - [x] HealthEntryRepository
  - [x] MetricRepository
  - [x] MetricTypeRepository
- [x] Add unit tests
  - [x] Test basic CRUD operations
  - [x] Test relationships between entities
  - [x] Test metric type validation

### UI Development
- [x] Design and implement layouts
  - [x] Main activity layout (entries list)
  - [x] Add/Edit entry screen
  - [x] Settings screen
- [x] Create RecyclerView adapter for health entries
- [x] Implement ViewModels
  - [x] Main ViewModel
  - [x] Entry ViewModel
  - [x] Settings ViewModel
- [x] Add form validation
- [x] Implement date/time picker
- [x] Add input validation

### Visualization Integration
- [ ] Set up Grafana data source
- [ ] Create basic dashboards
  - [ ] Workout history
  - [ ] Progress tracking
  - [ ] Statistical analysis
- [ ] Implement data export features
  - [ ] Excel export
  - [ ] JSON export
  - [ ] CSV export

### Polish and Testing
- [ ] Add error handling
- [ ] Implement loading indicators
- [ ] Add user feedback (Snackbars/Toasts)
- [ ] Write UI tests
- [ ] Perform integration testing
- [ ] Add data export functionality

### Documentation
- [ ] Add code documentation
- [ ] Create user guide
- [ ] Document Docker setup process
- [ ] Add screenshots to README

### Optional Enhancements
- [ ] Add data visualization
- [x] ~~Implement dark mode~~ (Removed on April 4, 2025)
- [ ] Add widgets
- [ ] Create backup/restore functionality
- [ ] Add BMI calculator
- [x] ~~Implement reminder notifications~~ (Removed on April 4, 2025)

## Progress Notes

### March 24, 2025
- Set up basic Android project structure
- Configured Gradle with necessary dependencies
- Added essential Android configuration files
- Initialized Git repository
- Created initial project documentation
- Defined project structure and requirements

### March 25, 2025
- Designed and implemented SQLite database schema
- Created Docker container for database
- Implemented data import from Excel
- Added flexible fields for future extensibility
- Set up database indexes for performance optimization

### March 26, 2025
- Added comprehensive test suite following TDD principles
  - Unit tests for data models (HealthEntry, MetricValue, MetricType)
  - Integration tests for Room database and DAOs
  - ViewModel tests with coroutines and Flow
- Updated build.gradle with test dependencies
  - Added Mockito for mocking
  - Added coroutines test libraries
  - Added Room testing support
  - Added Hilt testing dependencies
- Fixed ViewModel test issues
  - Properly handled coroutines with StandardTestDispatcher
  - Initialized LiveData with empty list
  - Added test coverage for CRUD operations
  - Improved test readability with clear Given/When/Then structure
- Fixed app crashes and improved database handling
  - Updated minSdk to 26 for LocalDateTime support
  - Removed setSupportActionBar to resolve action bar conflict
  - Added fallbackToDestructiveMigration for database schema changes
  - Added index on entryId in MetricValue for better performance
- Implemented repository layer
  - Created HealthEntryRepository for managing health entries
  - Created MetricRepository for managing metric values
  - Created MetricTypeRepository for managing metric types
  - Updated DAOs to support repository methods
- Enhanced ViewModels with repository pattern
  - Refactored HealthTrackerViewModel to use repositories
  - Created EntryViewModel for add/edit functionality
  - Created SettingsViewModel for app preferences
  - Added loading state indicators and error handling
- Implemented UI components
  - Created EntryActivity for adding and editing health entries
  - Created SettingsActivity for managing user preferences
  - Updated MainActivity to integrate with new activities
  - Added form validation for required fields
  - Implemented date/time picker for entry timestamps
  - Added menu system for settings access
- Fixed MetricType data model
  - Changed id from String to Long with autoGenerate
  - Updated related repositories and DAOs
- Resolved database startup crashes
  - Added error handling in SampleDataProvider to prevent crashes during sample data insertion
  - Updated database version from 1 to 2 to handle schema changes
  - Added allowMainThreadQueries for development testing purposes
  - Fixed Toolbar setup in MainActivity to avoid conflicts with NoActionBar theme
- Successfully tested app on Pixel 7a device
- Pushed all changes to GitHub

### April 4, 2025
- Localized the entire application into French
  - Translated all UI elements, including buttons, labels, error messages, and notifications
  - Created French string resources in values-fr/strings.xml
  - Updated HealthTrackerApp.kt to use French as the default language
  - Ensured all metric displays (weight, waist) use localized formats
- Simplified the application by removing features
  - Removed dark mode option from settings
  - Removed notifications functionality
  - Removed associated UI elements, code, and string resources
- Refactored settings screen
  - Removed appearance section entirely
  - Streamlined the settings interface
- Tested all changes on Pixel 7a device

### April 30, 2025
- Amélioré l'affichage des tuiles sur la page principale
  - Ajouté l'affichage de la date en haut de chaque tuile
  - Ajouté l'affichage du nom de l'utilisateur sous la date
  - Ajouté l'affichage conditionnel de la masse (si disponible)
  - Ajouté l'affichage conditionnel de la masse graisseuse (si disponible)
  - Ajouté l'affichage conditionnel du tour de taille (si disponible)
- Implémenté une architecture de données plus robuste
  - Créé la classe HealthEntryWithUser pour associer les entrées de santé avec les utilisateurs
  - Mis à jour le DAO pour récupérer les entrées avec les informations des utilisateurs
  - Mis à jour le Repository pour exposer les nouvelles méthodes
  - Mis à jour le ViewModel pour gérer les entrées avec les informations des utilisateurs
- Amélioré l'interface utilisateur
  - Modifié l'adaptateur pour afficher les informations de manière conditionnelle
  - Ajouté des styles visuels pour distinguer les différentes informations
  - Optimisé l'affichage pour masquer les champs vides
- Réorganisé les mesures de base dans l'activité "Modifier le suivi de santé"
  - Modifié l'ordre des champs pour avoir : Masse, Masse graisseuse, puis tour de taille
  - Mis à jour le fichier XML de mise en page (activity_entry.xml)
  - Mis à jour la logique dans EntryActivity.kt pour refléter le nouvel ordre
  - Mis à jour le ViewModel pour maintenir la cohérence des données
- Testé toutes les modifications sur un appareil Pixel 7a

### April 30, 2025 (suite)
- Amélioré le champ "Date et heure" dans l'activité de Suivi de Santé
  - Ajouté l'initialisation automatique avec la date et l'heure actuelles lors de l'ouverture de l'activité
  - Modifié le sélecteur pour permettre de choisir à la fois la date ET l'heure
  - Implémenté un flux en deux étapes : d'abord sélection de la date, puis sélection de l'heure
  - Ajouté les chaînes de ressources nécessaires en français et en anglais
  - Mis à jour la logique de sauvegarde pour utiliser la date et l'heure sélectionnées
- Testé les modifications sur un appareil Pixel 7a

### May 5, 2025
- Finalisé la localisation française de l'application
  - Vérifié que tous les textes de l'interface utilisateur sont correctement traduits
  - Confirmé le bon fonctionnement de l'affichage des métriques avec le format français
  - Validé que tous les messages d'erreur et notifications sont en français
- Réorganisé l'ordre des mesures de base dans les activités d'ajout et de modification de données
  - Modifié l'ordre pour avoir : Masse (kg), Masse graisseuse (kg), puis Tour de taille (cm)
  - Mis à jour les fichiers XML de mise en page (activity_add_entry.xml, activity_add_entry_multi_user.xml, activity_entry.xml)
  - Assuré la cohérence de l'ordre dans toutes les interfaces utilisateur
- Changé l'unité de masse graisseuse de pourcentage (%) à kilogrammes (kg)
  - Ajouté de nouvelles chaînes de ressources pour la masse graisseuse en kg
  - Mis à jour l'affichage des valeurs de masse graisseuse dans toute l'application
  - Modifié les étiquettes et les unités dans les interfaces de saisie et d'affichage
- Confirmé la suppression complète des fonctionnalités non désirées
  - Vérifié l'absence de toute référence au mode sombre dans le code et les ressources
  - Confirmé la suppression de toutes les fonctionnalités de notification
  - Validé que l'interface utilisateur ne contient plus d'options pour ces fonctionnalités
- Effectué des tests finaux sur un appareil Pixel 7a
  - Testé le flux complet d'ajout et de modification des entrées de santé
  - Vérifié l'affichage correct des données utilisateur et des métriques
  - Confirmé le bon fonctionnement du sélecteur de date et d'heure
