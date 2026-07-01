# 06 — Componentes y pantallas existentes

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## Convenciones de estado

- ✅ **Completo**: Funcional para el alcance actual
- ⏳ **Parcial**: Implementado pero con limitaciones conocidas
- 🔶 **Placeholder**: Estructura presente pero sin lógica real
- ❌ **No implementado**: No existe

---

## Pantallas principales

### 1. Dashboard principal (`Inicio`)

| Atributo | Valor |
|----------|-------|
| Archivo | `SaseScreens.kt` (sección inicial) + `LiquidGlassDashboard.kt` |
| Propósito | Vista general con métricas institucionales mock |
| Datos que consume | `MockSaseData.students`, `MockSaseData.audits` |
| Estado | ✅ Completo (mock) |
| Navegación | Sidebar con 6 ítems |
| Riesgos | Métricas son estáticas, no reflejan datos reales |

### 2. Portal Familia (`Portal Familia`)

| Atributo | Valor |
|----------|-------|
| Archivo | `SaseScreens.kt` (sección `PreApplicationFamilyPortal`) |
| Propósito | Formulario de pre-solicitud para la familia |
| Datos que consume | `PreApplicationViewModel.submitFamilyPreApplication()` |
| Estado | ✅ Completo (mock) |
| Validaciones | CURP duplicado, formato de CURP |
| Riesgos | No hay autenticación real; cualquier rol puede acceder |

### 3. Panel de Pre-Solicitudes (`Pre-Solicitudes`)

| Atributo | Valor |
|----------|-------|
| Archivo | `SecretariaPreApplicationDashboardScreen.kt` |
| Propósito | Revisión, aceptación, readiness de pre-solicitudes |
| Datos que consume | `PreApplicationViewModel.sharedPreApplications` |
| Estado | ✅ Completo (mock) |
| Acciones | Aceptar, rechazar, marcar duplicada, solicitar corrección, marcar ready |
| Riesgos | Readiness check depende de fotos simuladas |

### 4. Panel de Altas Oficiales (`Altas Oficiales`)

| Atributo | Valor |
|----------|-------|
| Archivo | `SaseScreens.kt` (sección de altas) |
| Propósito | Iniciar alta oficial desde pre-solicitud aceptada y ready |
| Datos que consume | `PreApplicationViewModel.officialStudents` |
| Estado | ✅ Completo (mock) |
| Validaciones | CURP duplicado, matrícula duplicada, folio duplicado |
| Post-alta | Crea `OfficialStudent` + `Student`, redirige a expediente |

### 5. Expediente del Alumno (`StudentRecord`)

| Atributo | Valor |
|----------|-------|
| Archivo | `StudentRecordScreen.kt` |
| Propósito | Vista completa del expediente maestro del alumno |
| Datos que consume | `LabViewModel.saseStudents` |
| Estado | ✅ Completo (mock, vista de consulta) |
| Secciones | Info general, documentos, observaciones, incidentes, auditoría, sección credencial |
| Navegación | Desde altas oficiales, credenciales, o dashboard |
| Riesgos | No tiene edición; solo lectura |

### 6. Dashboard de Credenciales (`Credenciales`)

| Atributo | Valor |
|----------|-------|
| Archivo | `StudentCredentialDashboardScreen.kt` |
| Propósito | Lista de alumnos con alta oficial, selección para vista previa |
| Datos que consume | `PreApplicationViewModel.officialStudents`, `LabViewModel.saseStudents` |
| Estado | ✅ Completo |
| Layout | Lista lateral + vista previa (Desktop), o vista única (mobile) |
| Filtro | Solo alumnos con estado ALTA_OFICIAL_* |
| Riesgos | Depende de que exista `OfficialStudent` con datos completos |

### 7. Vista Previa de Credencial (`CredentialPreviewScreen`)

| Atributo | Valor |
|----------|-------|
| Archivo | `CredentialPreviewScreen.kt` |
| Propósito | Renderiza frente y reverso de la credencial escolar |
| Datos que consume | `StudentCredentialPreview.fromStudent(student)` |
| Estado | ✅ Completo (visual, sin PDF/impresión) |
| Frente | Foto (placeholder), escuela, nombre, grado, grupo, matrícula, CURP, estatus |
| Reverso | Escuela, turno, ciclo, matrícula, folio origen, estado "Vista previa", sello mock, QR mock, privacidad |
| Toggle | Frente/Reverso (selector visual tipo pill) |
| Exportación | ❌ No implementado. Nota: "fase futura" |
| Riesgos | Foto es placeholder; no hay cámara real |

### 8. Inscripciones Digitales (`Inscripciones`)

| Atributo | Valor |
|----------|-------|
| Archivo | `SecretariaEnrollmentDashboard.kt`, `SmartEnrollmentTable.kt` |
| Propósito | Panel de inscripción digital con tabla de alumnos |
| Datos que consume | `MockEnrollmentData` |
| Estado | ✅ Completo (mock) |
| Riesgos | Es un flujo paralelo al de pre-solicitud → alta oficial; no están conectados |

---

## Componentes compartidos

| Componente | Archivo | Propósito |
|-----------|---------|-----------|
| `GlassCard` | `SaseScreens.kt` | Tarjeta con efecto frost/glass |
| `LiquidGlassCard` | `SaseScreens.kt` | Tarjeta con efecto líquido/glossy |
| `MetricGlassCard` | `SaseScreens.kt` | Tarjeta de métrica con vidrio |
| `SaseSidebar` | `SaseScreens.kt` | Barra lateral de navegación |
| `CredentialCard` | `CredentialPreviewScreen.kt` | Frente de credencial |
| `CredentialCardBack` | `CredentialPreviewScreen.kt` | Reverso de credencial |
| `FaceButton` | `CredentialPreviewScreen.kt` | Botón de alternancia Frente/Reverso |
| `CredentialPill` | `CredentialPreviewScreen.kt` | Pill badge para grado/grupo |
| `InfoRow` / `BackInfoRow` | `CredentialPreviewScreen.kt` | Filas de datos en credencial |
| `DetailLine` | `CredentialPreviewScreen.kt` | Línea de detalle en sección informativa |
| `CredentialPreviewCard` | `StudentCredentialDashboardScreen.kt` | Mini vista previa en dashboard de credenciales |

## Colores y tema

| Constante | Valor |
|-----------|-------|
| `SaseNavy` | Azul marino institucional |
| `SaseGreen` | Verde institucional |
| `SaseBlue` | Azul claro |
| `SaseBgSoft` | Fondo suave |
| `SaseMuted` | Texto atenuado |
| `SaseOrange` | Naranja para advertencias |
| `SaseBorder` | Color de borde |
| `SaseDanger` | Rojo para errores |

Definidos al inicio de `SaseScreens.kt`. Tema oscuro por defecto (`MyApplicationTheme` en `Theme.kt`).
