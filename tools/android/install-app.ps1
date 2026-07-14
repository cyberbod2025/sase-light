#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Installs the debug APK on the connected device/emulator.
.DESCRIPTION
    Locates the APK (via build-debug.ps1 or direct path), runs
    adb install -r, verifies the installed package, and reports.
#>
param(
    [string]$Serial = ""
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

Write-Step "Install APK"

# 1. Resolve APK path
$apk = $Script:APK_PATH
if (-not (Test-Path $apk)) {
    Write-Warn "APK no encontrado en ruta por defecto. Intentando build-debug.ps1..."
    $buildResult = & "$PSScriptRoot\build-debug.ps1"
    $apk = $buildResult.ApkPath
}

if (-not (Test-Path $apk)) {
    Write-Fail "APK no encontrado. Ejecuta build-debug.ps1 primero."
    exit 1
}
Write-Ok "APK: $apk"

# 2. Get device serial
if ($Serial) { $targetDevice = $Serial } else {
    $devices = Get-AdbDevices
    $targetDevice = $devices | Where-Object { $_ -match "^emulator-\d+|\w+" } | Select-Object -First 1
}
if (-not $targetDevice) {
    Write-Fail "No hay dispositivo conectado. Ejecuta start-emulator.ps1 primero."
    exit 1
}
Write-Ok "Dispositivo: $targetDevice"

# 3. Install
Write-Info "Instalando..."
$installOutput = & $Script:ADB -s $targetDevice install -r $apk 2>&1
$exitCode = $LASTEXITCODE
Write-Info $installOutput

if ($exitCode -ne 0) {
    if ($installOutput -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE|INSTALL_FAILED_VERSION_DOWNGRADE") {
        Write-Warn "Instalación falló por conflicto de versión. Reintento con -r -d..."
        $installOutput = & $Script:ADB -s $targetDevice install -r -d $apk 2>&1
        $exitCode = $LASTEXITCODE
        Write-Info $installOutput
    }
    if ($exitCode -ne 0) {
        Write-Fail "Instalación falló (exit code: $exitCode)"
        exit $exitCode
    }
}

Write-Ok "Instalación exitosa"

# 4. Verify installed package
$pkgInfo = & $Script:ADB -s $targetDevice shell dumpsys package $Script:APPLICATION_ID 2>&1
if ($pkgInfo -match "versionName=([^\s]+)") {
    $versionName = $matches[1]
    Write-Ok "Paquete: $Script:APPLICATION_ID (versionName: $versionName)"
} else {
    Write-Warn "No se pudo verificar $Script:APPLICATION_ID en el dispositivo"
    Write-Info $pkgInfo
}

# 5. Report
$result = [PSCustomObject]@{
    DeviceSerial = $targetDevice
    ApkPath      = $apk
    PackageName  = $Script:APPLICATION_ID
    ExitCode     = $exitCode
}
Write-Output $result
