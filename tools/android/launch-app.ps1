#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Launches the SASE Light app on the connected device.
.DESCRIPTION
    Resolves the applicationId and main activity, launches via
    adb shell am start, waits briefly, and confirms the app
    is in the foreground via dumpsys.
#>
param(
    [string]$Serial = ""
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

Write-Step "Launch App"

# 1. Resolve device
if ($Serial) { $targetDevice = $Serial } else {
    $devices = Get-AdbDevices
    $targetDevice = $devices | Where-Object { $_ -match "^emulator-\d+|\w+" } | Select-Object -First 1
}
if (-not $targetDevice) {
    Write-Fail "No hay dispositivo conectado"
    exit 1
}
Write-Ok "Dispositivo: $targetDevice"

# 2. Determine main activity
$intentAction = "android.intent.action.MAIN"
$categoryLauncher = "android.intent.category.LAUNCHER"

# Try reading from package manager first
$pmResolve = & $Script:ADB -s $targetDevice shell "pm resolve-activity --brief $Script:APPLICATION_ID" 2>$null
$activity = $null
$pmText = $pmResolve | Out-String -Stream | Where-Object { $_ -match "/" } | Select-Object -Last 1
if ($pmText -match "([a-zA-Z0-9_\.]+/[a-zA-Z0-9_\.a-zA-Z]+)") {
    $activity = $matches[1]
} elseif ($pmText -match "\.([a-zA-Z_]\w*)\s*$") {
    $activity = "$($Script:APPLICATION_ID)/.$($matches[1])"
} else {
    # Fallback: use MAIN_ACTIVITY from config
    $activity = "$Script:APPLICATION_ID/$Script:MAIN_ACTIVITY"
}
Write-Ok "Actividad: $activity"

# 3. Launch
Write-Info "Lanzando: $activity"
$launchOutput = & $Script:ADB -s $targetDevice shell am start -n $activity -a $intentAction -c $categoryLauncher 2>&1
$exitCode = $LASTEXITCODE
Write-Info $launchOutput

if ($exitCode -ne 0 -or $launchOutput -match "Error|error") {
    Write-Fail "Lanzamiento falló"
    exit 1
}

# 4. Wait and confirm foreground
Start-Sleep -Seconds 5

$dumpsys = & $Script:ADB -s $targetDevice shell dumpsys activity activities 2>&1
$resumed = $dumpsys | Select-String -Pattern "(?:m|top)ResumedActivity.*$Script:APPLICATION_ID" | Select-Object -First 1
$focused = $dumpsys | Select-String -Pattern "(?:m|top)Focused(?:App|Activity).*$Script:APPLICATION_ID" | Select-Object -First 1
$windowFocused = $dumpsys | Select-String -Pattern "mFocusedWindow.*$Script:APPLICATION_ID" | Select-Object -First 1

if ($resumed) {
    Write-Ok "App en primer plano: $($resumed.Line.Trim())"
} elseif ($focused) {
    Write-Ok "App enfocada: $($focused.Line.Trim())"
} elseif ($windowFocused) {
    Write-Ok "App con ventana enfocada: $($windowFocused.Line.Trim())"
} else {
    Write-Warn "No se pudo confirmar primer plano. Diagnóstico guardado."
    $dumpsys | Out-File -FilePath (Join-Path $Script:EVIDENCE_BASE "launch-dumpsys.log") -Encoding utf8
}

$result = [PSCustomObject]@{
    DeviceSerial = $targetDevice
    Activity     = $activity
    InForeground = [bool]($resumed -or $focused)
    ExitCode     = $exitCode
}
Write-Output $result
