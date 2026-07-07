# SASE Light — Codespaces Git/PR Workflow Skill

**INTERNAL OPERATIONAL CONTEXT — NO SECRETS**

Esta skill define el flujo seguro para trabajar con agentes IA en SASE Light desde GitHub Codespaces.

## Entorno oficial actual

- **GitHub Codespaces / Linux container**
- Trabajar desde la raíz actual del repositorio.
- NO usar rutas Windows como `C:\HUGO_SYSTEM\Projects\sase-light`.
- NO usar `.\gradlew.bat`.
- Usar `./gradlew :composeApp:desktopTest --no-daemon`.

## Fuente de verdad

GitHub/main es la fuente técnica de verdad.

## Flujo institucional que no debe romperse

Pre-solicitud familiar → Secretaría → Alta oficial → Expediente → Credencial

No inventar rutas, modelos, pantallas, tablas, campos ni flujos que contradigan este flujo.

## Reglas duras

1. No inventar modelos, tablas, campos, pantallas ni flujos.
2. No activar backend/Supabase sin autorización explícita.
3. No implementar PDF/impresión sin autorización explícita.
4. No activar Gemini ni IA externa sin autorización explícita.
5. No usar datos reales.
6. No exponer credenciales, tokens, API keys, .env, service-role keys ni datos sensibles.
7. No incluir información médica, familiar, UDEEI o Trabajo Social en credenciales.
8. No tocar tema visual global sin autorización explícita.
9. No tocar `Color.kt`, `Theme.kt` ni `Type.kt` fuera de scope.
10. No commitear estado local de agentes.
11. No usar `git add .`.
12. No hacer stash, reset, restore, force push ni borrar archivos sin autorización explícita.

## Ritual obligatorio de arranque en Codespaces

```
pwd
git checkout main
git pull origin main
git status --short
./gradlew :composeApp:desktopTest --no-daemon
```

Si `git status --short` no está limpio, detenerse y reportar.

No continuar.
No limpiar por cuenta propia.
No hacer stash.
No restaurar.
No borrar.
No commitear.

## Manejo de estado local de agentes

Estas rutas no son producto:
- `.codex/`
- `.opencode/`
- `*.patch`
- metadata local
- estado de worktrees/agentes

Si aparecen como untracked o modificadas:
1. Detenerse.
2. Reportar.
3. Pedir autorización explícita.

Con autorización, se puede agregar localmente a:
`.git/info/exclude`

Ejemplo:
```
printf "\n.codex/\n" >> .git/info/exclude
printf "\n.opencode/\n" >> .git/info/exclude
git status --short
```

No tocar `.gitignore` salvo autorización explícita.

## Crear rama

```
git checkout -b tipo/scope-descriptivo
```

Ejemplos:
- `fix(portal): validate split student name fields`
- `docs(system): add internal agent context and codespaces workflow skill`
- `chore(ui): fix Spanish accent labels`

## Durante la tarea

Antes de editar, declarar:
1. Objetivo.
2. Scope.
3. Archivos permitidos.
4. Archivos prohibidos.
5. Validación.
6. Riesgos.

No tocar archivos fuera del scope. Si aparece una necesidad fuera del scope, detenerse y reportar.

## Validación antes de commit

```
git status --short
git diff --stat
./gradlew :composeApp:desktopTest --no-daemon
```

Confirmar que solo cambiaron archivos autorizados. Si aparece cualquier archivo inesperado, detenerse.

## Staging seguro

Prohibido: `git add .`

Usar rutas específicas:
```
git add ruta/exacta/archivo1.md ruta/exacta/archivo2.md
```

## Commit

```
git commit -m "tipo(scope): descripción"
```

Ejemplos:
- `docs(system): add internal agent context and codespaces workflow skill`
- `fix(portal): validate split student name fields`
- `chore(ui): fix Spanish accent labels`

## Push

```
git push -u origin nombre-de-rama
```

No usar `--force` salvo autorización explícita.

## Pull Request

Cada PR debe incluir:
- **Scope**: qué cambia.
- **Files**: lista de archivos modificados.
- **Validation**: `:composeApp:desktopTest PASS`.
- **Security**: no secrets, no API keys, no tokens, no .env, no real student/family data.
- **Out of scope**: qué NO se tocó.

El PR debe ser pequeño, revisable y con objetivo único.

## Después de abrir PR

Esperar CI. No mergear si:
- CI falla.
- Hay archivos fuera de scope.
- Hay comentarios críticos sin resolver.
- El diff contiene cambios no autorizados.
- Se tocaron datos sensibles.
- Se tocó tema visual sin autorización.

## Después de merge

```
git checkout main
git pull origin main
git status --short
./gradlew :composeApp:desktopTest --no-daemon
git log --oneline -6
```

Si `git status --short` no queda limpio, detenerse.

## Manejo de conflictos

Si aparece conflicto (`AA`, `UU`, `<<<<<<< HEAD`, `=======`, `>>>>>>>`):
Detenerse y reportar. No resolver a ciegas.

Si el usuario autoriza abortar:
```
git merge --abort
```

No usar `git reset --hard` sin autorización explícita.

## Tema visual

SASE Light mantiene estética vigente:
- tema oscuro por defecto
- Liquid Glass cuando aplique

No tocar sin autorización:
- `composeApp/src/commonMain/kotlin/com/example/ui/theme/Color.kt`
- `composeApp/src/commonMain/kotlin/com/example/ui/theme/Theme.kt`
- `composeApp/src/commonMain/kotlin/com/example/ui/theme/Type.kt`

Si aparecen modificados sin estar en scope, detenerse y reportar.

## Datos sensibles

Nunca incluir:
- CURP reales
- teléfonos reales
- domicilios reales
- datos médicos reales
- datos familiares reales
- UDEEI
- Trabajo Social
- credenciales
- tokens
- API keys
- .env
- service-role keys

Los mocks son demo.

## Formato de entrega final

```
## Resultado
- Rama:
- Archivos modificados:
- Resumen:
- Validación:
- Commit:
- PR:
- CI:
- Git status final:
- Observaciones:
```

## Regla final

Si algo no está claro, detenerse y preguntar.

No adivinar.
No mezclar scopes.
No maquillar fallos.
No romper main.