# 01_PRD.md — Product Requirements Document
**Project Name:** Meemo — Android Digital Assistant  
**Tagline:** *"Your Android. Your Commands. Your Assistant."*  
**Package Namespace:** `com.marufallammahim.meemo`  
**Platform:** Native Android (Kotlin + Jetpack Compose)  
**Document Version:** 1.0.0  
**Status:** Baseline Verified Specification  

---

## A. Executive Summary
**Meemo** is an independent, privacy-first, native Android digital assistant engineered in Kotlin and Jetpack Compose. It enables users to interact with their Android device through natural-language speech or typed commands in **English**, **Bengali (বাংলা)**, or mixed **Bengali-English (Banglish)**.

Unlike opaque cloud-only assistants or toy chat interfaces that simulate responses, Meemo operates on a strict **Deterministic Capability & Verification Pipeline**:
1. Capture speech (via manual trigger or optional foreground wake-word detection) or typed text.
2. Normalize input and resolve user-defined aliases.
3. Match intents deterministically offline first, with an optional schema-validated AI fallback for complex phrasing.
4. Check Android version compatibility, hardware presence, runtime permissions, and special access states via the `CapabilityManager`.
5. Execute the action using the least-privileged official Android API (or isolated, policy-approved Accessibility/Root adapters when explicitly authorized).
6. Verify the actual hardware or system outcome before reporting results via UI cards and Text-to-Speech (TTS).

---

## B. Problem Statement
Everyday Android device tasks—toggling the flashlight, checking battery health, opening specific installed browsers or regional apps, adjusting audio streams, jumping into deeply nested system settings, or hearing incoming message notifications while hands-free—often require navigating multi-layered settings menus and disparate launcher pages.

Existing solutions suffer from three major gaps:
1. **Cloud Dependency & Privacy Friction:** Conventional assistants often require cloud connectivity and broad account telemetry even for local device actions like toggling the torch or launching an installed app.
2. **Bilingual & Regional Friction:** Users in Bangladesh and South Asia frequently mix Bengali script (`ফ্ল্যাশলাইট চালু করো`), transliterated Banglish (`flashlight on koro`), and English (`turn on the flashlight`). Standard assistants often fail when commands mix scripts or colloquial phrasing.
3. **Dishonest or Opaque Capability Reporting:** Many utility apps either fail silently when Android API restrictions block an operation (e.g., direct Wi-Fi toggling on Android 10+) or make exaggerated claims about root/automation capabilities.

Meemo solves these problems by combining **dynamic application discovery**, **offline-first deterministic command parsing**, **permission-aware execution with transparent fallbacks**, and **strict verification of outcomes**.

---

## C. Target Personas

| Persona | Profile & Language Preference | Core Needs | Device Profile |
| :--- | :--- | :--- | :--- |
| **1. Everyday Bilingual Commuter (Primary)** | Speaks Bengali & English interchangeably; uses messaging apps and quick utilities on the go. | Fast voice/text app launching, torch control, volume adjustment, and opt-in message read-aloud over headphones. | Non-rooted Android 10–15 smartphone. |
| **2. Hands-Free & Accessibility-Focused User** | Prefers spoken feedback and minimal touch navigation; benefits from high-contrast UI and large touch targets. | Clear TTS responses, notification read-aloud with quiet hours, and guided settings navigation. | Non-rooted Android 9–15 device with TalkBack or large font scaling. |
| **3. Privacy-Conscious Offline User** | Avoids cloud AI keys and permanent message logging; wants deterministic local control. | 100% offline execution of core device commands, local alias management, and one-tap data deletion. | Non-rooted Android 8.0+ device, frequently offline. |
| **4. Power / Rooted Enthusiast** | Technical user with Magisk/KernelSU who wants safe, auditable privileged commands without arbitrary shell risks. | Isolated, allowlisted root actions (e.g., system battery stats inspection, safe service checks) with explicit confirmation. | Rooted Android 11–15 device. |

---

## D. Product Goals

