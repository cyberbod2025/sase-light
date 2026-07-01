# 04 — Flujo Secretaría → Alta oficial → Expediente → Credencial

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## Flujo institucional esperado

```
[Portal Familia]                    → Familia envía pre-solicitud
       ↓
[Secretaría revisa]                 → Revisa documentos, acepta o solicita corrección
       ↓
[Readiness check]                   → Fotos, documentos, consentimientos
       ↓
[Alta oficial]                      → Genera OfficialStudent + Student (expediente maestro)
       ↓
[Expediente maestro]                → Visible desde StudentRecordScreen
       ↓
[Credencial - vista previa]         → Frente + Reverso (sin PDF/impresión real)
```

## Estado de implementación por paso

### Paso 1: Pre-solicitud familiar → ✅ Implementado

- **Qué hace**: La familia llena un formulario con datos del alumno, responsables, ficha médica, contexto sociofamiliar, antecedentes UDEII, documentos declarados y consentimientos.
- **Dónde**: `PreApplicationFamilyPortal` en `SaseScreens.kt`
- **Modelo**: `PreApplication` en `PreApplicationModels.kt`
- **Validaciones**: CURP duplicado, folio duplicado, formato de CURP.
- **Mocks**: `MockPreApplicationData.kt` con 10+ pre-solicitudes precargadas.

### Paso 2: Revisión de Secretaría → ✅ Implementado

- **Qué hace**: Secretaría ve lista de pre-solicitudes, puede aceptar, rechazar, marcar como duplicada, solicitar corrección.
- **Dónde**: `SecretariaPreApplicationDashboardScreen.kt`
- **Readiness**: Secretaría puede marcar pre-solicitud como "lista para alta oficial" tras verificar fotos y documentos.
- **Guardrails**: No se puede marcar como lista si faltan fotos o documentos pendientes. Estado `BLOCKED` con lista de pendientes.

### Paso 3: Alta oficial → ✅ Implementado

- **Qué hace**: Desde una pre-solicitud en estado `READY`, Secretaría inicia alta oficial.
- **Dónde**: `OfficialEnrollmentDashboard` en `SaseScreens.kt`
- **Modelos**: `OfficialStudent` (en `OfficialStudentModels.kt`), `Student` (expediente maestro en `SaseEntities.kt`)
- **Validaciones**: CURP duplicado, matrícula duplicada, folio duplicado.
- **Matrícula**: Se genera automáticamente con formato `S310-[10 chars CURP]-G[Grado]`.
- **Propagación**: El `Student` (expediente maestro) se crea automáticamente con los datos oficiales.

### Paso 4: Expediente maestro → ✅ Implementado (vista básica)

- **Qué hace**: Muestra todos los datos del `Student` oficial.
- **Dónde**: `StudentRecordScreen.kt`
- **Secciones**: Información general, documentos, observaciones, incidentes, auditoría, sección de credencial.
- **Nota**: No todos los campos tienen edición. Es principalmente una vista de consulta.

### Paso 5: Credencial → ✅ Implementado (vista previa visual, SIN PDF)

- **Qué hace**: Muestra frente y reverso de la credencial escolar.
- **Dónde**: `CredentialPreviewScreen.kt` y `StudentCredentialDashboardScreen.kt`
- **Modelo**: `StudentCredentialPreview` (proyección computada desde `Student`)
- **Frente**: Foto (placeholder), nombre, grado, grupo, matrícula, CURP, estatus.
- **Reverso**: Escuela, turno, ciclo escolar, matrícula oficial, folio origen, estado, sello mock, QR mock, privacidad.
- **Exportación/impresión**: NO implementado. Marcado como "fase futura".

## Lo que falta

| Paso | Estado | Observación |
|------|--------|-------------|
| Captura real de foto del alumno | No implementado | Solo simulación vía `simulateCaptureStudentPhoto()` |
| Validación Médico Escolar | No implementado | `ValidacionArea` existe pero sin UI ni lógica |
| Validación Trabajo Social | No implementado | Mismo caso |
| Validación UDEII | No implementado | Mismo caso |
| Validación Dirección | No implementado | Mismo caso |
| PDF de credencial | Fase futura | Explícitamente no implementado |
| Impresión de credencial | Fase futura | Explícitamente no implementado |

## Relación entre modelos en el flujo

```
PreApplication (pre-solicitud familiar)
    │
    ├──→ OfficialStudent (alumno oficial, con matrícula)
    │        │
    │        └──→ Student (expediente maestro, propagado desde alta oficial)
    │                 │
    │                 └──→ StudentCredentialPreview (proyección computada, no persistida)
    │
    └──→ ReadinessStatus (PENDING → BLOCKED → READY → CONVERTED)
```
