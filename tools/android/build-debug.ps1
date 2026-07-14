#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Builds the Android debug APK for SASE Light.
.DESCRIPTION
    Compiles the :composeApp:assembleDebug task, locates the APK,
    computes SHA-256, and outputs a reusable object.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

Write-Step "Build Debug APK"

# 1. Locate Gradle wrapper
$gradlew = Join-Path $Script:PROJECT_ROOT "gradlew"
if (-not (Test-Path $gradlew)) {
    Write-Fail "gradlew no encontrado en $gradlew"
    exit 1
}
Write-Ok "Gradle wrapper: $gradlew"

# 2. Inspect available tasks for composeApp (informational)
Write-Info "Identificando tarea Android debug..."

# 3. Execute build
Write-Info "Ejecutando: $gradlew :composeApp:assembleDebug --no-daemon"
$buildOutput = & $gradlew :composeApp:assembleDebug --no-daemon 2>&1
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    Write-Fail "Build falló (exit code: $exitCode)"
    Write-Host $buildOutput -ForegroundColor Red
    exit $exitCode
}
Write-Ok "Build exitoso"

# 4. Locate APK
$apkVariants = @(
    $Script:APK_PATH,
    (Join-Path $Script:PROJECT_ROOT "composeApp\build\outputs\apk\debug\*.apk"),
    (Join-Path $Script:PROJECT_ROOT "composeApp\build\outputs\apk\androidDebug\*.apk")
)

$apk = $null
foreach ($pattern in $apkVariants) {
    $matches = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue
    if ($matches) {
        $apk = $matches[0].FullName
        break
    }
}

if (-not $apk) {
    Write-Fail "APK no encontrado después del build"
    exit 1
}

# 5. SHA-256
$sha = (Get-FileHash -Path $apk -Algorithm SHA256).Hash.ToLower()
$size = (Get-Item $apk).Length / 1MB

Write-Ok "APK: $apk"
Write-Ok "Tamaño: $([math]::Round($size, 2)) MB"
Write-Ok "SHA-256: $sha"

# 6. Output object for pipeline consumption
$result = [PSCustomObject]@{
    ApkPath    = $apk
    Sha256     = $sha
    SizeMB     = [math]::Round($size, 2)
    ExitCode   = $exitCode
}
Write-Output $result
