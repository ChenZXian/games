[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$PackId,
  [Parameter(Mandatory=$true)]
  [string]$Title,
  [string]$SourcePath = "",
  [string]$DownloadUrl = "",
  [string]$DownloadPageUrl = "",
  [Parameter(Mandatory=$true)]
  [string]$SourceUrl,
  [string]$LibraryRoot = "shared_assets/game_art",
  [string]$License = "CC0-1.0",
  [string]$LicenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/",
  [string]$ArtRoles = "",
  [string]$GameTypes = "",
  [string]$StyleTags = "",
  [string]$QualityTags = "production,free,open,cc0",
  [switch]$Replace
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "import_game_art_pack.py"
$argsList = @(
  "--pack-id", $PackId,
  "--title", $Title,
  "--source-url", $SourceUrl,
  "--library-root", $LibraryRoot,
  "--license", $License,
  "--license-url", $LicenseUrl,
  "--art-roles", $ArtRoles,
  "--game-types", $GameTypes,
  "--style-tags", $StyleTags,
  "--quality-tags", $QualityTags
)
if ($SourcePath) { $argsList += @("--source-path", $SourcePath) }
if ($DownloadUrl) { $argsList += @("--download-url", $DownloadUrl) }
if ($DownloadPageUrl) { $argsList += @("--download-page-url", $DownloadPageUrl) }
if ($Replace) { $argsList += "--replace" }
python $script @argsList
