[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [string]$Title = "",
  [string]$Direction = "",

  [Parameter(Mandatory=$true)]
  [string]$SelectedConcept,

  [Parameter(Mandatory=$true)]
  [string]$UiSkin,

  [string]$RequirementsMarkdown = "",
  [string]$RequirementsMarkdownPath = "",
  [string]$GameplayDiversityJson = "",
  [string]$GameplayDiversityJsonPath = "",
  [string]$VisualIdentityJson = "",
  [string]$VisualIdentityJsonPath = "",

  [switch]$Force
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
  -Status "draft" `
  -RequirementsMarkdown $RequirementsMarkdown `
  -RequirementsMarkdownPath $RequirementsMarkdownPath `
  -GameplayDiversityJson $GameplayDiversityJson `
  -GameplayDiversityJsonPath $GameplayDiversityJsonPath `
  -VisualIdentityJson $VisualIdentityJson `
  -VisualIdentityJsonPath $VisualIdentityJsonPath `
  -Force:$Force
