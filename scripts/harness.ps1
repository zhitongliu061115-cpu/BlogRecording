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
Invoke-HarnessStep "OpenSpec validation" "openspec.cmd" @("validate", $Change)
Invoke-HarnessStep "Gradle clean" ".\gradlew.bat" @("clean")
Invoke-HarnessStep "Unit tests" ".\gradlew.bat" @("testDebugUnitTest")
Invoke-HarnessStep "Debug build" ".\gradlew.bat" @(":app:assembleDebug")
Invoke-HarnessStep "Android lint" ".\gradlew.bat" @(":app:lintDebug")
Invoke-HarnessStep "Android instrumentation test APK build" ".\gradlew.bat" @("assembleDebugAndroidTest")

Write-Host ""
Write-Host "==> Artifacts"
Write-Host "- app/build/outputs/apk/debug/app-debug.apk"
Write-Host "- app/build/reports/tests/testDebugUnitTest/"
Write-Host "- app/build/reports/lint-results-debug.html"
Write-Host ""
Write-Host "==> Manual QA checklist"
Write-Host "- 创建播客 A"
Write-Host "- A 开始录音"
Write-Host "- A 暂停"
Write-Host "- A 续录"
Write-Host "- 创建播客 B"
Write-Host "- A 暂停后 B 录制"
Write-Host "- B 暂停后 A 续录"
Write-Host "- A 完成并总结"
Write-Host "- 杀进程恢复"
Write-Host "- 系统内录音权限取消"
Write-Host "- DeepSeek Key 缺失"

if ($ConnectedAndroidTest) {
    Invoke-HarnessStep "Connected Android instrumentation tests" ".\gradlew.bat" @("connectedDebugAndroidTest")
} else {
    Write-Host ""
    Write-Host "==> Connected Android instrumentation tests"
    Write-Host "Skipped by default. Run scripts\harness.ps1 -ConnectedAndroidTest when a device or emulator is connected."
}
