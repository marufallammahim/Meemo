# 02_AppFlow.md — Screen Flows, Navigation Architecture, and State Machine
**Project Name:** Meemo — Android Digital Assistant  
**Package Namespace:** `com.marufallammahim.meemo`  
**Document Version:** 1.0.0  

---

## 1. Assistant Core Execution State Machine

Every voice, text, alias, or AI-assisted command in Meemo is governed by an explicit, deterministic state machine (`AssistantExecutionState`) managed inside `AssistantViewModel` and `AssistantEngine`.

### 1.1 Explicit States
| State | Description | Allowed Outgoing Transitions | UI & Mascot Representation |
| :--- | :--- | :--- | :--- |
| `IDLE` | Ready for user voice or text input. | `LISTENING`, `PROCESSING` | Mascot: `Idle` (gentle ear twitch); mic & text input enabled. |
| `LISTENING` | Microphone active via `SpeechRecognizerController`. | `TRANSCRIBING`, `CANCELLED`, `FAILED`, `UNAVAILABLE` | Mascot: `Listening` (pulsing burgundy aura); active mic banner + Cancel button. |
| `TRANSCRIBING` | Speech audio captured; awaiting final transcript from recognizer. | `PROCESSING`, `CANCELLED`, `FAILED` | Mascot: `Thinking`; partial transcript preview displayed. |
| `PROCESSING` | Normalizing text, resolving language/aliases, matching intent & entities. | `CHECKING_CAPABILITIES`, `AWAITING_CONFIRMATION`, `FAILED`, `CANCELLED` | Mascot: `Thinking`; status pill shows `"Understanding command..."`. |
| `CHECKING_CAPABILITIES` | Verifying hardware presence, API level, runtime permissions, and service state. | `AWAITING_CONFIRMATION`, `EXECUTING`, `UNAVAILABLE`, `FAILED` | Mascot: `Thinking`; status pill shows `"Checking device capabilities..."`. |
| `AWAITING_CONFIRMATION` | Action is sensitive (e.g., Root command, destructive clear, or ambiguous app choice). | `EXECUTING`, `CANCELLED` | Confirmation dialog / ambiguity selector card with Confirm & Cancel buttons. |
| `EXECUTING` | Dispatching validated `MeemoAction` to the target `ActionExecutor`. | `VERIFYING`, `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`, `CANCELLED` | Mascot: `Thinking`; progress indicator on active command card. |
| `VERIFYING` | Confirming hardware/system state change (e.g., `TorchCallback` state check). | `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED` | Mascot: `Thinking`; status pill shows `"Verifying outcome..."`. |
| `SUCCESS` | Action completed and verified; TTS speaks response if enabled. | `IDLE`, `LISTENING`, `PROCESSING` | Mascot: `Success` / `Speaking`; green status badge + result card. |
| `PARTIAL_SUCCESS` | System settings opened or fallback action taken awaiting user interaction. | `IDLE`, `LISTENING`, `PROCESSING` | Mascot: `Speaking`; amber badge explaining required user step. |
| `FAILED` | Operational error or denied permission with structured recovery option. | `IDLE`, `LISTENING`, `PROCESSING` | Mascot: `Error`; error card with `errorCode`, explanation, and Retry/Settings button. |
| `CANCELLED` | User explicitly stopped speech, listening, or confirmation. | `IDLE`, `LISTENING`, `PROCESSING` | Mascot: `Idle`; neutral badge `"Command cancelled"`. |
| `UNAVAILABLE` | Requested hardware, sensor, or Android capability is absent on this device. | `IDLE`, `LISTENING`, `PROCESSING` | Mascot: `Error`; informational card explaining hardware/OS limitation. |

### 1.2 State Transition Diagram
```text
                   ┌───────────────────────────────────────────┐
                   ▼                                           │
                [IDLE] ──────(Text Submitted)──────┐           │
                   │                               │           │
             (Mic Tapped)                          ▼           │
                   ▼                         [PROCESSING]      │
              [LISTENING] ──(Audio Done)──► [TRANSCRIBING]     │
                   │                               │           │
                   │                         (Normalized)      │
                   │                               ▼           │
                   │                   [CHECKING_CAPABILITIES] │
                   │                        │        │         │
                   │           (Needs Auth/ │        │(Ready)  │
                   │            Ambiguous)  ▼        ▼         │
                   │            [AWAITING_CONFIRMATION]        │
                   │                        │        │         │
                   │                  (Confirmed)    │         │
                   │                        ▼        ▼         │
                   │                       [EXECUTING]         │
                   │                            │              │
                   │                            ▼              │
                   │                       [VERIFYING]         │
                   │                            │              │
                   ├────────────────────────────┴──────────────┤
                   ▼                                           │
   [SUCCESS] / [PARTIAL_SUCCESS] / [FAILED] / [CANCELLED] / [UNAVAILABLE]
```

