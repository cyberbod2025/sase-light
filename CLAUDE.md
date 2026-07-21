# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

SASE-310 ("SASE Light") — dashboard de administración escolar (Secretaría y Expedientes) construido con Compose Multiplatform (Kotlin 2.1.20, Compose MP 1.7.3), con targets Android, Desktop (JVM) e iOS. Los datos del alumnado son mock en memoria (sin base de datos, sin persistencia — todo se reinicia al reiniciar la app). `:composeApp` es el único módulo de Gradle. La única costura de backend real esbozada hasta ahora es la autenticación de personal (ver `data/auth/` y `supabase/migrations/`), deliberadamente limitada a staff y todavía **no conectada** al cliente.

## Visión y alcance final de SASE (brújula del proyecto)

**Objetivo general:** convertirse en el sistema operativo institucional de la Secundaria 310 — una plataforma única que concentre, organice y dé seguimiento a la información escolar del alumnado desde el primer contacto con la familia hasta su egreso.

**Principio central:** un dato se captura una sola vez y se reutiliza de manera segura en todas las áreas que lo necesitan.

**El problema que resuelve.** Hoy muchos procesos escolares se sostienen con formularios separados, hojas impresas, archivos duplicados, mensajes dispersos y datos que cada área vuelve a pedir. SASE busca evitar: que las familias entreguen la misma información varias veces; que Secretaría capture de nuevo lo que ya llenó la familia; que Dirección, Trabajo Social, Médico Escolar, UDEII y docentes trabajen con versiones diferentes; que incidencias, acuerdos, documentos o seguimientos se pierdan; y que la escuela tenga datos almacenados pero no información útil para decidir.

**Recorrido completo del alumno:**
```
Portal de la familia → Pre-solicitud → Revisión de Secretaría → Validación de documentos y requisitos
→ Estado READY → Alta oficial → Matrícula → Expediente institucional único → Asignación de grupo
→ Credencial → Seguimiento durante el ciclo escolar → Historial institucional
```
La familia podría iniciar el trámite desde casa mediante una URL o QR. Secretaría revisaría pendientes, cotejaría originales, resolvería conflictos y convertiría la información en un expediente oficial sin volver a capturarla.

**Qué debe hacer SASE cuando esté al 100 %:**
- *Admisión e inscripción:* pre-solicitudes, validaciones, duplicidades, documentos, alta oficial, matrícula, grupo y credencialización.
- *Expediente institucional único:* datos personales y familiares; responsables y personas autorizadas; historial de inscripciones; documentación; información médica; información socioeconómica; consentimientos; alertas y factores de riesgo; seguimiento académico y de convivencia.
- *Operación escolar cotidiana:* asistencia, retardos, incidencias, citatorios, acuerdos familiares, seguimiento académico, intervenciones, canalizaciones, tutoría, apoyos de UDEII, alertas institucionales.
- *Trabajo coordinado por áreas* — cada área ve solo lo que necesita según su función; el sistema deberá trabajar con roles, permisos, privacidad y trazabilidad: Secretaría (inscripción, matrícula, documentos, expediente), Dirección (indicadores, alertas, decisiones y seguimiento institucional), Trabajo Social (contexto familiar, intervenciones y acuerdos), Médico Escolar (antecedentes y alertas de salud), UDEII (BAP, apoyos, ajustes y seguimiento), docentes y tutoría (asistencia, desempeño, convivencia y evolución del alumno).
- *Inteligencia institucional:* la meta no es solo almacenar, sino ayudar a detectar alumnos con ausencias recurrentes, casos sin seguimiento, documentación pendiente, riesgos de abandono, patrones de incidencias, necesidades de intervención, acuerdos vencidos y grupos que requieren apoyo. La IA puede resumir, orientar y proponer acciones, pero las decisiones siguen siendo humanas e institucionales.

**Propuesta de valor.** SASE no es "otro formulario escolar": es un sistema modular de acompañamiento y seguimiento escolar que transforma datos dispersos en un expediente vivo, reutilizable y útil para actuar a tiempo. Su ventaja diferencial es que conecta todo el ciclo institucional:
```
Captura → Validación → Expediente → Operación → Seguimiento → Alertas → Decisiones → Evidencia
```

**Dónde estamos ahora.** La versión actual es todavía un prototipo demostrable, mock e in-memory. Se está endureciendo el primer flujo real: Pre-solicitud → Secretaría → Alta oficial → Expediente → Decisión de grupo → Credencial mock. La corrección de CURP no es "el objetivo de SASE": es una prueba fundamental de la filosofía completa — evitar duplicidades, proteger la identidad del alumno, impedir altas incorrectas y conservar trazabilidad.

**En una frase:** SASE Light busca que la Secundaria 310 deje de operar con información fragmentada y pueda acompañar a cada estudiante mediante un expediente institucional único, seguro, vivo y útil para todas las áreas de la escuela.

## Comandos