### Milestone 1: Initial Buildable & Verifiable Release (Current Scope)
- Complete Clean Architecture + MVVM foundation in a single well-structured `:app` module (`com.marufallammahim.meemo`).
- Full offline deterministic command engine supporting English, Bengali, and mixed phrasing across 18 core intents.
- Dynamic application discovery (`PackageManager` + `<queries>`) and default browser resolution.
- Real hardware/platform adapters for Camera2 Torch (`CameraManager`), Battery (`BatteryManager` + `ACTION_BATTERY_CHANGED`), Audio (`AudioManager`), Settings navigation (`android.provider.Settings`), Speech Recognition (`SpeechRecognizer`), and Text-to-Speech (`TextToSpeech`).
- Opt-in `MeemoNotificationListener` (`NotificationListenerService`) with transient privacy-first message read-aloud, package allowlist/blocklist, duplicate suppression, and quiet hours.
- Opt-in `MeemoAccessibilityService` for user-authorized interface assistance and settings guidance.
- Optional, strictly isolated Root subsystem (`RootManager`, `RootCommandPolicy`, `ShellExecutor`) executing only pre-approved parameter-validated command arrays.
- Optional schema-validated AI provider interface (`AiInterpreter`) with encrypted local credential storage (`AndroidKeyStore`).
- Room relational database (10 tables, explicit migration strategy) and DataStore preferences.

### Long-Term Roadmap Vision (Post-MVP)
- On-device neural wake-word acoustic model bundling (e.g., LiteRT/ONNX keyword spotting) replacing Android continuous speech recognizer loops.
- Signed external plugin distribution ecosystem with sandboxed IPC contracts.
- Multi-step time/event-triggered background automation routines via `WorkManager` and system broadcast receivers.
- Optional end-to-end encrypted cross-device alias synchronization via Supabase with Row-Level Security (RLS).

---

## E. MVP User Stories

### US-01: Dynamic Browser Resolution (`FR-APP-002`)
- **As a** user, **I can** type or say *"Open my browser"* (or *"ব্রাউজার খোলো"*), **so that** Meemo resolves and launches my default or preferred installed web browser.
- **Preconditions:** At least one browser application supporting `ACTION_VIEW` with `http://`/`https://` and `CATEGORY_BROWSABLE` is installed.
- **Trigger:** User submits `"Open my browser"` via text or speech.
- **Expected Behavior:** Normalizer strips wake phrase -> Parser identifies `OPEN_BROWSER` -> `BrowserResolver` queries `PackageManager` -> Launches default browser or prompts user if multiple unconfigured browsers exist -> UI displays and speaks `"Opened [Browser Name]"`.
- **Acceptance Criteria:** Never hardcodes Chrome; resolves any installed browser dynamically; returns `ActionResult.Ambiguous` with candidate list if no default is set and multiple browsers match.
- **Failure Behavior:** If no browser is installed or visible, returns `ActionResult.Unavailable` with error code `ERR_BROWSER_NOT_FOUND` and recovery action to open Play Store or Application Settings.
- **Privacy Implications:** Queries local `PackageManager` only; zero network transmission.
- **Relevant Automated Tests:** `BrowserResolverTest`, `CommandEngineIntegrationTest`.

### US-02: Verified Flashlight Control (`FR-DEVICE-001`)
- **As a** user, **I can** say *"Hello Meemo, turn on the flashlight"* (or *"টর্চ চালু করো"*), **so that** the device camera torch turns on and reports verified status.
- **Preconditions:** Device has `PackageManager.FEATURE_CAMERA_FLASH` and a valid back/external camera unit with `FLASH_INFO_AVAILABLE == true`.
- **Trigger:** Voice or text command matching `TORCH_ON` or `TORCH_OFF`.
- **Expected Behavior:** `CapabilityManager` verifies flash hardware -> `TorchActionExecutor` invokes `CameraManager.setTorchMode(cameraId, true)` -> Reads confirmed state from registered `CameraManager.TorchCallback` -> Reports `ActionResult.Success`.
- **Acceptance Criteria:** UI and TTS report success only after `setTorchMode` completes without `CameraAccessException` and callback confirms state.
- **Failure Behavior:** If hardware is absent (`CapabilityResult.HardwareUnavailable`), camera is in use (`ERR_TORCH_IN_USE`), or API throws `CameraAccessException`, Meemo reports the exact reason and never claims success.
- **Privacy Implications:** Does not open a camera preview or capture images; uses Android's zero-permission `setTorchMode` API.
- **Relevant Automated Tests:** `TorchCapabilityTest`, `CommandParserTest`.

