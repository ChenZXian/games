param(
  [Parameter(Mandatory=$false)][string]$Project = "",
  [Parameter(Mandatory=$false)][string]$GameId = "",
  [Parameter(Mandatory=$false)][string]$PackId = "",
  [Parameter(Mandatory=$false)][string]$Preset = "",
  [Parameter(Mandatory=$false)][string]$UiSkin = "",
  [Parameter(Mandatory=$false)][string]$StyleTags = "",
  [Parameter(Mandatory=$false)][int]$MinScore = 0,
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/ui",
  [switch]$ListPacks,
  [switch]$ListPresets,
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ($ListPacks -and $ListPresets) {
  throw "Choose either -ListPacks or -ListPresets, not both"
}

if (-not $ListPacks -and -not $ListPresets -and -not (Test-Path -LiteralPath $Project)) {
  throw "Project path not found: $Project"
}

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "assign_ui.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @($py, "--library-root", $LibraryRoot)

if ($ListPacks) {
  $argsList += "--list-packs"
}
elseif ($ListPresets) {
  if ($PackId -eq "") {
    throw "PackId is required with -ListPresets"
  }
  $argsList += @("--list-presets", "--pack-id", $PackId)
}
else {
  if ($Project -eq "") {
    throw "Project is required when applying a UI pack"
  }
  if ($GameId -eq "") {
    throw "GameId is required when applying a UI pack"
  }
  if ($PackId -eq "") {
    if ($StyleTags -eq "") {
      throw "PackId is required when applying a UI pack unless StyleTags is provided for automatic matching"
    }
  }
  $argsList += @(
    "--project", $Project,
    "--game-id", $GameId
  )
  if ($PackId -ne "") {
    $argsList += @("--pack-id", $PackId)
  }
}

if ($Preset -ne "") {
  $argsList += @("--preset", $Preset)
}
if ($UiSkin -ne "") {
  $argsList += @("--ui-skin", $UiSkin)
}
if ($StyleTags -ne "") {
  $argsList += @("--style-tags", $StyleTags)
}
if ($MinScore -gt 0) {
  $argsList += @("--min-score", "$MinScore")
}
if ($DryRun) {
  $argsList += "--dry-run"
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $ListPacks -and -not $ListPresets) {
  Write-Host "UI_ASSIGN_OK=true"
}
