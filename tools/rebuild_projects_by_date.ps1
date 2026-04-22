[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

function Get-RepoRoot {
  $dir = (Get-Location).Path
  for ($i = 0; $i -lt 12; $i++) {
    if ((Test-Path (Join-Path $dir "docs")) -and
        (Test-Path (Join-Path $dir "registry")) -and
        (Test-Path (Join-Path $dir "games")) -and
        (Test-Path (Join-Path $dir "tools"))) {
      return $dir
    }
    $parent = Split-Path $dir -Parent
    if ($parent -eq $dir) { break }
    $dir = $parent
  }
  throw "Repo root not found."
}

function Write-Utf8File($path, $content) {
  $dir = Split-Path $path -Parent
  if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
  }
  [System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
}

$root = Get-RepoRoot
Set-Location $root

$registryPath = Join-Path $root "registry\produced_games.json"
$outputPath = Join-Path $root "registry\projects_by_date.json"

if (!(Test-Path $registryPath)) {
  throw "Missing registry/produced_games.json"
}

$registry = Get-Content -LiteralPath $registryPath -Raw | ConvertFrom-Json
$games = @()
if ($null -ne $registry.games) {
  $games = @($registry.games)
}

$groupMap = @{}
$unknown = @()

foreach ($game in $games) {
  $gameId = if ($null -ne $game.id) { [string]$game.id } else { "" }
  $createdAt = if ($null -ne $game.created_at) { [string]$game.created_at } else { "" }
  $entry = [ordered]@{
    id = $gameId
    name = if ($null -ne $game.name) { [string]$game.name } else { "" }
    created_at = $createdAt
    path = if ([string]::IsNullOrWhiteSpace($gameId)) { "" } else { "games/$gameId" }
    ui_skin = if ($null -ne $game.ui_skin) { [string]$game.ui_skin } else { "" }
  }

  if ($createdAt -match '^\d{4}-\d{2}') {
    $month = $createdAt.Substring(0, 7)
    if (-not $groupMap.ContainsKey($month)) {
      $groupMap[$month] = @()
    }
    $groupMap[$month] += $entry
  }
  else {
    $unknown += $entry
  }
}

$months = @()
foreach ($month in ($groupMap.Keys | Sort-Object -Descending)) {
  $months += [ordered]@{
    month = $month
    total = @($groupMap[$month]).Count
    games = @($groupMap[$month] | Sort-Object created_at, id -Descending)
  }
}

$data = [ordered]@{
  version = 1
  generated_at_utc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  grouping = "created_at_month"
  path_policy = "active projects remain direct children under games/<game_id>"
  months = $months
  unknown_created_at = @($unknown | Sort-Object id)
}

$json = $data | ConvertTo-Json -Depth 6
Write-Utf8File $outputPath ($json + [Environment]::NewLine)

Write-Host "PROJECT_DATE_INDEX=$outputPath"
Write-Host "PROJECT_DATE_GROUPS=$($months.Count)"
Write-Host "PROJECT_DATE_UNKNOWN=$(@($unknown).Count)"
