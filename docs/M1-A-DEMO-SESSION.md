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
