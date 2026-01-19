param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Tag,
  [Parameter(Mandatory=$false)][string]$AssignProject = "",
  [Parameter(Mandatory=$false)][string]$MaxKb = "1000"
)

$ErrorActionPreference = "Stop"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "fetch_bgm.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

& python $py --game-id $GameId --tag $Tag --assign-project $AssignProject --max-kb $MaxKb
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "BGM_FETCH_OK=true"