### US-03: Accurate Battery & Device Status (`FR-DEVICE-002`)
- **As a** user, **I can** ask *"What is my battery status?"* or *"Device info"*, **so that** Meemo reads real system metrics.
- **Preconditions:** Standard Android runtime (`minSdk 26`).
- **Trigger:** Voice or text command matching `GET_BATTERY_STATUS` or `GET_DEVICE_INFORMATION`.
- **Expected Behavior:** Reads `BatteryManager.BATTERY_PROPERTY_CAPACITY`, charging status, plug type, voltage, and `android.os.Build` properties -> Formats concise summary -> Speaks and renders metrics card.
- **Acceptance Criteria:** Every metric comes from real Android APIs; any unavailable sensor value is explicitly marked `"Unavailable"`.
- **Failure Behavior:** If battery sticky broadcast is null, falls back to `BatteryManager.getIntProperty` or returns `ActionResult.Unavailable`.
- **Privacy Implications:** Read-only local system properties; never transmitted externally.
- **Relevant Automated Tests:** `DeviceInfoActionTest`, `ActionDispatcherTest`.

### US-04: Bilingual Text-to-Speech Feedback (`FR-VOICE-002`)
- **As a** user, **I can** hear assistant responses spoken aloud in English or Bengali and interrupt speech at any time.
- **Preconditions:** Android TTS engine installed on device; TTS enabled in Meemo preferences.
- **Trigger:** Completion of any command or incoming read-aloud notification; or user tapping `"Stop Speaking"` / saying `"Stop"`.
- **Expected Behavior:** `TtsController` checks `isLanguageAvailable(Locale("bn", "BD"))` or `Locale.US` -> Speaks response with configured speech rate -> Shows active speaking indicator with one-tap stop button.
- **Acceptance Criteria:** Gracefully degrades to text-only with an informational banner (`ERR_TTS_VOICE_MISSING`) if Bengali or English voice pack is missing on the device.
- **Failure Behavior:** Engine init failure sets `TtsState.Unavailable` without crashing or blocking text responses.
- **Privacy Implications:** Synthesizes speech locally via system TTS service.
- **Relevant Automated Tests:** `TtsControllerTest`, `AssistantViewModelTest`.

### US-05: Permission & Special-Access Transparency (`FR-PRIVACY-002`)
- **As a** user, **I can** see why a command failed or requires authorization and jump directly to the relevant Android permission or settings screen.
- **Preconditions:** User triggers a feature requiring `RECORD_AUDIO`, Notification Access, Accessibility, or Root.
- **Trigger:** Command execution or Settings toggle when permission/service is disabled.
- **Expected Behavior:** Transitions state machine to `CHECKING_CAPABILITIES` -> Returns `ActionResult.PermissionRequired` with rationale and a direct action button (`ACTION_NOTIFICATION_LISTENER_SETTINGS`, `ACTION_ACCESSIBILITY_SETTINGS`, or runtime permission prompt).
- **Acceptance Criteria:** Never silently fails; never requests unrelated permissions upfront at app launch.
- **Failure Behavior:** If user denies permission permanently, shows non-intrusive card linking to `ACTION_APPLICATION_DETAILS_SETTINGS`.
- **Privacy Implications:** Enforces strict least-privilege access.
- **Relevant Automated Tests:** `CapabilityManagerTest`, `PermissionFlowTest`.

### US-06: Offline Basic Command Execution (`FR-COMMAND-002`)
- **As a** user, **I can** execute all supported local commands without an AI API key or internet connection.
- **Preconditions:** Airplane mode or no internet connection; no AI provider configured.
- **Trigger:** User submits any supported local command (apps, browser, torch, battery, device info, volume, media, settings, aliases, stop/cancel).
- **Expected Behavior:** Deterministic parser resolves intent with confidence `>= 0.85` and executes locally with zero network calls.
- **Acceptance Criteria:** 100% of deterministic commands succeed offline; no cloud error is shown for local operations.
- **Failure Behavior:** Unrecognized natural-language queries when offline return `ERR_COMMAND_UNRECOGNIZED` with helpful example commands.
- **Privacy Implications:** Zero data leaves the device.
- **Relevant Automated Tests:** `OfflineExecutionTest`, `DeterministicIntentMatcherTest`.

