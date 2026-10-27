# Build, Test & Verification Guide (`docs/development/BUILD_AND_TEST.md`)

## 1. Local Android Studio Setup
1. Extract `Meemo-Android-Digital-Assistant.zip`.
2. Open the `Meemo/` folder in Android Studio (Ladybug 2024.2.1+ with JDK 17).
3. Ensure Android SDK Platform 35 is installed in SDK Manager.
4. Run `./gradlew testDebugUnitTest assembleDebug`.

## 2. Running Automated Tests
- **JVM Unit Tests:** `./gradlew testDebugUnitTest`
- **Instrumentation / Room Migration / Compose UI Tests:** `./gradlew connectedDebugAndroidTest` (requires connected Android device or emulator API 26–35).
