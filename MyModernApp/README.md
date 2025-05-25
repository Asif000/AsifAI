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

## SMS Forwarding Feature

This application includes a feature to automatically forward incoming SMS messages to a designated phone number.

### How it Works

1.  **Permissions:** The app requires the following permissions to function correctly:
    *   `android.permission.RECEIVE_SMS`: To listen for incoming SMS messages.
    *   `android.permission.READ_SMS`: To read the content of the incoming SMS. (Note: While `RECEIVE_SMS` often provides message content, `READ_SMS` might be needed for full access or future enhancements and is good practice to request if message content is processed).
    *   `android.permission.SEND_SMS`: To forward the SMS to the target phone number.
    These permissions will be requested when the app starts. Please ensure they are granted for the feature to work.

2.  **Target Phone Number Configuration:**
    The phone number to which SMS messages will be forwarded can be configured in two ways:
    *   **Firebase Remote Config (Recommended):**
        *   The primary method for setting the target phone number is through Firebase Remote Config.
        *   In your Firebase project console, navigate to "Remote Config".
        *   Add a parameter with the key `target_sms_forward_number`.
        *   Set the value of this parameter to the desired target phone number (e.g., `+11234567890`).
        *   Publish your Remote Config changes.
        *   The app will fetch this value when it starts (and periodically, based on fetch interval settings). If a valid number is found, it will be used for forwarding.
    *   **Local Fallback UI:**
        *   If Firebase Remote Config is not available, fails to fetch, or does not have `target_sms_forward_number` set, you can set a target phone number directly within the app.
        *   The app provides an input field to enter and save a phone number locally. This number will be used if the remote configuration is not active.
        *   The UI will indicate the source of the currently active target number (Local or Remote).

3.  **Forwarding Process:**
    *   When an SMS is received, if all permissions are granted and a target phone number is configured (either via Remote Config or local fallback), the app will:
        *   Construct a new message in the format: "Fwd from [Original Sender]: [Original Message Body]".
        *   Send this new message to the configured target phone number.

### Firebase Setup (`google-services.json`)

*   This project uses Firebase for Remote Configuration.
*   You **must** replace the placeholder `app/google-services.json` with your own `google-services.json` file from your Firebase project.
    1.  Go to your Firebase project console.
    2.  Add this Android app to your project (if you haven't already) using the package name `com.example.mymodernapp`.
    3.  Download the `google-services.json` file.
    4.  Place it in the `MyModernApp/app/` directory, replacing the existing placeholder.
*   **The app will not build correctly or connect to Firebase services without the correct `google-services.json` file.**
