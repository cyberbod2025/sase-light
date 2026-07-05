# 07 — Tareas Pendientes Priorizadas

> Sprint: H1 — Documental de Contexto
> Fecha: 2026-07-04
> Último merge documentado: PR #12 `700d09a`

## Prioridades

| # | Tarea | Prioridad | Área | Dependencias |
|---|---|---|---|---|
| P1 | Agregar suite de tests unitarios para `LabViewModel` | 🟡 Media | Testing | — |
| P2 | Agregar suite de tests unitarios para flujo de enrollment | 🟡 Media | Testing | — |
| P3 | Integrar capa de persistencia (reemplazar mocks) | 🟡 Media | Data | — |
| P4 | Agregar CI status badge al README | 🟢 Baja | Docs | — |
| P5 | Actualizar README (CI/CD pipeline existe, eliminar "No CI/CD") | 🟢 Baja | Docs | — |
| P6 | Evaluar cobertura de pruebas en flujo de credenciales | 🟢 Baja | Testing | — |
| P7 | Revisar deprecation warnings de Compose (Divider → HorizontalDivider, Icons.AutoMirrored) | 🟢 Baja | Code Health | — |
| P8 | Mantener cambios visuales de tema en PR dedicado y autorizado | 🟢 Baja | Process | — |

## Leyenda

| Prioridad | Significado |
|---|---|
| 🔴 Alta | Bloqueante para funcionalidad en producción |
| 🟡 Media | Mejora significativa sin bloquear |
| 🟢 Baja | Housekeeping, docs o deuda técnica menor |

## Notas

- **P3**: Actualmente toda la data se pierde al reiniciar la app.
- **P7**: ~30 warnings de deprecation visibles en compilación desktop.
- **P8**: No mezclar reemplazos de tema oscuro / Liquid Glass en PRs text-only o docs. `.codex/`, `.opencode/`, patches locales y estado de agentes no deben commitearse.

## Completado en H1

| Entrega | Descripción | Commit |
|---|---|---|
| H0 — PR #1 | Sanitizar CURP master student | `2fe2b4c` |
| PR-D — PR #2 | Eliminar código muerto confirmInitialGroup | `c7bbf06` |
| PR #5 | Ignorar archivos locales de entorno | `d8815c6` |
| PR #6 | Eliminar integración Gemini y dependencias | `963878f` |
| PR #8 | Agregar guardrails de test para `MockSaseData` | `2bd562c` |
| PR #10 | Mejorar salidas de navegación desde dashboards | `477c13a` |
| PR #12 | Corregir 18 textos visibles de UI: acentos y typo | `1cc2036` |
