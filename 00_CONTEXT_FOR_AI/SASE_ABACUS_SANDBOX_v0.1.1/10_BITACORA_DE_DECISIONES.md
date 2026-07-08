# 10 — Bitácora de Decisiones

> Última actualización: 2026-07-05

## Registro

### 2026-07-05 — PR #19: Reglas multi-criterio de asignación de grupo

**Contexto**: La sugerencia de grupo solo balanceaba por cantidad de alumnos. No consideraba sexo, edad ni promedio académico.

**Decisión**: Implementar scoring multi-criterio en `suggestInitialGroup`: (penalización sexo × 3) + (penalización edad × 2) + (penalización promedio × 1). Capacidad máxima 30 por grupo. Agregar campos `alumnoSexo`, `alumnoEdad`, `promedio` a `OfficialStudent`. Helper `calculateAgeFromBirthDate` parsea `dd/Mes/yyyy`.

**Impacto**: Grados 2-3 ahora reciben sugerencia equilibrada por sexo, edad y promedio. `desktopTest` PASS y CI PASS. PR #19 mergeado a `main` (`724b6cc`). 3 archivos modificados.

---

### 2026-07-05 — PR #18: Ocultar acciones de Secretario para otros roles

**Contexto**: Los botones "Registrar incidencia" y "Escalar caso" eran visibles para todos los roles al ver un expediente de alumno.

**Decisión**: Agregar parámetro `userRole: AppRole = AppRole.SECRETARIA` a `StudentRecordScreen`. Envolver ambos botones en `if (userRole == AppRole.SECRETARIA)`. Pasar `currentRole` desde `SaseAppContent`.

**Impacto**: Solo el rol Secretaría puede registrar incidencias y escalar casos desde el expediente. Otros roles (Familia, Docente, etc.) ven el expediente sin botones de acción sensible. `desktopTest` PASS y CI PASS. PR #18 mergeado a `main` (`46bcd95`). 2 archivos modificados.

---

### 2026-07-05 — PR #17: Clarificar copia del portal de preregistro familiar

**Contexto**: El texto del portal familiar no distinguía claramente entre pre-inscripción y reinscripción.

**Decisión**: Aclarar la copia en `PreApplicationFamilyPortalScreen.kt` para que el usuario entienda el contexto del trámite.

**Impacto**: Mejora de experiencia de usuario en el portal familiar. `desktopTest` PASS y CI PASS. PR #17 mergeado a `main` (`0df8d04`). 1 archivo modificado.

---

### 2026-07-05 — PR #16: Clarificar acciones no disponibles en expediente

**Contexto**: Los botones de edición y documentos en `StudentRecordScreen` prometían funcionalidad que no existía.

**Decisión**: Cambiar textos y comportamiento de los botones para indicar que son funciones no disponibles, usando toasts informativos.

**Impacto**: El usuario entiende que esas funciones no están implementadas aún. `desktopTest` PASS y CI PASS. PR #16 mergeado a `main` (`ebecf69`). 1 archivo modificado.

---

### 2026-07-05 — PR #15: Habilitar navegación sidebar en expediente

**Contexto**: `StudentRecordScreen` no tenía navegación lateral funcional.

**Decisión**: Conectar `SaseSidebar` con `onItemClick` en `StudentRecordScreen.kt` para permitir navegación entre secciones del expediente.

**Impacto**: El usuario puede navegar lateralmente dentro del expediente del alumno. `desktopTest` PASS y CI PASS. PR #15 mergeado a `main` (`9beed58`). 1 archivo modificado.

---

### 2026-07-05 — PR #14: Instrucciones de agente HUGO SYSTEM

**Contexto**: Se necesitaba documentar las instrucciones operativas para agentes que trabajen en el proyecto.

**Decisión**: Crear `HUGO_SYSTEM_AGENT_INSTRUCTIONS.md` con rol, reglas doradas, workflow y formato de respuesta. Incorporar feedback de Codex sobre verificación de estado antes de checkout/pull.

**Impacto**: Agentes tienen referencia central de operación. CI PASS. PR #14 mergeado a `main` (`cd11106`). 1 archivo nuevo.

---

### 2026-07-05 — PR #13: Actualizar living memory

**Contexto**: Los archivos de memoria del proyecto no reflejaban los PRs #8 y #10.

**Decisión**: Actualizar los 3 archivos de memoria (`03_`, `07_`, `10_`) con los registros correspondientes.

**Impacto**: Memoria del proyecto sincronizada. CI PASS. PR #13 mergeado a `main`.

---

### 2026-07-04 — PR #12: Corrección de textos visibles de UI

**Contexto**: La UI contenía etiquetas visibles sin acentos y un typo menor en pantallas del flujo institucional.

