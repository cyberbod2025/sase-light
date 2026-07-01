# SASE_ABACUS_SANDBOX v0.1.1

## Propósito

Esta carpeta contiene contexto curado para **agentes externos de inteligencia artificial** (como Abacus AI). Su objetivo es reducir ambigüedad y proporcionar una base documental clara, segura y auditable sobre el estado actual de **SASE Light** en su commit base.

## Naturaleza de este documento

- **No es fuente de verdad del código.** El repositorio Git sigue siendo la fuente técnica de verdad.
- **No contiene datos reales de alumnos, CURP, teléfonos, domicilios, expedientes ni credenciales reales.**
- **No contiene llaves, tokens, variables .env ni secretos.**
- Es una **capa de contexto interpretativo**, no una especificación vinculante.
- Al momento del commit base, la documentación identifica los datos disponibles como **mock o simulados**. Antes de usar esta carpeta con cualquier agente externo, se debe verificar que no se hayan incorporado datos reales, CURP reales, teléfonos, domicilios, expedientes o credenciales reales.

## Commit base

```
4f4bc84 feat(sase): add credential back preview
```

## Estado

Documentación de contexto, no implementación. No modifica código funcional del repositorio.

---

## Caducidad del contexto

Este contexto se considera **potencialmente desactualizado** y debe ser revisado/regenerado si ocurre cualquiera de las siguientes condiciones:

1. **Cambia el flujo institucional** — Pre-solicitud familiar → Secretaría → Alta oficial → Expediente → Credencial.
2. **Se modifican modelos de datos** — Cualquier `data class`, `enum`, `interface` o `object` en el repositorio.
3. **Se conecta Supabase** o cualquier base de datos externa.
4. **Se agregan datos reales** de alumnos, CURP, teléfonos o domicilios al repositorio.
5. **Cambia el commit base** — Se agregan nuevos commits al branch `main`.
6. **Pasan más de 7 días** desde la fecha de generación (2026-06-30) sin actualización.

### Acción recomendada al vencer

- Revisar cada archivo contra el estado actual del repositorio.
- Actualizar referencias a modelos, pantallas y flujos.
- Verificar que no se hayan filtrado datos sensibles.
- Actualizar el commit base y la fecha de generación.
- Eliminar o marcar secciones que ya no correspondan.

---

## Archivos incluidos

| # | Archivo | Contenido |
|---|---------|-----------|
| 01 | `01_CONTEXTO_GENERAL_SASE_LIGHT.md` | Propósito, alcance, stack técnico |
| 02 | `02_REGLAS_QUE_NO_DEBE_ROMPER.md` | Reglas duras del proyecto |
| 03 | `03_ESTADO_ACTUAL_DEL_PROYECTO.md` | Módulos, pantallas, placeholders |
| 04 | `04_FLUJO_SECRETARIA_ALTA_EXPEDIENTE_CREDENCIAL.md` | Flujo institucional |
| 05 | `05_MODELOS_Y_DATOS_ACTUALES.md` | Modelos, campos, relaciones |
| 06 | `06_COMPONENTES_Y_PANTALLAS_EXISTENTES.md` | Pantallas y componentes UI |
| 07 | `07_TASKS_PENDIENTES_PRIORIZADAS.md` | Tareas pendientes priorizadas |
| 08 | `08_CRITERIOS_DE_AUDITORIA.md` | Criterios de revisión |
| 09 | `09_PROMPTS_PARA_ABACUS.md` | Prompts seguros para IA |
| 10 | `10_BITACORA_DE_DECISIONES.md` | Decisiones arquitectónicas |
| — | `README.md` | Este archivo |
