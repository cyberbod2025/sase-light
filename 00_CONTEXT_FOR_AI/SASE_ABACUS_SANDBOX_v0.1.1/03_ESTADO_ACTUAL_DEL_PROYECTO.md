# 03 — Estado Actual del Proyecto

> Sprint: H1 — Documental de Contexto
> Fecha: 2026-07-05
> Último merge documentado: PR #19 `724b6cc`

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
- No reemplazar el tema oscuro / Liquid Glass por una paleta clara institucional sin autorización explícita y scope dedicado.

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

### PR #12 — Corrección de textos visibles de UI

**Commit:** `1cc2036` — chore(ui): fix Spanish accent labels
**CI:** mergeado a main en `700d09a`; `desktopTest` PASS; CI PASS
**Cambio:** Corregidos 18 textos visibles de UI con acentos y un typo en `PreApplicationFamilyPortalScreen.kt`, `SaseScreens.kt`, `SecretariaEnrollmentDashboard.kt` y `CredentialPreviewScreen.kt`. Cambios text-only, sin lógica de UI, modelos, mocks ni tema visual.

### Cierre operativo post PR #12 — Cambios locales fuera de scope

**Estado:** Cambios locales no solicitados en `Color.kt`, `Theme.kt` y `Type.kt` fueron restaurados.
**Motivo:** Intentaban reemplazar el tema oscuro / Liquid Glass por un tema claro institucional, contradiciendo la convención vigente y el scope de PR #12.
**Regla:** No conservar ni reintroducir cambios visuales de tema fuera de scope. `.codex/`, `.opencode/`, patches locales y archivos de estado de agentes son estado local y no deben commitearse.

### PR #13 — Actualizar living memory con PRs #8 y #10

**Commit:** docs(update-living-memory-pr8-pr10)
**CI:** PASS
**Cambio:** Actualizados los 3 archivos de memoria del proyecto (`03_`, `07_`, `10_`) con los registros de PR #8 y PR #10.

### PR #14 — Agregar instrucciones de agente HUGO SYSTEM

**Commit:** docs(hugo): add agent instructions
**CI:** PASS
**Cambio:** Creado `HUGO_SYSTEM_AGENT_INSTRUCTIONS.md` con instrucciones operativas para agentes (rol, reglas doradas, workflow, formato). Incorpora feedback de Codex: fix de `git status --short` antes de checkout/pull.

### PR #15 — Habilitar navegación sidebar en expediente del alumno

**Commit:** fix(sase): enable student record sidebar navigation
**CI:** PASS
**Cambio:** Conectado `SaseSidebar` con `onItemClick` en `StudentRecordScreen.kt` para permitir navegación lateral desde el expediente del alumno.

### PR #16 — Clarificar acciones no disponibles en expediente

**Commit:** fix(sase): clarify unavailable student record actions
**CI:** PASS (Build Android ✅, Test Desktop ✅, Build Desktop ✅)
**Cambio:** Cambiados botones de edición/documentos en `StudentRecordScreen.kt` para indicar que son funciones no disponibles (toasts informativos), sin prometer funcionalidad real.

### PR #17 — Clarificar copia del portal de preregistro familiar

**Commit:** fix(sase): clarify family preregistration copy
**CI:** PASS (Build Android ✅, Test Desktop ✅, Build Desktop ✅)
**Cambio:** Aclarado el texto del portal familiar en `PreApplicationFamilyPortalScreen.kt` para distinguir entre pre-inscripción y reinscripción.

### PR #18 — Ocultar acciones de Secretario para otros roles

**Commit:** fix(sase): hide incident/escalation actions for non-secretary roles
**CI:** PASS (Build Android ✅, Test Desktop ✅, Build Desktop ✅)
**Cambio:** Agregado parámetro `userRole` a `StudentRecordScreen`. Botones "Registrar incidencia" y "Escalar caso" solo visibles para rol `SECRETARIA`. Pasado `currentRole` desde `SaseAppContent`.

### PR #19 — Reglas multi-criterio de asignación de grupo

**Commit:** feat(sase): multi-criteria group assignment rules
**CI:** PASS (Build Android ✅, Test Desktop ✅, Build Desktop ✅)
**Cambio:** `OfficialStudent` ahora incluye `alumnoSexo`, `alumnoEdad`, `promedio`. `suggestInitialGroup` usa scoring: (sexo×3) + (edad×2) + (promedio×1). Capacidad máxima 30/grupo. Helper `calculateAgeFromBirthDate` calcula edad desde `dd/Mes/yyyy`. Mock data actualizado con sexo/edad/promedio.

## Archivos fuente

```
commonMain/ → 30 archivos en 14 subdirectorios
commonTest/ → 2 archivos de prueba
```
