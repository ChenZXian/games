param(
  [Parameter(Mandatory=$false)][string]$PackId = "",
  [Parameter(Mandatory=$false)][string]$SourcePath = "",
  [Parameter(Mandatory=$false)][string]$DownloadUrl = "",
  [Parameter(Mandatory=$false)][string]$LibraryRoot = "shared_assets/ui"
)

$ErrorActionPreference = "Stop"

if (($SourcePath -eq "") -and ($DownloadUrl -eq "")) {
  throw "One of SourcePath or DownloadUrl is required"
}
if (($SourcePath -ne "") -and ($DownloadUrl -ne "")) {
  throw "Choose either SourcePath or DownloadUrl, not both"
}

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  throw "python not found in PATH"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $scriptDir "import_ui_pack.py"

if (-not (Test-Path -LiteralPath $py)) {
  throw "Missing script: $py"
}

$argsList = @(
  $py,
  "--library-root", $LibraryRoot
)

if ($PackId -ne "") {
  $argsList += @("--pack-id", $PackId)
}

if ($SourcePath -ne "") {
  $argsList += @("--source-path", $SourcePath)
}
if ($DownloadUrl -ne "") {
  $argsList += @("--download-url", $DownloadUrl)
}

& python @argsList
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "UI_IMPORT_OK=true"
