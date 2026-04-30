[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$Project,

  [string]$GameId = "",

  [string]$TemplateRoot = "templates/base_mini_game"
)

$ErrorActionPreference = "Stop"

function Get-RepoRoot([string]$StartPath) {
  $current = (Resolve-Path -LiteralPath $StartPath).Path
  for ($i = 0; $i -lt 12; $i++) {
    if ((Test-Path -LiteralPath (Join-Path $current "docs")) -and
        (Test-Path -LiteralPath (Join-Path $current "registry")) -and
        (Test-Path -LiteralPath (Join-Path $current "games")) -and
        (Test-Path -LiteralPath (Join-Path $current "tools"))) {
      return $current
    }
    $parent = Split-Path -Parent $current
    if (-not $parent -or $parent -eq $current) { break }
    $current = $parent
  }
  throw "Repo root not found from: $StartPath"
}

function Read-JsonObject([string]$PathValue) {
  if (-not (Test-Path -LiteralPath $PathValue)) { return $null }
  try {
    return Get-Content -LiteralPath $PathValue -Raw | ConvertFrom-Json
  }
  catch {
    return $null
  }
}

function Add-Finding([System.Collections.Generic.List[object]]$List, [string]$Severity, [string]$Code, [string]$PathValue, [string]$Detail) {
  $List.Add([pscustomobject]@{
    severity = $Severity
    code = $Code
    path = $PathValue
    detail = $Detail
  }) | Out-Null
}

function Get-Text([string]$PathValue) {
  if (-not (Test-Path -LiteralPath $PathValue)) { return "" }
  return [System.IO.File]::ReadAllText($PathValue)
}

if (-not (Test-Path -LiteralPath $Project)) {
  throw "Project path not found: $Project"
}

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$repoRoot = Get-RepoRoot $projectPath
if ([string]::IsNullOrWhiteSpace($GameId)) {
  $GameId = Split-Path -Leaf $projectPath
}

$findings = New-Object 'System.Collections.Generic.List[object]'
$templatePath = Join-Path $repoRoot $TemplateRoot
$templateContractPath = Join-Path $templatePath "identity_contract.json"
$templateContract = Read-JsonObject $templateContractPath

$registryPath = Join-Path $repoRoot "registry/produced_games.json"
$registry = Read-JsonObject $registryPath
if ($null -eq $registry) {
  throw "Registry JSON is missing or invalid: $registryPath"
}

$otherGameIds = @()
if ($null -ne $registry.games) {
  $otherGameIds = @($registry.games | ForEach-Object { [string]$_.id } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_ -ne $GameId })
}

$requirementsDir = Join-Path $repoRoot ("artifacts/requirements/" + $GameId)
$requirementsMetadataPath = Join-Path $requirementsDir "metadata.json"
$gameplayPath = Join-Path $requirementsDir "gameplay_diversity.json"
$visualIdentityPath = Join-Path $requirementsDir "visual_identity.json"
$iconMetadataPath = Join-Path $repoRoot ("artifacts/icons/" + $GameId + "/metadata.json")
$stringsPath = Join-Path $projectPath "app/src/main/res/values/strings.xml"
$uiAssignmentPath = Join-Path $projectPath "app/src/main/assets/ui/ui_pack_assignment.json"
$gameArtAssignmentPath = Join-Path $projectPath "app/src/main/assets/game_art/game_art_assignment.json"
$runtimeArtMapPath = Join-Path $projectPath "app/src/main/assets/game_art/runtime_art_map.json"
$audioAssignmentPath = Join-Path $projectPath "app/src/main/assets/audio/audio_assignment.json"

$requirementsMetadata = Read-JsonObject $requirementsMetadataPath
if ($null -eq $requirementsMetadata) {
  Add-Finding $findings "error" "requirements_missing" $requirementsMetadataPath "Requirements metadata is missing or invalid."
} else {
  if ([string]$requirementsMetadata.game_id -ne $GameId) {
    Add-Finding $findings "error" "requirements_game_id_mismatch" $requirementsMetadataPath "Requirements metadata game_id does not match the project game id."
  }
  if ([string]$requirementsMetadata.status -ne "confirmed") {
    Add-Finding $findings "error" "requirements_not_confirmed" $requirementsMetadataPath "Requirements metadata must be confirmed before initialization is treated as identity-clean."
  }
}

$gameplayContract = Read-JsonObject $gameplayPath
if ($null -eq $gameplayContract) {
  Add-Finding $findings "error" "gameplay_contract_missing" $gameplayPath "Gameplay diversity contract is missing or invalid."
} elseif ([string]$gameplayContract.game_id -ne $GameId) {
  Add-Finding $findings "error" "gameplay_contract_mismatch" $gameplayPath "Gameplay diversity contract game_id does not match the project game id."
}

$visualIdentity = Read-JsonObject $visualIdentityPath
if ($null -eq $visualIdentity) {
  Add-Finding $findings "error" "visual_identity_missing" $visualIdentityPath "Visual identity contract is missing or invalid."
} elseif ([string]$visualIdentity.game_id -ne $GameId) {
  Add-Finding $findings "error" "visual_identity_mismatch" $visualIdentityPath "Visual identity contract game_id does not match the project game id."
}

$stringsText = Get-Text $stringsPath
if ([string]::IsNullOrWhiteSpace($stringsText)) {
  Add-Finding $findings "error" "strings_missing" $stringsPath "strings.xml is missing or empty."
}

