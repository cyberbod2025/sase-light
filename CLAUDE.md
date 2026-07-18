# Adaptador para Claude

`AGENTS.md` es la fuente operativa principal. `HUGO_SYSTEM_AGENT_INSTRUCTIONS.md` contiene las reglas transversales. Este archivo solo adapta esas reglas para Claude.

## Proyecto

SASE Light es una demo Compose Multiplatform/KMP para Secretaría y expedientes. El módulo activo es `:composeApp`, con Android, Desktop JVM e iOS. La arquitectura actual usa `LabViewModel` y `PreApplicationViewModel`, datos mock in-memory y navegación común mediante `Screen`.

Flujo obligatorio:

```text
Pre-solicitud familiar → Secretaría → READY → Alta oficial → Expediente → Decisión de grupo → Credencial mock
```

## Comandos Windows

```powershell
.\gradlew.bat :composeApp:desktopTest --no-daemon
.\gradlew.bat :composeApp:assembleDebug --no-daemon
.\gradlew.bat :composeApp:compileKotlinDesktop --no-daemon
.\gradlew.bat :composeApp:desktopRun --no-daemon
```

Las pruebas están en `composeApp/src/commonTest` y el CI también valida Android, Desktop tests y compilación Desktop. No afirmar PASS sin ejecutar la validación correspondiente.

## Reglas de trabajo

Usar microtareas, alcance explícito y validación proporcional. No inventar arquitectura. No activar backend, IA externa, PDF o impresión sin autorización. No usar datos reales ni exponer secretos.

Las auditorías read-only no pueden cambiar ramas, limpiar el checkout, ejecutar staging, commit, push, PR, merge, rebase, restore, reset, stash ni `git clean` (`git clean -f`, `git clean -fd` o cualquier variante que elimine archivos no rastreados). Las fases de implementación requieren autorización explícita de Hugo. Si Git está sucio, diagnosticar e informar; no limpiar automáticamente.
