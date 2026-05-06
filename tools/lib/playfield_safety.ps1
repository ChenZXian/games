function Get-AndroidAttrValue($node, [string]$localName) {
  if ($null -eq $node) { return "" }
  $ns = "http://schemas.android.com/apk/res/android"
  $value = ""
  try {
    $value = [string]$node.GetAttribute($localName, $ns)
  }
  catch {
    $value = ""
  }
  if (-not [string]::IsNullOrWhiteSpace($value)) {
    return $value
  }
  $fallbackName = "android:$localName"
  if ($node.Attributes) {
    foreach ($attr in $node.Attributes) {
      if ($attr.Name -ieq $fallbackName) {
        return [string]$attr.Value
      }
    }
  }
  return ""
}

function Get-ElementDescendants($node) {
  $result = @()
  if ($null -eq $node) { return $result }
  foreach ($child in $node.ChildNodes) {
    if ($child.NodeType -eq [System.Xml.XmlNodeType]::Element) {
      $result += $child
      $result += Get-ElementDescendants $child
    }
  }
  return $result
}

function Test-NonZeroLayoutValue([string]$value) {
  if ([string]::IsNullOrWhiteSpace($value)) { return $false }
  $normalized = $value.Trim().ToLowerInvariant()
  if ($normalized -in @("0", "0dp", "0dip", "0px")) { return $false }
  return $true
}

function Test-FullSpanLayoutValue([string]$value) {
  if ([string]::IsNullOrWhiteSpace($value)) { return $false }
  $normalized = $value.Trim().ToLowerInvariant()
  return $normalized -in @("match_parent", "fill_parent", "0dp")
}

function Test-GameplayViewNode($node) {
  if ($null -eq $node) { return $false }
  $name = [string]$node.Name
  if ([string]::IsNullOrWhiteSpace($name)) { return $false }
  return $name -match '(?i)(^|\.)(gameview|surfaceview|textureview)$'
}

function Test-OverlayAnchorNode($node) {
  if ($null -eq $node) { return $false }
  $gravity = (Get-AndroidAttrValue $node "layout_gravity").ToLowerInvariant()
  if ($gravity -match 'top|bottom|start|end|left|right') {
    return $true
  }
  $anchorAttrs = @(
    "layout_alignParentTop",
    "layout_alignParentBottom",
    "layout_alignParentStart",
    "layout_alignParentEnd",
    "layout_alignParentLeft",
    "layout_alignParentRight",
    "layout_above",
    "layout_below",
    "layout_toStartOf",
    "layout_toEndOf",
    "layout_toLeftOf",
    "layout_toRightOf",
    "layout_alignTop",
    "layout_alignBottom"
  )
  foreach ($attr in $anchorAttrs) {
    $value = (Get-AndroidAttrValue $node $attr).ToLowerInvariant()
    if ($value -eq "true" -or -not [string]::IsNullOrWhiteSpace($value)) {
      return $true
    }
  }
  $constraints = @(
    "layout_constraintTop_toTopOf",
    "layout_constraintTop_toBottomOf",
    "layout_constraintBottom_toBottomOf",
    "layout_constraintBottom_toTopOf",
    "layout_constraintStart_toStartOf",
    "layout_constraintStart_toEndOf",
    "layout_constraintEnd_toEndOf",
    "layout_constraintEnd_toStartOf"
  )
  foreach ($attr in $constraints) {
    $value = Get-AndroidAttrValue $node $attr
    if (-not [string]::IsNullOrWhiteSpace($value)) {
      return $true
    }
  }
  return $false
}

function Get-GameplayReserveEvidence($node) {
  $evidence = New-Object 'System.Collections.Generic.HashSet[string]'
  $marginAttrs = @(
    "layout_margin",
    "layout_marginTop",
    "layout_marginBottom",
    "layout_marginStart",
    "layout_marginEnd",
    "layout_marginLeft",
    "layout_marginRight"
  )
  $paddingAttrs = @(
    "padding",
    "paddingTop",
    "paddingBottom",
    "paddingStart",
    "paddingEnd",
    "paddingLeft",
    "paddingRight"
  )
  $current = $node
  while ($null -ne $current -and $current.NodeType -eq [System.Xml.XmlNodeType]::Element) {
    foreach ($attr in $marginAttrs) {
      $value = Get-AndroidAttrValue $current $attr
      if (Test-NonZeroLayoutValue $value) {
        [void]$evidence.Add($attr)
      }
    }
    foreach ($attr in $paddingAttrs) {
      $value = Get-AndroidAttrValue $current $attr
      if (Test-NonZeroLayoutValue $value) {
        [void]$evidence.Add($attr)
      }
    }
    $constraintHints = @{
      "layout_constraintTop_toBottomOf" = "top_constraint_gap"
      "layout_constraintBottom_toTopOf" = "bottom_constraint_gap"
      "layout_below" = "top_relative_gap"
      "layout_above" = "bottom_relative_gap"
    }
    foreach ($key in $constraintHints.Keys) {
      $value = Get-AndroidAttrValue $current $key
      if (-not [string]::IsNullOrWhiteSpace($value)) {
        [void]$evidence.Add($constraintHints[$key])
      }
    }
    $current = $current.ParentNode
  }
  return @($evidence)
}