**Decisión**: Corregir 18 textos visibles en `PreApplicationFamilyPortalScreen.kt`, `SaseScreens.kt`, `SecretariaEnrollmentDashboard.kt` y `CredentialPreviewScreen.kt`, manteniendo el cambio como text-only.

**Impacto**: Mejora de calidad lingüística sin cambiar lógica de UI, modelos, mocks, navegación, tests ni tema visual. `desktopTest` PASS y CI PASS. PR #12 mergeado a `main` (`700d09a`) con commit `1cc2036`.

---

### 2026-07-04 — Cierre operativo post PR #12: cambios locales de tema fuera de scope

**Contexto**: Después del merge de PR #12 aparecieron cambios locales no solicitados en `Color.kt`, `Theme.kt` y `Type.kt` que intentaban reemplazar el tema oscuro / Liquid Glass por una paleta clara institucional.

**Decisión**: Restaurar esos cambios locales y no conservarlos en `main`, porque contradecían la convención vigente y no pertenecían al scope de PR #12.

**Impacto**: `desktopTest` quedó PASS. Se refuerza que cambios visuales de tema requieren autorización explícita y PR dedicado. `.codex/`, `.opencode/`, patches locales y archivos de estado de agentes son estado local y no deben commitearse; no se toca `.gitignore` para esto.

---

### 2026-07-04 — PR #10: Mejorar salidas de navegación desde dashboards

**Contexto**: Varias pantallas dashboard necesitaban rutas de salida más explícitas para regresar al inicio o continuar el flujo institucional sin dejar al usuario en vistas terminales.

**Decisión**: Ajustar acciones de navegación en `PreApplicationFamilyPortalScreen`, `SaseScreens`, `StudentCredentialDashboardScreen` y `SecretariaPreApplicationDashboardScreen`, manteniendo navegación manual con `Screen` y `LabViewModel`.

**Impacto**: Mejora de continuidad operativa en dashboards. No introduce librerías de navegación, modelos nuevos ni cambios de backend. PR #10 mergeado a `main` (`3e36e87`) con commit `477c13a`.

---

### 2026-07-04 — PR #8: Guardrails de `MockSaseData`

**Contexto**: El proyecto usa datos in-memory mock y necesitaba cobertura específica para proteger altas, búsquedas y actualizaciones de estudiantes contra duplicados o normalización incorrecta.

**Decisión**: Agregar `MockSaseDataTest.kt` con pruebas para `addStudent`, `studentByCurp`, `updateStudent`, unicidad de CURP, normalización de entrada y no duplicación de registros existentes.

**Impacto**: La suite `desktopTest` cubre ahora `MockSaseData` además de analytics y guardrails de pre-solicitud. No cambia datos productivos ni introduce persistencia. PR #8 mergeado a `main` (`66eb71f`) con commit `2bd562c`.

---

### 2026-07-01 — PR-D: Eliminar código muerto en confirmInitialGroup

**Contexto**: Code review detectó línea inalcanzable en `confirmInitialGroup`:
```kotlin
val currentStudent = updatedStudent ?: return OfficialEnrollmentResult.Error(...)
```
`updatedStudent` ya se verificaba contra `null` en el `if` inmediato anterior, por lo que la rama `?:` dentro del `else` era muerta.

**Decisión**: Reemplazar el bloque `if/else` con un early return `?:` limpio. Eliminar la doble comprobación anidada.

**Alternativa descartada**: Usar `!!` — se evitó por preferencia de código más legible.

**Impacto**: -3 líneas, comportamiento equivalente. PR #2 mergeado via squash a `main` (`c7bbf06`). Build #37 ✅, Build #38 ✅.

---

### 2026-07-01 — H0: Sanitizar CURP de master student para evitar enlace stale

**Contexto**: `MockSaseData` contenía un master student con `curp = "CURP-DEMO-01"` y `enrollmentId = "2023-00258"`. `OFF-101` también usaba `CURP-DEMO-01`, provocando que el dashboard enlazara por CURP un expediente stale.

**Decisión**: Cambiar la CURP del master student a `CURP-SASE-01`. Los datos demo de enrollment, official student y pre-application mantienen `CURP-DEMO-01`.

**Alternativa descartada**: Cambiar la CURP de OFF-101 — eso alteraría la semántica del OFF como registro de alumno oficial demo.

**Impacto**: Tests actualizados. Build y tests verdes. PR #1 mergeado via squash a `main` (`2fe2b4c`). CI Build #35 ✅, Build #36 ✅.

---

### 2026-07-04 — PR #6: Eliminar integración Gemini

**Contexto**: El proyecto incluía integración con Gemini API (generación de imágenes por IA) que nunca se usó en funcionalidad real. Dependía de Ktor (HTTP client), kotlinx.serialization, Napier logging y Secrets Gradle Plugin. Además, mantenía un `.env.example` con `GEMINI_API_KEY`, funciones `getApiKey()` en 4 plataformas (Android, Desktop, iOS, common), y una UI de prueba (`GeminiTestCard`). En iOS la función retornaba `""` — Gemini nunca fue funcional en esa plataforma.

