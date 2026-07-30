[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,
  [switch]$Write,
  [switch]$Json,
    [double]$WarnThreshold = 0.30,
    [double]$FailThreshold = 0.42
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "check_mechanic_fingerprint.py"
$argsList = @($script, "--repo", (Get-Location).Path, "--game-id", $GameId, "--warn-threshold", "$WarnThreshold", "--fail-threshold", "$FailThreshold")
if ($Write) { $argsList += "--write" }
if ($Json) { $argsList += "--json" }
python @argsList
exit $LASTEXITCODE
