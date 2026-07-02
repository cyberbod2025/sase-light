# 07 — Tareas Pendientes Priorizadas

> Sprint: H1 — Documental de Contexto
> Fecha: 2026-07-01
> Último merge: `c7bbf06`

## Prioridades

| # | Tarea | Prioridad | Área | Dependencias |
|---|---|---|---|---|
| P1 | Agregar `INTERNET` permission al `AndroidManifest.xml` | 🔴 Alta | Android | — |
| P2 | Implementar `getApiKey()` en iOS | 🔴 Alta | iOS | — |
| P3 | Agregar suite de tests unitarios para `LabViewModel` | 🟡 Media | Testing | — |
| P4 | Agregar suite de tests unitarios para flujo de enrollment | 🟡 Media | Testing | — |
| P5 | Integrar capa de persistencia (reemplazar mocks) | 🟡 Media | Data | — |
| P6 | Agregar CI status badge al README | 🟢 Baja | Docs | — |
| P7 | Actualizar README (CI/CD pipeline existe, eliminar "No CI/CD") | 🟢 Baja | Docs | — |
| P8 | Evaluar cobertura de pruebas en flujo de credenciales | 🟢 Baja | Testing | — |
| P9 | Documentar API de Gemini y su modelo usado | 🟢 Baja | Docs | — |
| P10 | Revisar deprecation warnings de Compose (Divider → HorizontalDivider, Icons.AutoMirrored) | 🟢 Baja | Code Health | — |

## Leyenda

| Prioridad | Significado |
|---|---|
| 🔴 Alta | Bloqueante para funcionalidad en producción |
| 🟡 Media | Mejora significativa sin bloquear |
| 🟢 Baja | Housekeeping, docs o deuda técnica menor |

## Notas

- **P1**: Sin `INTERNET` las llamadas a Gemini API fallan en Android.
- **P2**: Gemini no funciona en iOS; `getApiKey()` retorna `""`.
- **P5**: Actualmente toda la data se pierde al reiniciar la app.
- **P10**: ~30 warnings de deprecation visibles en compilación desktop.

## Completado en H1

| Entrega | Descripción | Commit |
|---|---|---|
| H0 — PR #1 | Sanitizar CURP master student | `2fe2b4c` |
| PR-D — PR #2 | Eliminar código muerto confirmInitialGroup | `c7bbf06` |
