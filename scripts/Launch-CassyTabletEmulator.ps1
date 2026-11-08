<#
.SYNOPSIS
    Meluncurkan Android Emulator Tablet POS Cassy (Medium_Tablet) dengan akselerasi hardware.

.DESCRIPTION
    Skrip ini memulai AVD Medium_Tablet dalam orientasi Landscape (2560x1600 @ 320 dpi),
    menggunakan akselerasi GPU host dan menunggu hingga Android OS selesai booting.
    Sangat ideal untuk pengujian visual UI 10-55-35 rhythm Cassy POS.

.EXAMPLE
    pwsh -ExecutionPolicy Bypass -File scripts/Launch-CassyTabletEmulator.ps1
#>

[CmdletBinding()]
param(
    [string]$AvdName = "Medium_Tablet",
    [int]$MemoryMb = 4096
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Deteksi lokasi Android SDK
$androidSdk = $env:ANDROID_HOME
if (-not $androidSdk) {
    $androidSdk = $env:ANDROID_SDK_ROOT
}
if (-not $androidSdk) {
    $androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$emulatorExe = Join-Path $androidSdk "emulator\emulator.exe"
$adbExe = Join-Path $androidSdk "platform-tools\adb.exe"

if (-not (Test-Path -LiteralPath $emulatorExe)) {
    throw "Binary Android Emulator tidak ditemukan di $emulatorExe. Pastikan Android SDK sudah terpasang."
}
if (-not (Test-Path -LiteralPath $adbExe)) {
    throw "Binary ADB tidak ditemukan di $adbExe. Pastikan Platform Tools sudah terpasang."
}

Write-Host "=== Memeriksa Ketersediaan AVD: $AvdName ===" -ForegroundColor Cyan
$availableAvds = & $emulatorExe -list-avds
if ($availableAvds -notcontains $AvdName) {
    Write-Warning "AVD '$AvdName' tidak terdaftar. AVD yang tersedia: $($availableAvds -join ', ')"
    throw "Silakan buat AVD '$AvdName' melalui Android Studio Device Manager atau pilih AVD yang ada."
}

Write-Host "[OK] AVD '$AvdName' ditemukan." -ForegroundColor Green
Write-Host "Meluncurkan emulator dengan akselerasi GPU host (RAM: ${MemoryMb}MB)..." -ForegroundColor Cyan

# Jalankan emulator di background
$emulatorArgs = @(
    "-avd", $AvdName,
    "-gpu", "host",
    "-accel", "on",
    "-memory", $MemoryMb.ToString(),
    "-no-snapshot-load"
)

Start-Process -FilePath $emulatorExe -ArgumentList $emulatorArgs

Write-Host "Menunggu booting Android OS selesai..." -ForegroundColor DarkCyan
$bootTimeoutSeconds = 120
$elapsed = 0
$bootCompleted = $false

while ($elapsed -lt $bootTimeoutSeconds) {
    Start-Sleep -Seconds 3
    $elapsed += 3
    
    $bootStatus = (& $adbExe shell getprop sys.boot_completed 2>$null)
    if ($bootStatus -match '1') {
        $bootCompleted = $true
        break
    }
    Write-Host -NoNewline "."
}

Write-Host ""
if ($bootCompleted) {
    Write-Host "[BERHASIL] Tablet POS Emulator ($AvdName) siap digunakan!" -ForegroundColor Green
    Write-Host "Perangkat terdeteksi via ADB:" -ForegroundColor DarkCyan
    & $adbExe devices
} else {
    Write-Warning "Emulator masih dalam proses booting di latar belakang (timeout pemeriksaan 120s tercapai)."
}

