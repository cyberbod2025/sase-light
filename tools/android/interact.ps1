#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Finds and taps UI elements via uiautomator dump + ADB input tap.
.DESCRIPTION
    Parses the accessibility hierarchy XML to locate elements by
    content-desc, text, or class, then taps their center coordinates.
    Use -List to dump all visible nodes for debugging.
#>
param(
    [string]$ContentDesc = "",
    [string]$Text = "",
    [string]$Class = "",
    [int]$Index = 0,
    [switch]$List,
    [string]$Serial = "emulator-5554",
    [string]$OutDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\lib\config.ps1"

if ($Serial) { $targetDevice = $Serial } else {
    $devices = Get-AdbDevices
    $targetDevice = $devices | Where-Object { $_ -match "^emulator-\d+" } | Select-Object -First 1
    if (-not $targetDevice) { Write-Fail "No emulator found"; exit 1 }
}

# Dump hierarchy
$dumpRemote = "/sdcard/interact_dump_$([System.IO.Path]::GetRandomFileName()).xml"
$dumpLocal = Join-Path $env:TEMP "interact_dump.xml"
& $Script:ADB -s $targetDevice shell uiautomator dump $dumpRemote 2>$null
& $Script:ADB -s $targetDevice pull $dumpRemote $dumpLocal 2>$null
& $Script:ADB -s $targetDevice shell rm $dumpRemote 2>$null

if (-not (Test-Path $dumpLocal)) { Write-Fail "UI dump failed"; exit 1 }

# Parse XML
function Parse-Nodes($xml, $nodes) {
    $result = @()
    foreach ($node in $nodes) {
        $boundStr = $node.GetAttribute("bounds")  # format: [x1,y1][x2,y2]
        $contentDesc = $node.GetAttribute("content-desc")
        $text = $node.GetAttribute("text")
        $class = $node.GetAttribute("class")
        $clickable = $node.GetAttribute("clickable")
        if ($boundStr -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
            $x1 = [int]$matches[1]; $y1 = [int]$matches[2]
            $x2 = [int]$matches[3]; $y2 = [int]$matches[4]
            $cx = [int](($x1 + $x2) / 2); $cy = [int](($y1 + $y2) / 2)
        } else { $cx = 0; $cy = 0 }
        $result += [PSCustomObject]@{
            ContentDesc = $contentDesc
            Text = $text
            Class = $class
            Clickable = ($clickable -eq "true")
            Bounds = $boundStr
            CenterX = $cx
            CenterY = $cy
        }
        $children = $node.SelectNodes("node")
        if ($children.Count -gt 0) { $result += Parse-Nodes $xml $children }
    }
    return $result
}

[xml]$xmlDoc = Get-Content $dumpLocal -Raw
$allNodes = Parse-Nodes $xmlDoc $xmlDoc.SelectNodes("//node")

if ($List) {
    Write-Host "UI Hierarchy Nodes:" -ForegroundColor Cyan
    for ($i = 0; $i -lt $allNodes.Count; $i++) {
        $n = $allNodes[$i]
        $info = "  [$i] class=$($n.Class)"
        if ($n.Text) { $info += " text='$($n.Text)'" }
        if ($n.ContentDesc) { $info += " cd='$($n.ContentDesc)'" }
        $info += " clickable=$($n.Clickable) bounds=$($n.Bounds)"
        Write-Host $info
    }
    exit 0
}

# Find matching nodes
$matches = $allNodes
if ($ContentDesc) { $matches = $matches | Where-Object { $_.ContentDesc -eq $ContentDesc -or $_.ContentDesc -like "*$ContentDesc*" } }
if ($Text) { $matches = $matches | Where-Object { $_.Text -eq $Text -or $_.Text -like "*$Text*" } }
if ($Class) { $matches = $matches | Where-Object { $_.Class -like "*$Class*" } }

if ($matches.Count -eq 0) {
    Write-Warn "No matching element found"
    exit 1
}

$target = $matches[$Index]
Write-Ok ("Found: cd='$($target.ContentDesc)' text='$($target.Text)' bounds=$($target.Bounds)")

# Tap center
Write-Info ("Tapping at: ($($target.CenterX), $($target.CenterY))")
$result = & $Script:ADB -s $targetDevice shell input tap $($target.CenterX) $($target.CenterY) 2>&1
Start-Sleep -Seconds 2

# Save hierarchy after tap if OutDir specified
if ($OutDir -and (Test-Path $OutDir)) {
    $dumpRemote2 = "/sdcard/interact_after_$([System.IO.Path]::GetRandomFileName()).xml"
    $dumpLocal2 = Join-Path $OutDir "ui-hierarchy-after.xml"
    & $Script:ADB -s $targetDevice shell uiautomator dump $dumpRemote2 2>$null
    & $Script:ADB -s $targetDevice pull $dumpRemote2 $dumpLocal2 2>$null
    & $Script:ADB -s $targetDevice shell rm $dumpRemote2 2>$null
    Write-Ok ("After-hierarchy: $dumpLocal2")
}

Write-Output $target
