# tools/validate.ps1
# Monorepo validator for generated Android game projects.
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/<new_game_id>
#   powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -All
# Exit code:
#   0 pass, 2 fail

[CmdletBinding()]
param(
  [string]$Project = "",
  [switch]$All
)

$ErrorActionPreference = "Stop"

function Write-Ok($m){ Write-Host "[OK]  $m" }
function Write-Warn($m){ Write-Host "[WARN] $m" }
function Write-Fail($m){ Write-Host "[FAIL] $m" }

function Get-RepoRoot {
  $dir = (Get-Location).Path
  for ($i=0; $i -lt 12; $i++) {
    if ((Test-Path (Join-Path $dir "docs")) -and
        (Test-Path (Join-Path $dir "registry")) -and
        (Test-Path (Join-Path $dir "games")) -and
        (Test-Path (Join-Path $dir "tools"))) { return $dir }
    $parent = Split-Path $dir -Parent
    if ($parent -eq $dir) { break }
    $dir = $parent
  }
  throw "Repo root not found."
}

function Discover-Projects($root){
  $list = @()
  $gamesDir = Join-Path $root "games"
  if (!(Test-Path $gamesDir)) { return $list }
  foreach ($d in (Get-ChildItem -Path $gamesDir -Directory -ErrorAction SilentlyContinue)) {
    $hasSettings = (Test-Path (Join-Path $d.FullName "settings.gradle")) -or (Test-Path (Join-Path $d.FullName "settings.gradle.kts"))
    $hasApp = Test-Path (Join-Path $d.FullName "app")
    if ($hasSettings -and $hasApp) { $list += $d.FullName }
  }
  return $list
}

function Read-Text($path){
  if (!(Test-Path $path)) { return $null }
  return Get-Content -LiteralPath $path -Raw
}

function Match-First($text, $pattern){
  if ($null -eq $text) { return $null }
  $m = [regex]::Match($text, $pattern, "IgnoreCase")
  if ($m.Success) { return $m.Groups[1].Value }
  return $null
}

function Has-NonAscii($filePath){
  $bytes = [System.IO.File]::ReadAllBytes($filePath)
  foreach ($b in $bytes) { if ($b -gt 127) { return $true } }
  return $false
}

function Add-FilesByFilter([System.Collections.Generic.List[string]]$out, $path, $filter){
  if (Test-Path $path) {
    foreach ($f in (Get-ChildItem -Path $path -Recurse -File -Filter $filter -ErrorAction SilentlyContinue)) {
      $out.Add($f.FullName) | Out-Null
    }
  }
}

