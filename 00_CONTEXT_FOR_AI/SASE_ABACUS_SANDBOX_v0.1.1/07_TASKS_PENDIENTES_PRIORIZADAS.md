# 07 — Tareas pendientes priorizadas

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

> **Nota**: Estas tareas se deducen del estado actual del repositorio. No son especificaciones externas. Representan lo que naturalmente sigue después del flujo implementado actualmente.

---

## Alta prioridad (desbloquean flujo institucional)

### H0. Saneamiento de datos mock con apariencia realista

- **Estado actual**: `MockSaseData`, `MockPreApplicationData`, `MockOfficialStudentData` y `MockEnrollmentData` contienen CURP, teléfonos y domicilios simulados con apariencia realista que podrían confundirse con datos reales.
- **Dependencia**: Ninguna.
- **Archivos probables**: `MockSaseData.kt`, `MockPreApplicationData.kt`, `MockOfficialStudentData.kt`, `MockEnrollmentData.kt`.
- **Acción**: Revisar y reemplazar nombres, CURP, teléfonos y domicilios con datos explícitamente ficticios antes de compartir demos, capturas o contexto con agentes externos.
- **Criterio de aceptación**: Todos los datos de prueba deben ser explícitamente ficticios: `ALUMNO_DEMO_001`, `CURP_DEMO_000000HDFXXX00`, `TEL_DEMO_0000000000`, `DOMICILIO_DEMO_SIN_VALOR_REAL`.

### H1. Captura real de foto del alumno

- **Estado actual**: Placeholder — `simulateCaptureStudentPhoto()` simula la captura
- **Dependencia**: Ninguna (es independiente)
- **Archivos probables**: `PreApplicationViewModel.kt`, UI de captura
- **Riesgo**: Requiere cámara (complejo en Desktop, más simple en Android/iOS)

### H2. Validación Médico Escolar

- **Estado actual**: `ValidacionArea` existe en `OfficialStudent` pero sin UI ni lógica
- **Dependencia**: No técnica. Puede requerir identificación visual/foto según criterio institucional, pero la lógica de validación médica no debe depender técnicamente de la captura de foto.
- **Archivos probables**: Nueva pantalla o sección en expediente
- **Riesgo**: Datos médicos sensibles deben protegerse

### H3. Validación Trabajo Social

- **Estado actual**: Mismo caso que Médico Escolar
- **Dependencia**: Ninguna directa
- **Riesgo**: Datos sociofamiliares sensibles

### H4. Asignación real de grupo

- **Estado actual**: `grupoAsignado` existe pero la UI de asignación es básica
- **Dependencia**: Flujo de alta oficial (ya implementado)
- **Riesgo**: Bajo

---

## Media prioridad (mejoras visuales / organización)

### M1. Edición de expediente maestro

- **Estado actual**: `StudentRecordScreen` es solo lectura
- **Beneficio**: Secretaría podría actualizar datos sin recrear el registro
- **Riesgo**: Requiere validaciones de permisos y auditoría

### M2. Vista de alumno desde credencial

- **Estado actual**: Botón "Ver expediente completo" funciona
- **Mejora posible**: Mejorar navegación entre credencial y expediente
- **Riesgo**: Bajo

### M3. Responsive design para tablets

- **Estado actual**: Breakpoints en 850dp y 600dp (parcial)
- **Beneficio**: Mejor experiencia en pantallas medianas

---

## Baja prioridad (extras, refinamientos, fases futuras)

### L1. Exportación PDF de credencial

- **Estado actual**: Explícitamente no implementado
- **Nota**: No iniciar sin autorización

### L2. Impresión de credencial

- **Estado actual**: Explícitamente no implementado
- **Nota**: No iniciar sin autorización

### L3. Módulo Docente

- **Estado actual**: No existe (solo está en el enum `AppRole`)
- **Nota**: Requiere definición de alcance

### L4. Módulo UDEII

- **Estado actual**: Placeholder (`ValidacionArea` + `AntecedentesUdeii`)
- **Nota**: Depende de datos de pre-solicitud

### L5. Autenticación real

- **Estado actual**: `AppRole` es un enum mock, seleccionable desde UI
- **Nota**: Requiere backend

### L6. Conectar Supabase / base de datos

- **Estado actual**: No hay conexión a base de datos
- **Nota**: No iniciar sin autorización

### L7. CRUD completo de alumnos

- **Estado actual**: Solo creación vía alta oficial y consulta
- **Nota**: Requiere definición de alcance

### L8. Notificaciones / alertas

- **Estado actual**: No implementado
- **Nota**: Fuera de alcance actual

---

## Resumen de prioridades

| Prioridad | Count | Ejemplos |
|-----------|-------|----------|
| 🔴 Alta | 4 | Foto real, validaciones Médico/TS/UDEII, asignación grupo |
| 🟡 Media | 3 | Edición expediente, navegación, responsive |
| 🟢 Baja | 8 | PDF, impresión, módulos futuros, auth, backend |
