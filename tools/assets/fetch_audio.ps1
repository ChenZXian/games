param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Type,
  [Parameter(Mandatory=$true)][string]$Tag,
  [Parameter(Mandatory=$false)][string]$Role = "",
  [Parameter(Mandatory=$false)][string]$AssignProject = "",
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/audio",
  [Parameter(Mandatory=$false)][string]$MaxKb = "1000"
)

$ErrorActionPreference = "Stop"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "fetch_audio.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @(
  $py,
  "--game-id", $GameId,
  "--type", $Type,
  "--tag", $Tag,
  "--library-root", $LibraryRoot,
  "--max-kb", $MaxKb
)

if ($Role -ne "") {
  $argsList += @("--role", $Role)
}
if ($AssignProject -ne "") {
  $argsList += @("--assign-project", $AssignProject)
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "AUDIO_FETCH_OK=true"
