$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    throw "ADB was not found at $adb."
}

$health = Invoke-RestMethod -Uri "http://127.0.0.1:8010/health" -TimeoutSec 5
if (-not ($health.ok -eq $true -and $health.service -eq "duddy-portugues-api")) {
    throw "The Duddy backend is not healthy on http://127.0.0.1:8010/health."
}

$devices = & $adb devices
if (-not ($devices -match "\tdevice")) {
    throw "No USB-debugging Android device is connected."
}

& $adb reverse tcp:8010 tcp:8010 | Out-Null
$reverseList = & $adb reverse --list

$deviceCheck = & $adb shell "toybox nc -z -w 5 127.0.0.1 8010; echo nc_status:`$?"
if (-not ($deviceCheck -match "nc_status:0")) {
    throw "The phone still cannot reach 127.0.0.1:8010 through adb reverse. Output: $deviceCheck"
}

Write-Host "Backend healthy: http://127.0.0.1:8010"
Write-Host "ADB reverse active:"
Write-Host $reverseList
Write-Host "Phone tunnel check passed. Retry pronunciation scoring in the app."
