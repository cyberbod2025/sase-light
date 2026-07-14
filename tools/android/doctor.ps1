#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Diagnoses the Android testing environment for SASE Light.
.DESCRIPTION
    Verifies Java, Gradle, Android SDK, ADB, emulator, AVD, APK,
    device connections, and evidence storage. Reports a final summary.
#>

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

$results = @()

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "  SASE Light — Android Doctor" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# ---- Java ----
Write-Step "Java"
$javaFound = $false
$javaDetail = ""
$javaStatus = "NO DISPONIBLE"

# 1. Check PATH via Get-Command
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $jv = & java -version 2>&1
    $jvText = $jv | Out-String
    if ($jvText -match '"(\d+\.\d+\.\d+)"') {
        $javaStatus = "DISPONIBLE EN PATH"
        $javaDetail = "OpenJDK $($matches[1])"
        $javaFound = $true
    }
}

# 2. Check JAVA_HOME
if (-not $javaFound -and $env:JAVA_HOME) {
    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $javaExe) {
        $jv = & $javaExe -version 2>&1
        $jvText = $jv | Out-String
        if ($jvText -match '"(\d+\.\d+\.\d+)"') {
            $javaStatus = "DISPONIBLE MEDIANTE JAVA_HOME"
            $javaDetail = "OpenJDK $($matches[1]) en $env:JAVA_HOME"
            $javaFound = $true
        }
    }
}

# 3. Check Gradle's JDK (via javaHome in gradle.properties or toolchain)
if (-not $javaFound) {
    $gradleProps = Join-Path $Script:PROJECT_ROOT "gradle.properties"
    $localProps = Join-Path $Script:PROJECT_ROOT "local.properties"
    foreach ($propFile in @($gradleProps, $localProps)) {
        if (Test-Path $propFile) {
            $content = Get-Content $propFile -Raw
            if ($content -match 'org\.gradle\.java\.home=(.+)') {
                $gradleJavaHome = $matches[1].Trim()
                $gradleJavaExe = Join-Path $gradleJavaHome "bin\java.exe"
                if (Test-Path $gradleJavaExe) {
                    $jv = & $gradleJavaExe -version 2>&1
                    $jvText = $jv | Out-String
                    if ($jvText -match '"(\d+\.\d+\.\d+)"') {
                        $javaStatus = "DISPONIBLE MEDIANTE GRADLE"
                        $javaDetail = "OpenJDK $($matches[1]) configurado en $([System.IO.Path]::GetFileName($propFile))"
                        $javaFound = $true
                        break
                    }
                }
            }
        }
    }
}

if ($javaFound) {
    Write-Ok $javaDetail
    $results += [PSCustomObject]@{ Component = "Java"; Status = $javaStatus; Detail = $javaDetail }
} else {
    Write-Fail "Java no disponible en PATH, JAVA_HOME ni configuración Gradle"
    $results += [PSCustomObject]@{ Component = "Java"; Status = "NO DISPONIBLE"; Detail = "No en PATH, JAVA_HOME ni Gradle" }
}

# ---- Gradle Wrapper ----
Write-Step "Gradle Wrapper"
$gradlew = Join-Path $Script:PROJECT_ROOT "gradlew"
if (Test-Path $gradlew) {
    Write-Ok "gradlew presente"
    $results += [PSCustomObject]@{ Component = "Gradle Wrapper"; Status = "DISPONIBLE"; Detail = "gradlew presente" }
} else {
    Write-Fail "gradlew no encontrado"
    $results += [PSCustomObject]@{ Component = "Gradle Wrapper"; Status = "NO DISPONIBLE"; Detail = "gradlew no encontrado" }
}

# ---- Android SDK ----
Write-Step "Android SDK"
if ($Script:ANDROID_SDK -and (Test-Path $Script:ANDROID_SDK)) {
    $sourceInfo = if ($Script:ANDROID_SDK_SOURCE) { " (fuente: $Script:ANDROID_SDK_SOURCE)" } else { "" }
    Write-Ok "SDK en: $($Script:ANDROID_SDK)$sourceInfo"
    $results += [PSCustomObject]@{ Component = "Android SDK"; Status = "DISPONIBLE"; Detail = "$($Script:ANDROID_SDK) [$Script:ANDROID_SDK_SOURCE]" }
} else {
    Write-Fail "Android SDK no encontrado"
    $results += [PSCustomObject]@{ Component = "Android SDK"; Status = "NO DISPONIBLE"; Detail = "No encontrado" }
}

# ---- ANDROID_SDK source ----
Write-Step "SDK Resolution"
$src = if ($Script:ANDROID_SDK_SOURCE) { $Script:ANDROID_SDK_SOURCE } else { "desconocida" }
Write-Ok "Fuente: $src ($Script:ANDROID_SDK)"
$results += [PSCustomObject]@{ Component = "SDK Resolution"; Status = "DISPONIBLE"; Detail = "$src -> $Script:ANDROID_SDK" }

