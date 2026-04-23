param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [Parameter(Mandatory=$true)][string]$Theme,
  [Parameter(Mandatory=$false)][string]$Project = "",
  [Parameter(Mandatory=$false)][string]$Mood = "",
  [Parameter(Mandatory=$false)][string]$Pacing = "",
  [Parameter(Mandatory=$false)][string]$StyleTags = "",
  [Parameter(Mandatory=$false)][string]$BgmRoles = "",
  [Parameter(Mandatory=$false)][string]$SfxRoles = "",
  [Parameter(Mandatory=$false)][int]$BgmSeconds = 24,
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/audio",
  [switch]$DryRun,
  [switch]$NoFetch,
  [switch]$NoSynth
)

$ErrorActionPreference = "Stop"

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "ensure_audio_bundle.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @(
  $py,
  "--game-id", $GameId,
  "--theme", $Theme,
  "--library-root", $LibraryRoot,
  "--bgm-seconds", "$BgmSeconds"
)

if ($Project -ne "") {
  $argsList += @("--project", $Project)
}
if ($Mood -ne "") {
  $argsList += @("--mood", $Mood)
}
if ($Pacing -ne "") {
  $argsList += @("--pacing", $Pacing)
}
if ($StyleTags -ne "") {
  $argsList += @("--style-tags", $StyleTags)
}
if ($BgmRoles -ne "") {
  $argsList += @("--bgm-roles", $BgmRoles)
}
if ($SfxRoles -ne "") {
  $argsList += @("--sfx-roles", $SfxRoles)
}
if ($DryRun) {
  $argsList += "--dry-run"
}
if ($NoFetch) {
  $argsList += "--no-fetch"
}
if ($NoSynth) {
  $argsList += "--no-synth"
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "AUDIO_BUNDLE_OK=true"
