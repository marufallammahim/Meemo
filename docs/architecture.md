# Meemo architecture

## Foundation module graph

The application is being migrated from the original single Android module into the module graph defined by the Meemo Master Build Prompt.

- `app`: Android application, activity, navigation root and Hilt composition root.
- `core:model`: pure Kotlin models shared by the application.
- `core:common`: dispatchers, result types, logging contracts and common extensions.
- `core:ui`: Material 3 theme and reusable Compose components.
- `core:database`: Room database, entities and DAOs.
- `core:datastore`: user settings persistence.
- `domain`: use cases and repository contracts; depends only on `core:model` and `core:common`.
- `nlu`: offline command normalization and intent parsing.
- `capability`: capability contracts and planning.
- `capability:android`, `:accessibility`, `:root`: isolated capability implementations.
- `voice`: speech input, speech output and wake-word abstractions.
- `feature:*`: independent user-facing feature modules.

## Dependency direction

Feature modules may depend on domain and core modules. Domain may depend only on core:model and core:common. Capability, NLU and voice implement domain contracts. The app composes the graph with Hilt. No module may depend on the app module.

## Migration policy

The existing implementation is retained while responsibilities are moved into their target modules. A migration must preserve observable behavior and tests. No phase may begin until the previous phase's acceptance criteria pass.
