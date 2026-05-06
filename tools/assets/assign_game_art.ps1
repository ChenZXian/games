[CmdletBinding()]
param(
  [string]$Project = "",
  [string]$GameId = "",
  [string]$PackId = "",
  [string]$Theme = "",
  [string]$GameType = "",
  [string]$ArtRoles = "",
  [string]$StyleTags = "",
  [string]$LibraryRoot = "shared_assets/game_art",
  [switch]$ListPacks,
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "assign_game_art.py"
$argsList = @("--library-root", $LibraryRoot)
if ($Project) { $argsList += @("--project", $Project) }
if ($GameId) { $argsList += @("--game-id", $GameId) }
if ($PackId) { $argsList += @("--pack-id", $PackId) }
if ($Theme) { $argsList += @("--theme", $Theme) }
if ($GameType) { $argsList += @("--game-type", $GameType) }
if ($ArtRoles) { $argsList += @("--art-roles", $ArtRoles) }
if ($StyleTags) { $argsList += @("--style-tags", $StyleTags) }
if ($ListPacks) { $argsList += "--list-packs" }
if ($DryRun) { $argsList += "--dry-run" }
python $script @argsList