Invalid transitions (such as jumping directly from `LISTENING` to `SUCCESS` without `PROCESSING`, `CHECKING_CAPABILITIES`, and `EXECUTING`) are rejected by `AssistantStateMachine.transitionTo()`.

---

## 2. Typed Navigation Graph (`MeemoDestination`)

Meemo uses strongly typed Jetpack Compose Navigation routes (`@Serializable sealed interface MeemoDestination`) managed in `presentation/navigation/MeemoNavHost.kt`.

### 2.1 Complete Screen Inventory (27 Flows & States Covered)

| # | Screen / Flow Name | Route Identifier (`MeemoDestination`) | ViewModel | Entry Condition & Purpose |
| :--- | :--- | :--- | :--- | :--- |
| 1 | **Splash & Initialization** | `MeemoDestination.Splash` | `MainViewModel` | App cold start; initializes DataStore, checks `onboardingCompleted`, preloads capability snapshot. |
| 2 | **First-Launch Onboarding** | `MeemoDestination.Onboarding` | `OnboardingViewModel` | Shown when `onboardingCompleted == false`; introduces Meemo's honest capability philosophy. |
| 3 | **Language Selection** | `MeemoDestination.Onboarding` (Step 2) & `Settings` | `OnboardingViewModel` / `SettingsViewModel` | Select Primary Language: `ENGLISH`, `BENGALI`, or `BILINGUAL_AUTO`. |
| 4 | **Privacy & Permissions Overview** | `MeemoDestination.Onboarding` (Step 3) & `PrivacySettings` | `OnboardingViewModel` / `SettingsViewModel` | Explains optional permissions (`RECORD_AUDIO`, Notification Access, Accessibility, Root) with zero forced prompts. |
| 5 | **Main Assistant Screen** | `MeemoDestination.AssistantHome` | `AssistantViewModel` | Primary workspace: Meemo Mascot header, active state banner, quick capability chips, recent conversation feed, voice/text bar. |
| 6 | **Text Command Entry** | `MeemoDestination.AssistantHome` (Bottom Bar) | `AssistantViewModel` | Multi-script text field (`en`/`bn`) with instant submit, clear, and suggestion chips. |
| 7 | **Voice Listening Interface** | `MeemoDestination.AssistantHome` (Overlay/Sheet) | `AssistantViewModel` | Active microphone wave animation, live partial transcript, locale badge, and Cancel button. |
| 8 | **Speech Processing State** | `MeemoDestination.AssistantHome` (`TRANSCRIBING`/`PROCESSING`) | `AssistantViewModel` | Visual feedback while normalizing speech and matching intents. |
| 9 | **Command Confirmation Dialog** | `MeemoDestination.AssistantHome` (`AWAITING_CONFIRMATION`) | `AssistantViewModel` | Modal confirmation for privileged/root actions or disambiguation picker when multiple apps match. |
| 10 | **Action Execution State** | `MeemoDestination.AssistantHome` (`EXECUTING`/`VERIFYING`) | `AssistantViewModel` | Determinate/indeterminate progress card displaying target adapter and verification step. |
| 11 | **Result & Error Feedback** | `MeemoDestination.AssistantHome` (Result Card) | `AssistantViewModel` | Structured card displaying verified outcome, TTS stop control, or `MeemoError` recovery button. |
| 12 | **Conversation History** | `MeemoDestination.History` | `HistoryViewModel` | Searchable chronological list of sessions and command records with single-item or bulk delete. |
| 13 | **Installed Application Discovery** | `MeemoDestination.InstalledApps` | `AppsViewModel` | Live searchable directory of launchable installed apps and resolved browsers with direct launch & alias creation. |
| 14 | **Notification Reading Settings** | `MeemoDestination.NotificationSettings` | `NotificationSettingsViewModel` | Opt-in consent toggle, listener service status, package allowlist/blocklist, quiet hours, and content privacy mode. |
| 15 | **Wake-Word Settings** | `MeemoDestination.VoiceSettings` | `VoiceSettingsViewModel` | Opt-in `"Hello Meemo"` foreground service toggle, battery disclosure, sensitivity, and manual mic fallback info. |
| 16 | **Text-to-Speech Settings** | `MeemoDestination.VoiceSettings` | `VoiceSettingsViewModel` | Spoken feedback toggle, speech rate slider (`0.5x`–`2.0x`), pitch, and `en`/`bn` voice availability diagnostics. |
| 17 | **AI Provider Configuration** | `MeemoDestination.AiRootSettings` | `AiRootSettingsViewModel` | Optional AI fallback toggle, endpoint URL, model name, encrypted API key entry (`AndroidKeyStore`), and privacy notice. |
| 18 | **Offline Capability Information** | `MeemoDestination.CapabilitiesDiagnostics` | `CapabilitiesViewModel` | Matrix showing all offline-ready commands vs. features requiring Android speech packs or network AI. |
| 19 | **Root Capability Status** | `MeemoDestination.AiRootSettings` | `AiRootSettingsViewModel` | Root binary detection status, opt-in root policy toggle, and read-only list of approved `RootCommandPolicy` templates. |
| 20 | **Accessibility Settings Guidance** | `MeemoDestination.CapabilitiesDiagnostics` | `CapabilitiesViewModel` | Explains `MeemoAccessibilityService` scope, displays active status, and provides direct shortcut to Android Accessibility Settings. |
| 21 | **Permission Management** | `MeemoDestination.CapabilitiesDiagnostics` | `CapabilitiesViewModel` | Real-time status cards for Microphone, Post Notifications, Notification Listener, and Camera Flash hardware. |
| 22 | **Memory & Custom Aliases** | `MeemoDestination.MemoryAliases` | `MemoryAliasesViewModel` | Create, edit, validate, and delete custom voice/text command aliases (e.g., `"work browser" -> "org.mozilla.firefox"`). |
| 23 | **Automation & Routines** | `MeemoDestination.MemoryAliases` (Routines Tab) | `MemoryAliasesViewModel` | Inspectable multi-action user routines (e.g., `"Bedtime mode"` -> Torch off + Media pause + Battery check). |
| 24 | **Plugin Registry** | `MeemoDestination.CapabilitiesDiagnostics` (Plugins Tab) | `CapabilitiesViewModel` | Built-in capability plugin registry showing version, supported intents, required permissions, and health status. |
| 25 | **About & Diagnostics** | `MeemoDestination.AboutDiagnostics` | `CapabilitiesViewModel` | App version, SDK info, redacted diagnostic log viewer, and capability snapshot export. |
| 26 | **Privacy & Data Deletion** | `MeemoDestination.PrivacySettings` | `SettingsViewModel` | History persistence toggle, consent ledger inspection, and one-tap `"Delete All Local Data"` with confirmation. |
| 27 | **Error Recovery & Unavailable Screen** | Inline Component (`ErrorBannerCard` / `UnavailableCapabilitySheet`) | Shared across ViewModels | Displayed whenever hardware is absent or permissions are revoked mid-session, offering immediate recovery actions. |