# Chequeo informativo de variables de entorno
$envHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "User"),
           [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Machine"),
           [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Process")
$envRoot = [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "User"),
           [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "Machine"),
           [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "Process")
$effectiveEnvHome = $envHome | Where-Object { $_ } | Select-Object -First 1
$effectiveEnvRoot = $envRoot | Where-Object { $_ } | Select-Object -First 1
if ($effectiveEnvHome) { Write-Info "  ANDROID_HOME disponible en entorno" }
if ($effectiveEnvRoot) { Write-Info "  ANDROID_SDK_ROOT disponible en entorno" }

# ---- ADB ----
Write-Step "ADB"
if (Test-Path $Script:ADB) {
    $ver = & $Script:ADB version 2>&1 | Select-Object -First 1
    Write-Ok "ADB: $ver"
    $results += [PSCustomObject]@{ Component = "ADB"; Status = "DISPONIBLE"; Detail = $Script:ADB }
} else {
    Write-Fail "ADB no encontrado en SDK"
    $results += [PSCustomObject]@{ Component = "ADB"; Status = "NO DISPONIBLE"; Detail = "No en SDK" }
}

# ---- Emulator ----
Write-Step "Emulator"
if (Test-Path $Script:EMULATOR) {
    $ever = & $Script:EMULATOR -version 2>&1 | Select-Object -First 1
    Write-Ok "Emulator: $ever"
    $results += [PSCustomObject]@{ Component = "Emulator (binario)"; Status = "DISPONIBLE"; Detail = $Script:EMULATOR }
} else {
    Write-Fail "Emulator no encontrado"
    $results += [PSCustomObject]@{ Component = "Emulator (binario)"; Status = "NO DISPONIBLE"; Detail = "No en SDK" }
}

# ---- AVD ----
Write-Step "AVD"
$avds = Get-EmulatorAvds
if ($avds -and @($avds).Count -gt 0) {
    Write-Ok "AVD(s) disponible(s): $($avds -join ', ')"
    $results += [PSCustomObject]@{ Component = "AVD"; Status = "DISPONIBLE"; Detail = $avds[0] }
} else {
    Write-Fail "Ningún AVD creado"
    $results += [PSCustomObject]@{ Component = "AVD"; Status = "NO DISPONIBLE"; Detail = "No hay AVD" }
}

# ---- Dispositivo conectado ----
Write-Step "Dispositivo / Emulador"
$devices = Get-AdbDevices
if ($devices -and @($devices).Count -gt 0) {
    Write-Ok "Dispositivo(s): $($devices -join ', ')"
    $results += [PSCustomObject]@{ Component = "Dispositivo"; Status = "DISPONIBLE"; Detail = $devices[0] }
} else {
    Write-Warn "Ningún dispositivo conectado. Usa start-emulator.ps1"
    $results += [PSCustomObject]@{ Component = "Dispositivo"; Status = "DESCONECTADO"; Detail = "Ninguno (usar start-emulator.ps1)" }
}

# ---- APK ----
Write-Step "APK Debug"
if (Test-Path $Script:APK_PATH) {
    $size = [math]::Round((Get-Item $Script:APK_PATH).Length / 1MB, 1)
    $apkDetail = $Script:APK_PATH + " (" + $size + " MB)"
    Write-Ok ("APK: " + $apkDetail)
    $results += [PSCustomObject]@{ Component = "APK Debug"; Status = "DISPONIBLE"; Detail = $apkDetail }
} else {
    Write-Warn "APK no encontrado. Ejecuta: ./gradlew :composeApp:assembleDebug"
    $results += [PSCustomObject]@{ Component = "APK Debug"; Status = "NO GENERADO"; Detail = "Ejecutar build-debug.ps1" }
}

# ---- Evidencia ----
Write-Step "Directorio de evidencia"
$evDir = New-Item -ItemType Directory -Path $Script:EVIDENCE_BASE -Force
if (Test-Path $evDir) {
    Write-Ok "Evidencia: $($evDir.FullName)"
    $results += [PSCustomObject]@{ Component = "Evidencia"; Status = "DISPONIBLE"; Detail = $evDir.FullName }
}

# ---- Resumen ----
Write-Host ""
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "  RESUMEN" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
$allOk = $true
foreach ($r in $results) {
    $icon = switch -wildcard ($r.Status) {
        "DISPONIBLE*" { "OK" }
        "NO DISPONIBLE" { "MISSING"; $allOk = $false }
        "NO CONFIGURADO" { "WARN" }
        "DESCONECTADO" { "WARN" }
        "NO GENERADO" { "WARN"; $allOk = $false }
        default { "?" }
    }
    $color = if ($r.Status -like "DISPONIBLE*") { "Green" } elseif ($r.Status -eq "NO DISPONIBLE") { "Red" } else { "Yellow" }
    Write-Host "  [$icon] $($r.Component): $($r.Detail)" -ForegroundColor $color
}

Write-Host ""
if ($allOk) {
    Write-Host "ENTORNO LISTO" -ForegroundColor Green
}
elseif ($results | Where-Object { $_.Status -eq "NO DISPONIBLE" }) {
    Write-Host "ENTORNO NO LISTO — faltan componentes esenciales" -ForegroundColor Red
} else {
    Write-Host "ENTORNO PARCIAL — algunos componentes requieren atención" -ForegroundColor Yellow
}
