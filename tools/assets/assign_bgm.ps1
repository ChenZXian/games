param(
  [Parameter(Mandatory=$true)][string]$Project,
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$false)][string]$BgmId = "",
  [Parameter(Mandatory=$false)][string]$Tag = "",
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/audio"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Project)) {
  throw "Project path not found: $Project"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ps = Join-Path $scriptDir "assign_audio.ps1"

if (-not (Test-Path -LiteralPath $ps)) {
  throw "Missing script: $ps"
}

& $ps -Project $Project -GameId $GameId -Type "bgm" -Role "play" -AudioId $BgmId -Tag $Tag -LibraryRoot $LibraryRoot
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "BGM_ASSIGN_OK=true"
