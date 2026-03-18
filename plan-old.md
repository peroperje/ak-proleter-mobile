# Strategic Plan: AK Proleter Voice Assistant (Pseudo-Monorepo)

## 1. Context & Environment
**Structure**: Pseudo-Monorepo.
- `/backend`: Existing Next.js + Prisma + PostgreSQL application.
- `/android`: New Native Kotlin + Jetpack Compose application.

**Goal**: Enable a voice-driven interface for coaches (admins) and athletes (users) to record results and manage events.
**Target User**: Technically non-proficient coaches on an athletic field.

## 2. Core Architectural Pillars

### A. Multi-Language Speech Engine
- **Dynamic Locale**: Support for Serbian (sr-RS) and English (en-US).
- **Implementation**:
    - Android `SpeechRecognizer` configured at runtime based on user settings.
    - Localized UI via standard Android `strings.xml`.
    - Context-aware AI prompts passed to the backend in the selected language.

### B. Role-Based Access Control (RBAC)
- **Coach (ADMIN)**: High-level permissions. Can record results for any athlete by voice (e.g., "Record 12.5s for Jovan").
- **Athlete (USER)**: Restricted permissions. Can only record results for themselves (e.g., "Record my result 12.5s").
- **Security**: JWT tokens stored in `EncryptedSharedPreferences`. Backend must validate `athlete_id` against `user_id` for non-admins.

### C. Resource-Saving Logic (The "Efficiency Trick")
- **Level 1 (Local Regex)**: App first attempts to parse simple patterns (e.g., "12.5s") locally to save API costs.
- **Level 2 (Context Injection)**: App automatically attaches `currentEventId` (based on current time) and a list of "Recent Athletes" to the API request to help the AI map names accurately.
- **Level 3 (Minimalist LLM)**: Use Groq/OpenRouter (Llama 3.3) with a strict JSON-only output format and low `max_tokens`.

## 3. Implementation Roadmap

### Phase 1: Backend Preparation (/backend)
- **AI Service Upgrade**: Modify `src/app/lib/service/AISevice.ts` to accept language, role, and `contextHints`.
- **New API Endpoint**: Create `api/voice/process` to handle incoming text from Android.
- **Validation**: Ensure Prisma queries check for valid relationships between events, disciplines, and athletes.

### Phase 2: Android Foundation (/android)
- **Localization**: Setup `res/values/strings.xml` and `res/values-sr/strings.xml`.
- **Auth Module**: Implement login flow connecting to NextAuth credentials provider.
- **Language Manager**: Utility to switch UI and Speech Engine locales.

### Phase 3: Voice & Offline Features
- **Speech Component**: Implement "Push-to-Talk" button using `SpeechRecognizer`.
- **Local DB**: Room DB to cache:
    - Athletes (for name hints).
    - Disciplines & Event Categories (from Prisma Enums).
    - Pending Results (for offline sync).
- **WorkerManager**: Periodic background sync for `isSynced = false` records.

### Phase 4: Feedback Loop
- **Text-to-Speech (TTS)**: Auditory confirmation of recorded data in the selected language.
- **Haptic Feedback**: Vibrations for successful voice capture.

## 4. Development Strategy for AI Agent
- **Cross-Project Context**: Always check `backend/prisma/schema.prisma` before generating Kotlin data models to ensure field alignment.
- **API-First**: Generate the Next.js API route first to establish the communication "contract".
- **Mocking**: Use mock STT results during development to test LLM parsing logic without constantly speaking to the device.
