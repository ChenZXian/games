[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$Project,

  [switch]$SkipDoctor,
  [switch]$SkipValidate
)

$ErrorActionPreference = "Stop"

function Write-Section($title) { Write-Host ""; Write-Host "=== $title ===" }
function Write-Ok($msg) { Write-Host "[OK]  $msg" }
function Write-Warn($msg) { Write-Host "[WARN] $msg" }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" }

function Get-RepoRoot {
  $dir = (Get-Location).Path
  for ($i = 0; $i -lt 12; $i++) {
    if ((Test-Path (Join-Path $dir "docs")) -and
        (Test-Path (Join-Path $dir "registry")) -and
        (Test-Path (Join-Path $dir "games")) -and
        (Test-Path (Join-Path $dir "tools"))) {
      return $dir
    }
    $parent = Split-Path $dir -Parent
    if ($parent -eq $dir) { break }
    $dir = $parent
  }
  throw "Repo root not found."
}

function Read-Text($path) {
  if (!(Test-Path $path)) { return $null }
  return Get-Content -LiteralPath $path -Raw
}

function Match-First($text, $pattern) {
  if ($null -eq $text) { return $null }
  $m = [regex]::Match($text, $pattern, "IgnoreCase")
  if ($m.Success) { return $m.Groups[1].Value }
  return $null
}

function Run-PowerShellScript($scriptPath, [string[]]$argsList, $workdir) {
  $stdoutPath = [System.IO.Path]::GetTempFileName()
  $stderrPath = [System.IO.Path]::GetTempFileName()
  try {
    $psiArgs = @("-ExecutionPolicy", "Bypass", "-File", $scriptPath) + $argsList
    $proc = Start-Process -FilePath "powershell" -ArgumentList $psiArgs -WorkingDirectory $workdir -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -Wait -PassThru -NoNewWindow
    $stdout = ""
    $stderr = ""
    if (Test-Path $stdoutPath) { $stdout = Get-Content -LiteralPath $stdoutPath -Raw }
    if (Test-Path $stderrPath) { $stderr = Get-Content -LiteralPath $stderrPath -Raw }
    return @{
      ExitCode = $proc.ExitCode
      Output = ($stdout + $stderr)
    }
  }
  finally {
    Remove-Item -LiteralPath $stdoutPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $stderrPath -Force -ErrorAction SilentlyContinue
  }
}

function Test-AnyPathExists($base, $relativePaths) {
  foreach ($rel in $relativePaths) {
    if (Test-Path (Join-Path $base $rel)) { return $true }
  }
  return $false
}

function Get-StatusValue($value) {
  if ([string]::IsNullOrWhiteSpace($value)) { return "unknown" }
  return $value
}

$passCount = 0
$warnCount = 0
$failCount = 0

$root = Get-RepoRoot
Set-Location $root

$projDir = Join-Path $root $Project
if (!(Test-Path $projDir)) {
  Write-Fail "Project path not found: $projDir"
  exit 2
}

$projResolved = (Resolve-Path $projDir).Path
$gamesRoot = (Resolve-Path (Join-Path $root "games")).Path
$expectedPrefix = $gamesRoot + "\"
if (-not ($projResolved.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase))) {
  Write-Fail "Project must be under repo_root/games/<id>. Got: $projResolved"
  exit 2
}

$parent = Split-Path $projResolved -Parent
if (-not ((Split-Path $parent -Leaf) -ieq "games")) {
  Write-Fail "Project root must be a direct child of repo_root/games/. Got: $projResolved"
  exit 2
}

$gameId = Split-Path $projResolved -Leaf
$projectArg = "games/$gameId"
$doctorReady = $true
$validatorReady = $true
$registryReady = $true
$requirementsStatus = "untracked"
$iconStatus = "deferred"
$uiStatus = "deferred"
$audioStatus = "deferred"

