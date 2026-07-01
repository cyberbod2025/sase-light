# 01 — Contexto General SASE Light

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## ¿Qué es SASE Light?

SASE Light es una aplicación de escritorio (Compose Multiplatform / Kotlin Multiplatform) para la administración escolar de la **Escuela Secundaria Diurna No. 310 "Presidentes de México"**, turno vespertino.

SASE = Sistema de Acompañamiento y Seguimiento Escolar.

Al momento del commit base `4f4bc84`, el proyecto opera como prototipo funcional de navegación con datos identificados como mock o simulados en memoria. No tiene conexión a base de datos, backend ni servicios externos. Está diseñado para demostrar el flujo institucional completo: desde la pre-solicitud familiar hasta la generación de la credencial escolar. Antes de compartir esta documentación con agentes externos, debe verificarse que no se hayan incorporado datos reales, secretos o información sensible.

## Propósito institucional

El sistema busca reemplazar procesos manuales / fragmentados con una herramienta digital que:

- Unifique el registro de estudiantes desde la solicitud inicial hasta la emisión de credencial.
- Permita a Secretaría gestionar altas oficiales y expedientes.
- Proporcione vistas previas de credencial escolar.
- Sirva como base para futuros módulos (médico, trabajo social, UDEII, docentes).

## Problema que resuelve

- Los datos de alumnos están dispersos en formatos físicos y digitales no conectados.
- No existe un expediente maestro digital único por alumno.
- La generación de credenciales es manual y no está integrada con los datos de inscripción.
- No hay trazabilidad del estado de cada alumno (pre-solicitud → alta oficial → expediente → credencial).

## Alcance actual (junio 2026)

| Aspecto | Estado |
|---------|--------|
| Pre-solicitud familiar (Portal Familia) | Implementado (mock) |
| Revisión de Secretaría sobre pre-solicitudes | Implementado (mock) |
| Alta oficial desde pre-solicitud aceptada | Implementado (mock) |
| Generación de expediente maestro (`Student`) | Implementado (mock) |
| Vista previa de credencial (frente + reverso) | Implementado (completo, sin PDF) |
| Impresión / exportación PDF | No implementado (fase futura) |
| Autenticación / roles reales | No implementado (mock `AppRole`) |
| Base de datos / Supabase | No conectado |
| Módulo médico escolar | No implementado (placeholder) |
| Módulo Trabajo Social | No implementado (placeholder) |
| Módulo UDEII | No implementado (placeholder) |
| Módulo Docente | No implementado (placeholder) |
| Cámara real / captura de foto | No implementado (placeholder) |

## Stack técnico

- **Lenguaje**: Kotlin 2.1.20 (Multiplatform)
- **UI**: Compose Multiplatform 1.7.3
- **Targets activos**: Desktop (JVM), Android (parcial)
- **Build**: Gradle 8.11.1 con AGP 8.7.3
- **API externa**: Gemini API (solo para generación de imagen mock en flujo de credencial, no funcional en producción)
- **Datos**: Singleton `MockSaseData` en memoria. No hay persistencia.

## Limitaciones conocidas

- Al momento del commit base `4f4bc84`, los datos disponibles en el repositorio son mock o simulados. Antes de compartir esta documentación con agentes externos, debe verificarse que no se hayan incorporado datos reales, secretos o información sensible.
- No hay autenticación real. El rol se selecciona desde un selector mock.
- iOS no puede compilarse en Windows (targets nativos deshabilitados).
- La API key de Gemini debe cargarse desde `.env` para la función de imagen (solo Desktop/Android).
- No hay pruebas unitarias de UI (solo pruebas de lógica de negocio y modelos).