$templateMarkers = @("__GAME_ID__", "__APP_NAME__", "__SELECTED_CONCEPT__", "__UI_SKIN__", "template_game", "base_mini_game", "replace_me")
if ($null -ne $templateContract -and $null -ne $templateContract.forbidden_markers) {
  $templateMarkers = @($templateContract.forbidden_markers | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

$keyFiles = @(
  $stringsPath,
  $requirementsMetadataPath,
  $gameplayPath,
  $visualIdentityPath,
  $uiAssignmentPath,
  $gameArtAssignmentPath,
  $runtimeArtMapPath,
  $audioAssignmentPath
) | Where-Object { Test-Path -LiteralPath $_ }

foreach ($file in $keyFiles) {
  $text = Get-Text $file
  foreach ($marker in $templateMarkers) {
    if ($text -match [regex]::Escape($marker)) {
      Add-Finding $findings "error" "template_marker_present" $file ("Found unresolved template marker '" + $marker + "'.")
    }
  }
  foreach ($otherGameId in $otherGameIds) {
    if ($text -match [regex]::Escape($otherGameId)) {
      Add-Finding $findings "error" "foreign_game_id_reference" $file ("Found foreign game id reference '" + $otherGameId + "'.")
    }
  }
}

$iconMetadata = Read-JsonObject $iconMetadataPath
if ($null -eq $iconMetadata) {
  Add-Finding $findings "error" "icon_metadata_missing" $iconMetadataPath "Icon metadata is missing or invalid."
} else {
  if ([string]$iconMetadata.game_id -ne $GameId) {
    Add-Finding $findings "error" "icon_game_id_mismatch" $iconMetadataPath "Icon metadata game_id does not match the project game id."
  }
  if ([string]::IsNullOrWhiteSpace([string]$iconMetadata.icon_subject)) {
    Add-Finding $findings "error" "icon_subject_missing" $iconMetadataPath "Icon metadata must declare icon_subject."
  }
  if ([string]$iconMetadata.icon_duplicate_risk -eq "high") {
    Add-Finding $findings "error" "icon_duplicate_risk_high" $iconMetadataPath "Icon duplicate risk is high."
  }
}

$uiAssignment = Read-JsonObject $uiAssignmentPath
if ($null -ne $uiAssignment) {
  if ([string]$uiAssignment.game_id -ne $GameId) {
    Add-Finding $findings "error" "ui_assignment_mismatch" $uiAssignmentPath "UI assignment game_id does not match the project game id."
  }
  if ($null -ne $requirementsMetadata -and -not [string]::IsNullOrWhiteSpace([string]$requirementsMetadata.ui_skin) -and [string]$uiAssignment.ui_skin -ne [string]$requirementsMetadata.ui_skin) {
    Add-Finding $findings "error" "ui_skin_mismatch" $uiAssignmentPath "UI assignment ui_skin does not match the confirmed requirements metadata."
  }
}

$gameArtAssignment = Read-JsonObject $gameArtAssignmentPath
if ($null -ne $gameArtAssignment -and [string]$gameArtAssignment.game_id -ne $GameId) {
  Add-Finding $findings "error" "game_art_assignment_mismatch" $gameArtAssignmentPath "Game art assignment game_id does not match the project game id."
}

$runtimeArtMap = Read-JsonObject $runtimeArtMapPath
if ($null -ne $runtimeArtMap -and [string]$runtimeArtMap.game_id -ne $GameId) {
  Add-Finding $findings "error" "runtime_art_map_mismatch" $runtimeArtMapPath "Runtime art map game_id does not match the project game id."
}

$audioAssignment = Read-JsonObject $audioAssignmentPath
if ($null -ne $audioAssignment -and [string]$audioAssignment.game_id -ne $GameId) {
  Add-Finding $findings "error" "audio_assignment_mismatch" $audioAssignmentPath "Audio assignment game_id does not match the project game id."
}

$requiredPaths = @()
if ($null -ne $templateContract -and $null -ne $templateContract.required_paths) {
  $requiredPaths = @($templateContract.required_paths | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}
foreach ($relativePath in $requiredPaths) {
  $fullPath = Join-Path $projectPath $relativePath
  if (-not (Test-Path -LiteralPath $fullPath)) {
    Add-Finding $findings "warning" "required_identity_file_missing" $fullPath ("Missing expected identity rewrite target '" + $relativePath + "'.")
  }
}

$errorCount = @($findings | Where-Object { $_.severity -eq "error" }).Count
$warningCount = @($findings | Where-Object { $_.severity -eq "warning" }).Count
$ok = $errorCount -eq 0

Write-Host ("IDENTITY_RESIDUE_OK=" + ($(if ($ok) { "true" } else { "false" })))
Write-Host ("IDENTITY_RESIDUE_ERROR_COUNT=" + $errorCount)
Write-Host ("IDENTITY_RESIDUE_WARNING_COUNT=" + $warningCount)
Write-Host ("IDENTITY_TEMPLATE_ROOT=" + $templatePath)
Write-Host ("IDENTITY_PROJECT=" + $projectPath)
Write-Host ("IDENTITY_GAME_ID=" + $GameId)

if ($findings.Count -gt 0) {
  $findings | ConvertTo-Json -Depth 4
}
