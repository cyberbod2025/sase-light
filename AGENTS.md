# SASE Light — instrucciones operativas

## Precedencia

1. Solicitud explícita actual de Hugo.
2. Este `AGENTS.md`.
3. `00_CONTEXT_FOR_AI/HUGO_SYSTEM_AGENT_INSTRUCTIONS.md`.
4. Skill específica del entorno.
5. `CLAUDE.md` u otro adaptador.
6. `README.md`.
7. Checkpoints históricos, que no son normativos.

Una instrucción inferior no puede autorizar algo prohibido por una superior.

## Identidad y estado

SASE significa **Sistema de Acompañamiento y Seguimiento Escolar**. SASE Light es una demo de administración escolar construida con Compose Multiplatform/KMP.

- Módulo activo: `:composeApp`.
- Targets: Android, Desktop JVM e iOS.
- Datos actuales: mock e in-memory.
- No hay backend, base de datos ni persistencia real.
- El directorio raíz `app/` es stale y no forma parte del build; no editarlo.

Stack documentado: Kotlin 2.1.20, Compose Multiplatform 1.7.3, Gradle 8.11.1 y AGP 8.7.3.

## Arquitectura actual

La UI común usa dos ViewModels sin DI ni librería de navegación:

- `LabViewModel`: navegación, estudiantes y auditorías mediante `StateFlow`.
- `PreApplicationViewModel`: pre-solicitud familiar, revisión, readiness y conversión.

Las rutas estables incluyen dashboard de Secretaría, expedientes, inscripciones, Portal Familia, Pre-Solicitudes, Altas Oficiales, `StudentRecord`, vista previa y dashboard de credencial. Las referencias canónicas están en `composeApp/src/commonMain/kotlin/com/example/viewmodel/` y `ui/`.

Componentes estables del dominio: repositorios mock, `AnnualEnrollmentFlowCoordinator`, `AnnualEnrollmentPersistenceAdapter`, `InstitutionalStudentRecordResolver`, estados derivados y presentación institucional del expediente.

## Flujo institucional

```text
Pre-solicitud familiar
→ Revisión de Secretaría
→ READY
→ Alta oficial anual
→ Expediente institucional
→ Decisión de grupo
→ Credencial mock
```

Fotos, notificaciones, documentos, PDF, impresión, QR y credencial son mock o placeholder cuando el código así lo indica. No presentar la demo como sistema productivo.

## Pruebas y CI

- Tests activos: `composeApp/src/commonTest`.
- Ejecución local: `:composeApp:desktopTest`.
- Compilación Android: `:composeApp:assembleDebug`.
- Compilación Desktop: `:composeApp:compileKotlinDesktop`.
- CI existente: `.github/workflows/build.yml` en GitHub Actions/Linux.

Las expresiones “implementado”, “disponible” o “demo funcional” describen capacidades presentes en el código. No significan que el build, las pruebas o el recorrido visual hayan sido ejecutados y aprobados en la sesión actual. Cada informe debe distinguir entre implementado, probado, validado visualmente y pendiente de validar. No afirmar PASS si no se ejecutó en la sesión actual.

## Entornos

- Local principal: Windows + PowerShell + `.\gradlew.bat`.
- Alternativo soportado: GitHub Codespaces/Linux + `./gradlew`.
- CI: GitHub Actions/Linux.

Consultar la skill correspondiente al entorno. No cambiar de entorno durante una auditoría read-only.

## Git y autorización

### Auditoría read-only

No permite checkout, pull, fetch, cambio de ramas, staging, commit, push, PR, merge, rebase, restore, reset, stash ni clean. Tampoco permite Gradle, instalación o migraciones salvo autorización explícita.

### Implementación autorizada

Solo con autorización explícita de Hugo. Puede usar ramas pequeñas y PR, pero nunca:

- `git add .`;
- push automático o directo a `main`;
- force push;
- archivos fuera de alcance;
- `.codex/`, `.opencode/`, patches, builds o evidencias no autorizadas.

Si `git status --short` no está limpio: detener mutaciones, diagnosticar, informar y esperar alcance explícito. No limpiar automáticamente.

## Seguridad

No inventar arquitectura ni activar backend, Supabase, IA externa, PDF o impresión sin autorización. No usar ni exponer CURP, teléfonos, domicilios, datos médicos/familiares, credenciales, tokens, API keys o datos reales. Las credenciales mock no deben mostrar información médica, familiar, UDEII ni Trabajo Social.
