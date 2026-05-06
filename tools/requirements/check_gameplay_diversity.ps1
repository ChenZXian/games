[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [switch]$Strict
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "requirements_lib.ps1")

$root = Get-RepoRoot
Set-Location $root

$contractPath = Join-Path $root "artifacts\requirements\$GameId\gameplay_diversity.json"
$scriptPath = Join-Path $PSScriptRoot "check_gameplay_diversity.py"
$argsList = @($scriptPath, "--path", $contractPath)
if ($Strict) {
  $argsList += "--strict"
}

python @argsList
exit $LASTEXITCODE
