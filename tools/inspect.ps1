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
$gameplayDiversityStatus = "missing"
$visualIdentityStatus = "missing"
$implementationFidelityStatus = "untracked"
$iconStatus = "deferred"
$iconUniquenessStatus = "unknown"
$uiStatus = "deferred"
$gameArtStatus = "deferred"
$gameArtRuntimeStatus = "missing"
$audioStatus = "deferred"
$bgmStatus = "missing"

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
$gameplayDiversityPath = Join-Path $requirementsDir "gameplay_diversity.json"
$visualIdentityPath = Join-Path $requirementsDir "visual_identity.json"
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

if (Test-Path $gameplayDiversityPath) {
  try {
    $gameplayDiversity = Get-Content -LiteralPath $gameplayDiversityPath -Raw | ConvertFrom-Json
    $gameplayDiversityStatus = [string]$gameplayDiversity.status
    if ([string]::IsNullOrWhiteSpace($gameplayDiversityStatus)) {
      $gameplayDiversityStatus = "invalid"
      Write-Warn "Gameplay diversity contract status is missing: artifacts/requirements/$gameId/gameplay_diversity.json"
      $warnCount++
    } elseif ($gameplayDiversityStatus -eq "passed") {
      Write-Ok "Gameplay diversity: passed"
      $passCount++
    } else {
      Write-Warn "Gameplay diversity: $gameplayDiversityStatus"
      $warnCount++
    }
  }
  catch {
    $gameplayDiversityStatus = "invalid"
    Write-Warn "Gameplay diversity contract JSON is invalid: artifacts/requirements/$gameId/gameplay_diversity.json"
    $warnCount++
  }
} else {
  $gameplayDiversityStatus = "missing"
  Write-Warn "Gameplay diversity contract is missing: artifacts/requirements/$gameId/gameplay_diversity.json"
  $warnCount++
}

if (Test-Path $visualIdentityPath) {
  try {
    $visualIdentity = Get-Content -LiteralPath $visualIdentityPath -Raw | ConvertFrom-Json
    $visualIdentityStatus = [string]$visualIdentity.status
    if ([string]::IsNullOrWhiteSpace($visualIdentityStatus)) {
      $visualIdentityStatus = "invalid"
      Write-Warn "Visual identity contract status is missing: artifacts/requirements/$gameId/visual_identity.json"
      $warnCount++
    } elseif ($visualIdentityStatus -eq "passed") {
      Write-Ok "Visual identity: passed"
      $passCount++
    } else {
      Write-Warn "Visual identity: $visualIdentityStatus"
      $warnCount++
    }
  }
  catch {
    $visualIdentityStatus = "invalid"
    Write-Warn "Visual identity contract JSON is invalid: artifacts/requirements/$gameId/visual_identity.json"
    $warnCount++
  }
} else {
  $visualIdentityStatus = "missing"
  Write-Warn "Visual identity contract is missing: artifacts/requirements/$gameId/visual_identity.json"
  $warnCount++
}

