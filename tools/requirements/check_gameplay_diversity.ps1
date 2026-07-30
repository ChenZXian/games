[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)]
  [string]$GameId,

  [switch]$Strict
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "requirements_lib.ps1")
. (Join-Path $PSScriptRoot "validation_lib.ps1")

$root = Get-RepoRoot
Set-Location $root

$contractPath = Join-Path $root "artifacts\requirements\$GameId\gameplay_diversity.json"

if (!(Test-Path $contractPath)) {
  Write-Host "GAMEPLAY_DIVERSITY_STATUS=missing"
  Write-Host "GAMEPLAY_DIVERSITY_VALID=false"
  Write-Host "GAMEPLAY_DIVERSITY_ERRORS=1"
  Write-Host "ERROR=missing contract: $contractPath"
  if ($Strict) { exit 2 }
  exit 0
}

try {
  $data = Get-Content -LiteralPath $contractPath -Raw | ConvertFrom-Json
}
catch {
  Write-Host "GAMEPLAY_DIVERSITY_STATUS=invalid"
  Write-Host "GAMEPLAY_DIVERSITY_VALID=false"
  Write-Host "GAMEPLAY_DIVERSITY_ERRORS=1"
  Write-Host "ERROR=invalid json: $($_.Exception.Message)"
  if ($Strict) { exit 2 }
  exit 0
}

$status = [string]$data.status
if ([string]::IsNullOrWhiteSpace($status)) { $status = "invalid" }
$errors = @(Get-GameplayDiversityValidationErrors $data)
$valid = ($errors.Count -eq 0) -and ($status -eq "passed")

Write-Host "GAMEPLAY_DIVERSITY_STATUS=$status"
Write-Host "GAMEPLAY_DIVERSITY_VALID=$($valid.ToString().ToLower())"
Write-Host "GAMEPLAY_DIVERSITY_ERRORS=$($errors.Count)"
foreach ($error in $errors) {
  Write-Host "ERROR=$error"
}

if ($Strict -and -not $valid) { exit 2 }
exit 0
