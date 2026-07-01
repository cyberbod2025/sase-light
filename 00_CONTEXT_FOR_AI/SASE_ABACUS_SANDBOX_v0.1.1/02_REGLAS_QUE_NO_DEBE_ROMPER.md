# 02 — Reglas que no debe romper

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## Reglas duras del proyecto SASE Light

### 1. No inventar tablas ni modelos

No crear nuevas clases de datos, `data class`, `enum`, `interface`, o `object` que no existan en el repositorio actual. Si se necesita un modelo nuevo, debe ser explícitamente autorizado.

### 2. No inventar campos

No agregar propiedades a modelos existentes sin autorización expresa. Todos los campos deben estar documentados en `05_MODELOS_Y_DATOS_ACTUALES.md`.

### 3. No tratar mocks como datos reales

Los datos en `MockSaseData`, `MockPreApplicationData`, `MockOfficialStudentData`, y `MockEnrollmentData` son simulados. Cualquier referencia a ellos debe aclarar que son mocks. Nunca presentarlos como datos reales de alumnos.

### 4. No modificar `main` directamente

No hacer push/commit directo a `main` sin autorización. Toda contribución debe pasar por el proceso de micro-fases y validación de builds.

### 5. No implementar PDF / impresión sin autorización

La exportación PDF y la impresión están explícitamente marcadas como **fase futura**. No implementar ninguna funcionalidad de generación de PDF, exportación a archivo, impresión real, ni vista previa de impresión.

### 6. No conectar Supabase / backend / base de datos

El proyecto actualmente opera 100% en memoria. No agregar conexiones a Supabase, Firebase, PostgreSQL, MySQL ni ningún otro sistema de almacenamiento externo.

### 7. No romper el flujo Secretaría → alta oficial → expediente → credencial

El flujo central debe preservarse intacto:

```
Pre-solicitud familiar (envío)
  → Secretaría revisa y acepta
  → Readiness check (fotos, documentos)
  → Alta oficial (genera OfficialStudent + Student)
  → Expediente maestro visible
  → Credencial (vista previa frente + reverso)
```

Cada paso depende del anterior. No introducir caminos alternativos que salten pasos.

### 8. No mezclar expediente académico con datos médicos/familiares

El modelo `Student` (expediente maestro) contiene campos médicos y de contacto, pero estos deben tratarse como datos del expediente, no como datos de la credencial. La credencial (`StudentCredentialPreview`) NO debe incluir:

- Domicilio
- Teléfono
- Responsables familiares
- Observaciones
- Datos médicos
- UDEEI
- Trabajo Social
- Ficha médica familiar

### 9. No introducir funcionalidades fuera de fase

Cada micro-fase tiene un alcance definido en el plan de desarrollo. No agregar:

- Pantallas no planificadas
- Navegación a módulos no implementados
- Botones que llevan a rutas sin contenido
- Funcionalidades que requieran backend sin autorización

### 10. No exponer secretos ni datos sensibles

- No incluir API keys, tokens, ni variables de entorno en el código.
- No incluir CURP, nombres, teléfonos, correos, ni domicilios reales en documentación externa.
- No incluir datos de alumnos reales en logs, tests, ni capturas de pantalla.

### 11. Builds y tests deben pasar

Antes de cualquier entrega:

- `compileKotlinDesktop` debe ser BUILD SUCCESSFUL
- `desktopTest` debe ser BUILD SUCCESSFUL (0 fallos)

### 12. No modificar archivos fuera del alcance declarado

Cada tarea especifica qué archivos pueden modificarse. No tocar:

- Gradle / version catalog / settings
- HTML documental
- Modelos de datos no relacionados con la tarea
- Flujo de pre-solicitudes si no está en alcance
- Lógica de alta oficial si no está en alcance
- Guardrails de duplicados si no está en alcance

### 13. Los datos mock deben parecer claramente ficticios

Todo dato mock debe parecer claramente ficticio. No usar nombres, CURP, teléfonos o domicilios simulados con apariencia real cuando el material será compartido con agentes externos o personal no técnico.

Ejemplos de prácticas NO aceptables:
- CURP con formato realista combinado con nombres verosímiles
- Teléfonos con formato mexicano real (códigos de área válidos)
- Domicilios con calles y colonias existentes
- Nombres completos que coincidan con personas reales conocidas

Si se identifica algún mock con apariencia realista en el repositorio, debe documentarse como riesgo y reemplazarse con datos explícitamente ficticios antes de compartir con agentes externos.