```powershell
# Comando de validación estándar del proyecto (ejecutar antes de dar el trabajo por terminado)
.\gradlew.bat :composeApp:desktopTest --no-daemon

# Ejecutar una sola clase de test
.\gradlew.bat :composeApp:desktopTest --no-daemon --tests "com.example.viewmodel.PreApplicationCurpCorrectionTest"

# Compilar APK debug de Android
.\gradlew.bat :composeApp:assembleDebug --no-daemon

# Verificar compilación del target desktop sin ejecutar
.\gradlew.bat :composeApp:compileKotlinDesktop --no-daemon

# Ejecutar la app de escritorio
.\gradlew.bat :composeApp:desktopRun --no-daemon
```

Notas sobre la ejecución de Gradle:
- El entorno oficial del repositorio es Windows con PowerShell. Usar `.\gradlew.bat` desde la raíz del repositorio.
- Preferir `--no-daemon` para evitar problemas de daemons obsoletos.
- No usar pipes (`|`) con la salida de Gradle; redirigir a un archivo de log e inspeccionarlo si hace falta.
- `kotlin.compiler.execution.strategy=in-process` está configurado en `gradle.properties` para evitar errores de conexión con el daemon del compilador — no eliminarlo.

Los tests viven en `composeApp/src/commonTest/kotlin/com/example/` (source set de test común de KMP, ejecutado por `desktopTest`), organizados en `data/` (+ `data/enrollment/`, `data/auth/`), `integration/` (tests golden-path del flujo institucional completo), `ui/` (+ `ui/presolicitud/`, `ui/student/`) y `viewmodel/`.

El CI (`.github/workflows/build.yml`) ejecuta, en orden: `:composeApp:assembleDebug`, `:composeApp:desktopTest`, `:composeApp:compileKotlinDesktop` — replicar esto localmente antes de dar el trabajo por terminado.

### Pruebas en dispositivo Android (Windows)

`tools/android/` contiene scripts de PowerShell para verificación en emulador: `doctor.ps1`, `start-emulator.ps1`/`stop-emulator.ps1`, `build-debug.ps1`, `install-app.ps1`, `launch-app.ps1`, `interact.ps1`, `collect-state.ps1`, `record-screen.ps1`. Las capturas de pantalla y logs de estas ejecuciones van en `evidence/android/<task-id>/`.

## Arquitectura

**Dos ViewModels, sin DI, sin librería de navegación.** `LabViewModel` (`composeApp/src/commonMain/kotlin/com/example/viewmodel/LabViewModel.kt`) mantiene un `StateFlow<Screen>` para navegación; `Screen` es una sealed class (`SecretaryDashboard`, `StudentRecordsDashboard`, `EnrollmentDashboard`, `StudentRecord(id)`, `PreApplicationFamilyPortal`, `SecretariaPreApplicationDashboard`, `OfficialEnrollmentDashboard`, `CredentialPreview(studentId)`, `StudentCredentialDashboard`). `PreApplicationViewModel` es dueño del flujo de pre-solicitud familiar de forma independiente. Los tres entrypoints de plataforma (`androidMain/MainActivity.kt`, `desktopMain/Main.kt`, `iosMain/MainViewController.kt`) llaman al mismo `SaseAppContent(viewModel = LabViewModel())` definido en `ui/SaseScreens.kt`.

**Flujo institucional (no reordenar ni saltarse etapas):**
```
Pre-solicitud familiar → Secretaría → Alta oficial → Expediente → Credencial
```
Esto mapea directamente a modelos de datos y pantallas: `PreApplication` → revisión/validación de secretaría → `AnnualEnrollmentRecord` (alta oficial) → expediente `Student` → vista previa de credencial. Las features deben respetar este pipeline en lugar de inventar atajos entre etapas.

**Capa de datos** (`composeApp/src/commonMain/kotlin/com/example/data/`):
- `MockSaseData` — singleton raíz que mantiene `StateFlow`s de estudiantes, auditorías e inscripciones anuales. `resetForTests()` lo reinicializa para aislamiento entre tests.
- `repository/` — interfaces `StudentRepository`/`AuditRepository` con implementaciones `Mock*RepositoryImpl`. `LabViewModel` depende de las interfaces, de modo que un backend real podría conectarse después detrás de las mismas costuras.
- `enrollment/` — lado de alta oficial: `AnnualEnrollmentPlanner`, `AnnualEnrollmentCommitter`, `AnnualEnrollmentFlowCoordinator` (orquesta planeación + confirmación), `AnnualEnrollmentPersistenceAdapter`, `PermanentEnrollmentIdAllocator`, `SchoolMovementClassifier`.
- `presolicitud/` — lado de pre-solicitud familiar: `PreApplicationModels`, `MockPreApplicationData`, `PreApplicationAdministrativeUpdate`, más `MockOfficialStudentData`/`OfficialStudentModels` usados para cruzar pre-solicitudes contra registros oficiales (detección de CURP/folio duplicados).
- `auth/` — costura de autenticación y roles de personal (primera etapa de backend real, **solo staff, sin datos de menores**): interfaz `AuthRepository` con implementación demo `MockAuthRepositoryImpl` (credenciales placeholder `example.invalid`); `StaffModels` (`StaffRole`, `StaffProfile`, `AuthSession`, `AuthResult`); `StaffPermissions` — matriz rol → `SaseArea` que es la fuente de verdad en cliente y que la base replica con RLS. Esta costura existe pero **aún no está cableada** en `LabViewModel`/UI: la navegación sigue usando el selector mock `AppRole`.
- `DerivedEnrollmentStatus`, `InstitutionalStudentRecordResolver`, `SaseStudentAnalytics` — vistas computadas/derivadas sobre las entidades crudas, no estado almacenado; preferir extender estas clases antes que agregar nuevos campos mutables a `Student`.

