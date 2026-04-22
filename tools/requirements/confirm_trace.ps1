[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [string]$SelectedConcept = "",
  [string]$UiSkin = "",
  [string]$Title = "",
  [string]$Direction = ""
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "requirements_lib.ps1")

Update-RequirementsTrace `
  -GameId $GameId `
  -Title $Title `
  -Direction $Direction `
  -SelectedConcept $SelectedConcept `
  -UiSkin $UiSkin `
  -Stage "requirements" `
  -Status "confirmed"
