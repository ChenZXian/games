[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$Project,
  [switch]$Json
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "check_runtime_ui_safety.py"
$argsList = @($script, "--project", $Project)
if ($Json) { $argsList += "--json" }
python @argsList
exit $LASTEXITCODE
