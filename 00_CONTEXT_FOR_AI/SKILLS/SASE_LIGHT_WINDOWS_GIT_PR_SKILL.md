# SASE Light — skill Windows

`AGENTS.md` es la fuente operativa principal y `HUGO_SYSTEM_AGENT_INSTRUCTIONS.md` contiene las reglas transversales. Esta skill solo adapta comandos y operación a Windows/PowerShell.

## Entorno

- Windows workstation.
- PowerShell desde la raíz del repositorio.
- Gradle: `.\gradlew.bat`, siempre con `--no-daemon`.
- Logs: redirigir con `*>` y consultar con `Get-Content <log> -Tail 120`.

## Auditoría read-only

No cambiar ramas ni ejecutar checkout, pull, fetch, staging, commit, push, PR, merge, rebase, restore, reset, stash o clean. No ejecutar Gradle, instalaciones o migraciones sin autorización explícita.

Ante working tree sucio: detenerse, diagnosticar e informar.

## Implementación autorizada

Solo con autorización explícita de Hugo. Usar ramas pequeñas y PR cuando corresponda. Validación local típica:

```powershell
.\gradlew.bat :composeApp:desktopTest --no-daemon
```

Nunca hacer push automático a `main`, force push, `git add .` ni incluir `.codex/`, `.opencode/`, patches, builds o evidencias no autorizadas.
