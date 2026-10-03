# SASE-310

Dashboard de Secretaría y Expedientes con estética Liquid Glass.

Compose Multiplatform app targeting **Android**, **Desktop (JVM)**, and **iOS**.

## Stack

- Kotlin 2.1.20 + Compose Multiplatform 1.7.3
- Gradle 8.11.1, AGP 8.7.3

## Modules

Only `:composeApp` is active. The legacy `app/` module has been removed.

## Data and environments

The active module supports three explicit modes:

- `DEMO_LOCAL`: synthetic in-memory data that resets on restart.
- `SUPABASE_STAGING`: real Supabase Auth and persistence for the institutional
  flow and family pre-applications. It never falls back to mock data.
- `PRODUCTION`: intentionally blocked at boot until the production persistence
  and RLS review is complete.

The family portal remains anonymous by design. It uses the Supabase RPCs and a
short-lived, rotatable bearer token; it does not create Supabase Auth accounts.

The tracked database migrations live in `supabase/migrations/`, numbered
sequentially and applied in order. Each file's header comment states what it
fixes and why, and whether it has been applied to the staging Supabase project
(`SASE-Light`). That directory is the only source of truth for the schema's
state: this README deliberately names no migration numbers, because every
range written here went stale within a review round. To audit or deploy, apply
every file in the directory in order.

The family portal's access-token lifecycle (expiry, revocation, rotation,
anti-abuse rate limiting) is implemented across several of those migrations and
in `PreApplicationViewModel` / `SupabasePreApplicationRepositoryImpl`.

## Setup

1. Open in Android Studio (or IntelliJ with KMP plugin).
2. Run:

```bash
# Android debug
./gradlew :composeApp:assembleDebug

# Android fail-closed release binary
./gradlew :composeApp:assembleRelease

# Desktop
./gradlew :composeApp:desktopRun

# List all tasks
./gradlew tasks
```

## Staging configuration

Connected builds require explicit values and fail if they are missing:

```powershell
.\gradlew.bat :composeApp:assembleDebug --no-daemon `
  -Psase.environment=SUPABASE_STAGING `
  -Psase.supabaseUrl="https://PROJECT_REF.supabase.co" `
  -Psase.supabasePublishableKey="PUBLISHABLE_KEY"
```

Do not commit `local.properties`, `.env.*`, publishable keys or service-role
keys. The publishable key is supplied only by the host/build environment.

## CI and pilot status

GitHub Actions runs Android debug, Android release, Desktop tests and Desktop
compilation. The staging pilot build runs when the repository secrets
`SASE_SUPABASE_URL` and `SASE_SUPABASE_PUBLISHABLE_KEY` are configured.

The Android smoke script can build, install and launch a debug APK on one
authorized physical device or emulator. It does not replace the manual
institutional E2E checklist.

## Vercel

Vercel is not part of the SASE Light delivery path. This repository is a
Compose Multiplatform Android/Desktop/iOS application and does not produce a
web artifact or depend on Vercel. Existing Vercel deployments are external
integration history and are not the Android pilot artifact.

## Known limitations

- Production remains fail-closed until its persistence, RLS and operational
  review is explicitly approved.
- There is no packaged Desktop production distribution.
- iOS is a supported source target but is not a blocker for the Android pilot.