### US-07: Local History & Custom Aliases (`FR-MEMORY-001`)
- **As a** user, **I can** create custom aliases (e.g., *"Whenever I say work browser, open Firefox"*) and review or delete my local command history.
- **Preconditions:** Room database initialized.
- **Trigger:** User says/types alias creation command or uses the Memory & Aliases screen; or taps `"Clear All Data"` in Privacy settings.
- **Expected Behavior:** Saves alias in `custom_command_aliases` table; resolves `"Open my work browser"` to `org.mozilla.firefox` on future runs and re-validates that Firefox is still installed and permitted.
- **Acceptance Criteria:** Deleting history or revoking consent purges Room tables (`conversation_sessions`, `conversation_messages`, `command_history`) immediately within a single transaction.
- **Failure Behavior:** Duplicate alias phrase triggers update confirmation; uninstalled target app returns `ERR_ALIAS_TARGET_MISSING`.
- **Privacy Implications:** User has full visibility, toggleable history persistence, and instant purge controls.
- **Relevant Automated Tests:** `AliasResolutionTest`, `DatabaseRetentionTest`.

### US-08: Opt-In Notification Read-Aloud (`FR-NOTIFICATION-001`)
- **As a** user, **I can** authorize notification access so Meemo reads incoming messages from allowed apps aloud while respecting quiet hours and privacy redaction.
- **Preconditions:** User explicitly enables Notification Read-Aloud in Meemo, records consent in `consent_records`, and enables `MeemoNotificationListener` in Android Settings.
- **Trigger:** Incoming `StatusBarNotification` with `Notification.EXTRA_TEXT` from an allowlisted package (e.g., WhatsApp, Messenger, Messages).
- **Expected Behavior:** `NotificationPolicy` verifies active consent, package allowlist, non-quiet hours, and duplicate hash window -> `NotificationProcessor` extracts transient sender/text (or announces arrival only if content redaction is enabled) -> Enqueues in `NotificationReadAloud` -> Speaks via TTS.
- **Acceptance Criteria:** Notification message bodies are processed transiently in memory and **never** written to disk logs or permanent database tables by default.
- **Failure Behavior:** Hidden/redacted lockscreen notifications (`VISIBILITY_SECRET` or empty `EXTRA_TEXT`) announce only `"New notification from [App Name] (content hidden by system)"`.
- **Privacy Implications:** Transient memory processing; zero external transmission; instant stop via voice or button.
- **Relevant Automated Tests:** `NotificationPolicyTest`, `NotificationProcessorTest`.

---

## F. Functional Requirements Traceability Matrix

