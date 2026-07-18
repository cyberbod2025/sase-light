# SASE Light

Demo de **Sistema de Acompañamiento y Seguimiento Escolar** para administración de Secretaría y expedientes institucionales.

## Estado actual

Es una demo funcional de flujo institucional. Los datos son mock, viven en memoria y se pierden al reiniciar. No hay backend, base de datos, autenticación real ni persistencia.

## Flujo

```text
Pre-solicitud familiar → Revisión de Secretaría → READY → Alta oficial anual → Expediente → Decisión de grupo → Credencial mock
```

## Tecnología

- Kotlin 2.1.20
- Compose Multiplatform 1.7.3
- Gradle 8.11.1
- Android, Desktop JVM e iOS
- Módulo activo: `:composeApp`

## Ejecución local

Windows/PowerShell:

```powershell
.\gradlew.bat :composeApp:desktopTest --no-daemon
.\gradlew.bat :composeApp:desktopRun --no-daemon
.\gradlew.bat :composeApp:assembleDebug --no-daemon
.\gradlew.bat :composeApp:compileKotlinDesktop --no-daemon
```

Codespaces/Linux:

```bash
./gradlew :composeApp:desktopTest --no-daemon
./gradlew :composeApp:desktopRun --no-daemon
./gradlew :composeApp:assembleDebug --no-daemon
./gradlew :composeApp:compileKotlinDesktop --no-daemon
```

## Pruebas

```powershell
.\gradlew.bat :composeApp:desktopTest --no-daemon
.\gradlew.bat :composeApp:compileKotlinDesktop --no-daemon
```

CI ejecuta Android debug, `desktopTest` y compilación Desktop en GitHub Actions/Linux. Estos comandos explican cómo validar el proyecto; no significan que el build actual esté verde ni que las pruebas hayan pasado.

## Estructura

- `composeApp/src/commonMain`: dominio, ViewModels y UI común.
- `composeApp/src/commonTest`: pruebas comunes ejecutadas por Desktop.
- `composeApp/src/androidMain`, `desktopMain`, `iosMain`: entrypoints por plataforma.
- `tools/android`: diagnóstico y automatización Android.
- `evidence/android`: evidencia histórica o de validación; no incluirla sin autorización.

## Límites y privacidad

Fotos, notificaciones, documentos, QR, PDF, impresión y credencial son mock o placeholder cuando se indique en la UI. No usar datos reales de alumnos o familias. No presentar esta base como producto productivo.

El directorio `app/` es código stale y no forma parte del build activo.
