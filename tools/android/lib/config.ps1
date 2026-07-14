$Script:PROJECT_ROOT = Resolve-Path "$PSScriptRoot\..\..\.."

# --- Android SDK resolution (no hardcoded user paths) ---
function Resolve-AndroidSdk {
    $sources = @()

    # 1. sdk.dir in local.properties
    $localProps = Join-Path $Script:PROJECT_ROOT "local.properties"
    if (Test-Path $localProps) {
        $content = Get-Content $localProps -Raw
        if ($content -match 'sdk\.dir=(.+?)[\r\n]') {
            $path = $matches[1].Trim().Replace('/', '\').Replace('\\', '\')
            $sources += @{ Path = $path; Source = "local.properties" }
        }
    }

    # 2. ANDROID_SDK_ROOT
    $envRoot = [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "User"),
               [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "Machine"),
               [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "Process")
    foreach ($e in $envRoot) {
        if ($e) { $sources += @{ Path = $e; Source = "ANDROID_SDK_ROOT" }; break }
    }

    # 3. ANDROID_HOME
    if (-not ($sources | Where-Object { $_.Source -in "local.properties","ANDROID_SDK_ROOT" })) {
        $envHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "User"),
                   [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Machine"),
                   [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Process")
        foreach ($e in $envHome) {
            if ($e) { $sources += @{ Path = $e; Source = "ANDROID_HOME" }; break }
        }
    }

    # 4. Fallback: LOCALAPPDATA\Android\Sdk
    if (-not ($sources | Where-Object { $_.Source -in "local.properties","ANDROID_SDK_ROOT","ANDROID_HOME" })) {
        $localSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
        $sources += @{ Path = $localSdk; Source = "LOCALAPPDATA" }
    }

    foreach ($entry in $sources) {
        $normalized = $entry.Path -replace '\\', '\'
        if (Test-Path $normalized) {
            $Script:ANDROID_SDK_SOURCE = $entry.Source
            return $normalized
        }
    }

    throw "Android SDK no encontrado. Verifica local.properties, ANDROID_SDK_ROOT, ANDROID_HOME o instalación en $env:LOCALAPPDATA\Android\Sdk."
}

$Script:ANDROID_SDK = Resolve-AndroidSdk
$Script:ADB = Join-Path $Script:ANDROID_SDK "platform-tools\adb.exe"
$Script:EMULATOR = Join-Path $Script:ANDROID_SDK "emulator\emulator.exe"
$Script:APK_PATH = Join-Path $Script:PROJECT_ROOT "composeApp\build\outputs\apk\debug\composeApp-debug.apk"
$Script:APPLICATION_ID = "com.aistudio.labvirtual.kvmpx"
$Script:MAIN_ACTIVITY = "com.example.MainActivity"
$Script:EVIDENCE_BASE = Join-Path $Script:PROJECT_ROOT "evidence\android"
$Script:DEFAULT_AVD = "Pixel_7"

function Write-Step($msg) { Write-Host ">> $msg" -ForegroundColor Cyan }
function Write-Ok($msg) { Write-Host "   $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "   $msg" -ForegroundColor Yellow }
function Write-Fail($msg) { Write-Host "   $msg" -ForegroundColor Red }
function Write-Info($msg) { Write-Host "   $msg" -ForegroundColor Gray }

function Assert-Adb {
    if (-not (Test-Path $Script:ADB)) {
        Write-Fail "ADB no encontrado en: $($Script:ADB)"
        return $false
    }
    return $true
}

function Assert-Emulator {
    if (-not (Test-Path $Script:EMULATOR)) {
        Write-Fail "Emulator no encontrado en: $($Script:EMULATOR)"
        return $false
    }
    return $true
}

function Get-AdbDevices {
    if (-not (Assert-Adb)) { return @() }
    $output = & $Script:ADB devices 2>&1
    $lines = @($output -split "`n" | Where-Object { $_ -match "^[a-zA-Z0-9]" -and $_ -notmatch "^List" })
    return @($lines | ForEach-Object { ($_ -split "\s+")[0] })
}

function Get-EmulatorAvds {
    if (-not (Assert-Emulator)) { return @() }
    $output = & $Script:EMULATOR -list-avds 2>&1
    return @($output -split "`n" | Where-Object { $_.Trim() -ne "" })
}

function New-EvidenceDir {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $dir = Join-Path $Script:EVIDENCE_BASE $timestamp
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    return $dir
}
