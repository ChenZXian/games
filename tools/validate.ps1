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

. (Join-Path $PSScriptRoot "lib\playfield_safety.ps1")

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

function Normalize-GradlePathValue($value){
  if ([string]::IsNullOrWhiteSpace($value)) { return "" }
  $normalized = $value.Trim().Trim('"').Trim("'")
  $normalized = $normalized -replace '\\:', ':'
  $normalized = $normalized -replace '\\\\', '\'
  return $normalized
}

function Has-StyleName($stylesText, $styleName){
  if ($null -eq $stylesText) { return $false }
  $escaped = [regex]::Escape($styleName)
  return [regex]::IsMatch($stylesText, "<style\s+name\s*=\s*['""]$escaped['""]", "IgnoreCase")
}

function Get-ApplicationId($projectPath){
  $buildPath = Join-Path $projectPath "app\build.gradle"
  $buildKtsPath = Join-Path $projectPath "app\build.gradle.kts"
  $text = $null
  if (Test-Path $buildPath) { $text = Read-Text $buildPath }
  elseif (Test-Path $buildKtsPath) { $text = Read-Text $buildKtsPath }
  return Match-First $text 'applicationId\s*(?:=)?\s*["'']([^"'']+)["'']'
}

function Get-NamespaceId($projectPath){
  $buildPath = Join-Path $projectPath "app\build.gradle"
  $buildKtsPath = Join-Path $projectPath "app\build.gradle.kts"
  $text = $null
  if (Test-Path $buildPath) { $text = Read-Text $buildPath }
  elseif (Test-Path $buildKtsPath) { $text = Read-Text $buildKtsPath }
  return Match-First $text 'namespace\s*(?:=)?\s*["'']([^"'']+)["'']'
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

function Test-AnyPathExists($base, $relativePaths){
  foreach ($rel in $relativePaths) {
    if (Test-Path (Join-Path $base $rel)) { return $true }
  }
  return $false
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
  if (!(Test-Path (Join-Path $projResolved "gradle\wrapper\gradle-wrapper.jar"))) { $fails.Value++; Write-Fail "Missing gradle-wrapper.jar"; }
  else { Write-Ok "Gradle wrapper jar found" }
  if (!(Test-Path (Join-Path $projResolved "app\build.gradle")) -and !(Test-Path (Join-Path $projResolved "app\build.gradle.kts"))) { $fails.Value++; Write-Fail "Missing app/build.gradle(.kts)"; }
  if (!(Test-Path (Join-Path $projResolved "app\src\main\AndroidManifest.xml"))) { $fails.Value++; Write-Fail "Missing app/src/main/AndroidManifest.xml"; }

  $projectGradlePropsPath = Join-Path $projResolved "gradle.properties"
  $projectGradlePropsText = Read-Text $projectGradlePropsPath
  if ($null -eq $projectGradlePropsText) {
    $fails.Value++
    Write-Fail "Missing project gradle.properties"
  } else {
    $projectJavaHome = Match-First $projectGradlePropsText 'org\.gradle\.java\.home\s*=\s*([^\r\n]+)'
    if ([string]::IsNullOrWhiteSpace($projectJavaHome)) {
      $fails.Value++
      Write-Fail "Project gradle.properties must define org.gradle.java.home"
    } else {
      $javaHomePath = Normalize-GradlePathValue $projectJavaHome
      if (Test-Path $javaHomePath) {
        Write-Ok "Project org.gradle.java.home path exists"
      } else {
        $fails.Value++
        Write-Fail "Project org.gradle.java.home path is invalid: $projectJavaHome"
      }
    }
  }

  $settingsGradlePath = Join-Path $projResolved "settings.gradle"
  $settingsGradleText = Read-Text $settingsGradlePath
  $rootBuildGradleText = Read-Text (Join-Path $projResolved "build.gradle")
  if ($rootBuildGradleText -match 'id\s+["'']com\.android\.application["'']\s+version') {
    $hasPluginRepos = $settingsGradleText -match 'pluginManagement' -and
      $settingsGradleText -match 'google\s*\(\s*\)' -and
      $settingsGradleText -match 'mavenCentral\s*\(\s*\)' -and
      $settingsGradleText -match 'gradlePluginPortal\s*\(\s*\)'
    if ($hasPluginRepos) {
      Write-Ok "Gradle plugin repositories found"
    } else {
      $fails.Value++
      Write-Fail "settings.gradle must declare pluginManagement repositories google, mavenCentral, and gradlePluginPortal"
    }
  }

  # 3) GAME_GENERATION_STANDARD presence (repo-level)
  $std = Join-Path $root "docs\GAME_GENERATION_STANDARD.md"
  if (!(Test-Path $std)) { $fails.Value++; Write-Fail "Missing docs/GAME_GENERATION_STANDARD.md at repo root"; }

  # 3.5) Registry JSON integrity
  $registryPath = Join-Path $root "registry\produced_games.json"
  if (!(Test-Path $registryPath)) {
    $fails.Value++
    Write-Fail "Missing registry/produced_games.json at repo root"
  } else {
    try {
      $null = Get-Content -LiteralPath $registryPath -Raw | ConvertFrom-Json
      Write-Ok "Registry JSON is valid"
    }
    catch {
      $fails.Value++
      Write-Fail "Registry JSON is invalid: registry/produced_games.json"
    }
  }

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

  $namespaceId = Get-NamespaceId $projResolved
  $applicationId = Get-ApplicationId $projResolved
  if ($namespaceId -ne "com.android.boot") {
    $fails.Value++
    Write-Fail "Android namespace must be com.android.boot (got $namespaceId)"
  } else {
    Write-Ok "Android namespace OK"
  }
  if ([string]::IsNullOrWhiteSpace($applicationId)) {
    $fails.Value++
    Write-Fail "Missing applicationId in app/build.gradle"
  } elseif ($applicationId -eq "com.android.boot") {
    $fails.Value++
    Write-Fail "applicationId must be unique per game and must not be exactly com.android.boot"
  } else {
    $duplicates = @()
    foreach ($candidate in (Discover-Projects $root)) {
      $candidateResolved = (Resolve-Path $candidate).Path
      if ($candidateResolved -eq $projResolved) { continue }
      if ((Get-ApplicationId $candidateResolved) -eq $applicationId) {
        $duplicates += (Split-Path $candidateResolved -Leaf)
      }
    }
    if ($duplicates.Count -gt 0) {
      $fails.Value++
      Write-Fail "applicationId is duplicated by: $($duplicates -join ', ')"
    } else {
      Write-Ok "applicationId is unique: $applicationId"
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

  $canvasRiskFiles = @()
  $javaRoot = Join-Path $projResolved "app\src\main\java"
  if (Test-Path $javaRoot) {
    foreach ($javaFile in (Get-ChildItem -Path $javaRoot -Recurse -File -Filter "*.java" -ErrorAction SilentlyContinue)) {
      $javaText = Read-Text $javaFile.FullName
      if ($javaText -match 'lockCanvas\s*\(') {
        $hasUnlock = $javaText -match 'unlockCanvasAndPost\s*\('
        $hasCatch = $javaText -match '\bcatch\s*\('
        $hasFinally = $javaText -match '\bfinally\b'
        $hasNullGuard = $javaText -match '!=\s*null' -or $javaText -match '==\s*null'
        if (-not ($hasUnlock -and $hasCatch -and $hasFinally -and $hasNullGuard)) {
          $canvasRiskFiles += $javaFile.FullName
        }
      }
    }
  }
  if ($canvasRiskFiles.Count -gt 0) {
    $fails.Value++
    Write-Fail "Surface canvas drawing must protect lockCanvas/unlockCanvasAndPost with catch, finally, and a null guard. Example: $($canvasRiskFiles[0])"
    if ($canvasRiskFiles.Count -gt 1) { Write-Warn "Total Surface canvas risk files: $($canvasRiskFiles.Count)" }
  }

  # 6) Android resources: app_icon contract
  $resDir = Join-Path $projResolved "app\src\main\res"
  $iconFound = $false
  if (Test-Path $resDir) {
    $adaptiveIcon = Join-Path $resDir "mipmap-anydpi-v26\app_icon.xml"
    $foregroundPng = Join-Path $resDir "drawable\app_icon_fg.png"
    $foregroundXml = Join-Path $resDir "drawable\app_icon_fg.xml"
    $legacyIcons = Get-ChildItem -Path $resDir -Recurse -File -Filter "app_icon.png" -ErrorAction SilentlyContinue |
      Where-Object { $_.DirectoryName -match 'mipmap' }

    if ((Test-Path $adaptiveIcon) -or ($legacyIcons.Count -gt 0)) {
      $iconFound = $true
    }

    if (Test-Path $adaptiveIcon) { Write-Ok "Adaptive app icon resource found" }
    elseif ($legacyIcons.Count -gt 0) { Write-Warn "Only legacy bitmap app_icon resources found" }

    if ((Test-Path $foregroundPng) -or (Test-Path $foregroundXml)) {
      Write-Ok "App icon foreground resource found"
    } elseif (Test-Path $adaptiveIcon) {
      $warns.Value++
      Write-Warn "Adaptive app icon exists without app_icon_fg foreground resource"
    }

    if ($legacyIcons.Count -gt 0) {
      Write-Ok "Legacy bitmap app_icon resources found"
    }
  }
  if (!$iconFound) { $fails.Value++; Write-Fail "Missing @mipmap/app_icon resources" } else { Write-Ok "App icon resources found" }

  # 7) UI foundation checks
  $requiredUiFiles = @(
    "app\src\main\res\layout\activity_main.xml",
    "app\src\main\res\values\colors.xml",
    "app\src\main\res\values\dimens.xml",
    "app\src\main\res\values\styles.xml",
    "app\src\main\res\values\themes.xml"
  )
  foreach ($rel in $requiredUiFiles) {
    $full = Join-Path $projResolved $rel
    if (Test-Path $full) {
      Write-Ok "UI file found: $rel"
    } else {
      $fails.Value++
      Write-Fail "Missing required UI file: $rel"
    }
  }

  $stylesPath = Join-Path $projResolved "app\src\main\res\values\styles.xml"
  $stylesText = Read-Text $stylesPath
  if ($null -ne $stylesText) {
    $usesWidgetGame = $stylesText -match 'Widget\.Game'
    $hasWidget = Has-StyleName $stylesText "Widget"
    $hasWidgetGame = Has-StyleName $stylesText "Widget.Game"
    if ($usesWidgetGame -and -not $hasWidget) {
      $fails.Value++
      Write-Fail "styles.xml must define base style Widget when Widget.Game styles are present"
    }
    if ($usesWidgetGame -and -not $hasWidgetGame) {
      $fails.Value++
      Write-Fail "styles.xml must define base style Widget.Game when nested Widget.Game styles are present"
    }
    $buttonBaseNeeded = $false
    foreach ($m in [regex]::Matches($stylesText, '<style\s+name\s*=\s*["'']Widget\.Game\.Button\.[^"'']+["'']([^>]*)>', "IgnoreCase")) {
      if ($m.Groups[1].Value -notmatch '\bparent\s*=') {
        $buttonBaseNeeded = $true
      }
    }
    if ($buttonBaseNeeded -and -not (Has-StyleName $stylesText "Widget.Game.Button")) {
      $fails.Value++
      Write-Fail "styles.xml must define Widget.Game.Button when nested button styles omit an explicit parent"
    }
    $badParentPatterns = @(
      'Widget\.MaterialComponents\.ImageButton',
      'Widget\.MaterialComponents\.Button\.Icon',
      'Widget\.AppCompat\.ImageButton',
      'Widget\.AppCompat\.ProgressBar\.Horizontal'
    )
    foreach ($pattern in $badParentPatterns) {
      if ($stylesText -match $pattern) {
        $fails.Value++
        Write-Fail "styles.xml uses an unsupported style parent that can fail AAPT: $pattern"
      }
    }
  }

  $uiLogicalResources = @(
    @{ Name = "ui_panel"; Paths = @("app\src\main\res\drawable\ui_panel.xml", "app\src\main\res\drawable\ui_panel.png", "app\src\main\res\drawable\ui_panel.webp", "app\src\main\res\drawable\ui_panel.9.png") },
    @{ Name = "ui_button_primary"; Paths = @("app\src\main\res\drawable\ui_button_primary.xml", "app\src\main\res\drawable\ui_button_primary.png", "app\src\main\res\drawable\ui_button_primary.webp", "app\src\main\res\drawable\ui_button_primary.9.png") },
    @{ Name = "ui_button_secondary"; Paths = @("app\src\main\res\drawable\ui_button_secondary.xml", "app\src\main\res\drawable\ui_button_secondary.png", "app\src\main\res\drawable\ui_button_secondary.webp", "app\src\main\res\drawable\ui_button_secondary.9.png") }
  )
  foreach ($entry in $uiLogicalResources) {
    if (Test-AnyPathExists $projResolved $entry.Paths) {
      Write-Ok "UI resource found: $($entry.Name)"
    } else {
      $fails.Value++
      Write-Fail "Missing required logical UI resource: $($entry.Name)"
    }
  }

  $playfieldSafety = Test-PlayfieldSafety $projResolved
  if ($playfieldSafety.Status -eq "failed" -or $playfieldSafety.Status -eq "invalid" -or $playfieldSafety.Status -eq "missing") {
    $fails.Value++
    Write-Fail "Playfield safety failed: $($playfieldSafety.Summary)"
  } elseif ($playfieldSafety.Status -eq "warning") {
    $warns.Value++
    Write-Warn "Playfield safety warning: $($playfieldSafety.Summary)"
  } elseif ($playfieldSafety.Status -eq "passed") {
    Write-Ok "Playfield safety passed: $($playfieldSafety.Summary)"
  } else {
    $warns.Value++
    Write-Warn "Playfield safety could not be fully verified: $($playfieldSafety.Summary)"
  }

  $runtimeUiScript = Join-Path $root "tools\check_runtime_ui_safety.ps1"
  if (Test-Path $runtimeUiScript) {
    $runtimeOutput = & powershell -ExecutionPolicy Bypass -File $runtimeUiScript -Project $projResolved
    $runtimeExit = $LASTEXITCODE
    $runtimeText = ($runtimeOutput | Out-String)
    $runtimeStatus = Match-First $runtimeText 'RUNTIME_UI_SAFETY_STATUS=([^\r\n]+)'
    $adaptiveStatus = Match-First $runtimeText 'RUNTIME_UI_ADAPTIVE_STATUS=([^\r\n]+)'
    $runtimeRisk = Match-First $runtimeText 'RUNTIME_UI_OCCLUSION_RISK=([^\r\n]+)'
    $touchRisk = Match-First $runtimeText 'RUNTIME_UI_TOUCH_TARGET_RISK=([^\r\n]+)'
    $runtimeCollisions = Match-First $runtimeText 'RUNTIME_UI_COLLISIONS=([^\r\n]+)'
    $adaptiveErrors = Match-First $runtimeText 'RUNTIME_UI_ADAPTIVE_ERRORS=([^\r\n]+)'
    if ($runtimeExit -eq 0 -and $runtimeStatus -eq "passed" -and $runtimeRisk -eq "low" -and $adaptiveStatus -eq "passed" -and $touchRisk -eq "low") {
      Write-Ok "Runtime UI safety passed: $runtimeCollisions collision(s), $adaptiveErrors adaptive error(s)"
    } else {
      $fails.Value++
      Write-Fail "Runtime UI safety failed: status=$runtimeStatus adaptive=$adaptiveStatus occlusion=$runtimeRisk touch=$touchRisk collisions=$runtimeCollisions adaptive_errors=$adaptiveErrors"
    }
  } else {
    $warns.Value++
    Write-Warn "Runtime UI safety checker not found"
  }

  # 8) Baseline alignment quick checks (compileSdk/minSdk/targetSdk)
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

    $blockedDependencyPatterns = @(
      'androidx\.core:core(?:-ktx)?:1\.16\.',
      'androidx\.appcompat:appcompat:1\.7\.1',
      'androidx\.constraintlayout:constraintlayout:2\.2\.1'
    )
    foreach ($pattern in $blockedDependencyPatterns) {
      if ($appText -match $pattern) {
        $fails.Value++
        Write-Fail "Dependency version is not compatible with compileSdk 34 or this baseline: $pattern"
      }
    }
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
