[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [string]$Title = "",

  [Parameter(Mandatory=$true)]
  [string]$Direction,

  [string]$CandidatesMarkdown = "",
  [string]$CandidatesMarkdownPath = "",

  [switch]$Force
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "requirements_lib.ps1")

Update-RequirementsTrace `
  -GameId $GameId `
  -Title $Title `
  -Direction $Direction `
  -Stage "candidates" `
  -Status "candidates" `
  -CandidatesMarkdown $CandidatesMarkdown `
  -CandidatesMarkdownPath $CandidatesMarkdownPath `
  -Force:$Force
