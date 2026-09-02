#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Deterministic Android smoke run for SASE Light.
.DESCRIPTION
    Composes the existing build-debug / install-app / launch-app scripts
    into a single technical PASS/FAIL/BLOCKED verdict, per issue #43 and
    the checklist in docs/testing/ANDROID_SMOKE_CHECKLIST.md.

    Deliberately does not reimplement build/install/launch: those already
    live in build-debug.ps1, install-app.ps1 and launch-app.ps1 with
    SHA-256, retry-on-version-conflict and foreground confirmation this
    script would otherwise have to duplicate.

    A technical PASS means only that the app built, installed and opened.
    It does not replace the manual checklist in ANDROID_SMOKE_CHECKLIST.md.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "  SASE Light — Android Smoke" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

function Write-Verdict($verdict, $reason) {
    $color = switch ($verdict) { "PASS" { "Green" }; "FAIL" { "Red" }; default { "Yellow" } }
    Write-Host ""
    Write-Host "[$verdict TECNICO] $reason" -ForegroundColor $color
}

# 1. Exactly one authorized device is required: an ambiguous target device
#    is not a product defect, it is an environment precondition.
$devices = Get-AdbDevices
if (@($devices).Count -eq 0) {
    Write-Verdict "BLOCKED" "Ningún dispositivo Android autorizado conectado. Usa start-emulator.ps1 o conecta un dispositivo físico."
    exit 2
}
if (@($devices).Count -gt 1) {
    Write-Verdict "BLOCKED" "Hay $(@($devices).Count) dispositivos conectados. Conecta exactamente uno."
    exit 2
}
Write-Ok "Dispositivo objetivo: $($devices[0])"

# 2. Build
Write-Step "1/3 Compilando APK debug"
try {
    $build = & "$PSScriptRoot\build-debug.ps1"
} catch {
    Write-Verdict "FAIL" "Compilación Android falló: $($_.Exception.Message)"
    exit 1
}
if (-not $build -or -not (Test-Path $build.ApkPath)) {
    Write-Verdict "FAIL" "El build no produjo un APK utilizable."
    exit 1
}

# 3. Install
Write-Step "2/3 Instalando APK"
try {
    $install = & "$PSScriptRoot\install-app.ps1" -Serial $devices[0]
} catch {
    Write-Verdict "FAIL" "Instalación falló: $($_.Exception.Message)"
    exit 1
}

# 4. Launch
Write-Step "3/3 Abriendo SASE Light"
try {
    $launch = & "$PSScriptRoot\launch-app.ps1" -Serial $devices[0]
} catch {
    Write-Verdict "FAIL" "Lanzamiento falló: $($_.Exception.Message)"
    exit 1
}
if (-not $launch.InForeground) {
    Write-Verdict "FAIL" "La app no confirmó estar en primer plano tras el lanzamiento."
    exit 1
}

# 5. Sanitized evidence: only what docs/testing/ANDROID_SMOKE_CHECKLIST.md allows.
$evidenceDir = New-EvidenceDir
$summary = [PSCustomObject]@{
    Sha256      = $build.Sha256
    SizeMB      = $build.SizeMB
    Package     = $install.PackageName
    DeviceClass = "no registrado por este script: complétalo a mano en el resultado sanitizado"
    InForeground = $launch.InForeground
    Timestamp   = Get-Date -Format "o"
}
$summary | ConvertTo-Json | Out-File -FilePath (Join-Path $evidenceDir "smoke-summary.json") -Encoding utf8
Write-Ok "Evidencia sanitizada: $evidenceDir\smoke-summary.json"

Write-Verdict "PASS" "APK compilado (SHA-256 $($build.Sha256)), instalado y abierto en primer plano."
Write-Host "Completa ahora la checklist manual en docs\testing\ANDROID_SMOKE_CHECKLIST.md" -ForegroundColor Cyan
Write-Host "Registra el resultado final en docs\testing\ANDROID_SMOKE_RESULT_TEMPLATE.md" -ForegroundColor Cyan
exit 0
