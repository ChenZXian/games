[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [string]$Title = "",
  [string]$Direction = "",
  [string]$SelectedConcept = "",
  [string]$UiSkin = "",

  [ValidateSet("candidates","requirements")]
  [string]$Stage = "requirements",

  [ValidateSet("candidates","draft","confirmed")]
  [string]$Status = "draft",

  [string]$CandidatesMarkdown = "",
  [string]$CandidatesMarkdownPath = "",
  [string]$RequirementsMarkdown = "",
  [string]$RequirementsMarkdownPath = "",

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
  -Stage $Stage `
  -Status $Status `
  -CandidatesMarkdown $CandidatesMarkdown `
  -CandidatesMarkdownPath $CandidatesMarkdownPath `
  -RequirementsMarkdown $RequirementsMarkdown `
  -RequirementsMarkdownPath $RequirementsMarkdownPath `
  -Force:$Force
