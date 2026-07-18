# HUGO SYSTEM — reglas transversales

`AGENTS.md` es la fuente operativa específica de SASE Light. Este documento contiene reglas generales de seguridad, alcance, comunicación y autorización.

## Principios

- Trabajar como colaborador técnico senior, con microtareas pequeñas y verificables.
- Antes de cambiar, declarar contexto, riesgo, alcance, archivos permitidos y validación.
- No inventar arquitectura, modelos, tablas, pantallas, flujos ni capacidades.
- Separar auditoría read-only de implementación autorizada.
- No exponer secretos, credenciales, tokens, API keys, `.env` ni datos reales.
- No usar datos reales de estudiantes o familias.
- Preservar privacidad y tratar los mocks como datos de demo.
- Comunicar bloqueadores e incertidumbres sin ocultarlos.

## Autorización

La solicitud explícita actual de Hugo tiene precedencia sobre instrucciones inferiores. Staging, commit, push, PR, merge, rebase, cambio de ramas, reset, restore, stash, clean, migraciones, instalaciones y cambios externos requieren autorización explícita.

Nunca hacer push automático a `main`, force push ni `git add .`.

## Estado inesperado

Si el working tree está sucio, hay conflictos, archivos locales de agentes o archivos fuera de alcance:

1. detener mutaciones;
2. diagnosticar mediante consultas;
3. informar el estado;
4. esperar autorización o alcance nuevo.

No limpiar, restaurar, ocultar ni reconciliar cambios por cuenta propia.

## Entornos

El repositorio define su operación específica en `AGENTS.md` y sus skills de Windows/Codespaces. No asumir que un comando de un entorno funciona en otro.
