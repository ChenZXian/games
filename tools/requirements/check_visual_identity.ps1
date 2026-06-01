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

$contractPath = Join-Path $root "artifacts\requirements\$GameId\visual_identity.json"

if (!(Test-Path $contractPath)) {
  Write-Host "VISUAL_IDENTITY_STATUS=missing"
  Write-Host "VISUAL_IDENTITY_VALID=false"
  Write-Host "VISUAL_IDENTITY_ERRORS=1"
  Write-Host "ERROR=missing contract: $contractPath"
  if ($Strict) { exit 2 }
  exit 0
}

try {
  $data = Get-Content -LiteralPath $contractPath -Raw | ConvertFrom-Json
}
catch {
  Write-Host "VISUAL_IDENTITY_STATUS=invalid"
  Write-Host "VISUAL_IDENTITY_VALID=false"
  Write-Host "VISUAL_IDENTITY_ERRORS=1"
  Write-Host "ERROR=invalid json: $($_.Exception.Message)"
  if ($Strict) { exit 2 }
  exit 0
}

$status = [string]$data.status
if ([string]::IsNullOrWhiteSpace($status)) { $status = "invalid" }
$errors = @(Get-VisualIdentityValidationErrors $data)
$valid = ($errors.Count -eq 0) -and ($status -eq "passed")

Write-Host "VISUAL_IDENTITY_STATUS=$status"
Write-Host "VISUAL_IDENTITY_VALID=$($valid.ToString().ToLower())"
Write-Host "VISUAL_IDENTITY_ERRORS=$($errors.Count)"
foreach ($error in $errors) {
  Write-Host "ERROR=$error"
}

if ($Strict -and -not $valid) { exit 2 }
exit 0
