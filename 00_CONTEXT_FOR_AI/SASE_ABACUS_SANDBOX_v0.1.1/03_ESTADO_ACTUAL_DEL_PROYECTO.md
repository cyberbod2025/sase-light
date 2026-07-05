# 03 — Estado Actual del Proyecto

> Sprint: H1 — Documental de Contexto
> Fecha: 2026-07-04
> Últimos merges documentados: PR #8 `66eb71f`, PR #10 `3e36e87`

## Resumen

Aplicación Compose Multiplatform (KMP) para administración escolar — SASE (Sistema de Administración de Seguimiento Escolar). Targeta Android, Desktop (JVM) e iOS.

## Stack técnico

| Componente | Versión |
|---|---|
| Kotlin | 2.1.20 |
| Compose Multiplatform | 1.7.3 |
| Gradle | 8.11.1 |
| AGP | 8.7.3 |

## Arquitectura

- **Sin DI**: No hay framework de inyección de dependencias.
- **Sin navegación**: Navegación manual mediante `sealed class Screen` + estado en `LabViewModel`.
- **Single ViewModel**: `LabViewModel` orquesta toda la UI.
- **ViewModel adicional**: `PreApplicationViewModel` para el flujo de pre-solicitud.

## Datos

Todos los datos son **in-memory mock**:

- `MockSaseData` — estudiantes, auditorías, documentos
- `MockEnrollmentData` — solicitudes de inscripción
- `MockPreApplicationData` — pre-solicitudes
- `MockOfficialStudentData` — alumnos oficiales (OFF-*)
- No hay backend, no hay base de datos, no hay persistencia.

## Módulos activos

Solo `:composeApp`. El módulo `app/` legacy fue removido del build.

## CI/CD

Workflow: `.github/workflows/build.yml` — 3 jobs en ubuntu-latest:
1. Build Android (`assembleDebug`)
2. Test Desktop (`desktopTest`)
3. Build Desktop (`compileKotlinDesktop`)

Gatillado en push/PR a `main`.

## Puntos de entrada

| Target | Entrypoint |
|---|---|
| Android | `MainActivity.kt` → `SaseAppContent(viewModel = LabViewModel())` |
| Desktop | `Main.kt` (ventana 1280×800) → `SaseAppContent(viewModel = LabViewModel())` |
| iOS | `MainViewController.kt` → `SaseAppContent(viewModel = LabViewModel())` |

## Diseño

- "Liquid Glass" — `GlassCard`, `LiquidGlassCard`, `MetricGlassCard`
- Dark theme por defecto
- Breakpoints responsive: 850dp y 600dp

## Pruebas

- `commonTest` contiene 3 archivos:
  - `SaseStudentAnalyticsTest.kt`
  - `PreApplicationGuardrailsTest.kt`
  - `MockSaseDataTest.kt`
- Ejecución: `.\gradlew.bat :composeApp:desktopTest --no-daemon`

## Historial de entregas

### H0 — Sanitizar demo data (PR #1)

**Commit:** `2fe2b4c` — chore(sase): sanitize demo data and fix matricula guardrail test
**CI:** Build #35 ✅, Build #36 ✅ (main)
**Cambio:** CURP del master student `CURP-DEMO-01` → `CURP-SASE-01` para evitar enlace stale con OFF-101.

### PR-D — Eliminar código muerto (PR #2)

**Commit:** `c7bbf06` — fix(sase): remove unreachable group confirmation code
**CI:** Build #37 ✅, Build #38 ✅ (main)
**Cambio:** Línea muerta en `confirmInitialGroup` eliminada. `if/else` anidado reemplazado por early return.

### PR #5 — Ignorar archivos locales de entorno

**Commit:** `d8815c6` — chore(repo): ignore local environment files
**CI:** Build en PR #5 ✅, mergeado a main
**Cambio:** `.env.*`, `.vercel/` y `composeApp/build/` agregados a `.gitignore`.

### PR #6 — Eliminar integración Gemini

**Commit:** `963878f` — chore(gemini): remove Gemini AI integration and its dependencies
**CI:** Build en PR #6 ✅, mergeado a main
**Cambio:** Removidos `GeminiImageGenerator`, `GeminiViewModel`, `GeminiTestCard`, dependencias Ktor/Serialization/Napier/Secrets, `.env.example`, `getApiKey()` de 4 Platform files, `INTERNET` permission de AndroidManifest, referencias en docs. SASE Light queda sin integración Gemini activa y con superficie de secretos reducida.

### PR #8 — Guardrails de datos mock de estudiantes

**Commit:** `2bd562c` — test(sase): add mock student data guardrails
**CI:** mergeado a main en `66eb71f`
**Cambio:** Agregada suite `MockSaseDataTest.kt` para cubrir altas de estudiantes mock, unicidad de CURP, búsqueda normalizada por CURP, actualización sin duplicados y preservación de conteos ante IDs inexistentes. Refuerza que el origen de datos sigue siendo in-memory mock.

### PR #10 — Salidas de navegación desde dashboards

**Commit:** `477c13a` — fix(sase): improve dashboard navigation exits
**CI:** mergeado a main en `3e36e87`
**Cambio:** Mejoradas las salidas de navegación desde pantallas dashboard para mantener rutas claras de regreso al inicio/flujo institucional sin introducir navegación externa ni librerías nuevas.

## Archivos fuente

```
commonMain/ → 30 archivos en 14 subdirectorios
commonTest/ → 2 archivos de prueba
```