Write-Section "Inspect Target"
Write-Ok "Repo root: $root"
Write-Ok "Project: $projResolved"
Write-Ok "Game id: $gameId"
$passCount += 3

Write-Section "Environment"
$doctorScript = Join-Path $root "tools\env\doctor.ps1"
if ($SkipDoctor) {
  Write-Warn "Doctor skipped by caller"
  $warnCount++
}
elseif (!(Test-Path $doctorScript)) {
  Write-Warn "Doctor script not found: tools/env/doctor.ps1"
  $warnCount++
}
else {
  $doctor = Run-PowerShellScript $doctorScript @("-Project", $projectArg) $root
  if ($doctor.ExitCode -eq 0) {
    Write-Ok "Doctor passed"
    $passCount++
  } else {
    $doctorReady = $false
    $failCount++
    Write-Fail "Doctor failed"
    $doctorSummary = Match-First $doctor.Output 'Fails:\s*([0-9]+,\s*Warnings:\s*[0-9]+)'
    if ($doctorSummary) {
      Write-Warn "Doctor summary: Fails: $doctorSummary"
      $warnCount++
    }
  }
}

Write-Section "Project Validation"
$validatorScript = Join-Path $root "tools\validate.ps1"
if ($SkipValidate) {
  Write-Warn "Validator skipped by caller"
  $warnCount++
}
elseif (!(Test-Path $validatorScript)) {
  Write-Warn "Validator script not found: tools/validate.ps1"
  $warnCount++
}
else {
  $validator = Run-PowerShellScript $validatorScript @("-Project", $projectArg) $root
  if ($validator.ExitCode -eq 0) {
    Write-Ok "Validator passed"
    $passCount++
  } else {
    $validatorReady = $false
    $failCount++
    Write-Fail "Validator failed"
    $validatorSummary = Match-First $validator.Output 'Fails:\s*([0-9]+,\s*Warnings:\s*[0-9]+)'
    if ($validatorSummary) {
      Write-Warn "Validator summary: Fails: $validatorSummary"
      $warnCount++
    }
  }
}

Write-Section "Registry"
$registryPath = Join-Path $root "registry\produced_games.json"
if (!(Test-Path $registryPath)) {
  $registryReady = $false
  $failCount++
  Write-Fail "Missing registry/produced_games.json"
} else {
  $registry = $null
  try {
    $registry = Get-Content -LiteralPath $registryPath -Raw | ConvertFrom-Json
  }
  catch {
    $registryReady = $false
    $failCount++
    Write-Fail "Registry JSON is invalid: registry/produced_games.json"
  }
  $registryEntry = $null
  if ($null -ne $registry -and $null -ne $registry.games) {
    $registryEntry = $registry.games | Where-Object { $_.id -eq $gameId } | Select-Object -First 1
  }
  if ($null -eq $registry) {
  }
  elseif ($null -eq $registryEntry) {
    $registryReady = $false
    $failCount++
    Write-Fail "No registry entry found for game id: $gameId"
  } else {
    Write-Ok "Registry entry found: $($registryEntry.name)"
    $passCount++
    $entrySkin = $registryEntry.ui_skin
    if ([string]::IsNullOrWhiteSpace($entrySkin)) {
      Write-Warn "Registry entry does not declare ui_skin"
      $warnCount++
    } else {
      Write-Ok "Registry ui_skin: $entrySkin"
      $passCount++
    }
  }
}

Write-Section "Requirements Trace"
$requirementsDir = Join-Path $root "artifacts\requirements\$gameId"
$requirementsMetadataPath = Join-Path $requirementsDir "metadata.json"
$requirementsMarkdownPath = Join-Path $requirementsDir "requirements.md"
$requirementsCandidatesPath = Join-Path $requirementsDir "candidates.md"
$requirementsMetadata = $null
if (Test-Path $requirementsMetadataPath) {
  try {
    $requirementsMetadata = Get-Content -LiteralPath $requirementsMetadataPath -Raw | ConvertFrom-Json
  }
  catch {
    Write-Warn "Requirements metadata is invalid: artifacts/requirements/$gameId/metadata.json"
    $warnCount++
  }
}

