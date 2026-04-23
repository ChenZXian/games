[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "requirements_lib.ps1")

$root = Get-RepoRoot
Set-Location $root

$traceDir = Join-Path $root "artifacts\requirements\$GameId"
$metadataPath = Join-Path $traceDir "metadata.json"
$requirementsPath = Join-Path $traceDir "requirements.md"
$gameplayDiversityPath = Join-Path $traceDir "gameplay_diversity.json"
$metadata = Read-TraceMetadata $metadataPath

if ($null -eq $metadata) {
  throw "Requirements trace metadata not found: $metadataPath"
}

if (!(Test-Path $requirementsPath)) {
  throw "Requirements trace markdown not found: $requirementsPath"
}

if ([string]$metadata.current_stage -ne "requirements") {
  throw "Requirements trace current_stage must be requirements."
}

if ([string]$metadata.status -ne "confirmed") {
  throw "Requirements trace is not confirmed for $GameId. Current status: $([string]$metadata.status)"
}

if ([string]::IsNullOrWhiteSpace([string]$metadata.selected_concept)) {
  throw "Requirements trace is missing selected_concept."
}

if ([string]::IsNullOrWhiteSpace([string]$metadata.ui_skin)) {
  throw "Requirements trace is missing ui_skin."
}

$checkerPath = Join-Path $PSScriptRoot "check_gameplay_diversity.py"
python $checkerPath --path $gameplayDiversityPath --strict
if ($LASTEXITCODE -ne 0) {
  throw "Gameplay diversity contract is not passed for $GameId."
}

Write-Host "REQUIREMENTS_TRACE_OK=true"
Write-Host "REQUIREMENTS_TRACE_DIR=$traceDir"
Write-Host "REQUIREMENTS_METADATA=$metadataPath"
Write-Host "REQUIREMENTS_MARKDOWN=$requirementsPath"
Write-Host "REQUIREMENTS_STATUS=$([string]$metadata.status)"
Write-Host "GAMEPLAY_DIVERSITY_STATUS=passed"