| ID | Category | Requirement Description | Source Files | Automated Tests |
| :--- | :--- | :--- | :--- | :--- |
| **FR-VOICE-001** | Speech Input | Lifecycle-safe `SpeechRecognizer` integration supporting `en-US` and `bn-BD` with explicit `RECORD_AUDIO` check, active mic indicator, and timeout handling. | `voice/SpeechRecognizerController.kt`, `voice/VoiceController.kt` | `VoiceControllerTest` |
| **FR-VOICE-002** | Speech Output | `TextToSpeech` controller with `en` and `bn` locale checks, configurable speech rate, queue management, and immediate stop/interruption. | `tts/MeemoTtsEngine.kt`, `tts/TtsController.kt` | `TtsControllerTest` |
| **FR-VOICE-003** | Wake Word | Optional `WakeWordDetector` abstraction with explicit opt-in foreground service (`MeemoWakeWordService`), mic notification, and manual trigger fallback. | `voice/WakeWordDetector.kt`, `services/MeemoWakeWordService.kt` | `WakeWordDetectorTest` |
| **FR-COMMAND-001** | Normalization | Normalize whitespace, casing, punctuation, wake phrases (`Hello Meemo`, `হ্যালো মিমো`), and Bengali/English numerals without stripping app names. | `assistant/parser/CommandNormalizer.kt`, `assistant/parser/LanguageResolver.kt` | `CommandNormalizerTest` |
| **FR-COMMAND-002** | Intent & Entities | Deterministic matching for all 18 core intents with entity extraction, confidence scoring, and ambiguity detection. | `assistant/intent/DeterministicIntentMatcher.kt`, `assistant/entity/EntityExtractor.kt` | `DeterministicIntentMatcherTest`, `EntityExtractorTest` |
| **FR-APP-001** | App Discovery | Dynamic `PackageManager` discovery of launchable activities with exact, prefix, and fuzzy ranking plus ambiguity resolution. | `apps/AppManager.kt`, `apps/PackageResolver.kt` | `PackageResolverTest` |
| **FR-APP-002** | Browser Resolution | Dynamic resolution of `Intent.ACTION_VIEW` (`https://`) browsers, respecting default browser or presenting candidate list. | `apps/BrowserResolver.kt`, `actions/app/OpenBrowserActionExecutor.kt` | `BrowserResolverTest` |
| **FR-DEVICE-001** | Torch Control | Verified flashlight control via `CameraManager.setTorchMode` and `TorchCallback` state tracking with hardware availability checks. | `actions/device/TorchActionExecutor.kt`, `capabilities/adapters/TorchCapabilityAdapter.kt` | `TorchCapabilityTest` |
| **FR-DEVICE-002** | Device & Battery | Accurate battery level, charging state, health, voltage, Android SDK, and hardware capability snapshot reporting. | `actions/device/DeviceInfoActionExecutor.kt` | `DeviceInfoActionTest` |
| **FR-DEVICE-003** | Audio & Media | Stream volume adjustment (`STREAM_MUSIC`, `STREAM_RING`, `STREAM_ALARM`) and `AudioManager.dispatchMediaKeyEvent` media controls. | `actions/media/VolumeMediaActionExecutor.kt` | `VolumeMediaActionTest` |
| **FR-DEVICE-004** | Settings Routing | Safe intent routing to Wi-Fi, Bluetooth, Display, Sound, Notification Listener, Accessibility, and App Info settings. | `actions/settings/SettingsActionExecutor.kt` | `SettingsActionTest` |
| **FR-NOTIFICATION-001** | Read-Aloud | Opt-in `NotificationListenerService` with package filtering, duplicate suppression (SHA-256 content window), quiet hours, and transient speech queue. | `notifications/MeemoNotificationListener.kt`, `notifications/NotificationProcessor.kt`, `notifications/NotificationPolicy.kt` | `NotificationPolicyTest`, `NotificationProcessorTest` |
| **FR-ROOT-001** | Isolated Root | Isolated `RootManager`, `RootCommandPolicy`, and `ShellExecutor` enforcing allowlisted command templates, parameter regex validation, and timeouts. | `root/RootManager.kt`, `root/RootCommandPolicy.kt`, `root/ShellExecutor.kt` | `RootCommandPolicyTest` |
| **FR-AI-001** | Hybrid AI Fallback | Optional `AiInterpreter` interface validating JSON responses against known `MeemoIntent` schemas before routing through the standard safety pipeline. | `assistant/engine/AiInterpreter.kt`, `data/remote/OpenAiCompatibleService.kt` | `AiFallbackValidationTest` |
| **FR-MEMORY-001** | Memory & Aliases | Local Room storage for custom command aliases, conversation history, automation rules, and consent records with full purge support. | `data/database/MeemoDatabase.kt`, `memory/AliasManager.kt` | `AliasResolutionTest`, `MeemoDatabaseTest` |
| **FR-PRIVACY-001** | Security & Redaction | Redacted structured logging (`SafeLogger`), encrypted API key storage (`AndroidKeyStore`), and zero default persistence of notification bodies. | `core/logging/SafeLogger.kt`, `core/security/EncryptedCredentialStore.kt` | `SafeLoggerRedactionTest` |

---

