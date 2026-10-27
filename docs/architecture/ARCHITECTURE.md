# Meemo Architecture Overview (`docs/architecture/ARCHITECTURE.md`)

Meemo follows **Clean Architecture + MVVM** with unidirectional data flow:
1. **Presentation Layer (`presentation/`):** Jetpack Compose screens, `MeemoMascot` vector canvas, and `@HiltViewModel` classes exposing immutable `StateFlow` UI state.
2. **Assistant Engine Layer (`assistant/`):** `CommandNormalizer`, `LanguageResolver`, `DeterministicIntentMatcher`, `EntityExtractor`, `ActionPlanner`, `ResponseGenerator`, and `AssistantEngine`.
3. **Domain Layer (`domain/`):** Pure Kotlin models, repository interfaces, `ProcessCommandUseCase`, and `CommandSafetyValidator`.
4. **Capabilities & Actions Layer (`capabilities/`, `actions/`, `apps/`, `voice/`, `tts/`, `notifications/`, `accessibility/`, `root/`):** Hardware and Android framework adapters isolated behind explicit capability checks.
5. **Data Layer (`data/`):** Room SQLite database (`MeemoDatabase` with 10 tables and `MIGRATION_1_2`), DataStore preferences, and `OpenAiCompatibleClient`.
