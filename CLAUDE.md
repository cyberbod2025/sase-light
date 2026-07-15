# CLAUDE.md

Guidance for AI assistants (Claude Code and others) working in this repository.

## What this is

**SASE-310** ("SASE Light") — a Compose Multiplatform (KMP) prototype for school
administration. SASE = *Sistema de Administración de Seguimiento Escolar*. It is a
secretary / student-records dashboard with a **"Liquid Glass"** visual aesthetic,
targeting **Android**, **Desktop (JVM)**, and **iOS** from a shared `commonMain`.

The product domain (dashboards, student records/expedientes, pre-solicitudes,
enrollment/inscripciones, credentials) is expressed in **Spanish**. UI strings,
enum labels, and many identifiers are Spanish; keep new user-facing copy in Spanish
to match.

This is a UI/flow prototype: **all data is in-memory mock** — no backend, no
database, no persistence. Everything resets on restart.

## Build & run

Use the Gradle wrapper. **Only `:composeApp` is an active module** (see
`settings.gradle.kts`). There is no `app/` module — it was removed; ignore any older
docs that mention it.

```bash
# Android debug APK
./gradlew :composeApp:assembleDebug

# Run the desktop app (window 1280x800)
./gradlew :composeApp:desktopRun

# Run the test suite (commonTest executes on the desktop/JVM target)
./gradlew :composeApp:desktopTest

# Compile-check desktop without running
./gradlew :composeApp:compileKotlinDesktop

# List all tasks
./gradlew tasks
```

Environment notes:
- Assume a **Linux/bash** shell. Do not use Windows paths (`C:\...`) or `gradlew.bat`
  in commands — those are legacy from the original author's machine.
- `gradle.properties` pins `kotlin.compiler.execution.strategy=in-process` to avoid
  "could not connect to Kotlin compile daemon" errors. Keep it.
- Configuration cache is **off**; caching and parallel are on; workers capped at 4.
- The CI build uses **JDK 17**. Android/desktop compile targets are JVM 11.
- Avoid piping Gradle output through `|`; if you need the log, redirect to a file and
  inspect with `tail`.

## Toolchain versions

Managed in `gradle/libs.versions.toml` (version catalog). Reference deps as
`libs.*` / `libs.plugins.*`, not hardcoded coordinates.

- Kotlin **2.1.20**, Compose Multiplatform **1.7.3**, AGP **8.7.3**, Gradle **8.11.1**
- Compose BOM 2024.12.01, kotlinx-coroutines 1.10.2
- `android { namespace = "com.example"; applicationId = "com.aistudio.labvirtual.kvmpx"; minSdk 24, target/compileSdk 35 }`

## Source layout

All shared code lives under `composeApp/src/commonMain/kotlin/com/example/`.
Package root is `com.example`.

```
commonMain/kotlin/com/example/
├── Platform.kt                      # expect Platform (actuals per target)
├── data/                            # domain models + in-memory mock data
│   ├── MockSaseData.kt              # central mutable mock singleton (students, audits, docs)
│   ├── SaseEntities.kt              # core data classes (Student, SaseAudit, ...)
│   ├── SaseStudentAnalytics.kt
│   ├── InstitutionalStudentRecordResolver.kt
│   ├── StudentCredentialPreview.kt
│   ├── enrollment/                  # annual enrollment ("inscripción anual v2") domain
│   │   ├── EnrollmentModels.kt
│   │   ├── AnnualEnrollmentFlowCoordinator.kt   # LEGACY vs ANNUAL_V2 flow orchestration
│   │   ├── AnnualEnrollmentPlanner.kt / Committer.kt / PersistenceAdapter.kt
│   │   ├── SchoolMovementClassifier.kt
│   │   ├── PermanentEnrollmentIdAllocator.kt
│   │   └── MockEnrollmentData.kt
│   ├── presolicitud/                # family pre-application ("pre-solicitud") domain
│   │   ├── PreApplicationModels.kt / OfficialStudentModels.kt
│   │   ├── PreApplicationAdministrativeUpdate.kt
│   │   └── Mock*Data.kt
│   └── repository/                  # repository interfaces + mock implementations
│       ├── StudentRepository.kt / MockStudentRepositoryImpl.kt
│       └── AuditRepository.kt / MockAuditRepositoryImpl.kt
├── ui/                              # Compose screens & components
│   ├── SaseScreens.kt               # SaseAppContent entrypoint + palette + shell (~1700 lines)
│   ├── PreApplicationFamilyPortalScreen.kt
│   ├── CredentialPreviewScreen.kt / StudentCredentialDashboardScreen.kt
│   ├── dashboard/ · enrollment/ · enrollment/digital/ · presolicitud/ · student/
│   └── theme/                       # Color.kt, Theme.kt, Type.kt
├── util/                            # ToastUtil.kt (LocalToast CompositionLocal)
└── viewmodel/
    ├── LabViewModel.kt              # primary VM: navigation, role, students, audits
    ├── PreApplicationViewModel.kt   # pre-solicitud + enrollment flows (~2300 lines)
    └── InstitutionalAnnualEnrollment.kt
```

Per-target entrypoints, all of which call `SaseAppContent(viewModel = LabViewModel())`:

| Target  | File |
|---------|------|
| Android | `androidMain/.../MainActivity.kt` |
| Desktop | `desktopMain/.../Main.kt` |
| iOS     | `iosMain/.../MainViewController.kt` |

## Architecture

