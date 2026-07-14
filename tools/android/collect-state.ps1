#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Captures full device/app state and stores in evidence directory.
.DESCRIPTION
    Generates a timestamped evidence folder under evidence/android/
    with screenshot, UI hierarchy, logcat, device info, git state,
    APK hash, and metadata.json.
#>
param(
    [string]$Serial = ""
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

Write-Step "Collect State"

# 1. Determine device
if ($Serial) { $targetDevice = $Serial } else {
    $devices = Get-AdbDevices
    $targetDevice = $devices | Where-Object { $_ -match "^emulator-\d+|\w+" } | Select-Object -First 1
}
if (-not $targetDevice) {
    Write-Fail "No hay dispositivo conectado"
    exit 1
}
Write-Ok "Dispositivo: $targetDevice"

# 2. Create evidence directory
$evidenceDir = New-EvidenceDir
Write-Ok "Evidencia: $evidenceDir"

# 3. Screenshot
Write-Info "Capturando screenshot..."
$screenshotPath = Join-Path $evidenceDir "screenshot.png"
& $Script:ADB -s $targetDevice exec-out screencap -p > $screenshotPath 2>&1
if ((Get-Item $screenshotPath -ErrorAction SilentlyContinue).Length -gt 0) {
    Write-Ok "  screenshot: $screenshotPath"
} else {
    Write-Warn "  screenshot falló"
}

# 4. UI hierarchy
Write-Info "Extrayendo jerarquía UI..."
$hierarchyPath = Join-Path $evidenceDir "ui-hierarchy.xml"
$dumpResult = & $Script:ADB -s $targetDevice shell uiautomator dump /dev/tmp/ui.xml 2>&1
if ($LASTEXITCODE -eq 0) {
    & $Script:ADB -s $targetDevice shell cat /dev/tmp/ui.xml 2>&1 | Out-File -FilePath $hierarchyPath -Encoding utf8
    & $Script:ADB -s $targetDevice shell rm /dev/tmp/ui.xml 2>$null
    if ((Get-Item $hierarchyPath -ErrorAction SilentlyContinue).Length -gt 0) {
        Write-Ok "  hierarchy: $hierarchyPath"
    } else {
        Write-Warn "  hierarchy dump vacío"
    }
} else {
    Write-Warn "  uiautomator dump falló (puede requerir API >= 18)"
    "uiautomator dump falló: $dumpResult" | Out-File -FilePath $hierarchyPath -Encoding utf8
}

# 5. Logcat (last 500 lines)
Write-Info "Capturando logcat..."
$logcatPath = Join-Path $evidenceDir "logcat.txt"
& $Script:ADB -s $targetDevice logcat -d -v brief 2>&1 | Select-Object -Last 500 | Out-File -FilePath $logcatPath -Encoding utf8
Write-Ok "  logcat: $logcatPath"

# 6. Current activity
$activityPath = Join-Path $evidenceDir "current-activity.txt"
$dumpsys = & $Script:ADB -s $targetDevice shell dumpsys activity activities 2>&1
$dumpsys | Out-File -FilePath $activityPath -Encoding utf8
Write-Ok "  activity: $activityPath"

# 7. Window state
$windowPath = Join-Path $evidenceDir "window-state.txt"
& $Script:ADB -s $targetDevice shell dumpsys window windows 2>&1 | Out-File -FilePath $windowPath -Encoding utf8
Write-Ok "  window: $windowPath"

# 8. Device info
$deviceInfoPath = Join-Path $evidenceDir "device-info.txt"
$deviceInfo = @"
Serial: $((& $Script:ADB -s $targetDevice shell getprop ro.serialno 2>&1).Trim())
Model: $((& $Script:ADB -s $targetDevice shell getprop ro.product.model 2>&1).Trim())
Manufacturer: $((& $Script:ADB -s $targetDevice shell getprop ro.product.manufacturer 2>&1).Trim())
Android: $((& $Script:ADB -s $targetDevice shell getprop ro.build.version.release 2>&1).Trim())
SDK: $((& $Script:ADB -s $targetDevice shell getprop ro.build.version.sdk 2>&1).Trim())
ABI: $((& $Script:ADB -s $targetDevice shell getprop ro.product.cpu.abi 2>&1).Trim())
"@
$deviceInfo | Out-File -FilePath $deviceInfoPath -Encoding utf8
Write-Ok "  device-info: $deviceInfoPath"

# 9. Package info
$pkgPath = Join-Path $evidenceDir "package-info.txt"
& $Script:ADB -s $targetDevice shell dumpsys package $Script:APPLICATION_ID 2>&1 | Out-File -FilePath $pkgPath -Encoding utf8
Write-Ok "  package: $pkgPath"

# 10. Git state
$gitStatusPath = Join-Path $evidenceDir "git-status.txt"
& git -C $Script:PROJECT_ROOT status --short 2>$null | Out-File -FilePath $gitStatusPath -Encoding utf8
$gitHeadPath = Join-Path $evidenceDir "git-head.txt"
& git -C $Script:PROJECT_ROOT rev-parse HEAD 2>$null | Out-File -FilePath $gitHeadPath -Encoding utf8
Write-Ok "  git state guardado"

# 11. APK SHA-256
$apkShaPath = Join-Path $evidenceDir "apk-sha256.txt"
if (Test-Path $Script:APK_PATH) {
    $apkSha = (Get-FileHash -Path $Script:APK_PATH -Algorithm SHA256).Hash.ToLower()
    $apkSha | Out-File -FilePath $apkShaPath -Encoding utf8
    Write-Ok "  apk-sha256: $apkSha"
} else {
    "APK no encontrado en $($Script:APK_PATH)" | Out-File -FilePath $apkShaPath -Encoding utf8
    Write-Warn "  APK no disponible para hash"
}

# 12. Metadata JSON
$metadataPath = Join-Path $evidenceDir "metadata.json"
$gitHead = (Get-Content $gitHeadPath -ErrorAction SilentlyContinue | Select-Object -First 1)
$branch = (& git -C $Script:PROJECT_ROOT branch --show-current 2>$null)
$deviceModel = (& $Script:ADB -s $targetDevice shell getprop ro.product.model 2>&1).Trim()
$androidVersion = (& $Script:ADB -s $targetDevice shell getprop ro.build.version.release 2>&1).Trim()
$currentActivity = ""
if ($dumpsys -match "mResumedActivity[^}]+ComponentInfo[^}]+/([^}\s]+)") {
    $currentActivity = $matches[1]
}

$metadata = @{
    timestamp        = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
    gitHead          = $gitHead
    branch           = $branch
    deviceSerial     = $targetDevice
    deviceModel      = $deviceModel
    androidVersion   = $androidVersion
    applicationId    = $Script:APPLICATION_ID
    currentActivity  = $currentActivity
    apkPath          = if (Test-Path $Script:APK_PATH) { $Script:APK_PATH } else { $null }
    apkSha256        = if ($apkSha) { $apkSha } else { $null }
}
$metadata | ConvertTo-Json -Compress | Out-File -FilePath $metadataPath -Encoding utf8
Write-Ok "  metadata: $metadataPath"

Write-Ok "Estado completo en: $evidenceDir"
Write-Output $evidenceDir