$implementationReviewPath = Join-Path $requirementsDir "implementation_review.json"
if ($requirementsStatus -eq "confirmed") {
  if (Test-Path $implementationReviewPath) {
    try {
      $implementationReview = Get-Content -LiteralPath $implementationReviewPath -Raw | ConvertFrom-Json
      $implementationFidelityStatus = [string]$implementationReview.status
      if ($implementationFidelityStatus -eq "passed") {
        Write-Ok "Implementation fidelity: passed"
        $passCount++
      } elseif ([string]::IsNullOrWhiteSpace($implementationFidelityStatus)) {
        $implementationFidelityStatus = "unreviewed"
        Write-Warn "Implementation fidelity: unreviewed"
        $warnCount++
      } else {
        Write-Warn "Implementation fidelity: $implementationFidelityStatus"
        $warnCount++
      }
    }
    catch {
      $implementationFidelityStatus = "invalid"
      Write-Warn "Implementation review JSON is invalid: artifacts/requirements/$gameId/implementation_review.json"
      $warnCount++
    }
  } else {
    $implementationFidelityStatus = "unreviewed"
    Write-Warn "Implementation fidelity: unreviewed (missing artifacts/requirements/$gameId/implementation_review.json)"
    $warnCount++
  }
} else {
  $implementationFidelityStatus = "not_ready"
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
$iconMetadata = $null
if (Test-Path $iconExportDir) {
  $iconExportFiles = Get-ChildItem -Path $iconExportDir -Recurse -File -ErrorAction SilentlyContinue
  $iconMetadataPath = Join-Path $iconExportDir "metadata.json"
  if (Test-Path $iconMetadataPath) {
    try {
      $iconMetadata = Get-Content -LiteralPath $iconMetadataPath -Raw | ConvertFrom-Json
    }
    catch {
      Write-Warn "Icon metadata JSON is invalid: artifacts/icons/$gameId/metadata.json"
      $warnCount++
    }
  }
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
if ($iconStatus -eq "complete") {
  $genericIconMotifs = @("shieldstar", "swordshield", "castle")
  $iconMotif = ""
  $iconSubject = ""
  if ($null -ne $iconMetadata) {
    $iconMotif = [string]$iconMetadata.motif
    $iconSubject = [string]$iconMetadata.icon_subject
  }
  if ([string]::IsNullOrWhiteSpace($iconMotif)) {
    $iconUniquenessStatus = "unreviewed"
    Write-Warn "Icon uniqueness: unreviewed (metadata does not declare motif)"
    $warnCount++
  } elseif ($genericIconMotifs -contains $iconMotif) {
    $iconUniquenessStatus = "generic"
    Write-Warn "Icon uniqueness: generic motif '$iconMotif' should be replaced with a game-specific subject"
    $warnCount++
  } elseif ([string]::IsNullOrWhiteSpace($iconSubject)) {
    $iconUniquenessStatus = "unreviewed"
    Write-Warn "Icon uniqueness: unreviewed (metadata does not declare icon_subject)"
    $warnCount++
  } else {
    $iconUniquenessStatus = "passed"
    Write-Ok "Icon uniqueness: passed ($iconMotif)"
    $passCount++
  }
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
$uiRecord = $null
$uiLibraryMatches = @()
if (Test-Path $uiRecordPath) {
  try {
    $uiRecord = Get-Content -LiteralPath $uiRecordPath -Raw | ConvertFrom-Json
  }
  catch {
    Write-Warn "UI assignment JSON is invalid: app/src/main/assets/ui/ui_pack_assignment.json"
    $warnCount++
  }
}
$uiIndexPath = Join-Path $root "shared_assets\ui\index.json"
if (Test-Path $uiIndexPath) {
  try {
    $uiIndex = Get-Content -LiteralPath $uiIndexPath -Raw | ConvertFrom-Json
    if ($null -ne $uiIndex.packs) {
      $uiLibraryMatches = @($uiIndex.packs | Where-Object {
        $usedBy = $_.used_by
        ($usedBy -eq $gameId) -or ($usedBy -is [System.Array] -and ($usedBy -contains $gameId))
      })
    }
  }
  catch {
    Write-Warn "UI index JSON is invalid: shared_assets/ui/index.json"
    $warnCount++
  }
}
if ($null -ne $uiRecord -and [string]$uiRecord.assignment_type -eq "project_local_xml_ui") {
  $uiStatus = "placeholder_only"
  Write-Warn "UI status: placeholder_only (UI Kit XML scaffold is not a shared or imported UI pack assignment)"
  $warnCount++
} elseif ($null -ne $uiRecord -and -not [string]::IsNullOrWhiteSpace([string]$uiRecord.pack_id) -and $uiLibraryMatches.Count -gt 0) {
  $uiStatus = "complete"
  Write-Ok "UI status: complete (shared UI pack tracked by app/src/main/assets/ui/ui_pack_assignment.json)"
  $passCount++
} elseif (Test-Path $uiRecordPath) {
  $uiStatus = "placeholder_only"
  Write-Warn "UI status: placeholder_only (UI assignment record exists, shared library linkage is incomplete)"
  $warnCount++
} elseif ($uiFoundationPresent) {
  $uiStatus = "placeholder_only"
  Write-Warn "UI status: placeholder_only (foundation exists, no UI assignment record found)"
  $warnCount++
} else {
  $uiStatus = "deferred"
  Write-Warn "UI status: deferred"
  $warnCount++
}

$gameArtRecordPath = Join-Path $projResolved "app\src\main\assets\game_art\game_art_assignment.json"
$projectGameArtDir = Join-Path $projResolved "app\src\main\assets\game_art"
$projectGameArtFiles = @()
if (Test-Path $projectGameArtDir) {
  $projectGameArtFiles = Get-ChildItem -Path $projectGameArtDir -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -ne "game_art_assignment.json" }
}
$gameArtLibraryMatches = @()
$gameArtRecord = $null
$gameArtRuntimeMap = $null
if (Test-Path $gameArtRecordPath) {
  try {
    $gameArtRecord = Get-Content -LiteralPath $gameArtRecordPath -Raw | ConvertFrom-Json
  }
  catch {
    Write-Warn "Game art assignment JSON is invalid: app/src/main/assets/game_art/game_art_assignment.json"
    $warnCount++
  }
}
$gameArtRuntimeMapPath = Join-Path $projResolved "app\src\main\assets\game_art\runtime_art_map.json"
if (Test-Path $gameArtRuntimeMapPath) {
  try {
    $gameArtRuntimeMap = Get-Content -LiteralPath $gameArtRuntimeMapPath -Raw | ConvertFrom-Json
    $gameArtRuntimeStatus = [string]$gameArtRuntimeMap.status
    if ([string]::IsNullOrWhiteSpace($gameArtRuntimeStatus)) {
      $gameArtRuntimeStatus = "invalid"
    }
  }
  catch {
    $gameArtRuntimeStatus = "invalid"
    Write-Warn "Game art runtime map JSON is invalid: app/src/main/assets/game_art/runtime_art_map.json"
    $warnCount++
  }
}
$gameArtRuntimeMapReady = $false
if ($null -ne $gameArtRuntimeMap) {
  $runtimeEntities = @($gameArtRuntimeMap.entities)
  if (($gameArtRuntimeStatus -eq "integrated" -or $gameArtRuntimeStatus -eq "passed" -or $gameArtRuntimeStatus -eq "complete") -and $runtimeEntities.Count -gt 0) {
    $gameArtRuntimeMapReady = $true
  }
}
$gameArtRuntimeUsesAssets = $false
$javaSourceDir = Join-Path $projResolved "app\src\main\java"
if (Test-Path $javaSourceDir) {
  $runtimeHit = Get-ChildItem -Path $javaSourceDir -Recurse -File -Filter "*.java" -ErrorAction SilentlyContinue |
    Select-String -Pattern "game_art/","BitmapFactory","AssetManager" -SimpleMatch -ErrorAction SilentlyContinue |
    Select-Object -First 1
  if ($null -ne $runtimeHit) {
    $gameArtRuntimeUsesAssets = $true
  }
}
$gameArtIndexPath = Join-Path $root "shared_assets\game_art\index.json"
if (Test-Path $gameArtIndexPath) {
  try {
    $gameArtIndex = Get-Content -LiteralPath $gameArtIndexPath -Raw | ConvertFrom-Json
    if ($null -ne $gameArtIndex.packs) {
      $gameArtLibraryMatches = @($gameArtIndex.packs | Where-Object {
        $usedBy = $_.used_by
        ($usedBy -eq $gameId) -or ($usedBy -is [System.Array] -and ($usedBy -contains $gameId))
      })
    }
  }
  catch {
    Write-Warn "Game art index JSON is invalid: shared_assets/game_art/index.json"
    $warnCount++
  }
}
if ($null -ne $gameArtRecord -and [string]$gameArtRecord.assignment_type -eq "project_local_canvas_art") {
  $gameArtStatus = "placeholder_only"
  Write-Warn "Game art status: placeholder_only (local Canvas art is not a shared gameplay art assignment)"
  $warnCount++
} elseif ((Test-Path $gameArtRecordPath) -and $gameArtLibraryMatches.Count -gt 0 -and $gameArtRuntimeUsesAssets -and $gameArtRuntimeMapReady) {
  $gameArtStatus = "complete"
  Write-Ok "Game art status: complete ($($projectGameArtFiles.Count) project file(s), $($gameArtLibraryMatches.Count) shared library match(es))"
  $passCount++
} elseif ((Test-Path $gameArtRecordPath) -and $gameArtLibraryMatches.Count -gt 0 -and $gameArtRuntimeUsesAssets -and -not $gameArtRuntimeMapReady) {
  $gameArtStatus = "placeholder_only"
  Write-Warn "Game art status: placeholder_only (runtime art map is not integrated)"
  $warnCount++
} elseif ((Test-Path $gameArtRecordPath) -and $gameArtLibraryMatches.Count -gt 0) {
  $gameArtStatus = "placeholder_only"
  Write-Warn "Game art status: placeholder_only (shared art is assigned but runtime usage was not detected)"
  $warnCount++
} elseif ((Test-Path $gameArtRecordPath) -or $projectGameArtFiles.Count -gt 0) {
  $gameArtStatus = "placeholder_only"
  Write-Warn "Game art status: placeholder_only (project art exists, shared library linkage is incomplete)"
  $warnCount++
} else {
  $gameArtStatus = "deferred"
  Write-Warn "Game art status: deferred"
  $warnCount++
}

$projectAudioDir = Join-Path $projResolved "app\src\main\assets\audio"
$projectAudioFiles = @()
if (Test-Path $projectAudioDir) {
  $projectAudioFiles = Get-ChildItem -Path $projectAudioDir -Recurse -File -ErrorAction SilentlyContinue
}
$projectBgmFiles = @($projectAudioFiles | Where-Object { $_.Name -like "bgm*" })
$audioLibraryMatches = @()
$bgmLibraryMatches = @()
$audioIndexPath = Join-Path $root "shared_assets\audio\index.json"
if (Test-Path $audioIndexPath) {
  try {
    $audioIndex = Get-Content -LiteralPath $audioIndexPath -Raw | ConvertFrom-Json
    if ($null -ne $audioIndex.audio) {
      $audioLibraryMatches = @($audioIndex.audio | Where-Object {
        $usedBy = $_.used_by
        ($usedBy -eq $gameId) -or ($usedBy -is [System.Array] -and ($usedBy -contains $gameId))
      })
      $bgmLibraryMatches = @($audioLibraryMatches | Where-Object { [string]$_.type -eq "bgm" -or [string]$_.role -match '^(menu|play|gameplay|climax|boss|win|fail)$' })
    }
  }
  catch {
    Write-Warn "Audio index JSON is invalid: shared_assets/audio/index.json"
    $warnCount++
  }
}
if ($projectBgmFiles.Count -gt 0 -and $bgmLibraryMatches.Count -gt 0) {
  $bgmStatus = "complete"
  Write-Ok "BGM status: complete ($($projectBgmFiles.Count) project BGM file(s), $($bgmLibraryMatches.Count) shared BGM match(es))"
  $passCount++
} elseif ($projectBgmFiles.Count -gt 0) {
  $bgmStatus = "placeholder_only"
  Write-Warn "BGM status: placeholder_only (project BGM exists, no shared library BGM linkage found)"
  $warnCount++
} else {
  $bgmStatus = "missing"
  Write-Warn "BGM status: missing"
  $warnCount++
}
if ($projectAudioFiles.Count -gt 0 -and $audioLibraryMatches.Count -gt 0 -and $bgmStatus -eq "complete") {
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
$deliveryReady = $canEnterPack -and ($requirementsStatus -eq "confirmed") -and ($gameplayDiversityStatus -eq "passed") -and ($visualIdentityStatus -eq "passed") -and ($implementationFidelityStatus -eq "passed") -and ($iconStatus -eq "complete") -and ($iconUniquenessStatus -eq "passed") -and ($uiStatus -eq "complete") -and ($gameArtStatus -eq "complete") -and ($audioStatus -eq "complete") -and ($bgmStatus -eq "complete")

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
} elseif ($gameplayDiversityStatus -ne "passed") {
  $nextStep = "Complete the gameplay diversity and content budget contract before release delivery"
} elseif ($visualIdentityStatus -ne "passed") {
  $nextStep = "Complete the visual identity contract before icon and UI release delivery"
} elseif ($implementationFidelityStatus -ne "passed") {
  $nextStep = "Review and repair implementation fidelity against the confirmed requirements before release delivery"
} elseif ($iconStatus -eq "deferred" -or $iconStatus -eq "placeholder_only") {
  $nextStep = "Run the icon workflow to export upload-ready icon assets"
} elseif ($iconUniquenessStatus -ne "passed") {
  $nextStep = "Regenerate a game-specific icon and avoid generic repeated motifs"
} elseif ($gameArtStatus -eq "deferred") {
  $nextStep = "Run the gameplay art workflow to assign tracked character, map, prop, effect, or background assets"
} elseif ($gameArtStatus -eq "placeholder_only") {
  $nextStep = "Complete gameplay art runtime mapping and animation or facing integration before release delivery"
} elseif ($audioStatus -eq "deferred") {
  $nextStep = "Run the audio workflow to assign at least one tracked BGM or SFX set"
} elseif ($bgmStatus -ne "complete") {
  $nextStep = "Run the audio workflow to assign tracked BGM files and shared library metadata"
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
Write-Host "GAMEPLAY_DIVERSITY_STATUS=$(Get-StatusValue $gameplayDiversityStatus)"
Write-Host "VISUAL_IDENTITY_STATUS=$(Get-StatusValue $visualIdentityStatus)"
Write-Host "IMPLEMENTATION_FIDELITY_STATUS=$(Get-StatusValue $implementationFidelityStatus)"
Write-Host "ICON_STATUS=$(Get-StatusValue $iconStatus)"
Write-Host "ICON_UNIQUENESS_STATUS=$(Get-StatusValue $iconUniquenessStatus)"
Write-Host "UI_STATUS=$(Get-StatusValue $uiStatus)"
Write-Host "GAME_ART_STATUS=$(Get-StatusValue $gameArtStatus)"
Write-Host "GAME_ART_RUNTIME_STATUS=$(Get-StatusValue $gameArtRuntimeStatus)"
Write-Host "AUDIO_STATUS=$(Get-StatusValue $audioStatus)"
Write-Host "BGM_STATUS=$(Get-StatusValue $bgmStatus)"
Write-Host "NEXT_STEP=$nextStep"

if ($canEnterPack) { exit 0 }
exit 2
