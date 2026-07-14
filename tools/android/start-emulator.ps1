#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Starts the Pixel_7 Android emulator (or configured AVD).
.DESCRIPTION
    Checks for an already-running emulator first. If none found,
    launches the configured AVD, waits for boot, and confirms
    the launcher is responsive.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

$avd = $Script:DEFAULT_AVD
if ($args.Count -gt 0) { $avd = $args[0] }

Write-Step "Start Emulator: $avd"

# 1. Check if already running
$devices = Get-AdbDevices
$emulatorRunning = $devices | Where-Object { $_ -match "^emulator-\d+" } | Select-Object -First 1
if ($emulatorRunning) {
    Write-Ok "Emulador ya activo: $emulatorRunning"
    Write-Output $emulatorRunning
    exit 0
}

# 2. Assert emulator binary exists
if (-not (Assert-Emulator)) { exit 1 }

# 3. Assert AVD exists
$avds = Get-EmulatorAvds
if ($avds -notcontains $avd) {
    Write-Fail "AVD '$avd' no encontrado. AVDs disponibles: $($avds -join ', ')"
    exit 1
}

# 4. Launch emulator
Write-Info "Lanzando emulador $avd..."
$emulatorLog = Join-Path $Script:EVIDENCE_BASE "emulator-startup.log"
$null = New-Item -ItemType Directory -Path (Split-Path $emulatorLog -Parent) -Force

$emuArgs = @(
    "-avd", $avd,
    "-no-snapshot-save"
)
Write-Info "Comando: $($Script:EMULATOR) $($emuArgs -join ' ')"

$emuProcess = Start-Process -FilePath $Script:EMULATOR -ArgumentList $emuArgs -NoNewWindow -RedirectStandardOutput $emulatorLog -PassThru
Write-Info "Emulador iniciado (PID: $($emuProcess.Id)). Esperando dispositivo..."

# 5. Wait for device
$timeout = 120
$waited = 0
$serial = $null
while ($waited -lt $timeout) {
    Start-Sleep -Seconds 3
    $currentDevices = Get-AdbDevices
    $emulator = $currentDevices | Where-Object { $_ -match "^emulator-\d+" } | Select-Object -First 1
    if ($emulator) {
        $serial = $emulator
        Write-Ok "Dispositivo detectado: $serial"
        break
    }
    $waited += 3
    Write-Info "  esperando... ($waited s)"
}

if (-not $serial) {
    Write-Fail "Emulador no detectado tras ${timeout}s. Revisa: $emulatorLog"
    exit 1
}

# 6. Wait for boot completed
Write-Info "Esperando sys.boot_completed=1..."
$bootTimeout = 180
$bootWaited = 0
$booted = $false
while ($bootWaited -lt $bootTimeout) {
    Start-Sleep -Seconds 5
    try {
        $bootAnim = & $Script:ADB -s $serial shell getprop init.svc.bootanim 2>$null
        $bootCompleted = & $Script:ADB -s $serial shell getprop sys.boot_completed 2>$null
        if ($bootAnim -is [array]) { $bootAnim = $bootAnim[-1] }
        if ($bootCompleted -is [array]) { $bootCompleted = $bootCompleted[-1] }
        $bootAnim = "$bootAnim".Trim()
        $bootCompleted = "$bootCompleted".Trim()
    } catch {
        $bootAnim = ""
        $bootCompleted = ""
    }
    if ($bootCompleted -eq "1") {
        $booted = $true
        break
    }
    $bootWaited += 5
    if ($bootWaited % 20 -eq 0) {
        Write-Info "  boot anim: $bootAnim, completed: $bootCompleted ($bootWaited s)"
    }
}

if (-not $booted) {
    Write-Fail "Boot no completado tras ${bootTimeout}s"
    & $Script:ADB -s $serial shell getprop 2>&1 | Out-File -FilePath (Join-Path $Script:EVIDENCE_BASE "boot-failure-getprop.log") -Encoding utf8
    exit 1
}

Write-Ok "Boot completado"

# 7. Confirm launcher responds
Start-Sleep -Seconds 3
$launcherCheck = & $Script:ADB -s $serial shell dumpsys activity 2>&1 | Select-String "mResumedActivity" | Select-Object -First 1
if ($launcherCheck) {
    Write-Info "  Launcher: $($launcherCheck.Line.Trim())"
} else {
    Write-Warn "  No se pudo confirmar launcher, pero boot completado"
}

Write-Ok "Emulador $serial listo"
Write-Output $serial
