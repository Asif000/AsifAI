# MyModernApp

This is a modern Android application built with Kotlin and Jetpack Compose.

## Prerequisites

- Android Studio (latest stable version recommended)
- Android SDK Platform corresponding to `compileSdk` version in `app/build.gradle.kts` (currently 34)
- JDK 1.8 or higher

## Building the Project

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd MyModernApp
    ```
2.  **Open in Android Studio:**
    - Launch Android Studio.
    - Select "Open an existing Android Studio project".
    - Navigate to and select the cloned `MyModernApp` directory.
3.  **Sync Gradle:**
    - Android Studio should automatically sync the project with Gradle. If not, click "Sync Project with Gradle Files" (often represented by an elephant icon).
4.  **Build the App:**
    - From the menu bar, select "Build" > "Make Project" or "Build" > "Build Bundle(s) / APK(s)" > "Build APK(s)".

## Running the App

1.  **Set up an Android Virtual Device (AVD) or connect a physical device:**
    - In Android Studio, go to "Tools" > "AVD Manager" to create or manage AVDs.
    - Ensure your physical device has USB debugging enabled.
2.  **Select the deployment target:**
    - Choose your AVD or connected device from the dropdown menu in the toolbar.
3.  **Run the App:**
    - Click the "Run 'app'" button (green play icon) in the toolbar or select "Run" > "Run 'app'" from the menu bar.

## Project Structure

-   `app/`: The main application module.
    -   `src/main/java/`: Kotlin source code.
    -   `src/main/res/`: Application resources (layouts, drawables, strings, etc.).
    -   `src/main/AndroidManifest.xml`: Application manifest file.
    -   `build.gradle.kts`: App-level Gradle build script.
-   `build.gradle.kts`: Project-level Gradle build script.
-   `settings.gradle.kts`: Gradle settings script.
-   `gradle/libs.versions.toml`: Version catalog for dependencies.
