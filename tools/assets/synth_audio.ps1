param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Type,
  [Parameter(Mandatory=$false)][string]$Role = "",
  [Parameter(Mandatory=$false)][string]$Tag = "default",
  [Parameter(Mandatory=$false)][string]$StyleTags = "",
  [Parameter(Mandatory=$false)][string]$Mood = "",
  [Parameter(Mandatory=$false)][string]$Pacing = "",
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/audio",
  [Parameter(Mandatory=$false)][int]$Seconds = 16,
  [Parameter(Mandatory=$false)][int]$SampleRate = 44100,
  [Parameter(Mandatory=$false)][string]$AssignProject = ""
)

$ErrorActionPreference = "Stop"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "synth_audio.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @(
  $py,
  "--game-id", $GameId,
  "--type", $Type,
  "--tag", $Tag,
  "--library-root", $LibraryRoot,
  "--seconds", "$Seconds",
  "--sample-rate", "$SampleRate"
)

if ($Role -ne "") {
  $argsList += @("--role", $Role)
}
if ($StyleTags -ne "") {
  $argsList += @("--style-tags", $StyleTags)
}
if ($Mood -ne "") {
  $argsList += @("--mood", $Mood)
}
if ($Pacing -ne "") {
  $argsList += @("--pacing", $Pacing)
}
if ($AssignProject -ne "") {
  $argsList += @("--assign-project", $AssignProject)
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "AUDIO_SYNTH_OK=true"
