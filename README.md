# 🏃 AK Proleter Mobile: AI-Powered Athletic Training Assistant

<div align="center">
  <p><em>Empowering coaches and athletes with a voice-driven performance pipeline.</em></p>
</div>

[![Tech Stack](https://img.shields.io/badge/Stack-Kotlin_|_Compose_|_Hilt_|_Room-blue.svg)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-Private-red.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/JDK-17-orange.svg)](https://openjdk.org/)

AK Proleter Mobile is a sophisticated AI-powered Android application designed to streamline the training and management workflows for athletes and coaches of **AK Proleter**. By leveraging a voice-first interface, it enables seamless recording of results, event management, and AI-driven performance insights, even in demanding field conditions.

---

## 📖 Table of Contents
- [✨ Key Features](#-key-features)
- [🏗️ Repository Structure](#️-repository-structure)
- [🛠️ Tech Stack](#️-tech-stack)
- [🚀 Quick Start](#-quick-start)
- [⚙️ Configuration](#️-configuration)
- [🏢 Architecture Overview](#-architecture-overview)
- [🧪 Testing](#-testing)
- [📄 License](#-license)

---

## ✨ Key Features

- **🎙️ AI-Powered Voice Assistant**: Native Android Speech-to-Text (`VoiceManager`) integrated with a centralized state machine for recording results (e.g., "Record 12.5s for Jovan").
- **🌍 Multi-Language Support**: Full support for **Serbian (sr-RS)** and **English (en-US)**, with dynamic locale switching for both UI and Speech Engine.
- **🛡️ Role-Based Access (RBAC)**:
  - **Coaches (ADMIN)**: Full access to record and manage results for any athlete.
  - **Athletes (USER)**: Restricted access to view and record personal training data.
- **📶 Offline-First Architecture**: Robust [Room](https://developer.android.com/training/data-storage/room) database integration ensures full functionality and data caching during poor network conditions on the track.
- **💡 Context-Aware AI**: Automatically attaches local context (current event, recent athletes) to voice processing requests for high-accuracy speech-to-data mapping.
- **🎨 Modern UI/UX**: Built entirely with Jetpack Compose and Material 3, featuring glassmorphism elements and smooth micro-animations.

---

## 🏗️ Repository Structure

| Path | Purpose | Type |
| :--- | :--- | :--- |
| `app/src/main/java/.../data` | Data layer (Local Room DB, Remote Retrofit API). | Kotlin |
| `app/src/main/java/.../ui` | Presentation layer (Compose, ViewModels, Theme). | Jetpack Compose |
| `app/src/main/java/.../di` | Hilt modules for dependency injection. | Kotlin |
| `app/src/main/java/.../voice` | Core voice orchestration and STT logic. | Kotlin |
| `app/src/main/java/.../util` | App-wide utilities, constants, and extensions. | Kotlin |

---

## 🛠️ Tech Stack

### Frontend & UI
- **Language**: Kotlin 1.9+
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design System**: Material 3
- **Navigation**: Compose Navigation + Auth State Switching

### Backend & Storage
- **Local DB**: [Room](https://developer.android.com/training/data-storage/room) (SQLite) with Offline Sync
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + OkHttp
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (Dagger-based)
- **Concurrency**: Kotlin Coroutines & `StateFlow`

### AI & Services
- **Speech Engine**: Android Native `SpeechRecognizer`
- **AI Backend**: Next.js API (External)
- **Auth**: JWT based integration with NextAuth

---

## 🚀 Quick Start

### 1. Prerequisites
- **Android Studio Iguana** (2023.2.1) or newer
- **JDK 17**
- **Android SDK 34** (targetSdkVersion)

### 2. Installation
```bash
git clone git@github.com:peroperje/ak-proleter-mobile.git
cd ak-proleter-mobile
./gradlew build
```

### 3. Execution Commands
| Task | Command | Target |
| :--- | :--- | :--- |
| **Build Debug APK** | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/` |
| **Run Unit Tests** | `./gradlew test` | JVM |
| **Run Linter** | `./gradlew ktlintCheck` | Formatting |

---

## ⚙️ Configuration

### API Connection
Edit `app/src/main/java/com/akproleter/mobile/util/Constants.kt` to configure your backend endpoint:
```kotlin
object Constants {
    const val BASE_URL = "https://your-api-endpoint.com/api/"
    const val DB_NAME = "ak_proleter_db"
}
```

### Localization
The app uses standard Android resources for localization:
- **English**: `app/src/main/res/values/strings.xml`
- **Serbian**: `app/src/main/res/values-sr/strings.xml`

---

## 🏢 Architecture Overview

This project follows a strict **MVVM (Model-View-ViewModel)** architecture combined with **Clean Architecture** principles:

1.  **UI Layer**: Jetpack Compose functions reacting to `UIState` exposed by ViewModels.
2.  **Domain/Repository Layer**: Logic that orchestrates data flow between the Local DB (Room) and Remote API (Retrofit).
3.  **Data Layer**: Actual implementations of network calls and database queries.

For more details, please refer to the [ARCHITECTURE.md](ARCHITECTURE.md) file.

---

## 🧪 Testing

We ensure reliability through a multi-layered testing strategy:
- **Unit Tests**: Business logic and Repository validation in `app/src/test/`.
- **Instrumentation Tests**: UI testing with Compose Testing library in `app/src/androidTest/`.

```bash
# Run all tests
./gradlew connectedAndroidTest
```

---

## 📄 License

Private project for personal use by AK Proleter. Developed by **[Petar Borovcanin](https://github.com/peroperje)**.