function Get-VisualIdentityPlayfieldContract($projectRoot) {
  $gameId = Split-Path $projectRoot -Leaf
  $path = Join-Path (Get-RepoRoot) "artifacts\requirements\$gameId\visual_identity.json"
  if (!(Test-Path $path)) { return $null }
  try {
    $contract = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    return $contract.ui_identity.playfield_safety
  }
  catch {
    return $null
  }
}

function Test-PlayfieldSafety($projectRoot) {
  $result = [ordered]@{
    Status = "unknown"
    Risk = "unknown"
    Summary = ""
    GameplayViewCount = 0
    AnchoredOverlayCount = 0
    FullSpanGameplayCount = 0
    ReservedSpaceDetected = $false
    ContractReservedEdges = @()
    ContractProtectedZones = @()
    Evidence = @()
  }

  $layoutPath = Join-Path $projectRoot "app\src\main\res\layout\activity_main.xml"
  if (!(Test-Path $layoutPath)) {
    $result.Status = "missing"
    $result.Risk = "unknown"
    $result.Summary = "activity_main.xml is missing"
    return [pscustomobject]$result
  }

  try {
    [xml]$xml = Get-Content -LiteralPath $layoutPath -Raw
  }
  catch {
    $result.Status = "invalid"
    $result.Risk = "high"
    $result.Summary = "activity_main.xml is not valid XML"
    return [pscustomobject]$result
  }

  $allNodes = @($xml.DocumentElement) + @(Get-ElementDescendants $xml.DocumentElement)
  $gameplayNodes = @($allNodes | Where-Object { Test-GameplayViewNode $_ })
  $result.GameplayViewCount = $gameplayNodes.Count
  if ($gameplayNodes.Count -eq 0) {
    $result.Status = "unknown"
    $result.Risk = "medium"
    $result.Summary = "No GameView or SurfaceView node detected in activity_main.xml"
    return [pscustomobject]$result
  }

  $overlayNodes = @()
  foreach ($node in $allNodes) {
    if ($gameplayNodes -contains $node) { continue }
    if (Test-OverlayAnchorNode $node) {
      $overlayNodes += $node
    }
  }
  $result.AnchoredOverlayCount = $overlayNodes.Count

  $evidence = New-Object 'System.Collections.Generic.HashSet[string]'
  foreach ($node in $gameplayNodes) {
    $width = Get-AndroidAttrValue $node "layout_width"
    $height = Get-AndroidAttrValue $node "layout_height"
    $isFullSpan = (Test-FullSpanLayoutValue $width) -and (Test-FullSpanLayoutValue $height)
    if ($isFullSpan) {
      $result.FullSpanGameplayCount++
    }
    foreach ($item in (Get-GameplayReserveEvidence $node)) {
      [void]$evidence.Add($item)
    }
  }
  $result.Evidence = @($evidence)
  $result.ReservedSpaceDetected = $result.Evidence.Count -gt 0

  $contract = Get-VisualIdentityPlayfieldContract $projectRoot
  if ($null -ne $contract) {
    if ($contract.reserved_edges) {
      $result.ContractReservedEdges = @($contract.reserved_edges)
    }
    if ($contract.protected_gameplay_zones) {
      $result.ContractProtectedZones = @($contract.protected_gameplay_zones)
    }
  }

  $hasContractReservations = ($result.ContractReservedEdges.Count -gt 0) -or ($result.ContractProtectedZones.Count -gt 0)
  $hasHighRiskPattern = ($result.FullSpanGameplayCount -gt 0) -and ($result.AnchoredOverlayCount -gt 0) -and (-not $result.ReservedSpaceDetected)

  if ($hasHighRiskPattern) {
    $result.Status = "failed"
    $result.Risk = "high"
    $result.Summary = "Full-span gameplay view with anchored overlays but no reserved playfield margins or padding detected"
  }
  elseif ($hasContractReservations -and -not $result.ReservedSpaceDetected) {
    $result.Status = "warning"
    $result.Risk = "medium"
    $result.Summary = "Visual identity declares playfield reservations, but matching layout reserve evidence was not detected"
  }
  elseif ($result.AnchoredOverlayCount -gt 0 -and $result.ReservedSpaceDetected) {
    $result.Status = "passed"
    $result.Risk = "low"
    $result.Summary = "Anchored overlays detected and layout reserve evidence is present"
  }
  elseif ($result.FullSpanGameplayCount -gt 0 -and $result.AnchoredOverlayCount -eq 0) {
    $result.Status = "passed"
    $result.Risk = "low"
    $result.Summary = "Full-span gameplay view detected without anchored overlay pressure"
  }
  else {
    $result.Status = "passed"
    $result.Risk = "low"
    $result.Summary = "Gameplay layout does not show overlay occlusion risk"
  }

  return [pscustomobject]$result
}
