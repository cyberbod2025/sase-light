# 03 — Estado actual del proyecto

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## Módulos existentes

| Módulo | Archivo(s) | Estado |
|--------|-----------|--------|
| Dashboard principal | `SaseScreens.kt` | Implementado (navegación + layout responsive) |
| Portal Familia | `PreApplicationFamilyPortal.kt` (en SaseScreens) | Implementado (mock, envío de pre-solicitud) |
| Panel Secretaría (pre-solicitudes) | `SecretariaPreApplicationDashboardScreen.kt` | Implementado (revisión, aceptación, readiness) |
| Panel de altas oficiales | `OfficialEnrollmentDashboard.kt` (en SaseScreens) | Implementado (desde pre-solicitud aceptada) |
| Expediente maestro | `StudentRecordScreen.kt` | Implementado (vista completa del alumno) |
| Credencial (dashboard) | `StudentCredentialDashboardScreen.kt` | Implementado (lista de alumnos con alta oficial) |
| Credencial (vista previa) | `CredentialPreviewScreen.kt` | Implementado (frente + reverso, sin PDF) |
| Inscripciones (digital) | `SecretariaEnrollmentDashboard.kt` | Implementado (panel de inscripción digital) |
| Tabla de inscripciones | `SmartEnrollmentTable.kt` | Implementado |
| Dashboard "Liquid Glass" | `LiquidGlassDashboard.kt` | Implementado (métricas mock) |

## Pantallas implementadas (navegación desde sidebar)

1. **Inicio** — Dashboard con métricas mock
2. **Inscripciones** — Panel de inscripción digital con tabla
3. **Portal Familia** — Formulario de pre-solicitud familiar
4. **Pre-Solicitudes** — Panel de revisión para Secretaría
5. **Altas Oficiales** — Conversión de pre-solicitud a alumno oficial
6. **Credenciales** — Dashboard de credenciales + vista previa frente/reverso
7. **Expediente del alumno** — Vista detallada del `Student` maestro (accesible desde credencial y altas)

## Módulos incompletos / placeholder

| Módulo | Estado | Notas |
|--------|--------|-------|
| Captura de foto del alumno | Placeholder | Botón existe pero no hay cámara real. Se simula con `simulateCaptureStudentPhoto()` |
| Captura de foto de responsables | Placeholder | Similar al anterior |
| Foto para credencial | Placeholder | `photoForCredential` flag existe en modelo pero no hay captura real |
| Validación Médico Escolar | Placeholder | `ValidacionArea` existe en `OfficialStudent` pero sin UI |
| Validación Trabajo Social | Placeholder | Similar |
| Validación UDEII | Placeholder | Similar |
| Validación Dirección | Placeholder | Similar |

## Funcionalidades deshabilitadas intencionalmente

- **Exportación PDF**: No implementada. Se muestra nota: "Exportación PDF e impresión: fase futura, no disponible en esta versión."
- **Impresión de credencial**: No implementada.
- **Botón "Imprimir"**: Eliminado en Paso 7D.

## Archivos modificados recientemente (últimos 2 commits)

| Commit | Archivos |
|--------|----------|
| `4f4bc84` feat(sase): add credential back preview | `CredentialPreviewScreen.kt`, `PreApplicationGuardrailsTest.kt` |
| `d97ecaf` feat(sase): add student credential preview | 7 archivos (credencial, dashboard, modelos, tests) |

## Estado de pruebas

| Suite | Resultado |
|-------|-----------|
| `desktopTest` | BUILD SUCCESSFUL — 39 tests, 0 fallos |
| `compileKotlinDesktop` | BUILD SUCCESSFUL |

Los tests se ejecutan en el target Desktop (JVM) porque iOS no puede compilarse en Windows.

## Dependencias técnicas

- **Único módulo activo**: `:composeApp` (declarado en `settings.gradle.kts`)
- **Directorio `app/`**: Es código muerto/stale. No incluido en el build. No modificarlo.
- **Gradle properties**: `kotlin.compiler.execution.strategy=in-process` (evita problemas con daemon)
- **Version catalog**: `gradle/libs.versions.toml`
