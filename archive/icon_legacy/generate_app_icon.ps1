param(
  [Parameter(Mandatory=$true)][string]$Project,
  [Parameter(Mandatory=$false)][string]$GameId = "",
  [Parameter(Mandatory=$false)][string]$Seed = "",
  [Parameter(Mandatory=$false)][string]$Background = "#101820"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Project)) {
  throw "Project path not found: $Project"
}

if ([string]::IsNullOrWhiteSpace($Seed) -and [string]::IsNullOrWhiteSpace($GameId)) {
  throw "Either -GameId or -Seed must be provided"
}

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "icon_resize.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$finalSeed = $Seed
if ([string]::IsNullOrWhiteSpace($finalSeed)) {
  $finalSeed = $GameId
}

& python $py --project $Project --seed $finalSeed --background $Background
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "ICON_GENERATION_OK=true"
