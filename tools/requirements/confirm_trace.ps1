[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [Parameter(Mandatory=$true)]
  [switch]$ExplicitUserConfirmation,

  [string]$SelectedConcept = "",
  [string]$UiSkin = "",
  [string]$Title = "",
  [string]$Direction = ""
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "requirements_lib.ps1")

if (-not $ExplicitUserConfirmation) {
  throw "confirm_trace.ps1 requires -ExplicitUserConfirmation after the user explicitly confirms the requirements."
}

$root = Get-RepoRoot
Set-Location $root

$traceDir = Join-Path $root "artifacts\requirements\$GameId"
$metadataPath = Join-Path $traceDir "metadata.json"
$requirementsPath = Join-Path $traceDir "requirements.md"
$existing = Read-TraceMetadata $metadataPath

if ($null -eq $existing) {
  throw "Cannot confirm requirements trace without existing metadata.json."
}

if (!(Test-Path $requirementsPath)) {
  throw "Cannot confirm requirements trace without existing requirements.md."
}

if ([string]$existing.current_stage -ne "requirements") {
  throw "Cannot confirm requirements trace unless current_stage is requirements."
}

if ([string]$existing.status -ne "draft") {
  throw "Cannot confirm requirements trace unless current status is draft."
}

Update-RequirementsTrace `
  -GameId $GameId `
  -Title $Title `
  -Direction $Direction `
  -SelectedConcept $SelectedConcept `
  -UiSkin $UiSkin `
  -Stage "requirements" `
  -Status "confirmed"
