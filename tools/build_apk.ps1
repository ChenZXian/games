# tools/build_apk.ps1
# Build APK deliverable for a single game project under games/<id>
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/<new_game_id>
# Optional:
#   -Variant debug|release (default: debug)

[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$Project,

  [ValidateSet("debug","release")]
  [string]$Variant = "debug"
)

$ErrorActionPreference = "Stop"

function Write-Ok($m){ Write-Host "[OK]  $m" }
function Write-Warn($m){ Write-Host "[WARN] $m" }
function Write-Fail($m){ Write-Host "[FAIL] $m" }

function Get-RepoRoot {
  $dir = (Get-Location).Path
  for ($i=0; $i -lt 12; $i++) {
    if ((Test-Path (Join-Path $dir "docs")) -and
        (Test-Path (Join-Path $dir "registry")) -and
        (Test-Path (Join-Path $dir "games")) -and
        (Test-Path (Join-Path $dir "tools"))) { return $dir }
    $parent = Split-Path $dir -Parent
    if ($parent -eq $dir) { break }
    $dir = $parent
  }
  throw "Repo root not found."
}

$root = Get-RepoRoot
Set-Location $root

$projDir = Join-Path $root $Project
if (!(Test-Path $projDir)) { Write-Fail "Project path not found: $projDir"; exit 2 }

$projResolved = (Resolve-Path $projDir).Path

# Must be direct child of games/
$parent = Split-Path $projResolved -Parent
if (-not ((Split-Path $parent -Leaf) -ieq "games")) {
  Write-Fail "Project must be under repo_root/games/<id>. Got: $projResolved"
  exit 2
}

$gameId = Split-Path $projResolved -Leaf

Write-Ok "Repo root: $root"
Write-Ok "Project: $projResolved"
Write-Ok "Variant: $Variant"

# 1) Baseline enforcement: doctor (if exists)
$doctor = Join-Path $root "tools\env\doctor.ps1"
if (Test-Path $doctor) {
  Write-Host ""
  Write-Host "=== Run Doctor ==="
  & powershell -ExecutionPolicy Bypass -File $doctor
  if ($LASTEXITCODE -ne 0) {
    Write-Fail "Doctor failed. Stop."
    exit 2
  }
  Write-Ok "Doctor passed"
} else {
  Write-Warn "tools/env/doctor.ps1 not found; skip doctor"
}

# 2) Validator (if exists)
$validator = Join-Path $root "tools\validate.ps1"
if (Test-Path $validator) {
  Write-Host ""
  Write-Host "=== Run Validator ==="
  & powershell -ExecutionPolicy Bypass -File $validator -Project $Project
  if ($LASTEXITCODE -ne 0) {
    Write-Fail "Validator failed. Stop."
    exit 2
  }
  Write-Ok "Validator passed"
} else {
  Write-Warn "tools/validate.ps1 not found; skip validator"
}

# 3) Build APK using project wrapper
Write-Host ""
Write-Host "=== Build APK (gradlew) ==="
$gradlew = Join-Path $projResolved "gradlew.bat"
if (!(Test-Path $gradlew)) {
  Write-Fail "Missing gradlew.bat in project"
  exit 2
}

$task = if ($Variant -eq "release") { "assembleRelease" } else { "assembleDebug" }
Push-Location $projResolved
try {
  & $gradlew clean $task
  if ($LASTEXITCODE -ne 0) {
    Write-Fail "Gradle build failed ($task)"
    exit 2
  }
  Write-Ok "Gradle build OK ($task)"
}
finally {
  Pop-Location
}

# 4) Locate APK
$apkPath = $null
if ($Variant -eq "release") {
  $candidate = Join-Path $projResolved "app\build\outputs\apk\release\app-release.apk"
  if (Test-Path $candidate) { $apkPath = $candidate }
} else {
  $candidate = Join-Path $projResolved "app\build\outputs\apk\debug\app-debug.apk"
  if (Test-Path $candidate) { $apkPath = $candidate }
}

if ($null -eq $apkPath) {
  Write-Fail "APK not found under app/build/outputs/apk/$Variant/"
  exit 2
}
Write-Ok "APK found: $apkPath"

# 5) Copy to artifacts/apk/<id>/
Write-Host ""
Write-Host "=== Export APK ==="
$artRoot = Join-Path $root "artifacts\apk"
$targetDir = Join-Path $artRoot $gameId
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$stamp = (Get-Date).ToString("yyyyMMdd-HHmm")
$outName = "$gameId-$Variant-$stamp.apk"
$outPath = Join-Path $targetDir $outName

Copy-Item -LiteralPath $apkPath -Destination $outPath -Force

Write-Ok "Exported APK: $outPath"
Write-Host ""
Write-Host "FINAL_APK=$outPath"
exit 0