**Decisión**: Eliminar todo el código relacionado con Gemini: `GeminiImageGenerator`, `GeminiViewModel`, `GeminiTestCard`, dependencias Ktor/Serialization/Napier/Secrets, `.env.example`, `getApiKey()` de los 4 Platform files, `INTERNET` permission de AndroidManifest, y todas las referencias en README/AGENTS/metadata.

**Alternativa descartada**: Mantener el código desactivado — agregaba peso innecesario al build y confundía el alcance del proyecto.

**Impacto**: -353 líneas, -15 archivos. Build más rápido, APK más pequeño, sin manejo de secrets. SASE Light queda sin integración Gemini activa y superficie de secretos reducida a cero. PR #6 mergeado via squash a `main` (`963878f`). CI ✅.

---

### 2026-07-04 — PR #5: Ignorar archivos locales de entorno

**Contexto**: Archivos como `.env.local` (Vercel OIDC token) y `.vercel/` aparecían como untracked en `git status`. `composeApp/build/` también se generaba localmente y no debía trackearse.

**Decisión**: Agregar `.env.*`, `!.env.example`, `.vercel/` y `composeApp/build/` al `.gitignore`.

**Impacto**: `git status` limpio. PR #5 mergeado via squash a `main` (`d8815c6`). CI ✅.

---

### 2026-07-01 — Workflow CI/CD con GitHub Actions

**Contexto**: No existía CI pipeline. El README indicaba "No CI/CD pipeline."

**Decisión**: Crear `.github/workflows/build.yml` con 3 jobs en ubuntu-latest: Build Android, Test Desktop, Build Desktop.

**Impacto**: Cada push/PR a `main` ejecuta validación. Usado en PR #1 y PR #2.

---

### 2026-06-30 — Credential Back Preview

**Contexto**: Se requiere una vista previa del reverso de la credencial estudiantil.

**Decisión**: Agregar `CredentialPreviewScreen` con diseño Liquid Glass, mostrando información institucional y código QR mock.

**Impacto**: Nueva pantalla accesible desde el dashboard (`4f4bc84`).

---

### 2026-06-29 — Priority Institutional Form Templates

**Contexto**: Se necesita un set de formatos institucionales prioritarios como parte de la documentación del sistema.

**Decisión**: Agregar templates de formularios prioritarios como documentación (`634e309`).

**Impacto**: Documentación disponible en el repositorio.

---

### 2026-06-29 — Institutional Document Redesign Proposal

**Contexto**: Propuesta de rediseño de documentos institucionales.

**Decisión**: Documentar la propuesta de rediseño (`9129bd7`).

**Impacto**: Documentación disponible en el repositorio.

---

### Fecha anterior — Post-Enrollment Master Record Visibility

**Contexto**: Los registros maestros de alumnos debían ser visibles después de la inscripción oficial.

**Decisión**: Implementar visibilidad de registros maestros post-inscripción (`3f4b661`).

---

### Fecha anterior — Persistencia de Readiness y Propagación de Official Enrollment

**Contexto**: El estado de "listo para inscripción oficial" debía persistirse y propagarse al flujo de enrollment.

**Decisión**: Implementar persistencia en memoria del estado readiness y propagación automática al oficializar (`7add291`).

---

### Fecha anterior — Guardrails de Pre-Application Enrollment

**Contexto**: Cerrar guardrails de seguridad en el flujo de pre-solicitud de inscripción.

**Decisión**: Implementar validaciones duplicadas de CURP y matrícula, bloquear pre-solicitudes no ready (`321c561`).

---

### Fecha anterior — Contextual Official Enrollment Flow

**Contexto**: Se requiere un flujo contextual para la inscripción oficial desde la secretaría.

**Decisión**: Agregar flujo de inscripción oficial con selección de grupo y validaciones (`a95b98d`).

---

### Fecha anterior — Secretaria Pre-Application Readiness

**Contexto**: La secretaría necesita marcar pre-solicitudes como listas para inscripción oficial.

**Decisión**: Agregar readiness check con validación de documentos, fotos y datos completos (`fdc8856`).

---

## Convenciones del proyecto

| Aspecto | Decisión |
|---|---|---|
| DI | Sin framework — instanciación manual |
| Navegación | `sealed class Screen` + estado en ViewModel |
| Temas | Dark theme por defecto, Liquid Glass |
| Scope visual | No reemplazar tema oscuro / Liquid Glass sin autorización explícita y PR dedicado |
| Datos | Mock in-memory (singleton `object`) |
| Agentes locales | `.codex/`, `.opencode/`, patches locales y estado de agentes no se commitean |
| Commits | Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`) |
| Merge | Squash merge a `main` |
