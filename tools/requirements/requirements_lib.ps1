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

function Write-Utf8File($path, $content) {
  $dir = Split-Path $path -Parent
  if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
  }
  [System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
}

function Get-AllowedUiSkins {
  return @(
    "skin_dark_arcade",
    "skin_cartoon_light",
    "skin_neon_future",
    "skin_post_apocalypse",
    "skin_military_tech"
  )
}

function Assert-UiSkinValid([string]$UiSkin) {
  if ([string]::IsNullOrWhiteSpace($UiSkin)) {
    return
  }
  $allowed = Get-AllowedUiSkins
  if ($allowed -notcontains $UiSkin) {
    throw "UiSkin must be one of: $($allowed -join ', ')"
  }
}

function Read-TraceMetadata($metadataPath) {
  if (!(Test-Path $metadataPath)) {
    return $null
  }
  try {
    return Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
  }
  catch {
    throw "Existing metadata.json is invalid: $metadataPath"
  }
}

function Resolve-StringValue([string]$IncomingValue, $ExistingValue, [string]$Fallback = "") {
  if (-not [string]::IsNullOrWhiteSpace($IncomingValue)) {
    return $IncomingValue
  }
  if ($null -ne $ExistingValue -and -not [string]::IsNullOrWhiteSpace([string]$ExistingValue)) {
    return [string]$ExistingValue
  }
  return $Fallback
}

function Resolve-MarkdownContent(
  [string]$InlineContent,
  [string]$ContentPath,
  [string]$DefaultContent,
  [string]$Label
) {
  if (-not [string]::IsNullOrWhiteSpace($InlineContent) -and -not [string]::IsNullOrWhiteSpace($ContentPath)) {
    throw "Only one of $Label or ${Label}Path may be supplied."
  }
  if (-not [string]::IsNullOrWhiteSpace($ContentPath)) {
    if (!(Test-Path $ContentPath)) {
      throw "$Label path not found: $ContentPath"
    }
    return Get-Content -LiteralPath $ContentPath -Raw
  }
  if (-not [string]::IsNullOrWhiteSpace($InlineContent)) {
    return $InlineContent
  }
  return $DefaultContent
}

function New-CandidatesTemplate([string]$GameId, [string]$Title, [string]$Direction) {
@"
# Candidate Concepts

Game ID: $GameId
Title: $Title
Direction: $Direction

## Candidate List

1. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

2. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

3. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

4. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

5. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

6. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

7. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

8. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

9. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

10. Working Title:
Pitch:
Core Loop:
Controls:
Distinctive Hook:
Retention:
Duplicate Risk:

## Selection

Selected Concept:
Selection Rationale:

"@
}

function New-RequirementsTemplate([string]$GameId, [string]$Title, [string]$Direction, [string]$UiSkin, [string]$SelectedConcept) {
@"
# $Title

Game ID: $GameId
Direction: $Direction
Selected Concept: $SelectedConcept
Recommended UI Skin: $UiSkin

## Positioning

## Target Feel And Player Fantasy

## Core Gameplay Loop

## Controls And Input Model

## Win And Failure Conditions

## Progression Structure

## Level Or Run Structure

## Economy Rewards And Upgrades

## Screen Map

### Menu

### Gameplay HUD

### Pause

### Game Over

### Extra Screens

## UI Direction

### Layout Tone

### HUD Priorities

### Key Overlay Panels

### UI Asset Strategy

## Icon Direction

### Subject

### Silhouette

### Color Direction

### Tone

## Audio Direction

### Menu BGM

### Gameplay BGM

### Boss Or Climax BGM

### SFX Families

## Technical Implementation Notes

### Rendering Style

### State Model

### Important Entities

### Systems To Plan Early

## Differentiation Note

## Confirmation

Status: Draft

"@
}

