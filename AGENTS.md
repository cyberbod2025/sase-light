# SASE-310 — AGENTS.md

## Project

Compose Multiplatform (KMP) app for school administration (SASE = Sistema de Administración de Seguimiento Escolar). Targets Android, Desktop (JVM), iOS.

## Build

- **Only active module**: `:composeApp` (declared in `settings.gradle.kts`). The `app/` directory is **not** part of the build — it's stale and its plugins (KSP, Roborazzi, Firebase) are not in the version catalog.
- Version catalog: `gradle/libs.versions.toml` — AGP 8.7.3, Kotlin 2.1.20, Compose Multiplatform 1.7.3.
- Gradle properties enforce `kotlin.compiler.execution.strategy=in-process` (avoids daemon connection issues on some machines).

## Data

Three explicit runtime modes (`AppEnvironmentMode` in `environment/AppEnvironment.kt`), wired by `SaseCompositionRoot`:

- `DEMO_LOCAL` — in-memory mock (`MockSaseData` singleton and `Mock*RepositoryImpl`, `composeApp/src/commonMain/.../data/`). No backend, no persistence across restarts.
- `SUPABASE_STAGING` — real persistence via `Supabase*RepositoryImpl` against a Supabase project. Never falls back to mock data; missing configuration fails the build/boot instead of degrading silently.
- `PRODUCTION` — blocked at boot (`SaseCompositionRoot` returns a `ConfigurationFailure`) until RLS policies are validated.

The mode is never inferred at runtime with a default: connected modes require `sase.supabaseUrl` and `sase.supabasePublishableKey` (enforced in `composeApp/build.gradle.kts`).



## Entrypoints

| Target   | File |
|----------|------|
| Android  | `composeApp/src/androidMain/.../MainActivity.kt` |
| Desktop  | `composeApp/src/desktopMain/.../Main.kt` (window 1280x800) |
| iOS      | `composeApp/src/iosMain/.../MainViewController.kt` |

All three call `SaseAppContent(viewModel = LabViewModel())` from `commonMain`.

## Architecture

Two ViewModels, no DI framework, no navigation library. `LabViewModel` (`commonMain`) owns a sealed `Screen` class for navigation and the institutional flow; `PreApplicationViewModel` owns the family pre-application flow independently. `SaseCompositionRoot` is the single point where a runtime mode becomes concrete repository instances — see Data above.

## Design conventions

- "Liquid Glass" aesthetic — `GlassCard`, `LiquidGlassCard`, `MetricGlassCard` composables with frosted/glossy effects.
- Custom color palette in `SaseScreens.kt` (top of file): `SaseNavy`, `SaseGreen`, `SaseBlue`, etc.
- Dark theme is default (`MyApplicationTheme` in `Theme.kt` defaults to `darkTheme = true`).
- Responsive layout at 850dp and 600dp breakpoints.

## Important gotchas

- The `app/` directory at root is **dead code** — it is not included in `settings.gradle.kts` and references plugins not in the version catalog. Do not edit files there unless explicitly asked.
- No test suites are wired into the active build (the test files in `app/src/test/` belong to the stale module).

## Environment

- **Current operational environment**: Windows workstation
- All commands assume PowerShell from the repository root
- Use Windows paths and `.\gradlew.bat`

## Gradle execution

Always use:
- `.\gradlew.bat`
- `--no-daemon` to avoid stale daemon issues
- No pipes (`|`) during Gradle execution — use `*>` to redirect to log file if needed
- `Get-Content <log> -Tail 120` to inspect output

## Commit + PR rule

Never commit or push directly to `main`. Every change goes through a short branch, per the portfolio-wide branch governance (D-004 in `_Shared/COMMAND-CENTER/DECISIONES.md`):

After any approved microphase execution:

**IF:**
- build PASS
- tests PASS
- scope is clean
- no risky unexpected files
- recommendation is commit ready

**THEN execute:**
1. `git status`
2. `git add` only scoped files
3. `git commit` with a conventional commit message
4. `git push -u origin <branch>` (never `main`)
5. `gh pr create` targeting `main`
6. `gh pr checks <pr-number> --watch` (or `gh run list --branch <branch> --limit 3`)
7. wait until CI completes
8. report final CI status; merge (squash) and delete the branch only after Hugo confirms, unless he has explicitly pre-authorized autonomous merge for this task

**STRICT RULES:**
- Never add composeApp/build/
- Never add untracked files outside scope
- Never commit if build or tests fail
- Never commit if unexpected files are modified
- Never commit if scope is unclear
- Never proceed to next feature until CI is green
- Never push or merge directly to `main`

**REPORT FORMAT:**
- files committed
- commit hash
- PR number/URL
- workflow run ID
- Build Android
- Test Desktop
- Build Desktop
- errors if any

## Agent workflow references

- `00_CONTEXT_FOR_AI/HUGO_SYSTEM_AGENT_INSTRUCTIONS.md` — system instructions for AI agents (architecture, rules, aesthetics, security, communication)
- `00_CONTEXT_FOR_AI/SKILLS/SASE_LIGHT_WINDOWS_GIT_PR_SKILL.md` — secure Git/PR workflow skill for Windows (ritual, branching, validation, commit, PR, CI, conflicts, visual theme, sensitive data)

Before making changes, agents **must** read both files above. These define the current instruction architecture, Git/PR workflow, safety rules, scope rules, Windows validation ritual and SASE Light guardrails.

## Local agent state

Do not commit local agent state such as:
- `.codex/`
- `.opencode/`
- `*.patch`

If such files appear in `git status --short`, stop and ask for authorization.

## Scope discipline

If `git status --short` is not clean, stop.

Do not use:
```
git add .
```

Use explicit file paths only.
