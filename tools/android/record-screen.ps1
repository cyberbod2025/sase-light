#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Records the device screen via adb screenrecord.
.DESCRIPTION
    Records for a configurable duration (default 10s, max 60s),
    downloads the video, cleans up the temp file on device,
    and saves to the evidence directory.
.PARAMETER Duration
    Recording duration in seconds (1-60).
#>
param(
    [string]$Serial = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

$duration = 10
if ($args.Count -gt 0) {
    $duration = [int]$args[0]
    if ($duration -lt 1) { $duration = 1 }
    if ($duration -gt 60) { $duration = 60 }
}

Write-Step "Record Screen (${duration}s)"

# 1. Device
if ($Serial) { $targetDevice = $Serial } else {
    $devices = Get-AdbDevices
    $targetDevice = $devices | Where-Object { $_ -match "^emulator-\d+|\w+" } | Select-Object -First 1
}
if (-not $targetDevice) {
    Write-Fail "No hay dispositivo conectado"
    exit 1
}
Write-Ok "Dispositivo: $targetDevice"

# 2. Evidence dir
$evidenceDir = New-EvidenceDir

# 3. Record
$remotePath = "/sdcard/sase-record-temp.mp4"
$localPath = Join-Path $evidenceDir "screenrecord.mp4"

Write-Info "Grabando pantalla..."
$recordOutput = & $Script:ADB -s $targetDevice shell screenrecord --time-limit $duration --bit-rate 2M $remotePath 2>&1
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    Write-Fail "screenrecord falló (exit code: $exitCode)"
    Write-Info $recordOutput
    exit $exitCode
}

Write-Ok "Grabación completada"

# 4. Pull video
Write-Info "Descargando video..."
$pullOutput = & $Script:ADB -s $targetDevice pull $remotePath $localPath 2>&1
$pullExit = $LASTEXITCODE

if ($pullExit -ne 0 -or -not (Test-Path $localPath)) {
    Write-Fail "No se pudo descargar el video"
    Write-Info $pullOutput
    # Still try to clean up remote file
    & $Script:ADB -s $targetDevice shell rm $remotePath 2>$null
    exit 1
}

Write-Ok "Video: $localPath"
$size = (Get-Item $localPath).Length / 1MB
Write-Ok "Tamaño: $([math]::Round($size, 1)) MB"

# 5. Clean up remote file only
& $Script:ADB -s $targetDevice shell rm $remotePath 2>$null
Write-Ok "Archivo temporal en dispositivo eliminado"

$result = [PSCustomObject]@{
    EvidenceDir = $evidenceDir
    VideoPath   = $localPath
    Duration    = $duration
}
Write-Output $result
