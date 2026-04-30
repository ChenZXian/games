function Get-IconIntegrityNormalizedText([string]$Value) {
  if ([string]::IsNullOrWhiteSpace($Value)) { return "" }
  $normalized = $Value.ToLowerInvariant()
  $normalized = [regex]::Replace($normalized, '[^a-z0-9]+', ' ')
  $normalized = [regex]::Replace($normalized, '\s+', ' ').Trim()
  return $normalized
}

function Get-IconIntegrityTokenSet([string]$Value) {
  $set = New-Object 'System.Collections.Generic.HashSet[string]'
  $ignored = @(
    "a","an","the","and","or","of","to","in","on","at","by","for","with","from","into","over","under","behind","below","above","beside","near",
    "cartoon","icon","game","specific","small","tiny","clear","strong","round","visible","stylized"
  )
  $normalized = Get-IconIntegrityNormalizedText $Value
  if ([string]::IsNullOrWhiteSpace($normalized)) { return $set }
  foreach ($part in $normalized.Split(' ')) {
    if (-not [string]::IsNullOrWhiteSpace($part) -and ($ignored -notcontains $part)) {
      [void]$set.Add($part)
    }
  }
  return $set
}

function Get-IconIntegrityOverlapCount($LeftSet, $RightSet) {
  $count = 0
  foreach ($item in $LeftSet) {
    if ($RightSet.Contains($item)) {
      $count++
    }
  }
  return $count
}

function Get-SupportedIconMotifs {
  return @(
    "crownbridge",
    "scrapring",
    "mountainbunker",
    "commandcamp",
    "fistsign",
    "warpennant",
    "snowman",
    "castle",
    "bomb",
    "leaf",
    "zombie",
    "runner",
    "courier",
    "swordshield",
    "plane",
    "hook",
    "shieldstar"
  )
}

