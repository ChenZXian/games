param(
  [Parameter(Mandatory=$true)][string]$Project,
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$false)][string]$BgmId = "",
  [Parameter(Mandatory=$false)][string]$Tag = ""
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
$py = Join-Path $scriptDir "assign_bgm.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

& python $py --project $Project --game-id $GameId --bgm-id $BgmId --tag $Tag
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "BGM_ASSIGN_OK=true"
