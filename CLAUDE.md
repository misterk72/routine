# Claude's Guide to the HealthTracker Project

This document provides guidance for AI assistants on how to contribute to the HealthTracker project. It covers the project's architecture, coding conventions, and overall "vibe".

## 1. Project Overview

HealthTracker is a full-stack project consisting of:
1.  An **Android application** (written in Kotlin) for users to log health data (weight, body fat, etc.).
2.  A **backend service** (written in PHP) that syncs data from the Android app to a MariaDB database.
3.  A **Docker setup** to run the backend service and database.

The primary language for comments, logs, and UI text is **French**.

## 2. Architecture

### Android Application
-   **Language**: Kotlin
-   **Architecture**: Model-View-ViewModel (MVVM)
-   **UI**: Jetpack Compose is mentioned in the README, but the layout files suggest it's primarily an XML-based View system. Be mindful of this when working on the UI.
-   **Database**: Room for local persistence.
-   **Asynchronous Operations**: Kotlin Coroutines and `Flow` for managing background threads, and `WorkManager` for scheduled sync tasks.
-   **Dependency Injection**: Hilt is used for managing dependencies.
-   **Networking**: OkHttp for HTTP requests and Gson for JSON parsing.

### Backend Service
-   **Language**: Procedural PHP.
-   **Database**: MariaDB, accessed via PDO.
-   **API**: A single `sync.php` file serves as the main API endpoint. It handles both `GET` (fetch data) and `POST` (upload data) requests.
-   **Environment**: The backend is designed to be run inside a Docker container. The `docker-compose.yml` file in the `docker-sqlite/` directory defines the services.

## 3. Coding Style & "Vibe"

The project's "vibe" is **pragmatic and focused on functionality**. The code is straightforward and not over-engineered.

### General
-   **Language**: All user-facing strings, internal comments, and log messages are in **French**. This is a key characteristic of the project. Maintain this convention.
-   **Commits**: The `DEVLOG.md` shows a history of descriptive commit messages, often in French.

### Kotlin / Android
-   Follow standard Kotlin conventions.
-   Use Hilt for injecting dependencies (`@Inject`).
-   Use Coroutines for any long-running operations to avoid blocking the main thread.
-   Add extensive logging using `android.util.Log` for debugging. Use the `TAG` constants defined in the classes.
-   When adding new features, consider the existing MVVM pattern:
    -   Data classes in `data/`.
    -   DAOs in `data/`.
    -   Repositories in `data/repository/`.
    -   ViewModels in `ui/`.
    -   Activities/UI in `ui/`.

### PHP / Backend
-   The style is procedural. New functionality can be added as new functions.
-   Database interactions are done through the `PDO` extension. Use prepared statements to prevent SQL injection.
-   The `sync.php` script is the single entry point. Use a `switch` statement on `$_SERVER['REQUEST_METHOD']` to handle different HTTP methods.
-   The script includes logic to create/alter tables on the fly. This suggests a preference for a "zero-config" setup. If you modify the database schema, consider if this self-healing mechanism needs updating.
-   Error logging is done via `error_log()`.

## 4. Development Setup

Follow the instructions in `README.md` to get the project running. The key steps are:

1.  **Start the backend:**
    ```bash
    cd docker-sqlite
    docker-compose up --build -d
    ```
2.  **Configure the Android App:**
    -   Open the `HealthTracker` project in Android Studio.
    -   The app needs to know the IP address of the machine running the Docker containers. This is configured in `com.healthtracker.BuildConfig`. The `API_BASE_URL` is likely set up via Gradle properties.

## 5. Testing

The project has both unit and instrumentation tests for the Android app.

-   **Run unit tests:**
    ```bash
    ./gradlew test
    ```
-   **Run instrumentation tests:**
    ```bash
    ./gradlew connectedAndroidTest
    ```

Before submitting any changes, ensure that all existing tests pass. If you add new functionality, please add corresponding tests.