## G. Non-Functional Requirements
1. **Performance & Latency:** Deterministic local command normalization, intent matching, and action planning must complete in `< 25 ms` on a standard JVM thread. Database queries must run off the main thread (`Dispatchers.IO`).
2. **Reliability & State Safety:** ViewModels must expose immutable `StateFlow` state modeled via sealed interfaces (`AssistantExecutionState`) so invalid state combinations cannot occur.
3. **Accessibility:** Minimum `48dp x 48dp` touch targets on all interactive controls, explicit `contentDescription` and semantic roles on custom Canvas/vector components (including the Meemo mascot), WCAG AA contrast (`>= 4.5:1` for body text), and respect for system `ANIMATOR_DURATION_SCALE` (reduced motion).
4. **Localization & Typography:** First-class support for English (`en`) and Bengali (`bn`) strings, numerals (`০-৯` to `0-9` normalization), and system Bengali font fallbacks (`Noto Sans Bengali` / `sans-serif`).
5. **Privacy & Data Minimization:** Local-first operation; zero telemetry SDKs; zero logging of raw notification bodies or API tokens; explicit user opt-in recorded in `consent_records` before activating Notification Access, Wake Word, AI Interpretation, or Root Execution.
6. **Battery Efficiency:** No continuous wake-locks or hidden background loops; foreground wake-word service runs only when explicitly toggled on by the user with a visible persistent notification and stop action.

---

## H. Success Metrics & Evaluation Methodology
- **Deterministic Command Parsing Accuracy:** `>= 98%` exact intent and entity accuracy across the curated 60-phrase bilingual test corpus in `CommandNormalizerTest` and `DeterministicIntentMatcherTest`.
- **Zero Unauthorized Privileged Executions:** `100%` rejection rate of unapproved shell commands, shell metacharacters (`;`, `|`, `&&`, `` ` ``, `$()`), and unconfirmed root operations in `RootCommandPolicyTest`.
- **Permission Denial Resilience:** `100%` of permission-denied or hardware-unavailable test scenarios transition cleanly to `ActionResult.PermissionRequired` or `ActionResult.Unavailable` without throwing uncaught exceptions.
- **Privacy Redaction Verification:** `100%` of log entries passing through `SafeLogger` strip bearer tokens, API keys, and notification message bodies.

---

## I. Dependencies and Assumptions
- **Android Platform:** Requires Android 8.0+ (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`).
- **Speech & TTS Providers:** Relies on the device having a functional `RecognitionService` (e.g., Google Speech Services) and `TextToSpeech` engine installed for voice features; text input and UI output function independently if voice services are absent.
- **Package Visibility:** On Android 11+ (API 30+), dynamic discovery uses `<queries>` declarations in `AndroidManifest.xml` for launcher activities (`ACTION_MAIN` + `CATEGORY_LAUNCHER`), browsers (`ACTION_VIEW` + `https`), and media/settings intents.
- **System Settings Restrictions:** On Android 10+ (API 29+), apps cannot directly toggle Wi-Fi via `WifiManager.setWifiEnabled()`, and on Android 13+ (API 33+), apps cannot directly toggle Bluetooth without privileged system permissions. Meemo transparently opens the corresponding system settings panel (`Settings.Panel.ACTION_WIFI` / `Settings.ACTION_BLUETOOTH_SETTINGS`) and reports `ActionResult.PartialSuccess` (`Opened settings — waiting for user action`).

---

## J. Release Acceptance Criteria
1. All six specification documents (`01_PRD.md` through `06_Plan.md`) and root governance files (`Gimini.md`, `.cursorrules`, `README.md`, `SECURITY.md`, `CONTRIBUTING.md`, `LICENSE`) are complete and mutually consistent.
2. Complete Kotlin source tree under `com.marufallammahim.meemo` with zero placeholder `TODO()` implementations in completed features.
3. Comprehensive unit, database, integration, and UI test suites implemented and verified.
4. Self-contained ZIP archive (`Meemo-Android-Digital-Assistant.zip`) verified via extraction into a clean directory and SHA-256 checksum validation.

---

## K. Strict Out of Scope / Non-Goals
The following are explicitly excluded and prohibited:
1. Replacing the Android OS, launcher, or system lockscreen.
2. Bypassing Android runtime permissions, SELinux policies, or special-access consent screens.
3. Reading private database files of third-party messaging apps (e.g., WhatsApp/Messenger SQLite databases) without authorization.
4. Breaking message encryption or reading notification content hidden/redacted by the user's lockscreen privacy settings.
5. Silently enabling Accessibility or Notification Listener services without explicit user action in Android Settings.
6. Executing arbitrary shell strings (`sh -c ...`) or raw AI-generated shell commands via `su`.
7. Guaranteeing continuous background wake-word detection on aggressive OEM battery-killer firmware without foreground service disclosure.
8. Shipping embedded private API keys, signing keystores, or proprietary copyrighted mascot images.
