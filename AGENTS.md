# SASE-310 — AGENTS.md

## Project

Compose Multiplatform (KMP) app for school administration (SASE = Sistema de Administración de Seguimiento Escolar). Targets Android, Desktop (JVM), iOS.

## Build

- **Only active module**: `:composeApp` (declared in `settings.gradle.kts`). The `app/` directory is **not** part of the build — it's stale and its plugins (KSP, Roborazzi, Firebase) are not in the version catalog.
- Version catalog: `gradle/libs.versions.toml` — AGP 8.7.3, Kotlin 2.1.20, Compose Multiplatform 1.7.3.
- Gradle properties enforce `kotlin.compiler.execution.strategy=in-process` (avoids daemon connection issues on some machines).

## Data

All data is **in-memory mock** (`MockSaseData` singleton, `composeApp/src/commonMain/.../data/`). No backend, no database. Student records, audits, documents etc. are hardcoded lists.



## Entrypoints

| Target   | File |
|----------|------|
| Android  | `composeApp/src/androidMain/.../MainActivity.kt` |
| Desktop  | `composeApp/src/desktopMain/.../Main.kt` (window 1280x800) |
| iOS      | `composeApp/src/iosMain/.../MainViewController.kt` |

All three call `SaseAppContent(viewModel = LabViewModel())` from `commonMain`.

## Architecture

Simple single-ViewModel (`LabViewModel` in `commonMain`) with a sealed `Screen` class for navigation (`SecretaryDashboard` / `StudentRecord(id)`). No DI framework. No navigation library.

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

## Auto commit + CI rule

After any approved microphase execution:

**IF:**
- build PASS
- tests PASS
- scope is clean
- no risky unexpected files
- recommendation is commit ready

**THEN automatically execute:**
1. `git status`
2. `git add` only scoped files
3. `git commit` with suggested conventional commit message
4. `git push origin main`
5. `gh run list --branch main --limit 3`
6. `gh run view <latest-run-id>`
7. wait until workflow completes
8. report final CI status

**STRICT RULES:**
- Never add composeApp/build/
- Never add untracked files outside scope
- Never commit if build or tests fail
- Never commit if unexpected files are modified
- Never commit if scope is unclear
- Never proceed to next feature until CI is green

**REPORT FORMAT:**
- files committed
- commit hash
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
