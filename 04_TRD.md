# 04_TRD.md — Technical Requirements Document
**Project Name:** Meemo — Android Digital Assistant  
**Package Namespace:** `com.marufallammahim.meemo`  
**Document Version:** 1.0.0  

---

## A. Verified Technology Stack & Dependency Matrix

All versions are locked in `gradle/libs.versions.toml` and verified for mutual compatibility with **JDK 17** and **Gradle 8.9**. Only stable releases are used (zero alpha/beta libraries; encryption uses Android's built-in `AndroidKeyStore` + `AES/GCM/NoPadding` directly).

| Component / Library | Coordinates / Artifact | Locked Version | Purpose & Compatibility Justification |
| :--- | :--- | :--- | :--- |
| **JDK** | OpenJDK 17 (`JavaVersion.VERSION_17`) | `17.0.20.1` | Required by AGP 8.7.x and Kotlin 2.0.x; verified installed in environment. |
| **Gradle Wrapper** | `gradle-8.9-bin.zip` | `8.9` | Official compatible Gradle release for AGP 8.7.3. |
| **Android Gradle Plugin** | `com.android.application` | `8.7.3` | Stable AGP supporting `compileSdk 35` and `targetSdk 35`. |
| **Kotlin** | `org.jetbrains.kotlin.android` | `2.0.21` | Stable Kotlin 2.0 release with K2 compiler and Compose compiler plugin. |
| **Kotlin Compose Plugin** | `org.jetbrains.kotlin.plugin.compose` | `2.0.21` | Official Compose compiler plugin integrated with Kotlin 2.0+. |
| **Kotlin Serialization** | `org.jetbrains.kotlin.plugin.serialization` | `2.0.21` | Compile-time JSON & typed navigation route serialization. |
| **KSP** | `com.google.devtools.ksp` | `2.0.21-1.0.28` | Kotlin Symbol Processing for Room and Hilt code generation. |
| **Android SDK** | `minSdk` / `targetSdk` / `compileSdk` | `26` / `35` / `35` | API 26 (Android 8.0) to API 35 (Android 15). |
| **Jetpack Compose BOM** | `androidx.compose:compose-bom` | `2024.10.01` | Locks mutually compatible stable Compose UI, Foundation, Graphics, and Material3 versions. |
| **Material 3** | `androidx.compose.material3:material3` | BOM (`1.3.1`) | Material 3 components and dynamic/custom color schemes. |
| **Compose Navigation** | `androidx.navigation:navigation-compose` | `2.8.4` | Type-safe Kotlin Serialization route navigation. |
| **Lifecycle & ViewModel** | `androidx.lifecycle:lifecycle-*-ktx` / `-compose` | `2.8.7` | Lifecycle-aware `StateFlow` collection (`collectAsStateWithLifecycle`) and `ViewModel`. |
| **Activity Compose** | `androidx.activity:activity-compose` | `1.9.3` | Edge-to-edge Compose host activity and permission launchers. |
| **Core KTX** | `androidx.core:core-ktx` | `1.15.0` | Kotlin extensions for Android framework APIs. |
| **Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` | Asynchronous execution, `Flow`, and structured concurrency. |
| **Serialization JSON** | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.7.3` | Schema validation for AI responses, routine definitions, and database JSON columns. |
| **Dagger Hilt** | `com.google.dagger:hilt-android` | `2.52` | Compile-time dependency injection across ViewModels, Repositories, and Services. |
| **Hilt Navigation Compose** | `androidx.hilt:hilt-navigation-compose` | `1.2.0` | `hiltViewModel()` integration with Compose Navigation. |
| **Room Database** | `androidx.room:room-runtime` / `room-ktx` / `room-compiler` | `2.6.1` | Local relational SQLite persistence with Flow queries and explicit migrations. |
| **DataStore Preferences** | `androidx.datastore:datastore-preferences` | `1.1.1` | Fast, coroutine-safe key-value storage for theme, language, and quick toggles. |
| **WorkManager** | `androidx.work:work-runtime-ktx` | `2.10.0` | Reliable background maintenance (e.g., retention cleanup and scheduled routines). |
| **Retrofit 2** | `com.squareup.retrofit2:retrofit` | `2.11.0` | Optional HTTP client for user-configured AI provider interpretation. |
| **OkHttp 3** | `com.squareup.okhttp3:okhttp` | `4.12.0` | Timeouts, TLS enforcement, and redacted network interceptors. |
| **Unit Testing** | `junit:junit` / `io.mockk:mockk` / `app.cash.turbine:turbine` | `4.13.2` / `1.13.13` / `1.2.0` | Deterministic JVM unit tests for command engine, policies, and ViewModels. |
| **Android / UI Testing** | `androidx.test.ext:junit` / `espresso-core` / `ui-test-junit4` | `1.2.1` / `3.6.1` / BOM | Instrumentation tests for Room migrations, DAOs, and Compose UI flows. |

---

## B. Build Configuration & Android API Strategy

- **Namespace & Application ID:** `com.marufallammahim.meemo`
- **SDK Levels:**
  - `minSdk = 26` (Android 8.0 Oreo): Provides `NotificationChannel`, `CameraManager.setTorchMode`, `BatteryManager` properties, `AudioManager.dispatchMediaKeyEvent`, and `startForegroundService`.
  - `targetSdk = 35` & `compileSdk = 35` (Android 15): Full compliance with Android 14/15 foreground service types (`microphone` for optional wake word) and package visibility (`<queries>`).
- **API-Level Compatibility Matrix:**
  - **API 26–28:** Direct Wi-Fi state inspection; standard volume, torch, battery, notifications, and app launching.
  - **API 29+ (Android 10+):** Direct programmatic Wi-Fi toggling (`setWifiEnabled`) is restricted by Android OS; Meemo opens `Settings.Panel.ACTION_WIFI` or `Settings.ACTION_WIFI_SETTINGS` and reports `ActionResult.PartialSuccess`.
  - **API 30+ (Android 11+):** Package visibility requires explicit `<queries>` in `AndroidManifest.xml` for `Intent.ACTION_MAIN` (`CATEGORY_LAUNCHER`) and `Intent.ACTION_VIEW` (`https`).
  - **API 33+ (Android 13+):** Requires `POST_NOTIFICATIONS` runtime permission for foreground service notifications.
  - **API 34+ (Android 14+):** Requires `FOREGROUND_SERVICE_MICROPHONE` declaration when the user opts into the foreground wake-word detector service.

---

## C. Layered Clean Architecture & Confidence Thresholds

### 1. Dependency Rule
`presentation` -> `assistant` / `domain` <- `data` / `capabilities` / `actions` / `root` / `notifications` / `voice` / `tts`

### 2. Command Confidence Thresholds
- **Exact Deterministic Match (`>= 0.90`):** Immediate capability check and execution (or confirmation if the capability requires confirmation).
- **High-Confidence Fuzzy / Alias Match (`0.75 .. 0.89`):** Executes for safe read-only/launch commands; requests confirmation (`AWAITING_CONFIRMATION`) if multiple apps have similar scores within `0.08` delta (`ActionResult.Ambiguous`).
- **Low-Confidence Local Match (`0.45 .. 0.74`):** If optional AI provider is enabled and online, queries `AiInterpreter` for structured intent extraction; otherwise asks user for clarification.
- **Unrecognized (`< 0.45`):** Returns `ActionResult.Failed(MeemoError.UnrecognizedCommand)` with suggested valid commands in the user's active language.

---

## D. Intended Repository Folder Structure

```text
Meemo/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── mascot/
│       │   │       ├── meemo_mascot_idle.svg
│       │   │       └── meemo_mascot_listening.svg
│       │   ├── java/com/marufallammahim/meemo/
│       │   │   ├── MeemoApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── di/
│       │   │   │   ├── AppModule.kt
│       │   │   │   ├── DatabaseModule.kt
│       │   │   │   └── CapabilityModule.kt
│       │   │   ├── core/
│       │   │   │   ├── common/DispatcherProvider.kt
│       │   │   │   ├── error/MeemoError.kt
│       │   │   │   ├── logging/SafeLogger.kt
│       │   │   │   ├── permissions/PermissionChecker.kt
│       │   │   │   ├── result/ResultWrapper.kt
│       │   │   │   ├── security/EncryptedCredentialStore.kt
│       │   │   │   └── utilities/TimeProvider.kt
│       │   │   ├── data/
│       │   │   │   ├── database/
│       │   │   │   │   ├── MeemoDatabase.kt
│       │   │   │   │   ├── MeemoMigrations.kt
│       │   │   │   │   ├── dao/MeemoDaos.kt
│       │   │   │   │   └── entity/MeemoEntities.kt
│       │   │   │   ├── datastore/MeemoPreferencesDataStore.kt
│       │   │   │   ├── local/LocalDataSeeder.kt
│       │   │   │   ├── remote/OpenAiCompatibleClient.kt
│       │   │   │   └── repository/MeemoRepositoryImpl.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/DomainModels.kt
│       │   │   │   ├── repository/MeemoRepository.kt
│       │   │   │   ├── usecase/ProcessCommandUseCase.kt
│       │   │   │   └── validation/CommandSafetyValidator.kt
│       │   │   ├── assistant/
│       │   │   │   ├── command/NormalizedCommand.kt
│       │   │   │   ├── context/ConversationContextManager.kt
│       │   │   │   ├── engine/AssistantEngine.kt
│       │   │   │   ├── engine/AiInterpreter.kt
│       │   │   │   ├── entity/EntityExtractor.kt
│       │   │   │   ├── intent/MeemoIntent.kt
│       │   │   │   ├── intent/DeterministicIntentMatcher.kt
│       │   │   │   ├── parser/CommandNormalizer.kt
│       │   │   │   ├── parser/LanguageResolver.kt
│       │   │   │   ├── planning/ActionPlanner.kt
│       │   │   │   └── response/ResponseGenerator.kt
│       │   │   ├── capabilities/
│       │   │   │   ├── CapabilityManager.kt
│       │   │   │   ├── CapabilityRegistry.kt
│       │   │   │   ├── CapabilityResult.kt
│       │   │   │   └── adapters/PlatformCapabilityAdapters.kt
│       │   │   ├── actions/
│       │   │   │   ├── Action.kt
│       │   │   │   ├── ActionDispatcher.kt
│       │   │   │   ├── ActionExecutor.kt
│       │   │   │   ├── ActionResult.kt
│       │   │   │   ├── app/AppActionExecutors.kt
│       │   │   │   ├── device/DeviceActionExecutors.kt
│       │   │   │   ├── media/MediaActionExecutors.kt
│       │   │   │   └── settings/SettingsActionExecutors.kt
│       │   │   ├── apps/
│       │   │   │   ├── AppManager.kt
│       │   │   │   ├── BrowserResolver.kt
│       │   │   │   └── PackageResolver.kt
│       │   │   ├── voice/
│       │   │   │   ├── SpeechRecognizerController.kt
│       │   │   │   ├── VoiceController.kt
│       │   │   │   ├── VoiceState.kt
│       │   │   │   └── WakeWordDetector.kt
│       │   │   ├── tts/
│       │   │   │   ├── MeemoTtsEngine.kt
│       │   │   │   └── TtsController.kt
│       │   │   ├── notifications/
│       │   │   │   ├── MeemoNotificationListener.kt
│       │   │   │   ├── NotificationPolicy.kt
│       │   │   │   ├── NotificationProcessor.kt
│       │   │   │   └── NotificationReadAloud.kt
│       │   │   ├── accessibility/
│       │   │   │   └── MeemoAccessibilityService.kt
│       │   │   ├── root/
│       │   │   │   ├── RootCapability.kt
│       │   │   │   ├── RootCommandPolicy.kt
│       │   │   │   ├── RootManager.kt
│       │   │   │   └── ShellExecutor.kt
│       │   │   ├── memory/AliasManager.kt
│       │   │   ├── automation/AutomationEngine.kt
│       │   │   ├── plugins/PluginRegistryManager.kt
│       │   │   ├── services/MeemoWakeWordService.kt
│       │   │   ├── permissions/PermissionCoordinator.kt
│       │   │   ├── settings/SettingsCoordinator.kt
│       │   │   └── presentation/
│       │   │       ├── components/MeemoComponents.kt
│       │   │       ├── components/MeemoMascot.kt
│       │   │       ├── navigation/MeemoDestination.kt
│       │   │       ├── navigation/MeemoNavHost.kt
│       │   │       ├── screens/AssistantViewModel.kt
│       │   │       ├── screens/FeatureViewModels.kt
│       │   │       ├── screens/AssistantHomeScreen.kt
│       │   │       ├── screens/OnboardingScreen.kt
│       │   │       ├── screens/SecondaryScreens.kt
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Dimens.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── mipmap-anydpi-v26/
│       │       ├── values/
│       │       ├── values-bn/
│       │       └── xml/
│       ├── test/java/com/marufallammahim/meemo/
│       └── androidTest/java/com/marufallammahim/meemo/
├── docs/
├── gradle/
├── .github/workflows/android-ci.yml
├── 01_PRD.md .. 06_Plan.md
├── Gimini.md, .cursorrules, README.md, LICENSE, CONTRIBUTING.md, SECURITY.md
├── build.gradle.kts, settings.gradle.kts, gradle.properties, gradlew, gradlew.bat
```

---

## E. Third-Party & External Service Contracts

1. **Optional AI Interpretation Service (`OpenAiCompatibleClient`):**
   - **Optionality:** 100% optional; disabled by default. All 18 deterministic intents operate locally without it.
   - **Credentials:** User-supplied API key stored encrypted via `AndroidKeyStore` (`EncryptedCredentialStore.kt`); never hardcoded or committed.
   - **Timeouts & Retries:** Connect timeout `8s`, Read timeout `12s`, Write timeout `8s`; maximum `1` automatic retry on transient `503`/`429` with `1500ms` backoff.
   - **Contract Validation:** Requires strict JSON output matching `AiStructuredInterpretation(intentId, entities, confidence, explanation)`. Unknown intent names or shell payloads are rejected by `CommandSafetyValidator`.

---

## F. Security & Observability Constraints

1. **Zero Arbitrary Shell Execution:** `ShellExecutor` never invokes `sh -c <user_string>`. It accepts only an approved `RootCommandSpec` looked up from `RootCommandPolicy` with regex-validated parameter tokens passed as an argument list to `ProcessBuilder("su", "-c", validatedCommand)` with a strict `5000ms` timeout and `16 KB` output cap.
2. **Redacted Structured Logging (`SafeLogger`):** Automatically masks patterns matching `Bearer [A-Za-z0-9._-]+`, `sk-[A-Za-z0-9]+`, email addresses, phone numbers, and any field tagged `SENSITIVE_NOTIFICATION_BODY`.
3. **Exported Components:** `MainActivity` is exported (`android:exported="true"`). `MeemoNotificationListener` and `MeemoAccessibilityService` are protected by system signature permissions (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` and `android.permission.BIND_ACCESSIBILITY_SERVICE`). `MeemoWakeWordService` is `android:exported="false"`.