- **No DI framework, no navigation library.** Navigation is a `sealed class Screen`
  in `LabViewModel.kt`; `SaseAppContent` switches on `currentScreen` inside an
  `AnimatedContent`. Add a route by extending `Screen`, wiring it in the
  `when (screen)` block of `SaseAppContent`, and (if it's a sidebar destination)
  `secretarySidebarDestination(...)`.
- **State** is exposed as `kotlinx.coroutines.flow.StateFlow` from the ViewModels and
  collected in composables via `collectAsState()`.
- **Two ViewModels, plain classes** (not `androidx.lifecycle.ViewModel`):
  - `LabViewModel` — app-wide: current screen, mock `AppRole`, students & audits. It
    depends on `StudentRepository` / `AuditRepository` interfaces (mock impls injected
    by default constructor), which is the closest thing to a seam for testing.
  - `PreApplicationViewModel` — the large pre-solicitud + annual-enrollment engine.
    It is instantiated locally with `remember { PreApplicationViewModel() }` inside the
    screens that need it (e.g. `PreApplicationFamilyPortalScreen`), not passed down.
- **Mock data** flows from `MockSaseData` / `Mock*Data` objects through the repository
  mocks. Mutations mutate these in-memory structures; there is no write-through to disk.
- The enrollment engine supports two modes — `EnrollmentFlowMode.LEGACY` and
  `ANNUAL_V2` — coordinated by `AnnualEnrollmentFlowCoordinator`. When touching
  enrollment, respect the mode split and the planner → committer → persistence-adapter
  pipeline.

## Design & UI conventions

- **"Liquid Glass" aesthetic**: frosted/glossy card composables (`GlassCard`,
  `LiquidGlassCard`, `MetricGlassCard`, etc.) defined in the `ui/` layer. Reuse them
  rather than styling raw `Card`/`Box` when adding surfaces.
- **Two color systems, do not confuse them:**
  - The **product palette** lives at the top of `ui/SaseScreens.kt` as top-level
    `val`s: `SaseNavy`, `SaseNavy2`, `SaseGreen`, `SaseGreenDark`, `SaseBlue`,
    `SaseCyan`, `SaseViolet`, `SaseOrange`, `SaseRed`, `SaseText`, `SaseMuted`,
    `SaseBg`, `SaseBorder`, plus `SaseBackgroundBrush`. Most screen styling uses these.
  - The **Material theme** (`ui/theme/Theme.kt`, `MyApplicationTheme`, defaults
    `darkTheme = true`) uses a separate `Tech*`/`Slate*` palette from `Color.kt`.
- Responsive layouts branch on width breakpoints (≈850dp and ≈600dp).
- User feedback goes through the `LocalToast` CompositionLocal (a snackbar wired up in
  `SaseAppContent`); call `toast(...)` rather than managing your own snackbar state.
- Kotlin code style is `official` (`gradle.properties`). Match existing 4-space
  indentation and the Spanish-domain naming already in the files.

## Tests

- Tests live in `composeApp/src/commonTest/kotlin/com/example/` (~427 `@Test` methods)
  and run on the desktop target via `./gradlew :composeApp:desktopTest`. They use the
  multiplatform `kotlin("test")` API (`kotlin.test.*`), not JUnit annotations directly.
- Coverage is concentrated on the **domain/logic layer**: enrollment (planner,
  committer, persistence adapter, flow coordinator, id allocator, movement classifier),
  pre-application guardrails & synchronization, record resolvers/presentations, student
  analytics, and `LabViewModel` navigation. Compose UI rendering is not unit-tested.
- When you change domain logic, add or update the matching `*Test.kt`; the two
  `integration/` tests (`InstitutionalGoldenPathTest`, `...SecretaryEditingIntegrationTest`)
  exercise end-to-end flows and are the best guard against regressions.

## CI

`.github/workflows/build.yml` runs on push/PR to `main`, on `ubuntu-latest` / JDK 17:
1. `:composeApp:assembleDebug` (Build Android)
2. `:composeApp:desktopTest` (Test Desktop)
3. `:composeApp:compileKotlinDesktop` (Build Desktop)

Before pushing, run those three locally so CI stays green. Keep changes scoped to the
active module.

## Working conventions

- **Scope discipline.** Stage explicit paths — never `git add .`. If
  `git status --short` shows unexpected or unscoped changes, stop and check rather than
  sweeping them in. Never commit `composeApp/build/` or other build output.
- **Commit style.** History uses Conventional Commits with a domain scope, e.g.
  `feat(enrollment): ...`, `fix(secretaria): ...`, `feat(expediente): ...`. Match it.
- **Do not commit local agent state**: `.codex/`, `.opencode/`, `*.patch`. If any
  appear, stop and ask.
- `tools/android/*.ps1` are **Windows PowerShell** helper scripts (emulator, APK
  install/launch, screen capture) from the original dev environment — not usable in a
  Linux session; don't invoke them here.

## Additional context files

- `AGENTS.md` — companion agent brief. Mostly still accurate, but note two points where
  **this file supersedes it**: the `app/` module no longer exists, and the active module
  **does** now have a real `commonTest` suite (AGENTS.md predates both).
- `README.md` — short human-facing overview.
- `00_CONTEXT_FOR_AI/` — deeper background: `HUGO_SYSTEM_AGENT_INSTRUCTIONS.md`
  (architecture, aesthetics, safety rules), `SKILLS/` (Codespaces Git/PR and Android
  APK workflows), and `SASE_ABACUS_SANDBOX_v0.1.1/` (project state, prioritized tasks,
  decision log). Consult these for domain/workflow depth; they are written for
  Codespaces and may reference the original Windows environment.
- Root `sase-light-*.html` files are standalone design mockups, not part of the build.