function Update-RequirementsTrace {
  param(
    [Parameter(Mandatory=$true)]
    [string]$GameId,

    [string]$Title = "",
    [string]$Direction = "",
    [string]$SelectedConcept = "",
    [string]$UiSkin = "",

    [ValidateSet("candidates","requirements")]
    [string]$Stage = "requirements",

    [ValidateSet("candidates","draft","confirmed")]
    [string]$Status = "draft",

    [string]$CandidatesMarkdown = "",
    [string]$CandidatesMarkdownPath = "",
    [string]$RequirementsMarkdown = "",
    [string]$RequirementsMarkdownPath = "",

    [switch]$Force
  )

  if ($Stage -eq "candidates" -and $Status -ne "candidates") {
    throw "Stage 'candidates' must use status 'candidates'."
  }
  if ($Stage -eq "requirements" -and $Status -eq "candidates") {
    throw "Stage 'requirements' must use status 'draft' or 'confirmed'."
  }

  Assert-UiSkinValid $UiSkin

  $root = Get-RepoRoot
  Set-Location $root

  $traceDir = Join-Path $root "artifacts\requirements\$GameId"
  $metadataPath = Join-Path $traceDir "metadata.json"
  $candidatesPath = Join-Path $traceDir "candidates.md"
  $requirementsPath = Join-Path $traceDir "requirements.md"

  New-Item -ItemType Directory -Force -Path $traceDir | Out-Null

  $existing = Read-TraceMetadata $metadataPath
  $resolvedTitle = Resolve-StringValue $Title $existing.title $GameId
  $resolvedDirection = Resolve-StringValue $Direction $existing.direction ""
  $resolvedSelectedConcept = Resolve-StringValue $SelectedConcept $existing.selected_concept ""
  $resolvedUiSkin = Resolve-StringValue $UiSkin $existing.ui_skin ""
  $createdAt = if ($null -ne $existing -and $existing.created_at_utc) { [string]$existing.created_at_utc } else { (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ") }
  $existingSelectedAt = if ($null -ne $existing -and $existing.selected_at_utc) { [string]$existing.selected_at_utc } else { "" }
  $selectedAt = $existingSelectedAt

  if ([string]::IsNullOrWhiteSpace($existingSelectedAt) -and -not [string]::IsNullOrWhiteSpace($resolvedSelectedConcept)) {
    $selectedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  }

  if ($Stage -eq "candidates" -and ((-not (Test-Path $candidatesPath)) -or $Force)) {
    $candidateContent = Resolve-MarkdownContent $CandidatesMarkdown $CandidatesMarkdownPath (New-CandidatesTemplate $GameId $resolvedTitle $resolvedDirection) "CandidatesMarkdown"
    Write-Utf8File $candidatesPath $candidateContent
  }

  if ($Stage -eq "requirements" -and ((-not (Test-Path $requirementsPath)) -or $Force)) {
    $requirementsContent = Resolve-MarkdownContent $RequirementsMarkdown $RequirementsMarkdownPath (New-RequirementsTemplate $GameId $resolvedTitle $resolvedDirection $resolvedUiSkin $resolvedSelectedConcept) "RequirementsMarkdown"
    Write-Utf8File $requirementsPath $requirementsContent
  }

  if ($Status -eq "confirmed") {
    if (!(Test-Path $requirementsPath)) {
      throw "Cannot confirm requirements trace without requirements.md."
    }
    if ([string]::IsNullOrWhiteSpace($resolvedSelectedConcept)) {
      throw "Cannot confirm requirements trace without selected_concept."
    }
    if ([string]::IsNullOrWhiteSpace($resolvedUiSkin)) {
      throw "Cannot confirm requirements trace without ui_skin."
    }
  }

  $confirmedAt = ""
  if ($Status -eq "confirmed") {
    $confirmedAt = if ($null -ne $existing -and $existing.confirmed_at_utc) { [string]$existing.confirmed_at_utc } else { (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ") }
  }
  elseif ($null -ne $existing -and $existing.confirmed_at_utc) {
    $confirmedAt = [string]$existing.confirmed_at_utc
  }

  $files = [ordered]@{
    metadata_json = "artifacts/requirements/$GameId/metadata.json"
    requirements_md = if (Test-Path $requirementsPath) { "artifacts/requirements/$GameId/requirements.md" } else { "" }
    candidates_md = if (Test-Path $candidatesPath) { "artifacts/requirements/$GameId/candidates.md" } else { "" }
  }

  $metadata = [ordered]@{
    version = 1
    game_id = $GameId
    title = $resolvedTitle
    direction = $resolvedDirection
    selected_concept = $resolvedSelectedConcept
    status = $Status
    ui_skin = $resolvedUiSkin
    current_stage = $Stage
    created_at_utc = $createdAt
    updated_at_utc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    selected_at_utc = $selectedAt
    confirmed_at_utc = $confirmedAt
    files = $files
  }

  $json = $metadata | ConvertTo-Json -Depth 5
  Write-Utf8File $metadataPath ($json + [Environment]::NewLine)

  Write-Host "REQUIREMENTS_TRACE_DIR=$traceDir"
  Write-Host "REQUIREMENTS_METADATA=$metadataPath"
  if (Test-Path $candidatesPath) { Write-Host "REQUIREMENTS_CANDIDATES=$candidatesPath" }
  if (Test-Path $requirementsPath) { Write-Host "REQUIREMENTS_MARKDOWN=$requirementsPath" }
  if (-not [string]::IsNullOrWhiteSpace($resolvedSelectedConcept)) { Write-Host "REQUIREMENTS_SELECTED_CONCEPT=$resolvedSelectedConcept" }
  if (-not [string]::IsNullOrWhiteSpace($resolvedUiSkin)) { Write-Host "REQUIREMENTS_UI_SKIN=$resolvedUiSkin" }
  Write-Host "REQUIREMENTS_STAGE=$Stage"
  Write-Host "REQUIREMENTS_STATUS=$Status"
}
