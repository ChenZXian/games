[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,
  [string]$Project = "",
  [Parameter(Mandatory=$true)]
  [string]$Theme,
  [string]$GameType = "",
  [string]$ArtRoles = "",
  [string]$StyleTags = "",
  [string]$QualityTarget = "production",
  [string]$PackId = "",
  [int]$MaxPacks = 3,
  [string]$LibraryRoot = "shared_assets/game_art",
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "ensure_game_art_pack.py"
$argsList = @(
  "--game-id", $GameId,
  "--theme", $Theme,
  "--quality-target", $QualityTarget,
  "--max-packs", "$MaxPacks",
  "--library-root", $LibraryRoot
)
if ($Project) { $argsList += @("--project", $Project) }
if ($GameType) { $argsList += @("--game-type", $GameType) }
if ($ArtRoles) { $argsList += @("--art-roles", $ArtRoles) }
if ($StyleTags) { $argsList += @("--style-tags", $StyleTags) }
if ($PackId) { $argsList += @("--pack-id", $PackId) }
if ($DryRun) { $argsList += "--dry-run" }
python $script @argsList
