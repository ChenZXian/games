param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Theme,
  [Parameter(Mandatory=$false)][string]$Project = "",
  [Parameter(Mandatory=$false)][string]$UiSkin = "",
  [Parameter(Mandatory=$false)][string]$StyleTags = "",
  [Parameter(Mandatory=$false)][string]$Scope = "",
  [Parameter(Mandatory=$false)][string]$QualityTarget = "production",
  [Parameter(Mandatory=$false)][string]$PackId = "",
  [Parameter(Mandatory=$false)][string]$Preset = "",
  [Parameter(Mandatory=$false)][string]$ImportSource = "",
  [Parameter(Mandatory=$false)][string]$DownloadUrl = "",
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/ui",
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "ensure_ui_pack.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @(
  $py,
  "--game-id", $GameId,
  "--theme", $Theme,
  "--quality-target", $QualityTarget,
  "--library-root", $LibraryRoot
)

if ($Project -ne "") {
  $argsList += @("--project", $Project)
}
if ($UiSkin -ne "") {
  $argsList += @("--ui-skin", $UiSkin)
}
if ($StyleTags -ne "") {
  $argsList += @("--style-tags", $StyleTags)
}
if ($Scope -ne "") {
  $argsList += @("--scope", $Scope)
}
if ($PackId -ne "") {
  $argsList += @("--pack-id", $PackId)
}
if ($Preset -ne "") {
  $argsList += @("--preset", $Preset)
}
if ($ImportSource -ne "") {
  $argsList += @("--import-source", $ImportSource)
}
if ($DownloadUrl -ne "") {
  $argsList += @("--download-url", $DownloadUrl)
}
if ($DryRun) {
  $argsList += "--dry-run"
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "UI_ENSURE_OK=true"
