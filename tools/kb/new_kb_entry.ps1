# tools/kb/new_kb_entry.ps1
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools/kb/new_kb_entry.ps1 -Slug "gradle_wrapper_mismatch"
# Output:
#   Creates: kb/problems/YYYYMMDD-<slug>.md

[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$Slug
)

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
  throw "Repo root not found. Run inside monorepo."
}

function Normalize-Slug([string]$s) {
  $t = $s.Trim().ToLowerInvariant()
  $t = $t -replace '[^a-z0-9_]+', '_'
  $t = $t -replace '_+', '_'
  $t = $t.Trim('_')
  if ([string]::IsNullOrWhiteSpace($t)) { throw "Invalid slug." }
  return $t
}

$root = Get-RepoRoot
Set-Location $root

$Slug = Normalize-Slug $Slug

$kbDir = Join-Path $root "kb\problems"
New-Item -ItemType Directory -Force -Path $kbDir | Out-Null

$today = Get-Date
$ymd = $today.ToString("yyyyMMdd")
$dateIso = $today.ToString("yyyy-MM-dd")

# Create monotonically increasing id number for today
$existing = Get-ChildItem -Path $kbDir -Filter "$ymd-*.md" -ErrorAction SilentlyContinue
$seq = ($existing.Count + 1).ToString("000")
$kbId = "KB-$ymd-$seq"

$fileName = "$ymd-$Slug.md"
$filePath = Join-Path $kbDir $fileName

if (Test-Path $filePath) {
  Write-Host "Already exists: $filePath"
  exit 0
}

$template = @"
---
id: $kbId
date: $dateIso
tags: []
severity: medium
components: []
env:
  os: Windows 10/11
  jdk: "21"
  gradle: "8.14.3"
  agp: "8.13.2"
  compileSdk: 34
  minSdk: 24
  targetSdk: 34
---

# Symptom

# Error Log

# Root Cause

# Fix

# Prevention

# References
"@

Set-Content -LiteralPath $filePath -Value $template -Encoding UTF8
Write-Host "Created: $filePath"