function Test-IconGenerationIntegrity {
  param(
    [Parameter(Mandatory=$true)][string]$RepoRoot,
    [Parameter(Mandatory=$true)][string]$ProjectPath,
    [Parameter(Mandatory=$true)][string]$GameId,
    [Parameter(Mandatory=$false)]$VisualIdentity
  )

  $result = [ordered]@{
    Status = "missing"
    Trust = "low"
    Summary = "Icon metadata or generated assets are missing"
    MotifStatus = "missing"
    ContractAlignment = "missing"
    TimestampStatus = "missing"
  }

  $iconDir = Join-Path $RepoRoot ("artifacts\icons\" + $GameId)
  $metadataPath = Join-Path $iconDir "metadata.json"
  $uploadPath = Join-Path $iconDir ($GameId + "-upload-1024.png")
  $foregroundPath = Join-Path $ProjectPath "app\src\main\res\drawable\app_icon_fg.png"
  $legacyPaths = @(
    "app\src\main\res\mipmap-mdpi\app_icon.png",
    "app\src\main\res\mipmap-hdpi\app_icon.png",
    "app\src\main\res\mipmap-xhdpi\app_icon.png",
    "app\src\main\res\mipmap-xxhdpi\app_icon.png",
    "app\src\main\res\mipmap-xxxhdpi\app_icon.png"
  ) | ForEach-Object { Join-Path $ProjectPath $_ }

  if (-not (Test-Path $metadataPath) -or -not (Test-Path $uploadPath) -or -not (Test-Path $foregroundPath)) {
    return [pscustomobject]$result
  }
  foreach ($legacyPath in $legacyPaths) {
    if (-not (Test-Path $legacyPath)) {
      $result.Status = "failed"
      $result.Summary = "Icon integrity failed because one or more generated project icon bitmaps are missing"
      return [pscustomobject]$result
    }
  }

  $metadata = $null
  try {
    $metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
  }
  catch {
    $result.Status = "failed"
    $result.Summary = "Icon integrity failed because metadata.json is invalid"
    return [pscustomobject]$result
  }

  $motif = [string]$metadata.motif
  $iconSubject = [string]$metadata.icon_subject
  if ([string]::IsNullOrWhiteSpace($iconSubject)) {
    $iconSubject = [string]$metadata.subject
  }
  $iconSilhouette = [string]$metadata.icon_silhouette
  $visualSource = [string]$metadata.visual_identity_source
  $projectPathFromMetadata = [string]$metadata.project_path
  $primaryExport = [string]$metadata.primary_export
  $generationMode = [string]$metadata.generation_mode
  $reusePolicy = [string]$metadata.reuse_policy
  $foregroundHash = [string]$metadata.foreground_sha256
  $primaryExportHash = [string]$metadata.primary_export_sha256

  $supportedMotifs = Get-SupportedIconMotifs
  if ([string]::IsNullOrWhiteSpace($motif) -or ($supportedMotifs -notcontains $motif)) {
    $result.Status = "failed"
    $result.Trust = "low"
    $result.MotifStatus = "failed"
    $result.Summary = "Icon integrity failed because metadata motif is missing or unsupported by the generator"
    return [pscustomobject]$result
  }
  $result.MotifStatus = "passed"

  $assetPaths = @($uploadPath, $foregroundPath) + $legacyPaths
  $assetTimes = @()
  foreach ($assetPath in $assetPaths) {
    $assetTimes += (Get-Item -LiteralPath $assetPath).LastWriteTime
  }
  $metadataTime = (Get-Item -LiteralPath $metadataPath).LastWriteTime
  $latestAssetTime = ($assetTimes | Sort-Object -Descending | Select-Object -First 1)
  $earliestAssetTime = ($assetTimes | Sort-Object | Select-Object -First 1)
  $assetSpreadSeconds = [math]::Abs(($latestAssetTime - $earliestAssetTime).TotalSeconds)
  $metadataLeadSeconds = ($metadataTime - $latestAssetTime).TotalSeconds

  if ($assetSpreadSeconds -gt 120) {
    $result.TimestampStatus = "warning"
    $result.Status = "warning"
    $result.Trust = "low"
    $result.Summary = "Icon integrity warning because generated icon assets were not written within one generation window"
  } elseif ($metadataLeadSeconds -gt 120) {
    $result.TimestampStatus = "failed"
    $result.Status = "failed"
    $result.Trust = "low"
    $result.Summary = "Icon integrity failed because metadata.json is much newer than the generated icon assets"
    return [pscustomobject]$result
  } else {
    $result.TimestampStatus = "passed"
  }

  if (-not [string]::IsNullOrWhiteSpace($projectPathFromMetadata) -and ($projectPathFromMetadata -ne $ProjectPath)) {
    $result.Status = "failed"
    $result.Trust = "low"
    $result.Summary = "Icon integrity failed because metadata.json points at a different project path"
    return [pscustomobject]$result
  }
  if (-not [string]::IsNullOrWhiteSpace($primaryExport) -and ($primaryExport -ne $uploadPath)) {
    $result.Status = "failed"
    $result.Trust = "low"
    $result.Summary = "Icon integrity failed because metadata.json points at a different primary export"
    return [pscustomobject]$result
  }
  if ($generationMode -ne "fresh_render" -or $reusePolicy -ne "no_reuse") {
    $result.Status = "failed"
    $result.Trust = "low"
    $result.Summary = "Icon integrity failed because metadata does not declare fresh render and no-reuse policy"
    return [pscustomobject]$result
  }
  if ([string]::IsNullOrWhiteSpace($foregroundHash) -or [string]::IsNullOrWhiteSpace($primaryExportHash)) {
    $result.Status = "failed"
    $result.Trust = "low"
    $result.Summary = "Icon integrity failed because metadata is missing generated icon hashes"
    return [pscustomobject]$result
  }
  $actualForegroundHash = (Get-FileHash -LiteralPath $foregroundPath -Algorithm SHA256).Hash.ToLowerInvariant()
  $actualPrimaryExportHash = (Get-FileHash -LiteralPath $uploadPath -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actualForegroundHash -ne $foregroundHash -or $actualPrimaryExportHash -ne $primaryExportHash) {
    $result.Status = "failed"
    $result.Trust = "low"
    $result.Summary = "Icon integrity failed because metadata hashes do not match generated icon files"
    return [pscustomobject]$result
  }

  if ($null -ne $VisualIdentity -and $null -ne $VisualIdentity.icon_identity) {
    $contractSubject = [string]$VisualIdentity.icon_identity.subject
    $contractSilhouette = [string]$VisualIdentity.icon_identity.silhouette
    $subjectOverlap = Get-IconIntegrityOverlapCount (Get-IconIntegrityTokenSet $iconSubject) (Get-IconIntegrityTokenSet $contractSubject)
    $silhouetteOverlap = Get-IconIntegrityOverlapCount (Get-IconIntegrityTokenSet $iconSilhouette) (Get-IconIntegrityTokenSet $contractSilhouette)
    $subjectExact = (Get-IconIntegrityNormalizedText $iconSubject) -eq (Get-IconIntegrityNormalizedText $contractSubject)
    $silhouetteExact = (Get-IconIntegrityNormalizedText $iconSilhouette) -eq (Get-IconIntegrityNormalizedText $contractSilhouette)

    if ([string]::IsNullOrWhiteSpace($iconSubject) -or [string]::IsNullOrWhiteSpace($contractSubject)) {
      $result.ContractAlignment = "warning"
      if ($result.Status -eq "missing") { $result.Status = "warning" }
      if ($result.Trust -ne "low") { $result.Trust = "low" }
      $result.Summary = "Icon integrity warning because metadata subject or visual identity subject is missing"
    } elseif (-not $subjectExact -and $subjectOverlap -lt 2) {
      $result.ContractAlignment = "failed"
      $result.Status = "failed"
      $result.Trust = "low"
      $result.Summary = "Icon integrity failed because metadata subject does not align with visual_identity.json"
      return [pscustomobject]$result
    } elseif (-not [string]::IsNullOrWhiteSpace($contractSilhouette) -and -not $silhouetteExact -and $silhouetteOverlap -lt 1) {
      $result.ContractAlignment = "warning"
      if ($result.Status -eq "missing") { $result.Status = "warning" }
      if ($result.Trust -ne "low") { $result.Trust = "low" }
      $result.Summary = "Icon integrity warning because metadata silhouette weakly aligns with visual_identity.json"
    } else {
      $result.ContractAlignment = "passed"
    }

    $expectedVisualSource = Join-Path $RepoRoot ("artifacts\requirements\" + $GameId + "\visual_identity.json")
    if (-not [string]::IsNullOrWhiteSpace($visualSource) -and ($visualSource -ne $expectedVisualSource)) {
      $result.ContractAlignment = "warning"
      if ($result.Status -eq "missing") { $result.Status = "warning" }
      if ($result.Trust -ne "low") { $result.Trust = "low" }
      $result.Summary = "Icon integrity warning because metadata visual identity source does not match the current contract path"
    }
  } else {
    $result.ContractAlignment = "missing"
  }

  if ($result.Status -eq "missing") {
    $result.Status = "passed"
    $result.Trust = "high"
    $result.Summary = "Generated icon assets, metadata, and contract alignment passed"
  } elseif ($result.Status -eq "warning") {
    $result.Trust = "low"
  } elseif ($result.Status -eq "passed") {
    $result.Trust = "high"
  }

  return [pscustomobject]$result
}
