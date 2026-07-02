# 10 — Bitácora de Decisiones

> Última actualización: 2026-07-01

## Registro

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
|---|---|
| DI | Sin framework — instanciación manual |
| Navegación | `sealed class Screen` + estado en ViewModel |
| Temas | Dark theme por defecto, Liquid Glass |
| Datos | Mock in-memory (singleton `object`) |
| API Keys | Secrets Gradle Plugin + `.env` |
| Commits | Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`) |
| Merge | Squash merge a `main` |
