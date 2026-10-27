# Contributing to Meemo — Android Digital Assistant

Thank you for contributing to **Meemo** (`com.marufallammahim.meemo`). To keep the assistant trustworthy, privacy-preserving, and architecturally clean, please follow these engineering guidelines.

## 1. Read the Specifications First
Before opening a pull request or modifying code, review:
- `01_PRD.md` — Product requirements and strict non-goals.
- `02_AppFlow.md` — Screen flows and the `AssistantExecutionState` state machine.
- `03_UI_Spec.md` — Deep burgundy (`#5A0719`) design tokens, typography, and mascot states.
- `04_TRD.md` — Locked dependency versions and Clean Architecture rules.
- `05_Schema.md` — Room database schema (10 tables) and notification privacy rules.
- `06_Plan.md` — Atomic task checklist.
- `Gimini.md` & `.cursorrules` — Coding and verification discipline.

## 2. Core Engineering Principles
1. **Honest Capability Reporting:** Never simulate success when an Android API restricts an action. Use `ActionResult.PartialSuccess` when opening a system settings screen or `ActionResult.PermissionRequired` when user authorization is needed.
2. **Bilingual Parity:** Any new deterministic intent should include both English and Bengali (`বাংলা` + transliterated Banglish) trigger patterns in `DeterministicIntentMatcher.kt` and string resources in both `values/strings.xml` and `values-bn/strings.xml`.
3. **Security Isolation:** Do not add arbitrary shell execution. Any new root capability must be defined as a fixed, parameter-validated `RootCommandSpec` in `RootCommandPolicy.kt` with unit tests in `RootCommandPolicyTest.kt`.

## 3. Verification Before Submitting
Run the full verification suite locally:
```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```
Ensure all tests pass and update `06_Plan.md` if completing a tracked milestone.
