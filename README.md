# SASE-310

Dashboard de Secretaría y Expedientes con estética Liquid Glass.

Compose Multiplatform app targeting **Android**, **Desktop (JVM)**, and **iOS**.

## Stack

- Kotlin 2.1.20 + Compose Multiplatform 1.7.3
- Gradle 8.11.1, AGP 8.7.3

## Modules

Only `:composeApp` is active. The legacy `app/` module has been removed.

## Data

All data is **in-memory mock** (`MockSaseData` singleton). No backend, no database.

## Setup

1. Open in Android Studio (or IntelliJ with KMP plugin).
2. Run:

```bash
# Android
./gradlew :composeApp:assembleDebug

# Desktop
./gradlew :composeApp:desktopRun

# List all tasks
./gradlew tasks
```

## Known gaps

- No CI/CD pipeline.
- No tests in the active module.
- No persistence layer — all data lost on restart.
