param(
  [Parameter(Mandatory=$true)][string]$Project,
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Type,
  [Parameter(Mandatory=$false)][string]$Role = "",
  [Parameter(Mandatory=$false)][string]$AudioId = "",
  [Parameter(Mandatory=$false)][string]$Tag = "",
  [Parameter(Mandatory=$false)][string]$StyleTags = "",
  [Parameter(Mandatory=$false)][int]$MinScore = 0,
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/audio"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Project)) {
  throw "Project path not found: $Project"
}

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "assign_audio.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @(
  $py,
  "--project", $Project,
  "--game-id", $GameId,
  "--type", $Type,
  "--library-root", $LibraryRoot
)

if ($Role -ne "") {
  $argsList += @("--role", $Role)
}
if ($AudioId -ne "") {
  $argsList += @("--audio-id", $AudioId)
}
if ($Tag -ne "") {
  $argsList += @("--tag", $Tag)
}
if ($StyleTags -ne "") {
  $argsList += @("--style-tags", $StyleTags)
}
if ($MinScore -gt 0) {
  $argsList += @("--min-score", "$MinScore")
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "AUDIO_ASSIGN_OK=true"
