param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Tag,
  [Parameter(Mandatory=$false)][string]$AssignProject = "",
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/audio",
  [Parameter(Mandatory=$false)][string]$MaxKb = "1000"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ps = Join-Path $scriptDir "fetch_audio.ps1"

if (-not (Test-Path -LiteralPath $ps)) {
  throw "Missing script: $ps"
}

& $ps -GameId $GameId -Type "bgm" -Role "play" -Tag $Tag -AssignProject $AssignProject -LibraryRoot $LibraryRoot -MaxKb $MaxKb
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "BGM_FETCH_OK=true"
