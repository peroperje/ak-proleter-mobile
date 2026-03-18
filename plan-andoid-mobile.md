# Strategic Plan: AK Proleter Android Mobile App

## 1. Context & Environment
**Structure**: Native Kotlin + Jetpack Compose application.
**Project Root**: `/home/petar/Projects/ak-proleter-mobile` (this directory will hold the Android source code).
**Backend**: Existing Next.js + Prisma + PostgreSQL application (located at `/home/petar/Projects/ak-proleter`).

**Goal**: Enable a voice-driven interface for coaches (admins) and athletes (users) to record results and manage events.
**Target User**: Technically non-proficient coaches on an athletic field.

## 2. Core Architectural Pillars

### A. Multi-Language Speech Engine
- **Dynamic Locale**: Support for Serbian (sr-RS) and English (en-US).
- **Implementation**:
    - Android `SpeechRecognizer` configured at runtime based on user settings.
    - Localized UI via standard Android `strings.xml`.
    - Context-aware AI prompts passed to the existing backend in the selected language.

### B. Role-Based Access Control (RBAC) - Client Side
- **Coach (ADMIN)**: High-level permissions. Can record results for any athlete by voice (e.g., "Record 12.5s for Jovan").
- **Athlete (USER)**: Restricted permissions. Can only record results for themselves (e.g., "Record my result 12.5s").
- **Security**: JWT tokens stored in `EncryptedSharedPreferences`.
- **Session Management**: Integration with NextAuth tokens from the existing backend.

### C. Client-Side Efficiency & Context Logic
- **Level 1 (Local Regex)**: App first attempts to parse simple patterns (e.g., "12.5s") locally to save API costs.
- **Level 2 (Context Injection)**: App automatically attaches `currentEventId` (based on current time) and a list of "Recent Athletes" (cached locally) to the API request to help the AI map names accurately.

## 3. Implementation Roadmap (Android Only)

### Phase 1: Android Foundation
- **Project Initialization**: Setup Android project in the root folder with Jetpack Compose.
- **Localization**: Setup `res/values/strings.xml` and `res/values-sr/strings.xml`.
- **Auth Module**: Implement login flow connecting to the existing backend's NextAuth credentials provider.
- **Network Layer**: Retrofit or Ktor setup to communicate with the existing backend API endpoints (e.g., `/api/voice/process`).
- **Language Manager**: Utility to switch UI and Speech Engine locales.

### Phase 2: Voice & Offline Features
- **Speech Component**: Implement "Push-to-Talk" button using `SpeechRecognizer`.
- **Local DB**: Room DB to cache:
    - Athletes (for name hints).
    - Disciplines & Event Categories (fetched from the existing backend).
    - Pending Results (for offline sync).
- **WorkerManager**: Periodic background sync for `isSynced = false` records.

### Phase 3: Feedback Loop
- **Text-to-Speech (TTS)**: Auditory confirmation of recorded data in the selected language.
- **Haptic Feedback**: Vibrations for successful voice capture.

## 4. Development Strategy for AI Agent
- **Schema Alignment**: Always check `/home/petar/Projects/ak-proleter/prisma/schema.prisma` before generating Kotlin data models to ensure field alignment with the existing database.
- **API Communication**: Reference the existing API logic in the backend project when implementing the mobile networking layer.
- **Mocking**: Use mock STT results during development to test UI and parsing logic without constantly speaking to the device.
