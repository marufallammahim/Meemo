# Security & Privacy Policy — Meemo Android Digital Assistant

## 1. Core Security Principles
Meemo is engineered around **Least Privilege**, **Explicit User Consent**, **Strict Root Isolation**, and **Transient Notification Privacy**.

### 1.1 Zero Arbitrary Shell Execution
- UI components, ViewModels, and AI interpreters **cannot** invoke `su` or execute shell commands directly.
- All optional root capabilities are isolated in `com.marufallammahim.meemo.root`.
- `RootCommandPolicy` enforces a strict compile-time allowlist of `RootCommandSpec` entries (`ROOT_BATTERY_STATS_DUMP`, `ROOT_THERMAL_STATUS`, `ROOT_PACKAGE_PATH_CHECK`).
- `ShellExecutor` validates every parameter against a strict alphanumeric regex (`^[a-zA-Z0-9._-]{1,64}$`), rejects shell metacharacters (`;`, `|`, `&`, `` ` ``, `$`, `>`, `<`, `\n`), enforces a `5000ms` execution timeout, caps output at `16 KB`, and requires explicit user confirmation.

### 1.2 AI Interpretation Sandbox
- Cloud AI is **optional and disabled by default**.
- When enabled by the user, AI responses are parsed strictly into `AiStructuredInterpretation` and validated by `CommandSafetyValidator`.
- AI output can only map to registered `MeemoIntent` identifiers and still passes through `CapabilityManager` permission checks and confirmation gates. AI can never request arbitrary shell execution or bypass user consent.

### 1.3 Notification & Voice Privacy
- `MeemoNotificationListener` processes incoming notification text **exclusively in transient memory** for opt-in read-aloud.
- Notification message bodies are **never** stored in the SQLite database (`meemo_assistant.db`), never sent to an AI provider, and automatically redacted by `SafeLogger`.
- Hidden/private lockscreen notifications (`Notification.VISIBILITY_SECRET`) are respected and never scraped.

### 1.4 Credential Storage
- User-supplied AI API keys are encrypted at rest using Android's hardware-backed `AndroidKeyStore` (`AES/GCM/NoPadding`, 256-bit key) in `EncryptedCredentialStore.kt`.
- Zero API keys, signing keys, or service-role tokens are bundled in the source repository or APK.

## 2. Reporting a Vulnerability
If you discover a security or privacy issue in Meemo:
1. Do **not** post exploit payloads containing personal data in public issues.
2. Document the affected component (`root/`, `notifications/`, `assistant/`, or `data/`), reproduction steps, and Android API level.
