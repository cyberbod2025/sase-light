# SASE-310

Dashboard de Secretaría y Expedientes con estética Liquid Glass.

Compose Multiplatform app targeting **Android**, **Desktop (JVM)**, and **iOS**.

## Stack

- Kotlin 2.1.20 + Compose Multiplatform 1.7.3
- Ktor + kotlinx.serialization (Gemini API client)
- Napier logging, Secrets Gradle Plugin (`.env`)
- Gradle 8.11.1, AGP 8.7.3

## Modules

Only `:composeApp` is active. The legacy `app/` module has been removed.

## Data

All data is **in-memory mock** (`MockSaseData` singleton). No backend, no database.

## Setup

1. Open in Android Studio (or IntelliJ with KMP plugin).
2. Create `.env` at project root with `GEMINI_API_KEY=your_key` (see `.env.example`).
3. Run:

```bash
# Android
./gradlew :composeApp:assembleDebug

# Desktop
./gradlew :composeApp:desktopRun

# List all tasks
./gradlew tasks
```

## API Key resolution

| Platform | Source |
|----------|--------|
| Android | `BuildConfig.GEMINI_API_KEY` |
| Desktop | `System.getenv("GEMINI_API_KEY")` |
| iOS | `""` (not implemented — Gemini disabled on iOS) |

## Known gaps

- No CI/CD pipeline.
- No tests in the active module.
- No persistence layer — all data lost on restart.
- Gemini image generation non-functional on iOS (`getApiKey()` returns `""`).
- Android `AndroidManifest.xml` missing `INTERNET` permission (Gemini API calls will fail).
