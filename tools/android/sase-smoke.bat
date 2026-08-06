@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem SASE Light Android smoke runner.
rem Builds, installs and launches the debug APK without collecting device data.

set "APP_ID=com.aistudio.labvirtual.kvmpx"
set "ACTIVITY=.MainActivity"
set "APK=composeApp\build\outputs\apk\debug\composeApp-debug.apk"

where java >nul 2>nul || (
  echo [FAIL] Java no esta disponible en PATH.
  exit /b 1
)

for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~V"
echo %JAVA_VERSION% | findstr /r /c:"^21[\."]" >nul || (
  echo [FAIL] Se requiere JDK 21. Version detectada: %JAVA_VERSION%
  exit /b 1
)

if not exist gradlew.bat (
  echo [FAIL] Ejecuta este script desde la raiz del repositorio.
  exit /b 1
)

where adb >nul 2>nul || (
  echo [FAIL] adb no esta disponible en PATH.
  exit /b 1
)

for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
  if "%%B"=="device" set /a DEVICE_COUNT+=1
)
if not defined DEVICE_COUNT (
  echo [FAIL] No hay un dispositivo Android autorizado.
  exit /b 1
)
if not "%DEVICE_COUNT%"=="1" (
  echo [FAIL] Conecta exactamente un dispositivo Android. Detectados: %DEVICE_COUNT%
  exit /b 1
)

echo [1/3] Compilando APK debug...
call gradlew.bat :composeApp:assembleDebug --no-daemon
if errorlevel 1 (
  echo [FAIL] Fallo la compilacion Android.
  exit /b 1
)

if not exist "%APK%" (
  echo [FAIL] No se encontro el APK esperado: %APK%
  exit /b 1
)

echo [2/3] Instalando APK...
adb install -r "%APK%"
if errorlevel 1 (
  echo [FAIL] No se pudo instalar el APK.
  exit /b 1
)

echo [3/3] Abriendo SASE-310...
adb shell am force-stop %APP_ID% >nul
adb shell am start -n %APP_ID%/%ACTIVITY%
if errorlevel 1 (
  echo [FAIL] No se pudo iniciar la aplicacion.
  exit /b 1
)

echo.
echo [PASS TECNICO] APK compilado, instalado y abierto.
echo Completa ahora la checklist manual en docs\testing\ANDROID_SMOKE_CHECKLIST.md
exit /b 0