---

## 3. Detailed Screen Requirements & Back-Navigation Rules

### 3.1 Main Assistant Screen (`AssistantHome`)
- **Top Bar:** Meemo branding, active language chip (`EN` / `বাং` / `AUTO`), Stop Speaking button (visible whenever `TtsState.Speaking`), and quick icon button to `AboutDiagnostics`.
- **Hero Area:** Animated vector `MeemoMascot` reflecting the current `AssistantExecutionState` (`Idle`, `Listening`, `Thinking`, `Speaking`, `Success`, `Error`).
- **Quick Action Chips:** Horizontal scrollable suggestions (`"Turn on flashlight"`, `"Battery status"`, `"Open browser"`, `"ফ্ল্যাশলাইট চালু করো"`, `"ব্যাটারি কত?"`).
- **Conversation & Result Stream:** LazyColumn displaying user commands and structured assistant response cards (with capability badge, execution duration, verification status, and recovery action buttons when applicable).
- **Bottom Input Bar:** Text field (`OutlinedTextField`) with Send button and prominent Microphone FAB (`56dp`).
- **Bottom Navigation Bar:** 5 top-level destinations:
  1. `Assistant` (`MeemoDestination.AssistantHome`)
  2. `Apps` (`MeemoDestination.InstalledApps`)
  3. `Memory` (`MeemoDestination.MemoryAliases`)
  4. `Capabilities` (`MeemoDestination.CapabilitiesDiagnostics`)
  5. `Settings` (`MeemoDestination.PrivacySettings`)
- **Back Navigation:** If `AssistantExecutionState` is `LISTENING` or `AWAITING_CONFIRMATION`, pressing Back cancels the active operation and returns to `IDLE` rather than exiting the app. From `IDLE`, pressing Back exits the activity normally.

### 3.2 UI State Representation Rule
Every screen uses a single immutable `@Immutable data class` or `sealed interface` exposed as `StateFlow<ScreenUiState>` from its Hilt `@HiltViewModel`. Unrelated boolean flags (`isLoading`, `isError`, `isSuccess`) are never scattered across composables.