**Backend (`supabase/migrations/`):** un solo archivo `0001_staff_auth.sql` — enum `staff_role`, tabla `staff_profiles` (1:1 con `auth.users`), helpers `current_staff_role()`/`is_direccion()` (`security definer`) y políticas RLS que replican la matriz de `StaffPermissions` (cada quien lee su perfil; solo Dirección lee/escribe el directorio completo). Alcance deliberado: **ningún dato de alumnos ni familias entra a la base** hasta que roles y RLS estén probados. No hay proyecto Supabase activo conectado al cliente.

**Utilidades y ViewModels:** `util/ToastUtil.kt` (feedback efímero); en `viewmodel/`, además de `LabViewModel` y `PreApplicationViewModel`, está `InstitutionalAnnualEnrollment`.

**Capa de UI** (`composeApp/src/commonMain/kotlin/com/example/ui/`):
- `SaseScreens.kt` — pantallas de dashboard de nivel superior; también contiene las constantes de la paleta de colores personalizada (`SaseNavy`, `SaseGreen`, `SaseBlue`, etc.).
- `components/` — piezas reutilizables del design system por tipo: `buttons/`, `cards/`, `chips/`, `feedback/`, `fields/`, `navigation/`.
- `dashboard/`, `enrollment/` (con `enrollment/digital/SecretariaEnrollmentDashboard.kt`), `presolicitud/`, `student/` — pantallas específicas de cada etapa del flujo institucional.
- `theme/` — `Color.kt`, `Theme.kt`, `Type.kt` más `InstitutionalColor.kt`, `InstitutionalType.kt`, `Sase{Dimensions,Shapes,Spacing}.kt`. El tema oscuro es el default (`MyApplicationTheme(darkTheme = true)`). "Liquid Glass" es la estética de la casa — composables con efecto esmerilado/brillante `GlassCard`/`LiquidGlassCard`/`MetricGlassCard`. Breakpoints responsivos en 850dp y 600dp.

## Reglas específicas del proyecto

Provienen de las instrucciones de agentes del propio repo (`AGENTS.md`, `00_CONTEXT_FOR_AI/HUGO_SYSTEM_AGENT_INSTRUCTIONS.md`, `00_CONTEXT_FOR_AI/SKILLS/`) — leer esos archivos antes de cambios grandes o sensibles al flujo de trabajo:

- No inventar arquitectura, modelos, tablas, campos, pantallas o flujos que contradigan el flujo institucional de arriba.
- No activar un backend real/Supabase, PDF/impresión, ni IA externa (Gemini, etc.) sin autorización explícita. La costura `data/auth/` + migración `supabase/migrations/0001_staff_auth.sql` está deliberadamente acotada a personal (staff) y sin conectar al cliente; no ampliarla a datos de alumnos/familias ni cablearla a la UI sin autorización.
- Nunca usar datos reales de estudiantes/familias (CURP, teléfonos, direcciones, datos médicos/familiares). Los mocks son solo demo y usan correos `example.invalid` y valores placeholder — seguir esa convención.
- Las pantallas de credencial (`StudentCredentialDashboardScreen`/`CredentialPreviewScreen`) nunca deben mostrar información médica, familiar, de UDEII ni de Trabajo Social.
- No tocar `ui/theme/Color.kt`, `Theme.kt` ni `Type.kt` fuera de una tarea de tema visual explícitamente delimitada.
- Git: nunca `git add .` — stagear solo rutas de archivo explícitas. Nunca `stash`/`reset`/`restore`/force-push sin autorización explícita. Si `git status --short` no está limpio al inicio de una tarea, detenerse y reportar en lugar de continuar.
- No commitear estado local de agentes (`.codex/`, `.opencode/`, `*.patch`) ni `composeApp/build/`. Si esos archivos aparecen como untracked/modificados, detenerse y preguntar antes de actuar.
- Trabajar en commits pequeños y delimitados con mensajes de conventional commits; no commitear si el build o los tests fallan, y verificar que el CI quede en verde después de pushear (`gh run list --branch main --limit 3`).
