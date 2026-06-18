param(
    [string]$Change = "refactor-sdd-harness-alignment",
    [switch]$ConnectedAndroidTest
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Invoke-HarnessStep {
    param(
        [string]$Name,
        [string]$Command,
        [string[]]$Arguments
    )

    Write-Host ""
    Write-Host "==> $Name"
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

Invoke-HarnessStep "Git status" "git" @("status", "--short")
Invoke-HarnessStep "Git whitespace check" "git" @("diff", "--check")
Invoke-HarnessStep "Git diff stat" "git" @("diff", "--stat")
Invoke-HarnessStep "OpenSpec change status" "openspec.cmd" @("status", "--change", $Change, "--json")
Invoke-HarnessStep "Unit tests" ".\gradlew.bat" @("testDebugUnitTest")
Invoke-HarnessStep "Debug build" ".\gradlew.bat" @("assembleDebug")
Invoke-HarnessStep "Android instrumentation test APK build" ".\gradlew.bat" @("assembleDebugAndroidTest")

if ($ConnectedAndroidTest) {
    Invoke-HarnessStep "Connected Android instrumentation tests" ".\gradlew.bat" @("connectedDebugAndroidTest")
} else {
    Write-Host ""
    Write-Host "==> Connected Android instrumentation tests"
    Write-Host "Skipped by default. Run scripts\harness.ps1 -ConnectedAndroidTest when a device or emulator is connected."
}
