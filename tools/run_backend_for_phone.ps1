$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$python = Join-Path $backend ".venv\Scripts\python.exe"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $python)) {
    Write-Host "Creating backend virtual environment..."
    & "C:\Users\drdud\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe" -m venv (Join-Path $backend ".venv")
    & $python -m pip install -r (Join-Path $backend "requirements.txt")
}

if (-not $env:DEV_AUTH_BYPASS) {
    $env:DEV_AUTH_BYPASS = "true"
}
if (-not $env:COACH_PROVIDER) {
    $env:COACH_PROVIDER = "local"
}
if (-not $env:SPEECH_TO_TEXT_PROVIDER) {
    $env:SPEECH_TO_TEXT_PROVIDER = "openai"
}
if (-not $env:PRONUNCIATION_PROVIDER) {
    $env:PRONUNCIATION_PROVIDER = "azure"
}

if (Test-Path $adb) {
    try {
        $devices = & $adb devices
        if ($devices -match "\tdevice") {
            & $adb reverse tcp:8010 tcp:8010 | Out-Null
            Write-Host "ADB reverse tunnel active: device 127.0.0.1:8010 -> PC 127.0.0.1:8010"
        } else {
            Write-Host "No connected ADB device found. Connect the phone with USB debugging to enable adb reverse."
        }
    } catch {
        Write-Host "Could not configure adb reverse automatically: $($_.Exception.Message)"
    }
} else {
    Write-Host "ADB not found at $adb. Skipping automatic adb reverse setup."
}

Write-Host "Starting Duddy backend for physical phone testing..."
Write-Host "PC backend URL: http://127.0.0.1:8010"
Write-Host "Physical phone with USB debugging: DUDDY_BACKEND_URL=http://127.0.0.1:8010"
Write-Host "Android emulator: DUDDY_BACKEND_URL=http://10.0.2.2:8010"
Write-Host "Auth mode: DEV_AUTH_BYPASS=$env:DEV_AUTH_BYPASS"
Write-Host "Coach provider: $env:COACH_PROVIDER"
Write-Host "Pronunciation provider: $env:PRONUNCIATION_PROVIDER"
Write-Host ""
Write-Host "For real pronunciation scoring, set AZURE_SPEECH_KEY/AZURE_SPEECH_REGION or OPENAI_API_KEY in this shell before running."

$runningBackend = $false
try {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:8010/health" -TimeoutSec 3
    $runningBackend = $health.ok -eq $true -and $health.service -eq "duddy-portugues-api"
} catch {
    $runningBackend = $false
}

if ($runningBackend) {
    Write-Host "Duddy backend is already running on http://127.0.0.1:8010."
    Write-Host "Tunnel is ready. You can retry pronunciation scoring in the app."
    return
}

Set-Location $backend
& $python -m uvicorn app.main:app --host 0.0.0.0 --port 8010 --reload
