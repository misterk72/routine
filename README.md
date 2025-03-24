# Personal Health Tracker - Android App

## Project Overview
An Android application for tracking personal health metrics including:
- Body weight
- Waist measurements
- Health remarks/notes
- Timestamps for all entries
- Nextcloud integration for data storage

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
    
    // Nextcloud Android Library
    implementation 'com.github.nextcloud:android-library:2.1.0'
    
    // Coroutines for async operations
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // ViewModel and LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
}
```

## Project Structure

### 1. Data Models
- Create data classes for health metrics:
  ```kotlin
  data class HealthEntry(
      val id: Long,
      val timestamp: Long,
      val weight: Float?,
      val waistCircumference: Float?,
      val healthNote: String?,
      val syncStatus: Boolean
  )
  ```

### 2. Database Layer
- Use Room database for local storage
- Create DAOs (Data Access Objects) for CRUD operations
- Implement repository pattern for data management

### 3. Nextcloud Integration
- Implement Nextcloud authentication
- Create sync service for data backup
- Store data in JSON format on Nextcloud
- Handle offline capabilities

### 4. UI Components
1. Main Activity
   - List of health entries
   - Add new entry button
   - Sync status indicator

2. Add/Edit Entry Screen
   - Date/time picker
   - Weight input field
   - Waist measurement input field
   - Notes text area
   - Save button

3. Settings Screen
   - Nextcloud credentials
   - Sync frequency settings
   - Units preference (kg/lbs, cm/inches)

## Implementation Steps

1. **Project Setup**
   - Create new Android project in Android Studio
   - Configure build.gradle with required dependencies
   - Set up version control (Git)

2. **Database Implementation**
   - Create Room database classes
   - Implement data models
   - Create DAOs and Repository

3. **UI Development**
   - Design and implement main activity layout
   - Create add/edit entry screen
   - Implement settings screen
   - Add form validation

4. **Nextcloud Integration**
   - Implement Nextcloud authentication
   - Create sync service
   - Add background sync functionality
   - Handle error cases and offline mode

5. **Testing**
   - Unit tests for database operations
   - Integration tests for Nextcloud sync
   - UI testing for main flows

6. **Polishing**
   - Add data visualization (optional)
   - Implement data export feature
   - Add notifications for sync status
   - Error handling and user feedback

## Security Considerations
- Encrypt sensitive data in local storage
- Secure Nextcloud credentials
- Implement proper authentication flow
- Handle API tokens securely

## Future Enhancements
- Data visualization and trends
- Export to CSV/PDF
- Reminder notifications
- Health metrics goals
- Progress photos
- BMI calculation
- Integration with other health platforms

## Getting Started
1. Clone this repository
2. Open project in Android Studio
3. Configure your Nextcloud instance in settings
4. Build and run the application

## Notes
- Ensure you have a Nextcloud instance set up and accessible
- The app requires Android 7.0 or higher
- Internet permission is required for sync functionality
