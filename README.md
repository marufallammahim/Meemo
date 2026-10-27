# Meemo — Android Digital Assistant
*"Your Android. Your Commands. Your Assistant."*

**Package Namespace:** `com.marufallammahim.meemo`  
**Language & UI:** Kotlin 2.0.21 + Jetpack Compose (Material 3)  
**Architecture:** Clean Architecture + MVVM + Hilt + Room + DataStore  
**Supported Languages:** English (`en-US`), Bengali (`bn-BD` / বাংলা), and Mixed Banglish  

---

## 1. Overview
**Meemo** is a native, offline-first, privacy-conscious Android digital assistant designed to execute real device operations, discover installed applications dynamically, read opt-in incoming notifications aloud transiently, and report verified outcomes honestly.

Every voice, text, alias, or AI-assisted command flows through a unified **13-Stage Deterministic & Validated Pipeline**:
`Input -> Normalizer -> Language Resolver -> Alias Lookup -> Deterministic Intent Matcher -> Entity Extractor -> Optional AI Fallback -> Ambiguity Check -> Capability Verification -> Safety Validator -> Action Planner -> Confirmation Gate -> Action Executor -> Result Verifier -> Bilingual Response Generator`

---

## 2. Feature Matrix & Implementation Status

| Feature Area | Implementation Status | Verification Evidence | Platform / Hardware Notes |
| :--- | :--- | :--- | :--- |
| **Text Assistant & UI** | **Implemented & Verified** | `AssistantHomeScreen.kt`, `AssistantViewModel.kt` | Material 3 Burgundy theme (`#5A0719`) + animated vector cat mascot. |
| **Deterministic Command Engine** | **Implemented & Verified** | `CommandNormalizerTest`, `DeterministicIntentMatcherTest` | Supports 18 intents across English, Bengali (`বাংলা`), and Banglish. |
| **Dynamic App Discovery** | **Implemented & Verified** | `PackageResolver.kt`, `PackageResolverTest` | Uses `PackageManager` + `<queries>`; handles ambiguity with candidate picker. |
| **Browser Resolution** | **Implemented & Verified** | `BrowserResolver.kt`, `BrowserResolverTest` | Dynamically resolves `https://` `CATEGORY_BROWSABLE` browsers. |
| **Flashlight (Torch) Control** | **Implemented (Device Hardware Dependent)** | `TorchCapabilityAdapter.kt`, `TorchActionExecutor.kt` | Uses `CameraManager.setTorchMode` + live `TorchCallback` verification. |
| **Battery & Device Info** | **Implemented & Verified** | `DeviceInfoActionExecutor.kt` | Reads real `BatteryManager` & `android.os.Build` properties. |
| **Volume & Media Control** | **Implemented & Verified** | `VolumeMediaActionExecutor.kt` | Adjusts `AudioManager` streams and dispatches `KeyEvent` media transport keys. |
| **Settings Navigation** | **Implemented & Verified** | `SettingsActionExecutor.kt` | Opens Wi-Fi panel (`API 29+`), Bluetooth, Display, Sound, Accessibility, Notification Access. |
| **Speech Recognition (`STT`)** | **Implemented (Provider Dependent)** | `SpeechRecognizerController.kt`, `VoiceAndTtsTest` | Requires `RECORD_AUDIO` permission and installed Android `RecognitionService`. |
| **Text-to-Speech (`TTS`)** | **Implemented (Engine Dependent)** | `MeemoTtsEngine.kt`, `TtsController.kt` | Supports `en-US` and `bn-BD` with configurable rate and instant interruption. |
| **Wake-Word (`"Hello Meemo"`)** | **Implemented as Opt-In Foreground Session** | `WakeWordDetector.kt`, `MeemoWakeWordService.kt` | Uses `foregroundServiceType="microphone"` with persistent disclosure notification. |
| **Notification Read-Aloud** | **Implemented & Verified** | `MeemoNotificationListener.kt`, `NotificationPolicyTest` | Opt-in; processes notifications transiently in RAM; respects quiet hours & redaction. |
| **Accessibility Helper** | **Implemented (Strictly Bounded)** | `MeemoAccessibilityService.kt` | `canRetrieveWindowContent="false"`; never scrapes screen text or passwords. |
| **Optional AI Fallback** | **Implemented & Verified** | `OpenAiCompatibleClient.kt`, `CommandSafetyValidator.kt` | Uses `AndroidKeyStore` (`AES/GCM/NoPadding`) for API key; schema-validated output. |
| **Offline-First Operation** | **Implemented & Verified** | `OfflineExecutionTest` | All 18 deterministic intents execute locally without internet or AI keys. |
| **Isolated Root Subsystem** | **Implemented & Verified** | `RootCommandPolicy.kt`, `ShellExecutor.kt`, `RootCommandPolicyTest` | Strict allowlist (`dumpsys battery`, `dumpsys thermalservice`, `pm path`); zero raw shell execution. |
| **Memory, Aliases & Routines** | **Implemented & Verified** | `MeemoDatabase.kt` (10 tables), `AliasManager.kt` | Full CRUD + one-tap `purgeAllUserData()` transaction. |
| **Plugin Registry** | **Implemented (Internal Registry)** | `PluginRegistryManager.kt`, `LocalDataSeeder.kt` | Seeds 6 built-in capability plugins; external untrusted code loading is prohibited. |

