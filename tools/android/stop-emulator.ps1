#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Stops the Android emulator gracefully.
.DESCRIPTION
    Identifies the running emulator (only emulator-* devices,
    not physical phones) and executes adb emu kill. Does
    NOT affect physical devices.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

Write-Step "Stop Emulator"

# 1. Find emulator devices only
$devices = Get-AdbDevices
$emulators = $devices | Where-Object { $_ -match "^emulator-\d+" }

if (-not $emulators -or @($emulators).Count -eq 0) {
    Write-Warn "No hay emulador activo"
    exit 0
}

# 2. If a specific serial was provided as arg, use it
$targetSerial = $null
if ($args.Count -gt 0) {
    $targetSerial = $args[0]
    if ($emulators -notcontains $targetSerial) {
        Write-Fail "Emulador $targetSerial no está activo. Activos: $($emulators -join ', ')"
        exit 1
    }
} else {
    $targetSerial = $emulators[0]
        if (@($emulators).Count -gt 1) {
        Write-Warn "Múltiples emuladores activos: $($emulators -join ', '). Deteniendo: $targetSerial"
    }
}

# 3. Graceful kill
Write-Info "Deteniendo emulador $targetSerial..."
$killOutput = & $Script:ADB -s $targetSerial emu kill 2>&1
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
    Write-Ok "Emulador $targetSerial detenido"
} else {
    Write-Warn "emu kill devolvió código $exitCode (puede ser normal si el emulador ya se estaba cerrando)"
    Write-Info $killOutput
}

# 4. Verify stopped
Start-Sleep -Seconds 3
$remaining = Get-AdbDevices | Where-Object { $_ -match "^emulator-\d+" }
if (-not $remaining -or @($remaining).Count -eq 0) {
    Write-Ok "No quedan emuladores activos"
} else {
    Write-Warn "Aún activos: $($remaining -join ', ')"
}

Write-Output $targetSerial
