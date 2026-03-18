# 🏗️ AK Proleter Mobile Architecture: Source of Truth

## 1. Project Overview

AK Proleter Mobile is an AI-powered Android application designed for athletes and coaches of AK Proleter. It features a voice assistant to manage training logs, view events, and receive AI-driven insights through a centralized state machine.

## 2. Tech Stack & Environment

- **Core:** Android (Kotlin 1.9+)
- **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 styling
- **Networking:** [Retrofit](https://square.github.io/retrofit/) / [OkHttp](https://square.github.io/okhttp/) (Backend: Next.js API)
- **Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite) for offline-first data
- **Concurrency:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- **Voice Logic:** Native Android Speech-to-Text via `VoiceManager`

## 3. File Structure

- `app/src/main/java/com/akproleter/mobile/`: Root package
    - `data/`: Data layer
        - `local/`: Room DB components (`AkProleterDatabase`, `AkProleterDao`), `SessionManager`
        - `remote/`: Retrofit API interfaces (`ApiService`) and data models
        - `repositories/`: Single source of truth for UI (`VoiceRepository`, etc.)
    - `di/`: Hilt modules (`NetworkModule`, `DatabaseModule`)
    - `ui/`: Presentation layer
        - `auth/`: Login and Authentication UI/ViewModels
        - `voice/`: Voice Assistant UI/ViewModels
        - `theme/`: Compose Material 3 Theme tokens
    - `util/`: Application-wide utilities and constants
    - `voice/`: Core voice handling orchestration (`VoiceManager`)

## 4. Implementation Patterns

- **MVVM Architecture:** Strict separation between UI (Compose), state holders (ViewModels), and business logic (Repositories).
- **Offline-First:** All data should be cached in Room and synced with the backend via Repositories to ensure functionality in poor network conditions.
- **Unidirectional Data Flow (UDF):** ViewModels expose `StateFlow` consumed by Compose screens with `collectAsStateWithLifecycle`.
- **Hilt Singleton Scope:** Network clients, database, and repository instances must be scoped as `@Singleton` to avoid unnecessary recreations.
- **Sealed Class State:** UI states (e.g., `AuthState`, `VoiceState`) use sealed classes to represent specific, mutually exclusive app states.
- **Voice Orchestration:** All speech-to-text operations must flow through `VoiceManager` to centralize state management and ensure permission handling.

## 5. Efficiency Suggestions for AI Agent

To make my assistance more efficient, follow these guidelines:

- **Dependency Check:** Before adding a new feature, check `di/` to see if existing providers can be reused.
- **State Consistency:** Ensure all new screens follow the `xxxState` pattern used in `AuthState` and `VoiceState`.
- **Navigation:** The app currently uses a simple `Crossfade` in `MainActivity` for screen transitions based on authentication state. Use `NavHost` if complex nesting or back-stack handling is needed.
- **Constants Reference:** Always use `com.akproleter.mobile.util.Constants` for URLs, database names, and keys.
- **Coroutines Scope:** Always use `viewModelScope` in ViewModels for launching coroutines to ensure they are cancelled when the ViewModel is cleared.

> [!IMPORTANT]
> Always read this file at the start of every session. Ensure all code suggestions adhere to the tech stack and architecture defined here.
