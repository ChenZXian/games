param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [string]$Tag = "default",
  [string]$LibraryRoot = "shared_assets/audio",
  [int]$Seconds = 16,
  [int]$SampleRate = 44100,
  [string]$AssignProject = ""
)
$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ps = Join-Path $scriptDir "synth_audio.ps1"
if (-not (Test-Path -LiteralPath $ps)) {
  throw "Missing script: $ps"
}
& $ps -GameId $GameId -Type "bgm" -Role "play" -Tag $Tag -LibraryRoot $LibraryRoot -Seconds $Seconds -SampleRate $SampleRate -AssignProject $AssignProject
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "BGM_SYNTH_OK=true"