if (Test-Path $requirementsMarkdownPath) {
  if ($null -ne $requirementsMetadata -and [string]$requirementsMetadata.status -eq "confirmed") {
    $requirementsStatus = "confirmed"
  } else {
    $requirementsStatus = "draft"
  }
  Write-Ok "Requirements trace found: $requirementsMarkdownPath"
  $passCount++
}
elseif (Test-Path $requirementsCandidatesPath) {
  $requirementsStatus = "candidates"
  Write-Warn "Only candidate trace found: $requirementsCandidatesPath"
  $warnCount++
}
else {
  $requirementsStatus = "untracked"
  Write-Warn "No requirements trace found under artifacts/requirements/$gameId/"
  $warnCount++
}

Write-Section "Resource Tracks"
$resDir = Join-Path $projResolved "app\src\main\res"
$adaptiveIcon = Join-Path $resDir "mipmap-anydpi-v26\app_icon.xml"
$legacyIcons = @()
if (Test-Path $resDir) {
  $legacyIcons = Get-ChildItem -Path $resDir -Recurse -File -Filter "app_icon.png" -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -match 'mipmap' }
}
$projectIconPresent = (Test-Path $adaptiveIcon) -or ($legacyIcons.Count -gt 0)
$iconExportDir = Join-Path $root "artifacts\icons\$gameId"
$iconExportFiles = @()
if (Test-Path $iconExportDir) {
  $iconExportFiles = Get-ChildItem -Path $iconExportDir -Recurse -File -ErrorAction SilentlyContinue
}
if ($projectIconPresent -and $iconExportFiles.Count -gt 0) {
  $iconStatus = "complete"
  Write-Ok "Icon status: complete ($($iconExportFiles.Count) export file(s))"
  $passCount++
} elseif ($projectIconPresent) {
  $iconStatus = "placeholder_only"
  Write-Warn "Icon status: placeholder_only (project icon exists, no export found under artifacts/icons/$gameId)"
  $warnCount++
} else {
  $iconStatus = "deferred"
  Write-Warn "Icon status: deferred"
  $warnCount++
}

$uiRecordPath = Join-Path $projResolved "app\src\main\assets\ui\ui_pack_assignment.json"
$requiredUiFiles = @(
  "app\src\main\res\layout\activity_main.xml",
  "app\src\main\res\values\colors.xml",
  "app\src\main\res\values\dimens.xml",
  "app\src\main\res\values\styles.xml",
  "app\src\main\res\values\themes.xml"
)
$uiFoundationPresent = $true
foreach ($rel in $requiredUiFiles) {
  if (!(Test-Path (Join-Path $projResolved $rel))) {
    $uiFoundationPresent = $false
    break
  }
}
if (Test-Path $uiRecordPath) {
  $uiStatus = "complete"
  Write-Ok "UI status: complete (tracked by app/src/main/assets/ui/ui_pack_assignment.json)"
  $passCount++
} elseif ($uiFoundationPresent) {
  $uiStatus = "placeholder_only"
  Write-Warn "UI status: placeholder_only (foundation exists, no UI assignment record found)"
  $warnCount++
} else {
  $uiStatus = "deferred"
  Write-Warn "UI status: deferred"
  $warnCount++
}

