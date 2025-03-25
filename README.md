# Personal Health Tracker - Android App

## Project Overview
An Android application for tracking personal health metrics including:
- Body weight
- Waist measurements
- Health remarks/notes
- Timestamps for all entries
- SQLite database with Docker container for data storage
- Grafana integration for data visualization

## Technical Requirements

### Development Environment
- Android Studio (latest version)
- Minimum Android SDK: API 24 (Android 7.0)
- Target Android SDK: API 34 (Android 14)
- Kotlin programming language

### Dependencies
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

## Backend Architecture

### Database Structure
- SQLite database with Docker container
- Structured schema for workouts with flexible fields
- JSON support for custom data
- Tag-based categorization
- Free-form notes

### Database Schema
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

### Data Flow
1. Android app collects workout data
2. Data is synchronized with SQLite database
3. Grafana connects to SQLite for visualization
4. Data can be exported for analysis

## Project Structure

### 1. Data Models
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

### 2. Database Layer
- Use Room database for local storage
- Create DAOs (Data Access Objects) for CRUD operations
- Implement repository pattern for data management
- Sync with Dockerized SQLite database

### 3. UI Components
1. Main Activity
   - List of health entries
   - Add new entry button
   - Sync status indicator

2. Add/Edit Entry Screen
   - Date/time picker
   - Weight input field
   - Metric selection
   - Note input
   - Tag selection

### 4. Visualization
- Grafana dashboards for:
  - Workout history
  - Progress tracking
  - Statistical analysis
  - Trend visualization

## Setup Instructions

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
