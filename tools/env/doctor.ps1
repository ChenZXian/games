# tools/env/doctor.ps1 (monorepo-aware)
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1
#   powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1 -Project games/orbit_dodger
#   powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1 -FixHints

[CmdletBinding()]
param(
  [string]$Project = "",
  [switch]$FixHints
)

$ErrorActionPreference = "Stop"

function Write-Section($title) { Write-Host ""; Write-Host "=== $title ===" }
function Write-Ok($msg) { Write-Host "[OK]  $msg" }
function Write-Warn($msg) { Write-Host "[WARN] $msg" }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" }

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
  throw "Repo root not found. Run inside monorepo (docs/, registry/, games/, tools/)."
}

function Read-FileText($path) {
  if (!(Test-Path $path)) { return $null }
  return Get-Content -LiteralPath $path -Raw
}

function Match-First($text, $pattern) {
  if ($null -eq $text) { return $null }
  $m = [regex]::Match($text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
  if ($m.Success) { return $m.Groups[1].Value }
  return $null
}

function Run-Cmd($exe, $args, $workdir) {
  $p = New-Object System.Diagnostics.Process
  $p.StartInfo.FileName = $exe
  $p.StartInfo.Arguments = $args
  $p.StartInfo.WorkingDirectory = $workdir
  $p.StartInfo.RedirectStandardOutput = $true
  $p.StartInfo.RedirectStandardError = $true
  $p.StartInfo.UseShellExecute = $false
  $p.StartInfo.CreateNoWindow = $true
  [void]$p.Start()
  $out = $p.StandardOutput.ReadToEnd()
  $err = $p.StandardError.ReadToEnd()
  $p.WaitForExit()
  return @{ ExitCode = $p.ExitCode; Stdout = $out; Stderr = $err }
}

$failCount = 0
$warnCount = 0

Write-Section "Locate Repo Root"
$root = Get-RepoRoot
Write-Ok "Repo root: $root"
Set-Location $root

Write-Section "Baseline Targets (embedded; should match docs/ENVIRONMENT_BASELINE.md)"
$targetGradle = "8.14.3"
$targetAgp = "8.13.2"
$allowedJdkMajors = @("17","21")
$requiredCompileSdk = "34"
$requiredMinSdk = "24"
$requiredTargetSdk = "34"
Write-Ok "Targets: Gradle=$targetGradle, AGP=$targetAgp, allowedJDK={17,21}, compileSdk=$requiredCompileSdk, minSdk=$requiredMinSdk, targetSdk=$requiredTargetSdk"

Write-Section "Check Gradle Java Home (org.gradle.java.home) at repo root"
$gradlePropsPath = Join-Path $root "gradle.properties"
$gradlePropsText = Read-FileText $gradlePropsPath
$javaHome = Match-First $gradlePropsText 'org\.gradle\.java\.home\s*=\s*([^\r\n]+)'
if ([string]::IsNullOrWhiteSpace($javaHome)) {
  Write-Fail "org.gradle.java.home not found in repo-root gradle.properties (required by baseline)."
  $failCount++
  if ($FixHints) {
    Write-Host "Hint: Create/append $gradlePropsPath with:"
    Write-Host "  org.gradle.java.home=D:\\Java\\jdk-21"
  }
} else {
  $javaHome = $javaHome.Trim()
  Write-Ok "org.gradle.java.home = $javaHome"
  if (!(Test-Path $javaHome)) {
    Write-Fail "Path does not exist: $javaHome"
    $failCount++
  } else {
    $releaseFile = Join-Path $javaHome "release"
    $releaseText = Read-FileText $releaseFile
    $major = Match-First $releaseText 'JAVA_VERSION\s*=\s*\"([0-9]+)'
    if ([string]::IsNullOrWhiteSpace($major)) {
      Write-Warn "Cannot detect JDK major from $releaseFile"
      $warnCount++
    } else {
      if ($allowedJdkMajors -contains $major) { Write-Ok "JDK major detected: $major" }
      else { Write-Fail "JDK major $major not allowed (17 or 21)"; $failCount++ }
    }
  }
}

Write-Section "Check Android SDK Location (local.properties or ANDROID_SDK_ROOT)"
$sdkDir = $null
$localProps = Join-Path $root "local.properties"
if (Test-Path $localProps) {
  $lp = Read-FileText $localProps
  $sdkDir = Match-First $lp 'sdk\.dir\s*=\s*([^\r\n]+)'
  if (-not [string]::IsNullOrWhiteSpace($sdkDir)) {
    $sdkDir = ($sdkDir.Trim() -replace '\\\\','\')
    Write-Ok "local.properties sdk.dir = $sdkDir"
  }
}
if ([string]::IsNullOrWhiteSpace($sdkDir)) {
  $envSdk = $env:ANDROID_SDK_ROOT
  if ([string]::IsNullOrWhiteSpace($envSdk)) { $envSdk = $env:ANDROID_HOME }
  if ([string]::IsNullOrWhiteSpace($envSdk)) {
    Write-Fail "SDK location not found. Set local.properties sdk.dir OR ANDROID_SDK_ROOT."
    $failCount++
    if ($FixHints) {
      Write-Host "Hint (recommended): setx ANDROID_SDK_ROOT ""C:\Users\<You>\AppData\Local\Android\Sdk"""
    }
  } else {
    $sdkDir = $envSdk.Trim()
    Write-Ok "SDK from environment: $sdkDir"
  }
}
if (-not [string]::IsNullOrWhiteSpace($sdkDir) -and !(Test-Path $sdkDir)) {
  Write-Fail "SDK path does not exist: $sdkDir"
  $failCount++
}

Write-Section "Check Required SDK Packages"
if (-not [string]::IsNullOrWhiteSpace($sdkDir) -and (Test-Path $sdkDir)) {
  $platform34 = Join-Path $sdkDir "platforms\android-34"
  $platformTools = Join-Path $sdkDir "platform-tools"
  $adbExe = Join-Path $platformTools "adb.exe"
  if (Test-Path $platform34) { Write-Ok "Found: platforms/android-34" } else { Write-Fail "Missing: platforms/android-34"; $failCount++ }
  if (Test-Path $platformTools) { Write-Ok "Found: platform-tools" } else { Write-Fail "Missing: platform-tools"; $failCount++ }
  if (Test-Path $adbExe) { Write-Ok "Found: adb.exe" } else { Write-Fail "Missing: adb.exe (platform-tools)"; $failCount++ }

  $buildToolsDir = Join-Path $sdkDir "build-tools"
  $has34 = $false
  if (Test-Path $buildToolsDir) {
    $dirs = Get-ChildItem -Path $buildToolsDir -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
    foreach ($d in $dirs) { if ($d -match '^34\.' -or $d -eq '34.0.0') { $has34 = $true; break } }
  }
  if ($has34) { Write-Ok "Found: build-tools 34.x" } else { Write-Fail "Missing: build-tools 34.x"; $failCount++ }
}

function Resolve-Projects {
  param([string]$root, [string]$projectArg)
  $list = @()
  if (-not [string]::IsNullOrWhiteSpace($projectArg)) {
    $p = Join-Path $root $projectArg
    if (!(Test-Path $p)) { throw "Project path not found: $p" }
    $list += (Resolve-Path $p).Path
    return $list
  }

  # auto-discover under games/*
  $gamesDir = Join-Path $root "games"
  if (!(Test-Path $gamesDir)) { return $list }

  $candidates = Get-ChildItem -Path $gamesDir -Directory -ErrorAction SilentlyContinue
  foreach ($c in $candidates) {
    $hasSettings = (Test-Path (Join-Path $c.FullName "settings.gradle")) -or (Test-Path (Join-Path $c.FullName "settings.gradle.kts"))
    $hasWrapper = (Test-Path (Join-Path $c.FullName "gradlew.bat"))
    if ($hasSettings -and $hasWrapper) { $list += $c.FullName }
  }
  return $list
}

Write-Section "Discover Projects"
$projects = Resolve-Projects -root $root -projectArg $Project
if ($projects.Count -eq 0) {
  Write-Warn "No Android projects discovered under games/* (expected settings.gradle + gradlew.bat)."
  $warnCount++
} else {
  foreach ($p in $projects) { Write-Ok "Project: $p" }
}

foreach ($projDir in $projects) {
  Write-Section "Check Gradle Wrapper Version (per project)"
  $gradlew = Join-Path $projDir "gradlew.bat"
  if (!(Test-Path $gradlew)) {
    Write-Fail "gradlew.bat not found in project: $projDir"
    $failCount++
  } else {
    $r = Run-Cmd $gradlew "-v" $projDir
    if ($r.ExitCode -ne 0) {
      Write-Fail "gradlew -v failed in $projDir"
      Write-Host $r.Stderr
      $failCount++
    } else {
      $ver = Match-First $r.Stdout 'Gradle\s+([0-9]+\.[0-9]+(\.[0-9]+)?)'
      if ([string]::IsNullOrWhiteSpace($ver)) {
        Write-Warn "Unable to parse Gradle version in $projDir"
        $warnCount++
      } else {
        if ($ver -eq $targetGradle) { Write-Ok "Gradle version: $ver" }
        else { Write-Fail "Gradle mismatch in $projDir. Expected $targetGradle, got $ver"; $failCount++ }
      }
    }
  }

  Write-Section "Check AGP Version in Plugins DSL (project root files)"
  $agpFound = $false
  $scan = @()
  $scan += Get-ChildItem -Path $projDir -File -Filter "settings.gradle*" -ErrorAction SilentlyContinue
  $scan += Get-ChildItem -Path $projDir -File -Filter "build.gradle*" -ErrorAction SilentlyContinue
  foreach ($f in $scan) {
    $t = Read-FileText $f.FullName
    $m = [regex]::Match($t, 'id\s+["'']com\.android\.application["'']\s+version\s+["'']([0-9]+\.[0-9]+(\.[0-9]+)?)["'']', "IgnoreCase")
    if ($m.Success) {
      $agpFound = $true
      $v = $m.Groups[1].Value
      if ($v -eq $targetAgp) { Write-Ok "AGP pinned: $v (file: $($f.FullName))" }
      else { Write-Fail "AGP mismatch in $($f.FullName). Expected $targetAgp, got $v"; $failCount++ }
      break
    }
  }
  if (-not $agpFound) { Write-Warn "AGP plugins DSL not found in $projDir"; $warnCount++ }

  Write-Section "Check Project SDK Config (compileSdk/minSdk/targetSdk)"
  $appGradle = Join-Path $projDir "app\build.gradle"
  $appGradleKts = Join-Path $projDir "app\build.gradle.kts"
  $appText = $null
  if (Test-Path $appGradle) { $appText = Read-FileText $appGradle }
  elseif (Test-Path $appGradleKts) { $appText = Read-FileText $appGradleKts }

  if ($null -eq $appText) {
    Write-Warn "Cannot find app/build.gradle(.kts) in $projDir"
    $warnCount++
  } else {
    $cs = Match-First $appText 'compileSdk\s+([0-9]+)'
    $mins = Match-First $appText 'minSdk\s+([0-9]+)'
    $ts = Match-First $appText 'targetSdk\s+([0-9]+)'

    if ($cs -eq $requiredCompileSdk) { Write-Ok "compileSdk = $cs" } else { Write-Fail "compileSdk mismatch. Expected $requiredCompileSdk, got $cs"; $failCount++ }
    if ($mins -eq $requiredMinSdk) { Write-Ok "minSdk = $mins" } else { Write-Fail "minSdk mismatch. Expected $requiredMinSdk, got $mins"; $failCount++ }
    if ($ts -eq $requiredTargetSdk) { Write-Ok "targetSdk = $ts" } else { Write-Fail "targetSdk mismatch. Expected $requiredTargetSdk, got $ts"; $failCount++ }
  }
}

Write-Section "Summary"
Write-Host "Fails: $failCount, Warnings: $warnCount"
if ($failCount -eq 0) { Write-Ok "Doctor check PASSED."; exit 0 }
Write-Fail "Doctor check FAILED."
if (-not $FixHints) {
  Write-Host "Tip: re-run with -FixHints:"
  Write-Host "  powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1 -FixHints"
}
exit 2
