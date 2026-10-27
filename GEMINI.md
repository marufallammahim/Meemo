# Gimini.md — Mandatory Agent Engineering Instructions for Meemo
**Project:** Meemo — Android Digital Assistant (`com.marufallammahim.meemo`)

## Mandatory Instructions for Every Coding Session
1. **Read `Gimini.md`** before making any architectural or code changes.
2. **Read `.cursorrules`** to enforce Kotlin, Jetpack Compose, Clean Architecture, and security rules.
3. **Read all six specification files** (`01_PRD.md`, `02_AppFlow.md`, `03_UI_Spec.md`, `04_TRD.md`, `05_Schema.md`, `06_Plan.md`).
4. **Read the relevant source files and tests** before editing.
5. **Identify the target task and its prerequisites** in `06_Plan.md`.
6. **Check whether the task conflicts** with product requirements (`01_PRD.md`) or security constraints (`04_TRD.md`, `SECURITY.md`).
7. **Implement only the intended change** and its necessary dependencies without removing existing capabilities.
8. **Add or update automated tests** covering both happy paths and failure/denial edge cases.
9. **Run validation commands** (`./gradlew testDebugUnitTest assembleDebug lintDebug` when Android SDK is present, and static/behavioral verification checks).
10. **Fix any introduced regressions** immediately.
11. **Update `06_Plan.md`** immediately after a task is verified.
12. **Update documentation** whenever behavior, schema, or architecture changes.
13. **Report completed files, tests, failures, and environment limitations accurately.**

## Non-Negotiable Prohibitions
- **Never claim a task is complete** merely because code was generated; completion requires documented acceptance criteria to be satisfied or external environment blockers to be explicitly recorded.
- **Never make unrelated architectural changes** without documenting and validating them.
- **Never overwrite user-authored requirements** with an abbreviated specification.
- **Never replace real Android functionality with fake success responses** (e.g., never report `"Flashlight turned on"` unless `CameraManager.setTorchMode` succeeded and state was verified).
- **Never change dependency versions** in `gradle/libs.versions.toml` without verifying mutual compatibility.
- **Never place API secrets, signing credentials, private keys, or root credentials** in source control.
- **Never execute arbitrary AI-generated or user-supplied shell strings** via `su` or `sh -c`.
- **When blocked by hardware or environment constraints**, identify the exact blocker, preserve completed work, and proceed with independent tasks.