function Validate-Project($projDir, [ref]$fails, [ref]$warns){
  Write-Host ""
  Write-Host "=== Validate: $projDir ==="

  $root = Get-RepoRoot

  # 1) Path sanity (monorepo-aware)
  $gamesRoot = Join-Path $root "games"
  $expectedPrefix = (Resolve-Path $gamesRoot).Path + "\"
  $projResolved = (Resolve-Path $projDir).Path

  if (-not ($projResolved.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase))) {
    $fails.Value++
    Write-Fail "Project is not under repo_root/games/. Got: $projResolved"
    return
  }

  $parent = Split-Path $projResolved -Parent
  if (-not ((Split-Path $parent -Leaf) -ieq "games")) {
    $fails.Value++
    Write-Fail "Project root must be a direct child of repo_root/games/. Got: $projResolved"
    return
  }

  $nestedGamesDir = Join-Path $projResolved "games"
  if (Test-Path $nestedGamesDir) {
    $fails.Value++
    Write-Fail "Nested games directory detected inside project: $nestedGamesDir"
    return
  }

  # 2) Must have wrapper + app module
  if (!(Test-Path (Join-Path $projResolved "gradlew.bat"))) { $fails.Value++; Write-Fail "Missing gradlew.bat"; }
  if (!(Test-Path (Join-Path $projResolved "gradle\wrapper\gradle-wrapper.properties"))) { $fails.Value++; Write-Fail "Missing gradle wrapper properties"; }
  if (!(Test-Path (Join-Path $projResolved "app\build.gradle")) -and !(Test-Path (Join-Path $projResolved "app\build.gradle.kts"))) { $fails.Value++; Write-Fail "Missing app/build.gradle(.kts)"; }
  if (!(Test-Path (Join-Path $projResolved "app\src\main\AndroidManifest.xml"))) { $fails.Value++; Write-Fail "Missing app/src/main/AndroidManifest.xml"; }

  # 3) GAME_GENERATION_STANDARD presence (repo-level)
  $std = Join-Path $root "docs\GAME_GENERATION_STANDARD.md"
  if (!(Test-Path $std)) { $fails.Value++; Write-Fail "Missing docs/GAME_GENERATION_STANDARD.md at repo root"; }

  # 4) Enforce launch activity name + icon/label policy
  $requiredLauncher = "com.android.boot.MainActivity"
  $manifestPath = Join-Path $projResolved "app\src\main\AndroidManifest.xml"
  if (Test-Path $manifestPath) {
    $man = Read-Text $manifestPath
    if ($null -ne $man) {
      if ($man -notmatch 'android:name\s*=\s*"com\.android\.boot\.MainActivity"' -and
          $man -notmatch 'android:name\s*=\s*"\.MainActivity"') {
        $fails.Value++
        Write-Fail "Launcher Activity mismatch. Required: $requiredLauncher (check AndroidManifest.xml)"
      } else {
        Write-Ok "Launcher Activity looks OK"
      }

      if ($man -notmatch 'android:label\s*=\s*"@string/app_name"') { $fails.Value++; Write-Fail "Manifest label must be @string/app_name"; }
      else { Write-Ok "Manifest label OK" }

      if ($man -notmatch 'android:icon\s*=\s*"@mipmap/app_icon"') { $fails.Value++; Write-Fail "Manifest icon must be @mipmap/app_icon"; }
      else { Write-Ok "Manifest icon OK" }
    }
  }

  # 5) No Chinese / non-ASCII in AUTHORED files (WHITELIST ONLY)
  # This intentionally avoids scanning any build outputs (app/build, build, intermediates, etc.)
  $filesToScan = New-Object 'System.Collections.Generic.List[string]'

  # Source code
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "app\src\main\java") "*.java"
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "app\src\main\kotlin") "*.kt"

  # Manifest + selected resource XML
  if (Test-Path $manifestPath) { $filesToScan.Add($manifestPath) | Out-Null }

  Add-FilesByFilter $filesToScan (Join-Path $projResolved "app\src\main\res\values") "*.xml"
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "app\src\main\res\layout") "*.xml"
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "app\src\main\res\xml") "*.xml"

  # Gradle/config files at project root
  $rootCandidates = @(
    (Join-Path $projResolved "build.gradle"),
    (Join-Path $projResolved "build.gradle.kts"),
    (Join-Path $projResolved "settings.gradle"),
    (Join-Path $projResolved "settings.gradle.kts"),
    (Join-Path $projResolved "gradle.properties"),
    (Join-Path $projResolved "local.properties")
  )
  foreach ($p in $rootCandidates) { if (Test-Path $p) { $filesToScan.Add($p) | Out-Null } }

  # Gradle scripts under gradle/
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "gradle") "*.gradle"
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "gradle") "*.kts"
  Add-FilesByFilter $filesToScan (Join-Path $projResolved "gradle") "*.properties"

  $filesToScan = $filesToScan | Sort-Object -Unique

  $badFiles = @()
  foreach ($fp in $filesToScan) {
    if (Has-NonAscii $fp) { $badFiles += $fp }
  }

  if ($badFiles.Count -gt 0) {
    $fails.Value++
    Write-Fail "Non-ASCII characters found in authored files. Example:`n  $($badFiles[0])"
    if ($badFiles.Count -gt 1) { Write-Warn "Total files with non-ASCII: $($badFiles.Count)" }
  } else {
    Write-Ok "No non-ASCII chars found in authored files"
  }

  # 6) Android resources: mipmap/app_icon must exist (at least one density)
  $resDir = Join-Path $projResolved "app\src\main\res"
  $iconFound = $false
  if (Test-Path $resDir) {
    $icons = Get-ChildItem -Path $resDir -Recurse -File -Filter "app_icon.*" -ErrorAction SilentlyContinue
    if ($icons.Count -gt 0) { $iconFound = $true }
  }
  if (!$iconFound) { $fails.Value++; Write-Fail "Missing @mipmap/app_icon resources" } else { Write-Ok "App icon resources found" }

  # 7) Baseline alignment quick checks (compileSdk/minSdk/targetSdk)
  $appGradle = Join-Path $projResolved "app\build.gradle"
  $appGradleKts = Join-Path $projResolved "app\build.gradle.kts"
  $appText = $null
  if (Test-Path $appGradle) { $appText = Read-Text $appGradle }
  elseif (Test-Path $appGradleKts) { $appText = Read-Text $appGradleKts }

  if ($null -ne $appText) {
    $cs = Match-First $appText 'compileSdk\s+([0-9]+)'
    $mins = Match-First $appText 'minSdk\s+([0-9]+)'
    $ts = Match-First $appText 'targetSdk\s+([0-9]+)'

    if ($cs -ne "34") { $fails.Value++; Write-Fail "compileSdk must be 34 (got $cs)" } else { Write-Ok "compileSdk=34" }
    if ($mins -ne "24") { $fails.Value++; Write-Fail "minSdk must be 24 (got $mins)" } else { Write-Ok "minSdk=24" }
    if ($ts -ne "34") { $fails.Value++; Write-Fail "targetSdk must be 34 (got $ts)" } else { Write-Ok "targetSdk=34" }
  } else {
    $warns.Value++
    Write-Warn "Cannot read app/build.gradle(.kts) to verify SDK levels"
  }
}

$root = Get-RepoRoot
Set-Location $root

$fails = 0
$warns = 0

if ($All) {
  $projects = Discover-Projects $root
  if ($projects.Count -eq 0) { Write-Fail "No projects found under games/*"; exit 2 }
  foreach ($p in $projects) { Validate-Project $p ([ref]$fails) ([ref]$warns) }
} else {
  if ([string]::IsNullOrWhiteSpace($Project)) {
    Write-Fail "Specify -Project games/<id> or use -All"
    exit 2
  }
  $projDir = Join-Path $root $Project
  if (!(Test-Path $projDir)) { Write-Fail "Project path not found: $projDir"; exit 2 }
  Validate-Project $projDir ([ref]$fails) ([ref]$warns)
}

Write-Host ""
Write-Host "=== Validator Summary ==="
Write-Host "Fails: $fails, Warnings: $warns"
if ($fails -eq 0) { Write-Ok "Validator PASSED."; exit 0 }
Write-Fail "Validator FAILED."
exit 2
