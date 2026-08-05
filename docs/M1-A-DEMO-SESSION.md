# M1-A — Demo local y sesión institucional

## Modos de ejecución

La aplicación resuelve el ambiente una sola vez en `SaseCompositionRoot`.

| Modo | Autenticación | Datos operativos | Comportamiento seguro |
|---|---|---|---|
| `DEMO_LOCAL` | Usuarios sintéticos locales | Sintéticos y reiniciables | Muestra institución ficticia y aviso permanente |
| `SUPABASE_STAGING` | Supabase con configuración externa | Sintéticos | No cae a demo si falta configuración o falla la red |
| `PRODUCTION` | Deshabilitada en M1-A | Ninguno | Falla cerrado hasta validar persistencia y RLS |

Producción no habilita botones, usuarios ni repositorios de autenticación demo. M1-A no aplica migraciones, no crea usuarios y no modifica Supabase remoto.

## Ejecutar Desktop en DEMO_LOCAL

Desde la raíz del repositorio, en PowerShell:

```powershell
.\gradlew.bat --no-daemon :composeApp:run
```

Desktop selecciona explícitamente `DEMO_LOCAL` cuando no se suministra otro modo. Android Debug recibe el mismo modo desde `BuildConfig`; Android Release fuerza `PRODUCTION` y queda bloqueado de forma segura en este incremento.

### Limitación conocida: Desktop y el modo por defecto

Todas las ejecuciones Desktop de este incremento son de **desarrollo** (`.\gradlew.bat :composeApp:run`), no una distribución empaquetada. Si no se establece explícitamente la propiedad `sase.environment` (Gradle) o la system property/variable de entorno `SASE_APP_ENVIRONMENT`, Desktop usa `DEMO_LOCAL` — este modo siempre se identifica visualmente en la aplicación (institución ficticia y aviso de datos sintéticos), por lo que el fallback nunca es silencioso desde la perspectiva del usuario.

Hoy **no existe una distribución Desktop de producción** (no hay tarea `nativeDistributions`/`jpackage` en el proyecto). Antes de crear una, será obligatorio fijar explícitamente `PRODUCTION` (o `SUPABASE_STAGING`) en esa tarea, de la misma forma en que Android Release fuerza `PRODUCTION` de forma incondicional en `buildTypes.release` — una futura distribución Desktop no podrá heredar silenciosamente `DEMO_LOCAL` por ausencia de configuración. Esta limitación es un hueco documentado en el desarrollo actual, no un soporte de Desktop de producción ya resuelto.

## Configurar staging externamente

Antes de ejecutar Desktop:

```powershell
$env:SASE_APP_ENVIRONMENT = "SUPABASE_STAGING"
$env:SASE_APP_VERSION = "1.0"
$env:SASE_SUPABASE_URL = "https://PROJECT_REF.supabase.co"
$env:SASE_SUPABASE_PUBLISHABLE_KEY = "PUBLISHABLE_KEY"
.\gradlew.bat --no-daemon :composeApp:run
```

No se incluyen valores reales en el repositorio. Si falta uno, la app muestra un error de configuración y no sustituye staging por demo.

## Recorrido demo en cinco pasos

1. Abrir la app y confirmar `DEMO LOCAL`, la institución ficticia y el aviso de datos sintéticos.
2. Elegir **Dirección**, **Secretaría** o **Docente**.
3. Confirmar usuario, institución, rol, ciclo y modo en el encabezado.
4. Con Dirección, cambiar únicamente a Secretaría; con Docente, confirmar que no aparecen módulos operativos no autorizados.
5. Cerrar sesión, volver a entrar y usar **Reiniciar demo** para restaurar el estado sintético.

## Validación local

```powershell
.\gradlew.bat --no-daemon :composeApp:compileKotlinDesktop
.\gradlew.bat --no-daemon :composeApp:desktopTest
.\gradlew.bat --no-daemon :composeApp:assembleDebug
```

La auditoría local deriva institución, perfil, membresía y rol de la sesión efectiva. No acepta esos valores desde la UI y rechaza contenido sensible antes de persistir el evento local.

## Pendientes técnicos (auditoría M1-A)

Registrados a partir de la auditoría de solo lectura sobre `codex/m1-demo-session`. Ninguno bloqueó el cierre de M1-A; quedan pospuestos por decisión explícita.

- **Expediente: resultado de guardado no verificado en UI.** `StudentRecordScreen` ignora el `Boolean` que devuelve `updateStudent` y muestra "Cambios guardados" sin condicionarlo al resultado. Pertenece al módulo de expediente/estudiantes, fuera de alcance de M1-A; no es alcanzable hoy con ninguna combinación de permisos vigente.
- **Auditoría registrada después de la mutación.** En `LabViewModel` (`updateStudent`, `addStudent`, `addObservation`, `reportIncident`, `advanceIncident`) la escritura ocurre antes de validar el evento de auditoría; si el validador rechazara el evento, el dato ya cambió sin quedar registro. No es trivialmente disparable hoy porque los identificadores usados son generados por el sistema.
- **Etiqueta fija "Secretaría" en el pie del sidebar.** `SaseSidebar` no recibe el rol activo y siempre muestra "Secretaría", visible ahora también para Dirección. Preexistente a esta sesión, no es una regresión.
- **`SUPABASE_STAGING` combina autenticación real con repositorios mock de estudiantes/auditoría; iOS no soporta `SUPABASE_STAGING`/`PRODUCTION`.** Ya reflejado en la tabla de modos de este documento, pero sin comentario equivalente junto al código (`SaseCompositionRoot`, `PlatformEnvironment.ios.kt`).