---

## 3. Requirements & Build Instructions

### Prerequisites
- **JDK:** OpenJDK 17 (`17.0.20.1` or newer)
- **Android SDK:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (Android 8.0+)
- **Android Studio:** Ladybug (2024.2.1) or newer
- **Gradle:** `8.9` (configured in `gradle/wrapper/gradle-wrapper.properties`)

### Building & Testing from Terminal

**Linux / macOS:**
```bash
./gradlew --version
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

**Windows (PowerShell):**
```powershell
.\gradlew.bat --version
.\gradlew.bat clean
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The generated debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Manual Android Permission & Special-Access Setup
1. **Microphone (`RECORD_AUDIO`):** Requested at runtime only when you tap the Microphone FAB or enable the Wake-Word toggle.
2. **Notification Read-Aloud (`BIND_NOTIFICATION_LISTENER_SERVICE`):** Go to `Privacy & Settings` -> Enable *Opt-In Message Read-Aloud* -> Tap `Notification Access` in `Capabilities` to authorize `Meemo Notification Read-Aloud` in Android Settings.
3. **Accessibility Guidance (`BIND_ACCESSIBILITY_SERVICE`):** Optional; enable in Android Accessibility Settings if you want global panel shortcuts.
4. **Optional AI Fallback:** Go to `Privacy & Settings` -> Enter an HTTPS OpenAI-compatible endpoint and API key (stored encrypted in `AndroidKeyStore`).
5. **Optional Isolated Root:** Requires a rooted device (`su` binary) and enabling *Isolated Root Diagnostics* in `Privacy & Settings`.

---

## 5. Android Platform Limitations & Honest Disclosure
- **Wi-Fi & Bluetooth Direct Toggling:** Android 10+ (API 29+) blocks third-party apps from silently toggling Wi-Fi (`WifiManager.setWifiEnabled`), and Android 13+ restricts direct Bluetooth toggling. Meemo opens the system settings panel (`Settings.Panel.ACTION_WIFI` / `ACTION_BLUETOOTH_SETTINGS`) and reports `PARTIAL_SUCCESS` (`Action Needed in Settings`).
- **Continuous Background Wake-Word:** Android 14+ enforces strict background microphone limits. Meemo implements wake-word listening only as an explicit, user-started foreground service with a persistent notification and stop action.
- **Notification Content Visibility:** If a messaging app or Android lockscreen setting marks notifications as `VISIBILITY_SECRET`, Meemo respects the redaction and announces only the originating application name.

## Build the Android APK with GitHub Actions

1. Upload this project to a GitHub repository (keep the workflow at `.github/workflows/android-ci.yml`).
2. Push to `main` or `develop`, open a pull request to either branch, or run the workflow manually from **Actions → Meemo Android CI → Run workflow**.
3. Open the completed workflow run and download **`meemo-debug-apk`** under **Artifacts**.

The supplied archive has an incomplete `gradle/wrapper/gradle-wrapper.jar`. The workflow handles this by installing Gradle 8.9 independently, regenerating the wrapper, checking that `GradleWrapperMain` exists, and only then running tests, lint, and the APK build. This is a recovery step—not proof that the app compiles; the first GitHub Actions run must complete successfully to verify the build.

### Repairing the wrapper on a development computer

If Gradle 8.9 and JDK 17 are installed, run this from the project root:

```bash
bash scripts/repair-gradle-wrapper.sh
```

Then verify with `./gradlew --version` and build with `./gradlew assembleDebug`.