$projectAudioDir = Join-Path $projResolved "app\src\main\assets\audio"
$projectAudioFiles = @()
if (Test-Path $projectAudioDir) {
  $projectAudioFiles = Get-ChildItem -Path $projectAudioDir -Recurse -File -ErrorAction SilentlyContinue
}
$audioLibraryMatches = @()
$audioIndexPath = Join-Path $root "shared_assets\audio\index.json"
if (Test-Path $audioIndexPath) {
  try {
    $audioIndex = Get-Content -LiteralPath $audioIndexPath -Raw | ConvertFrom-Json
    if ($null -ne $audioIndex.audio) {
      $audioLibraryMatches = $audioIndex.audio | Where-Object {
        $usedBy = $_.used_by
        ($usedBy -is [System.Array] -and ($usedBy -contains $gameId))
      }
    }
  }
  catch {
    Write-Warn "Audio index JSON is invalid: shared_assets/audio/index.json"
    $warnCount++
  }
}
if ($projectAudioFiles.Count -gt 0 -and $audioLibraryMatches.Count -gt 0) {
  $audioStatus = "complete"
  Write-Ok "Audio status: complete ($($projectAudioFiles.Count) project file(s), $($audioLibraryMatches.Count) shared library match(es))"
  $passCount++
} elseif ($projectAudioFiles.Count -gt 0) {
  $audioStatus = "placeholder_only"
  Write-Warn "Audio status: placeholder_only (project audio exists, no shared library linkage found)"
  $warnCount++
} else {
  $audioStatus = "deferred"
  Write-Warn "Audio status: deferred"
  $warnCount++
}

Write-Section "Artifact History"
$apkDir = Join-Path $root "artifacts\apk\$gameId"
$apkFiles = @()
if (Test-Path $apkDir) {
  $apkFiles = Get-ChildItem -Path $apkDir -File -Filter "*.apk" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
}
if ($apkFiles.Count -gt 0) {
  Write-Ok "Existing APK exports: $($apkFiles.Count) (latest: $($apkFiles[0].Name))"
  $passCount++
} else {
  Write-Warn "No exported APK artifacts found yet"
  $warnCount++
}

$canEnterPack = $doctorReady -and $validatorReady -and $registryReady
$deliveryReady = $canEnterPack -and ($requirementsStatus -eq "confirmed") -and ($iconStatus -eq "complete") -and ($uiStatus -ne "deferred") -and ($audioStatus -ne "deferred")

$nextStep = ""
if (-not $doctorReady) {
  $nextStep = "Fix environment baseline issues before packaging"
} elseif (-not $validatorReady) {
  $nextStep = "Fix project validation failures before packaging"
} elseif (-not $registryReady) {
  $nextStep = "Add or repair the registry entry for this game"
} elseif ($requirementsStatus -eq "untracked") {
  $nextStep = "Create an authoritative requirements trace under artifacts/requirements/<game_id>/"
} elseif ($requirementsStatus -eq "candidates") {
  $nextStep = "Expand the selected concept into a full requirements document"
} elseif ($requirementsStatus -eq "draft") {
  $nextStep = "Confirm the requirements trace before treating the project as delivery-ready"
} elseif ($iconStatus -eq "deferred" -or $iconStatus -eq "placeholder_only") {
  $nextStep = "Run the icon workflow to export upload-ready icon assets"
} elseif ($audioStatus -eq "deferred") {
  $nextStep = "Run the audio workflow to assign at least one tracked BGM or SFX set"
} elseif (-not $deliveryReady) {
  $nextStep = "Close the remaining resource completion gaps before release delivery"
} else {
  $nextStep = "Project can enter packaging"
}

Write-Section "Inspect Summary"
Write-Host "Passes: $passCount, Warnings: $warnCount, Fails: $failCount"
Write-Host "CAN_ENTER_PACK=$($canEnterPack.ToString().ToLower())"
Write-Host "DELIVERY_READY=$($deliveryReady.ToString().ToLower())"
Write-Host "REQUIREMENTS_STATUS=$(Get-StatusValue $requirementsStatus)"
Write-Host "ICON_STATUS=$(Get-StatusValue $iconStatus)"
Write-Host "UI_STATUS=$(Get-StatusValue $uiStatus)"
Write-Host "AUDIO_STATUS=$(Get-StatusValue $audioStatus)"
Write-Host "NEXT_STEP=$nextStep"

if ($canEnterPack) { exit 0 }
exit 2
