# 03 — Estado Actual del Proyecto

> Sprint: H1 — Documental de Contexto
> Fecha: 2026-07-07
> Último merge documentado: PR #35

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

### PR #20 — Reemplazar println toast con Snackbar real

**Commit:** `b117f0c` — fix(sase): replace println toast with real Snackbar
**CI:** PASS
**Cambio:** Reemplazado `println("Llamando a tutor...")` con Snackbar de Material3 via `snackbarHostState.showSnackbar()` en `StudentRecordScreen`.

### PR #21 — Eliminar botón Edit muerto de expediente

**Commit:** `55f34a2` — fix(sase): remove dead edit button from student record
**CI:** PASS
**Cambio:** Eliminado botón "Edit" no funcional de `StudentRecordScreen` que no estaba conectado a funcionalidad de edición.

### PR #22 — Contacto de emergencia desde modelo Student

**Commit:** `3769750` — feat(sase): source emergency contact from Student model
**CI:** PASS
**Cambio:** La visualización de contacto de emergencia ahora obtiene datos de los campos reales del modelo `Student` (`emergencyContactName`, `emergencyContactRelation`, `emergencyContactPhone`, `emergencyContactEmail`) en lugar de valores hardcoded demo.

### PR #23 — Eliminar PlaceholderStep, Ver todos stub y FASE 1 label

**Commit:** `9fb32a1` — chore(sase): remove dead PlaceholderStep, Ver todos stub, FASE 1 label
**CI:** PASS
**Cambio:** Removidos `PlaceholderStep` composable no usado, botón "Ver todos" stub y label "FASE 1" del dashboard.

### PR #24 — Eliminar navegación admin en portal familiar

**Commit:** (PR #24) — fix(portal): remove admin navigation, fix dark-theme visibility
**CI:** PASS
**Cambio:** Eliminados elementos de navegación de administración del portal familiar. Corregida visibilidad en dark theme. Archivo: `PreApplicationFamilyPortalScreen.kt`.

### PR #25 — CURP auto-completa fecha nacimiento, sexo y entidad

**Commit:** (PR #25) — feat(portal): CURP auto-fill fecha nacimiento, sexo y entidad
**CI:** PASS
**Cambio:** Al ingresar CURP, auto-completa los campos de fecha de nacimiento, sexo y entidad federativa. Mejora de UX en formulario de pre-solicitud.

### PR #26 — Dividir nombre completo en 3 campos

**Commit:** (PR #26) — feat(portal): split nombre completo into apellido paterno, materno, nombre
**CI:** PASS
**Cambio:** Campo de nombre completo reemplazado por 3 campos separados: apellido paterno, apellido materno y nombre(s). Actualizados modelo y datos mock.

### PR #27 — Fecha de nacimiento seccionada en día, mes, año

**Commit:** (PR #27) — feat(portal): fecha nacimiento seccionada en día, mes, año
**CI:** PASS
**Cambio:** Campo de fecha de nacimiento reemplazado por 3 selectores desplegables para día, mes y año. Mejora de UX para ingreso de fechas.

### PR #28 — Trabajo social usa checkboxes con opción Otro

**Commit:** (PR #28) — feat(portal): trabajo social usa checkboxes con opción Otro
**CI:** PASS
**Cambio:** Sección de trabajo social ahora usa checkboxes con opción "Otro" en lugar de campos de texto.

### PR #29 — Scroll arriba al cambiar de sección

**Commit:** (PR #29) — feat(portal): scroll arriba al cambiar de sección
**CI:** PASS
**Cambio:** Auto-scroll al inicio de página al cambiar de sección en formulario multi-paso de pre-solicitud.

### PR #30 — Corrección de 5 bugs en portal

**Commit:** (PR #30) — fix(portal): 5 bugs reportados por Codex
**CI:** PASS
**Cambio:** Corregidos 5 bugs encontrados por Codex code review en el portal familiar.

### PR #31 — Gráficas de inscripción y botón Validar en dashboard

**Commit:** (PR #31) — feat(sase): add enrollment charts and validate button to secretary dashboard
**CI:** PASS
**Cambio:** Agregadas gráficas de inscripción al dashboard de secretaría. Agregado botón "Validar" para acciones de inscripción.

### PR #32 — Cerrar y Cancelar regresan al dashboard

**Commit:** (PR #32) — fix(portal): Cerrar y Cancelar regresan al dashboard
**CI:** PASS
**Cambio:** Los botones Cerrar y Cancelar en diálogos del portal ahora navegan correctamente de regreso al dashboard.

### PR #33 — Nueva solicitud cierra diálogo y regresa al dashboard

**Commit:** (PR #33) — fix(portal): Nueva solicitud cierra dialogo y regresa al dashboard
**CI:** PASS
**Cambio:** Diálogo de nueva solicitud se cierra y retorna al dashboard al confirmar envío.

### Commit directo `6d3d157` — Documentación de agente y contexto Codespaces

**Commit:** `6d3d157` — docs: update AGENTS.md for Codespaces + add internal agent context files
**CI:** PASS
**Cambio:** Creados `00_CONTEXT_FOR_AI/HUGO_SYSTEM_AGENT_INSTRUCTIONS.md` y `00_CONTEXT_FOR_AI/SKILLS/SASE_LIGHT_CODESPACES_GIT_PR_SKILL.md`. Actualizado AGENTS.md con secciones de entorno.

### PR #35 — Contexto de agente interno y workflow Codespaces

**Commit:** (PR #35) — docs(system): add internal agent context and codespaces workflow skill
**CI:** PASS
**Cambio:** Actualizado AGENTS.md con 3 nuevas secciones: Agent workflow references, Local agent state, Scope discipline.

## Archivos fuente

```
commonMain/ → 30 archivos en 14 subdirectorios
commonTest/ → 2 archivos de prueba
```
