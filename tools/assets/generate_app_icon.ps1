param(
  [Parameter(Mandatory=$true)][string]$Project,
  [Parameter(Mandatory=$false)][string]$GameId = "",
  [Parameter(Mandatory=$false)][string]$Seed = "",
  [Parameter(Mandatory=$false)][string]$Subject = "",
  [Parameter(Mandatory=$false)][string]$ExportRoot = ""
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Ensure-Directory([string]$PathValue) {
  if (-not (Test-Path -LiteralPath $PathValue)) {
    New-Item -ItemType Directory -Force -Path $PathValue | Out-Null
  }
}

function Write-TextUtf8([string]$PathValue, [string]$Content) {
  $dir = Split-Path -Parent $PathValue
  if ($dir) { Ensure-Directory $dir }
  [System.IO.File]::WriteAllText($PathValue, $Content, [System.Text.UTF8Encoding]::new($false))
}

function Remove-IfExists([string]$PathValue) {
  if (Test-Path -LiteralPath $PathValue) {
    Remove-Item -LiteralPath $PathValue -Force
  }
}

function Get-FileSha256([string]$PathValue) {
  if (-not (Test-Path -LiteralPath $PathValue)) { return "" }
  $hash = Get-FileHash -LiteralPath $PathValue -Algorithm SHA256
  return ([string]$hash.Hash).ToLowerInvariant()
}

function Get-RepoRoot([string]$StartPath) {
  $current = (Resolve-Path -LiteralPath $StartPath).Path
  for ($i = 0; $i -lt 12; $i++) {
    if ((Test-Path -LiteralPath (Join-Path $current "docs")) -and
        (Test-Path -LiteralPath (Join-Path $current "registry")) -and
        (Test-Path -LiteralPath (Join-Path $current "games")) -and
        (Test-Path -LiteralPath (Join-Path $current "tools"))) {
      return $current
    }
    $parent = Split-Path -Parent $current
    if (-not $parent -or $parent -eq $current) { break }
    $current = $parent
  }
  throw "Repo root not found from: $StartPath"
}

function Read-JsonObject([string]$PathValue) {
  if (-not (Test-Path -LiteralPath $PathValue)) { return $null }
  try {
    return (Get-Content -LiteralPath $PathValue -Raw | ConvertFrom-Json)
  }
  catch {
    return $null
  }
}

function Normalize-TextId([string]$Value) {
  if ([string]::IsNullOrWhiteSpace($Value)) { return "" }
  $normalized = $Value.ToLowerInvariant()
  $normalized = [regex]::Replace($normalized, '[^a-z0-9]+', ' ')
  $normalized = [regex]::Replace($normalized, '\s+', ' ').Trim()
  return $normalized
}

function Get-TokenSet([string]$Value) {
  $set = New-Object 'System.Collections.Generic.HashSet[string]'
  $ignored = @(
    "a","an","the","and","or","of","to","in","on","at","by","for","with","from","into","over","under","behind","below","above","beside","near",
    "cartoon","icon","game","specific","small","tiny","clear","strong","bronze","clay","round","visible","stylized",
    "battle","defense","defend","guard","command","commander","route","road","map","tower","banner","base"
  )
  $normalized = Normalize-TextId $Value
  if ([string]::IsNullOrWhiteSpace($normalized)) { return $set }
  foreach ($part in $normalized.Split(' ')) {
    if (-not [string]::IsNullOrWhiteSpace($part) -and ($ignored -notcontains $part)) {
      [void]$set.Add($part)
    }
  }
  return $set
}

function Get-OverlapCount($LeftSet, $RightSet) {
  $count = 0
  foreach ($item in $LeftSet) {
    if ($RightSet.Contains($item)) {
      $count++
    }
  }
  return $count
}

function Get-IconDuplicateReview([string]$RepoRoot, [string]$GameId, [string]$Subject, [string]$Motif, [string]$Silhouette) {
  $subjectTokens = Get-TokenSet $Subject
  $subjectNorm = Normalize-TextId $Subject
  $silhouetteNorm = Normalize-TextId $Silhouette
  $risk = "low"
  $matches = @()
  $metadataRoot = Join-Path $RepoRoot "artifacts\icons"
  if (-not (Test-Path -LiteralPath $metadataRoot)) {
    return [pscustomobject]@{
      Risk = $risk
      Matches = @()
    }
  }

  $metadataFiles = Get-ChildItem -Path $metadataRoot -Recurse -File -Filter "metadata.json" -ErrorAction SilentlyContinue
  foreach ($file in $metadataFiles) {
    $metadata = Read-JsonObject $file.FullName
    if ($null -eq $metadata) { continue }
    $otherGameId = [string]$metadata.game_id
    if ([string]::IsNullOrWhiteSpace($otherGameId) -or $otherGameId -eq $GameId) { continue }
    $otherSubject = [string]$metadata.icon_subject
    if ([string]::IsNullOrWhiteSpace($otherSubject)) { $otherSubject = [string]$metadata.subject }
    $otherMotif = [string]$metadata.motif
    $otherSilhouette = [string]$metadata.icon_silhouette
    $otherTokens = Get-TokenSet $otherSubject
    $subjectOverlap = Get-OverlapCount $subjectTokens $otherTokens
    $sameSubject = ($subjectNorm -ne "") -and ($subjectNorm -eq (Normalize-TextId $otherSubject))
    $sameMotif = (-not [string]::IsNullOrWhiteSpace($Motif)) -and ($Motif -eq $otherMotif)
    $sameSilhouette = ($silhouetteNorm -ne "") -and ($silhouetteNorm -eq (Normalize-TextId $otherSilhouette))
    $otherRisk = ""

    if ($sameSubject -or $sameMotif -or ($sameMotif -and $subjectOverlap -ge 1)) {
      $otherRisk = "high"
    } elseif ($sameSilhouette -or $subjectOverlap -ge 1) {
      $otherRisk = "medium"
    }

    if (-not [string]::IsNullOrWhiteSpace($otherRisk)) {
      $matches += [pscustomobject]@{
        game_id = $otherGameId
        risk = $otherRisk
        motif = $otherMotif
        icon_subject = $otherSubject
      }
      if ($otherRisk -eq "high") {
        $risk = "high"
      } elseif ($risk -ne "high") {
        $risk = "medium"
      }
    }
  }

  return [pscustomobject]@{
    Risk = $risk
    Matches = $matches
  }
}

function Join-TextParts([string[]]$Parts) {
  $filtered = @()
  foreach ($part in $Parts) {
    if (-not [string]::IsNullOrWhiteSpace($part)) {
      $filtered += $part.Trim()
    }
  }
  return ($filtered -join " ").Trim()
}

function Get-IconDirection([string]$RepoRoot, [string]$GameId, [string]$ExplicitSubject) {
  $result = [ordered]@{
    Subject = $ExplicitSubject
    Silhouette = ""
    VisualIdentitySource = ""
    Forbidden = @()
  }
  if ([string]::IsNullOrWhiteSpace($GameId)) {
    return [pscustomobject]$result
  }

  $visualIdentityPath = Join-Path $RepoRoot ("artifacts\requirements\" + $GameId + "\visual_identity.json")
  $visualIdentity = Read-JsonObject $visualIdentityPath
  if ($null -eq $visualIdentity) {
    return [pscustomobject]$result
  }

  $iconIdentity = $visualIdentity.icon_identity
  $iconSubject = ""
  $iconSilhouette = ""
  $forbidden = @()

  if ($null -ne $iconIdentity) {
    if ($null -ne $iconIdentity.subject) { $iconSubject = [string]$iconIdentity.subject }
    if ($null -ne $iconIdentity.silhouette) { $iconSilhouette = [string]$iconIdentity.silhouette }
    if ($null -ne $iconIdentity.forbidden_icon_reuse) {
      $forbidden += @($iconIdentity.forbidden_icon_reuse | ForEach-Object { [string]$_ })
    }
  }
  if ($null -ne $visualIdentity.forbidden_visual_reuse) {
    $forbidden += @($visualIdentity.forbidden_visual_reuse | ForEach-Object { [string]$_ })
  }

  if (-not [string]::IsNullOrWhiteSpace($iconSubject)) {
    $result.Subject = $iconSubject.Trim()
  } else {
    $result.Subject = $ExplicitSubject
  }
  $result.Silhouette = $iconSilhouette
  $result.VisualIdentitySource = $visualIdentityPath
  $result.Forbidden = $forbidden
  return [pscustomobject]$result
}

function New-Color([string]$Hex, [int]$Alpha = 255) {
  $value = $Hex.Trim().TrimStart('#')
  if ($value.Length -ne 6) {
    return [System.Drawing.Color]::FromArgb($Alpha, 32, 48, 74)
  }
  $r = [Convert]::ToInt32($value.Substring(0, 2), 16)
  $g = [Convert]::ToInt32($value.Substring(2, 2), 16)
  $b = [Convert]::ToInt32($value.Substring(4, 2), 16)
  return [System.Drawing.Color]::FromArgb($Alpha, $r, $g, $b)
}

function Get-DrawAlpha([int]$ShadowAlpha) {
  if ($ShadowAlpha -gt 0) { return $ShadowAlpha }
  return 255
}

function New-Brush([string]$Hex, [int]$Alpha = 255) {
  return [System.Drawing.SolidBrush]::new((New-Color $Hex $Alpha))
}

function New-PenFromHex([string]$Hex, [single]$Width, [int]$Alpha = 255) {
  return [System.Drawing.Pen]::new((New-Color $Hex $Alpha), $Width)
}

function New-Point([int]$X, [int]$Y) {
  return [System.Drawing.Point]::new($X, $Y)
}

function New-RoundedPath([int]$X, [int]$Y, [int]$Width, [int]$Height, [int]$Radius) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $diameter = $Radius * 2
  $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
  $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
  $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
  $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
  $path.CloseFigure()
  return $path
}

function Get-Motif([string]$Text, [string[]]$Forbidden = @()) {
  $value = $Text.ToLowerInvariant()
  $forbiddenText = (($Forbidden | ForEach-Object { [string]$_ }) -join " ").ToLowerInvariant()
  $banCastle = $forbiddenText -match 'castle|tower'
  $banZombie = $forbiddenText -match 'zombie'
  $banHelmet = $forbiddenText -match 'helmet|gas-mask|gas mask'
  $banShield = $forbiddenText -match 'shield|crest'
  if ($value -match 'prefecture|atlas|ticket|map pin|regional map pin') { return "atlaspin" }
  if ($value -match 'bridge|river|crossing|gatehouse|crowned bridge|route markers') { return "crownbridge" }
  if (($value -match 'ring|perimeter|outpost|lamp|yard') -and ($value -match 'scrap|barricade|hazard|feral|ghoul|quarantine')) { return "scrapring" }
  if ($value -match 'mountain|ridge|pass|bunker|flare|canyon|notch|rockslide') { return "mountainbunker" }
  if ($value -match 'barracks|camp|command|commander|wheat|resource|map grid|tactical map|town hut|worker|villager') { return "commandcamp" }
  if (($value -match 'fist|knuckle|glove') -and ($value -match 'sign|street|district|warrant|route|block')) { return "fistsign" }
  if ($value -match 'pennant|banner|standard|milestone|tactical-map|tactical map|tile pattern|tile grid') { return "warpennant" }
  if ($value -match 'snow|ice|frost') { return "snowman" }
  if ($value -match 'dragon|wyrm|drake|flame horn|horn crest|half-dragon|fire warrior') { return "dragonflame" }
  if ((-not $banCastle) -and $value -match 'castle|royal|keep|king') { return "castle" }
  if ($value -match 'bomb|bomber|blast') { return "bomb" }
  if ($value -match 'flower basket|blossom basket|basket|blossom crown|hero blossom|petal isle') { return "flowerbasket" }
  if ($value -match 'garden|plant|farm|orchard|pasture|bloom') { return "leaf" }
  if ((-not $banZombie) -and $value -match 'zombie|survival|barricade') { return "zombie" }
  if ($value -match 'runner|dash|escape|relic') { return "runner" }
  if ($value -match 'courier|switchyard|rail|delivery|clockwork') { return "courier" }
  if ((-not $banShield) -and (-not $banHelmet) -and $value -match 'war|battle|strike|campaign|territory|siege|frontier') { return "swordshield" }
  if ($value -match 'plane|air|sky|flight') { return "plane" }
  if ($value -match 'gold|miner|hook') { return "hook" }
  return "shieldstar"
}

function Get-Palette([string]$Motif) {
  switch ($Motif) {
    "atlaspin" {
      return @{
        BgStart = "#8FD7FF"; BgEnd = "#FFB874"; Primary = "#FFF9EC"; Secondary = "#2C5F9E"; Accent = "#F05D5E"; Outline = "#173A63"; Spot = "#FFE6A7"
      }
    }
    "crownbridge" {
      return @{
        BgStart = "#18314E"; BgEnd = "#6C2833"; Primary = "#E7D9B0"; Secondary = "#9AABB8"; Accent = "#D8A044"; Outline = "#16263D"; Spot = "#F6E7B8"
      }
    }
    "scrapring" {
      return @{
        BgStart = "#2A221B"; BgEnd = "#8A4B2F"; Primary = "#8E7A65"; Secondary = "#F2B544"; Accent = "#9BE86B"; Outline = "#18120D"; Spot = "#FFD27A"
      }
    }
    "fistsign" {
      return @{
        BgStart = "#1B2433"; BgEnd = "#C67B32"; Primary = "#D0B79C"; Secondary = "#E7E0D6"; Accent = "#ED9E3D"; Outline = "#6B5846"; Spot = "#FFD6B4"
      }
    }
    "commandcamp" {
      return @{
        BgStart = "#263326"; BgEnd = "#B7792F"; Primary = "#D9A441"; Secondary = "#EEE2B7"; Accent = "#5EE0BE"; Outline = "#1C2A24"; Spot = "#F7D784"
      }
    }
    "mountainbunker" {
      return @{
        BgStart = "#1E2B24"; BgEnd = "#6F4030"; Primary = "#7A8E86"; Secondary = "#D7A35B"; Accent = "#D94841"; Outline = "#18211D"; Spot = "#FFD589"
      }
    }
    "warpennant" {
      return @{
        BgStart = "#19263C"; BgEnd = "#6E2432"; Primary = "#E8E0D2"; Secondary = "#B68B44"; Accent = "#D85B58"; Outline = "#22304A"; Spot = "#F5E9BC"
      }
    }
    "snowman" {
      return @{
        BgStart = "#72D7FF"; BgEnd = "#2F8BFF"; Primary = "#FFFFFF"; Secondary = "#FF8B38"; Accent = "#1D4ED8"; Outline = "#15315C"; Spot = "#FFF1B8"
      }
    }
    "dragonflame" {
      return @{
        BgStart = "#28121C"; BgEnd = "#B13B1C"; Primary = "#8A141E"; Secondary = "#FFB44C"; Accent = "#FFE7B0"; Outline = "#230C12"; Spot = "#FFD38A"
      }
    }
    "castle" {
      return @{
        BgStart = "#A78BFA"; BgEnd = "#FB7185"; Primary = "#F7E7A8"; Secondary = "#5B4ABF"; Accent = "#FFFFFF"; Outline = "#34285E"; Spot = "#FFE9A8"
      }
    }
    "bomb" {
      return @{
        BgStart = "#FFD166"; BgEnd = "#FF5B5B"; Primary = "#2B2B33"; Secondary = "#FFDD55"; Accent = "#FFFFFF"; Outline = "#402118"; Spot = "#FFF3B0"
      }
    }
    "flowerbasket" {
      return @{
        BgStart = "#8EDBFF"; BgEnd = "#F7B8D6"; Primary = "#E5B577"; Secondary = "#FF6FA4"; Accent = "#FFE36D"; Outline = "#6A4B39"; Spot = "#EFFFFA"
      }
    }
    "leaf" {
      return @{
        BgStart = "#FFE46B"; BgEnd = "#63CE61"; Primary = "#42A84C"; Secondary = "#FFFFFF"; Accent = "#FF7E79"; Outline = "#274122"; Spot = "#FFF9C2"
      }
    }
    "zombie" {
      return @{
        BgStart = "#8EEB6D"; BgEnd = "#2BAE66"; Primary = "#C9FFAF"; Secondary = "#5B2366"; Accent = "#FFFFFF"; Outline = "#20331A"; Spot = "#F7FFB8"
      }
    }
    "runner" {
      return @{
        BgStart = "#60D3FF"; BgEnd = "#FF9E45"; Primary = "#FFFFFF"; Secondary = "#1E3A8A"; Accent = "#FFD166"; Outline = "#1E2A4A"; Spot = "#FFF3C4"
      }
    }
    "courier" {
      return @{
        BgStart = "#7DD3FC"; BgEnd = "#F59E0B"; Primary = "#FFFFFF"; Secondary = "#0F3D62"; Accent = "#F97316"; Outline = "#18314F"; Spot = "#FFF0B3"
      }
    }
    "swordshield" {
      return @{
        BgStart = "#60A5FA"; BgEnd = "#A855F7"; Primary = "#EAF2FF"; Secondary = "#FACC15"; Accent = "#FFFFFF"; Outline = "#1E2A4A"; Spot = "#FFE9A8"
      }
    }
    "plane" {
      return @{
        BgStart = "#7FDBFF"; BgEnd = "#6C5CE7"; Primary = "#F8FBFF"; Secondary = "#2E5BFF"; Accent = "#FFD166"; Outline = "#18314F"; Spot = "#E8F7FF"
      }
    }
    "hook" {
      return @{
        BgStart = "#67E8F9"; BgEnd = "#EAB308"; Primary = "#FFF7CC"; Secondary = "#8B5CF6"; Accent = "#FFFFFF"; Outline = "#21405A"; Spot = "#FFF0A6"
      }
    }
    default {
      return @{
        BgStart = "#7DD3FC"; BgEnd = "#F472B6"; Primary = "#FFFFFF"; Secondary = "#FACC15"; Accent = "#1D4ED8"; Outline = "#22304A"; Spot = "#FFF3A8"
      }
    }
  }
}

function Fill-ClosedCurve([System.Drawing.Graphics]$Graphics, [System.Drawing.Brush]$Brush, [System.Drawing.Point[]]$Points) {
  $Graphics.FillClosedCurve($Brush, $Points)
}

function Draw-Snowman([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $bodyBrush = New-Brush $Palette.Primary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $darkBrush = New-Brush $Palette.Outline $alpha
  $Graphics.FillEllipse($bodyBrush, 282 + $OffsetX, 410 + $OffsetY, 460, 400)
  $Graphics.DrawEllipse($outline, 282 + $OffsetX, 410 + $OffsetY, 460, 400)
  $Graphics.FillEllipse($bodyBrush, 360 + $OffsetX, 210 + $OffsetY, 304, 270)
  $Graphics.DrawEllipse($outline, 360 + $OffsetX, 210 + $OffsetY, 304, 270)
  $Graphics.FillRectangle($accentBrush, 316 + $OffsetX, 430 + $OffsetY, 392, 78)
  $Graphics.FillRectangle($accentBrush, 612 + $OffsetX, 470 + $OffsetY, 120, 58)
  $Graphics.FillEllipse($darkBrush, 444 + $OffsetX, 324 + $OffsetY, 34, 34)
  $Graphics.FillEllipse($darkBrush, 546 + $OffsetX, 324 + $OffsetY, 34, 34)
  $Graphics.FillPolygon($secondaryBrush, @(
    (New-Point (500 + $OffsetX) (352 + $OffsetY)),
    (New-Point (626 + $OffsetX) (392 + $OffsetY)),
    (New-Point (500 + $OffsetX) (424 + $OffsetY))
  ))
  $outline.Dispose()
  $bodyBrush.Dispose()
  $accentBrush.Dispose()
  $secondaryBrush.Dispose()
  $darkBrush.Dispose()
}

function Draw-FistSign([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 20 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $fistBrush = New-Brush $Palette.Primary $alpha
  $signBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $darkBrush = New-Brush $Palette.Outline $alpha
  $tapeBrush = New-Brush "#EFE6D3" $alpha
  $signPath = New-RoundedPath (292 + $OffsetX) (384 + $OffsetY) 426 224 34

  $Graphics.FillPath($signBrush, $signPath)
  $Graphics.DrawPath($outline, $signPath)
  $Graphics.FillRectangle($darkBrush, 346 + $OffsetX, 566 + $OffsetY, 34, 136)
  $Graphics.FillRectangle($darkBrush, 626 + $OffsetX, 548 + $OffsetY, 34, 154)
  $Graphics.FillRectangle($accentBrush, 356 + $OffsetX, 412 + $OffsetY, 288, 28)
  $Graphics.DrawLine($outline, 374 + $OffsetX, 450 + $OffsetY, 474 + $OffsetX, 516 + $OffsetY)
  $Graphics.DrawLine($outline, 548 + $OffsetX, 430 + $OffsetY, 486 + $OffsetX, 560 + $OffsetY)
  $Graphics.DrawLine($outline, 444 + $OffsetX, 522 + $OffsetY, 410 + $OffsetX, 592 + $OffsetY)
  $Graphics.FillRectangle($tapeBrush, 278 + $OffsetX, 354 + $OffsetY, 88, 30)
  $Graphics.FillRectangle($tapeBrush, 658 + $OffsetX, 334 + $OffsetY, 76, 30)

  $palmPath = New-RoundedPath (404 + $OffsetX) (270 + $OffsetY) 188 320 48
  $Graphics.FillPath($fistBrush, $palmPath)
  $Graphics.DrawPath($outline, $palmPath)
  foreach ($finger in @(
    @{ X = 382; Y = 226; W = 52; H = 122 },
    @{ X = 436; Y = 204; W = 54; H = 132 },
    @{ X = 492; Y = 218; W = 54; H = 126 },
    @{ X = 548; Y = 254; W = 50; H = 120 }
  )) {
    $fingerPath = New-RoundedPath ($finger.X + $OffsetX) ($finger.Y + $OffsetY) $finger.W $finger.H 16
    $Graphics.FillPath($fistBrush, $fingerPath)
    $Graphics.DrawPath($outline, $fingerPath)
    $fingerPath.Dispose()
  }
  $thumbPoints = @(
    (New-Point (570 + $OffsetX) (434 + $OffsetY)),
    (New-Point (664 + $OffsetX) (484 + $OffsetY)),
    (New-Point (700 + $OffsetX) (556 + $OffsetY)),
    (New-Point (632 + $OffsetX) (574 + $OffsetY)),
    (New-Point (552 + $OffsetX) (536 + $OffsetY))
  )
  $Graphics.FillPolygon($fistBrush, $thumbPoints)
  $Graphics.DrawPolygon($outline, $thumbPoints)
  $Graphics.FillRectangle($tapeBrush, 426 + $OffsetX, 466 + $OffsetY, 154, 42)
  $Graphics.FillRectangle($tapeBrush, 420 + $OffsetX, 544 + $OffsetY, 172, 38)
  $Graphics.DrawLine($outline, 482 + $OffsetX, 278 + $OffsetY, 482 + $OffsetX, 336 + $OffsetY)
  $Graphics.DrawLine($outline, 536 + $OffsetX, 288 + $OffsetY, 536 + $OffsetX, 344 + $OffsetY)
  $Graphics.DrawLine($outline, 588 + $OffsetX, 318 + $OffsetY, 588 + $OffsetX, 372 + $OffsetY)

  $weaponPoints = @(
    (New-Point (278 + $OffsetX) (712 + $OffsetY)),
    (New-Point (628 + $OffsetX) (646 + $OffsetY)),
    (New-Point (668 + $OffsetX) (676 + $OffsetY)),
    (New-Point (320 + $OffsetX) (742 + $OffsetY))
  )
  $Graphics.FillPolygon($darkBrush, $weaponPoints)
  $Graphics.FillRectangle($darkBrush, 612 + $OffsetX, 632 + $OffsetY, 112, 54)
  $Graphics.FillRectangle($darkBrush, 260 + $OffsetX, 694 + $OffsetY, 44, 98)
  $slashPen = New-PenFromHex "#C9605A" 8 $alpha
  $Graphics.DrawLine($slashPen, 278 + $OffsetX, 756 + $OffsetY, 356 + $OffsetX, 708 + $OffsetY)
  $Graphics.DrawLine($slashPen, 622 + $OffsetX, 670 + $OffsetY, 688 + $OffsetX, 762 + $OffsetY)

  $signPath.Dispose()
  $palmPath.Dispose()
  $outline.Dispose()
  $fistBrush.Dispose()
  $signBrush.Dispose()
  $accentBrush.Dispose()
  $darkBrush.Dispose()
  $tapeBrush.Dispose()
  $slashPen.Dispose()
}

function Draw-CommandCamp([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $mapBrush = New-Brush $Palette.Secondary $alpha
  $bronzeBrush = New-Brush $Palette.Primary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $darkBrush = New-Brush $Palette.Outline $alpha
  $wheatBrush = New-Brush $Palette.Spot $alpha
  $gridPen = New-PenFromHex "#8E6A38" 8 ([Math]::Max(70, [int]($alpha * 0.55)))
  $mapPath = New-RoundedPath (220 + $OffsetX) (292 + $OffsetY) 584 430 70
  $Graphics.FillPath($mapBrush, $mapPath)
  $Graphics.DrawPath($outline, $mapPath)
  foreach ($x in @(310, 430, 550, 670)) {
    $Graphics.DrawLine($gridPen, $x + $OffsetX, 322 + $OffsetY, $x + $OffsetX, 692 + $OffsetY)
  }
  foreach ($y in @(382, 482, 582)) {
    $Graphics.DrawLine($gridPen, 252 + $OffsetX, $y + $OffsetY, 772 + $OffsetX, $y + $OffsetY)
  }
  $Graphics.FillRectangle($bronzeBrush, 422 + $OffsetX, 386 + $OffsetY, 186, 164)
  $Graphics.DrawRectangle($outline, 422 + $OffsetX, 386 + $OffsetY, 186, 164)
  $Graphics.FillPolygon($darkBrush, @(
    (New-Point (388 + $OffsetX) (400 + $OffsetY)),
    (New-Point (514 + $OffsetX) (310 + $OffsetY)),
    (New-Point (642 + $OffsetX) (400 + $OffsetY))
  ))
  $Graphics.DrawPolygon($outline, @(
    (New-Point (388 + $OffsetX) (400 + $OffsetY)),
    (New-Point (514 + $OffsetX) (310 + $OffsetY)),
    (New-Point (642 + $OffsetX) (400 + $OffsetY))
  ))
  $Graphics.FillRectangle($accentBrush, 492 + $OffsetX, 468 + $OffsetY, 42, 82)
  $Graphics.DrawLine($outline, 296 + $OffsetX, 748 + $OffsetY, 734 + $OffsetX, 236 + $OffsetY)
  $Graphics.FillPolygon($darkBrush, @(
    (New-Point (724 + $OffsetX) (230 + $OffsetY)),
    (New-Point (818 + $OffsetX) (206 + $OffsetY)),
    (New-Point (768 + $OffsetX) (292 + $OffsetY))
  ))
  foreach ($i in 0..4) {
    $baseX = 304 + $OffsetX + $i * 24
    $Graphics.DrawLine($outline, $baseX, 620 + $OffsetY, $baseX + 66, 750 + $OffsetY)
    $Graphics.FillEllipse($wheatBrush, $baseX + 42, 610 + $OffsetY + $i * 16, 34, 54)
  }
  $Graphics.FillEllipse($accentBrush, 646 + $OffsetX, 556 + $OffsetY, 118, 86)
  $Graphics.FillRectangle($darkBrush, 674 + $OffsetX, 586 + $OffsetY, 64, 18)
  $mapPath.Dispose()
  $outline.Dispose()
  $mapBrush.Dispose()
  $bronzeBrush.Dispose()
  $accentBrush.Dispose()
  $darkBrush.Dispose()
  $wheatBrush.Dispose()
  $gridPen.Dispose()
}

function Draw-MountainBunker([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $mountainBrush = New-Brush $Palette.Primary $alpha
  $amberBrush = New-Brush $Palette.Secondary $alpha
  $flareBrush = New-Brush $Palette.Accent $alpha
  $glowBrush = New-Brush $Palette.Spot ([Math]::Max(90, [int]($alpha * 0.55)))
  $darkBrush = New-Brush $Palette.Outline $alpha
  $steelBrush = New-Brush "#A7B7B0" $alpha
  $pathBrush = New-Brush "#705447" $alpha

  $leftMountain = @(
    (New-Point (168 + $OffsetX) (758 + $OffsetY)),
    (New-Point (344 + $OffsetX) (350 + $OffsetY)),
    (New-Point (508 + $OffsetX) (604 + $OffsetY)),
    (New-Point (508 + $OffsetX) (858 + $OffsetY)),
    (New-Point (168 + $OffsetX) (858 + $OffsetY))
  )
  $rightMountain = @(
    (New-Point (516 + $OffsetX) (858 + $OffsetY)),
    (New-Point (516 + $OffsetX) (610 + $OffsetY)),
    (New-Point (710 + $OffsetX) (318 + $OffsetY)),
    (New-Point (856 + $OffsetX) (758 + $OffsetY)),
    (New-Point (856 + $OffsetX) (858 + $OffsetY))
  )
  $Graphics.FillPolygon($mountainBrush, $leftMountain)
  $Graphics.FillPolygon($mountainBrush, $rightMountain)
  $Graphics.DrawPolygon($outline, $leftMountain)
  $Graphics.DrawPolygon($outline, $rightMountain)

  $Graphics.FillPolygon($amberBrush, @(
    (New-Point (344 + $OffsetX) (350 + $OffsetY)),
    (New-Point (402 + $OffsetX) (438 + $OffsetY)),
    (New-Point (458 + $OffsetX) (386 + $OffsetY)),
    (New-Point (516 + $OffsetX) (498 + $OffsetY)),
    (New-Point (516 + $OffsetX) (604 + $OffsetY)),
    (New-Point (508 + $OffsetX) (604 + $OffsetY))
  ))
  $Graphics.FillPolygon($amberBrush, @(
    (New-Point (710 + $OffsetX) (318 + $OffsetY)),
    (New-Point (666 + $OffsetX) (416 + $OffsetY)),
    (New-Point (616 + $OffsetX) (372 + $OffsetY)),
    (New-Point (548 + $OffsetX) (516 + $OffsetY)),
    (New-Point (516 + $OffsetX) (610 + $OffsetY))
  ))

  $bunkerPath = New-RoundedPath (364 + $OffsetX) (598 + $OffsetY) 300 184 38
  $Graphics.FillPath($steelBrush, $bunkerPath)
  $Graphics.DrawPath($outline, $bunkerPath)
  $Graphics.FillRectangle($darkBrush, 474 + $OffsetX, 648 + $OffsetY, 82, 134)
  $Graphics.FillRectangle($amberBrush, 400 + $OffsetX, 632 + $OffsetY, 58, 34)
  $Graphics.FillRectangle($amberBrush, 570 + $OffsetX, 632 + $OffsetY, 58, 34)
  $Graphics.FillRectangle($pathBrush, 468 + $OffsetX, 782 + $OffsetY, 96, 72)
  $Graphics.FillEllipse($glowBrush, 434 + $OffsetX, 650 + $OffsetY, 42, 42)
  $Graphics.FillEllipse($glowBrush, 548 + $OffsetX, 650 + $OffsetY, 42, 42)

  $Graphics.DrawLine($outline, 514 + $OffsetX, 452 + $OffsetY, 514 + $OffsetX, 612 + $OffsetY)
  $Graphics.FillPolygon($flareBrush, @(
    (New-Point (514 + $OffsetX) (452 + $OffsetY)),
    (New-Point (628 + $OffsetX) (492 + $OffsetY)),
    (New-Point (514 + $OffsetX) (548 + $OffsetY))
  ))
  $flarePen = New-PenFromHex "#FFB067" 16 $alpha
  $flarePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $flarePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $Graphics.DrawLine($flarePen, 640 + $OffsetX, 244 + $OffsetY, 742 + $OffsetX, 140 + $OffsetY)
  $Graphics.FillEllipse($flareBrush, 720 + $OffsetX, 118 + $OffsetY, 74, 74)
  $Graphics.FillEllipse($glowBrush, 696 + $OffsetX, 96 + $OffsetY, 122, 122)

  foreach ($line in @(
    @(286, 748, 420, 700),
    @(734, 734, 606, 692),
    @(304, 804, 444, 756),
    @(720, 790, 584, 746)
  )) {
    $Graphics.DrawLine($outline, $line[0] + $OffsetX, $line[1] + $OffsetY, $line[2] + $OffsetX, $line[3] + $OffsetY)
  }

  $bunkerPath.Dispose()
  $outline.Dispose()
  $mountainBrush.Dispose()
  $amberBrush.Dispose()
  $flareBrush.Dispose()
  $glowBrush.Dispose()
  $darkBrush.Dispose()
  $steelBrush.Dispose()
  $pathBrush.Dispose()
  $flarePen.Dispose()
}

function Draw-CrownBridge([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $spotBrush = New-Brush $Palette.Spot $alpha
  $riverPen = New-PenFromHex $Palette.Accent 34 $alpha
  $riverPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $riverPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $riverPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $Graphics.DrawArc($riverPen, 168 + $OffsetX, 608 + $OffsetY, 670, 240, 200, 138)
  $Graphics.DrawArc($riverPen, 182 + $OffsetX, 650 + $OffsetY, 636, 188, 194, 132)

  $bridgePath = New-Object System.Drawing.Drawing2D.GraphicsPath
  $bridgePath.AddArc(298 + $OffsetX, 438 + $OffsetY, 424, 292, 198, 144)
  $bridgePath.AddLine(610 + $OffsetX, 562 + $OffsetY, 566 + $OffsetX, 562 + $OffsetY)
  $bridgePath.AddLine(454 + $OffsetX, 562 + $OffsetY, 410 + $OffsetX, 562 + $OffsetY)
  $bridgePath.CloseFigure()
  $Graphics.FillPath($primaryBrush, $bridgePath)
  $Graphics.DrawPath($outline, $bridgePath)
  $bridgePath.Dispose()

  $Graphics.FillRectangle($secondaryBrush, 248 + $OffsetX, 390 + $OffsetY, 144, 246)
  $Graphics.FillRectangle($secondaryBrush, 632 + $OffsetX, 390 + $OffsetY, 144, 246)
  $Graphics.DrawRectangle($outline, 248 + $OffsetX, 390 + $OffsetY, 144, 246)
  $Graphics.DrawRectangle($outline, 632 + $OffsetX, 390 + $OffsetY, 144, 246)
  $Graphics.FillRectangle($primaryBrush, 378 + $OffsetX, 302 + $OffsetY, 268, 210)
  $Graphics.DrawRectangle($outline, 378 + $OffsetX, 302 + $OffsetY, 268, 210)
  $Graphics.FillRectangle($secondaryBrush, 454 + $OffsetX, 404 + $OffsetY, 116, 108)
  $Graphics.DrawRectangle($outline, 454 + $OffsetX, 404 + $OffsetY, 116, 108)

  foreach ($x in @(278, 346, 410, 478, 546, 614, 678, 742)) {
    $Graphics.FillRectangle($primaryBrush, $x + $OffsetX, 248 + $OffsetY, 44, 66)
    $Graphics.DrawRectangle($outline, $x + $OffsetX, 248 + $OffsetY, 44, 66)
  }

  $crownPoints = @(
    (New-Point (390 + $OffsetX) (208 + $OffsetY)),
    (New-Point (446 + $OffsetX) (140 + $OffsetY)),
    (New-Point (512 + $OffsetX) (206 + $OffsetY)),
    (New-Point (570 + $OffsetX) (132 + $OffsetY)),
    (New-Point (634 + $OffsetX) (208 + $OffsetY)),
    (New-Point (634 + $OffsetX) (270 + $OffsetY)),
    (New-Point (390 + $OffsetX) (270 + $OffsetY))
  )
  $Graphics.FillPolygon($accentBrush, $crownPoints)
  $Graphics.DrawPolygon($outline, $crownPoints)
  foreach ($dot in @(@(444,176), @(570,170), @(512,212))) {
    $Graphics.FillEllipse($spotBrush, $dot[0] + $OffsetX, $dot[1] + $OffsetY, 26, 26)
  }

  foreach ($pin in @(@(324,718), @(418,680), @(512,736), @(612,686), @(708,722))) {
    $Graphics.FillEllipse($spotBrush, $pin[0] + $OffsetX, $pin[1] + $OffsetY, 32, 32)
    $Graphics.DrawEllipse($outline, $pin[0] + $OffsetX, $pin[1] + $OffsetY, 32, 32)
  }

  $riverPen.Dispose()
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
  $spotBrush.Dispose()
}

function Draw-Castle([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $Graphics.FillRectangle($primaryBrush, 276 + $OffsetX, 424 + $OffsetY, 472, 300)
  $Graphics.DrawRectangle($outline, 276 + $OffsetX, 424 + $OffsetY, 472, 300)
  $Graphics.FillRectangle($primaryBrush, 216 + $OffsetX, 332 + $OffsetY, 118, 392)
  $Graphics.FillRectangle($primaryBrush, 690 + $OffsetX, 332 + $OffsetY, 118, 392)
  $Graphics.FillRectangle($primaryBrush, 404 + $OffsetX, 244 + $OffsetY, 216, 480)
  $Graphics.FillRectangle($secondaryBrush, 442 + $OffsetX, 518 + $OffsetY, 140, 206)
  $Graphics.FillRectangle($accentBrush, 474 + $OffsetX, 196 + $OffsetY, 24, 92)
  $Graphics.FillPolygon($accentBrush, @(
    (New-Point (498 + $OffsetX) (196 + $OffsetY)),
    (New-Point (600 + $OffsetX) (226 + $OffsetY)),
    (New-Point (498 + $OffsetX) (272 + $OffsetY))
  ))
  foreach ($x in @(234, 430, 548, 708)) {
    $Graphics.FillRectangle($secondaryBrush, $x + $OffsetX, 280 + $OffsetY, 56, 64)
  }
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-Bomb([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $Graphics.FillEllipse($primaryBrush, 252 + $OffsetX, 330 + $OffsetY, 520, 520)
  $Graphics.DrawEllipse($outline, 252 + $OffsetX, 330 + $OffsetY, 520, 520)
  $Graphics.FillRectangle($secondaryBrush, 456 + $OffsetX, 230 + $OffsetY, 112, 102)
  $Graphics.DrawRectangle($outline, 456 + $OffsetX, 230 + $OffsetY, 112, 102)
  $Graphics.DrawArc($outline, 554 + $OffsetX, 150 + $OffsetY, 182, 182, 140, 154)
  $Graphics.FillEllipse($accentBrush, 666 + $OffsetX, 110 + $OffsetY, 80, 80)
  foreach ($pair in @(@(708, 76, 708, 36), @(756, 126, 804, 114), @(670, 140, 630, 188), @(738, 168, 770, 214))) {
    $Graphics.DrawLine($outline, $pair[0] + $OffsetX, $pair[1] + $OffsetY, $pair[2] + $OffsetX, $pair[3] + $OffsetY)
  }
  $Graphics.FillEllipse($accentBrush, 368 + $OffsetX, 450 + $OffsetY, 72, 72)
  $Graphics.FillEllipse($accentBrush, 470 + $OffsetX, 564 + $OffsetY, 72, 72)
  $Graphics.FillEllipse($accentBrush, 572 + $OffsetX, 450 + $OffsetY, 72, 72)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-Leaf([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 26 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $leftLeaf = @(
    (New-Point (508 + $OffsetX) (254 + $OffsetY)),
    (New-Point (290 + $OffsetX) (350 + $OffsetY)),
    (New-Point (242 + $OffsetX) (610 + $OffsetY)),
    (New-Point (454 + $OffsetX) (738 + $OffsetY)),
    (New-Point (590 + $OffsetX) (518 + $OffsetY))
  )
  $rightLeaf = @(
    (New-Point (528 + $OffsetX) (286 + $OffsetY)),
    (New-Point (748 + $OffsetX) (348 + $OffsetY)),
    (New-Point (790 + $OffsetX) (610 + $OffsetY)),
    (New-Point (570 + $OffsetX) (760 + $OffsetY)),
    (New-Point (438 + $OffsetX) (510 + $OffsetY))
  )
  Fill-ClosedCurve $Graphics $primaryBrush $leftLeaf
  Fill-ClosedCurve $Graphics $secondaryBrush $rightLeaf
  $Graphics.DrawClosedCurve($outline, $leftLeaf)
  $Graphics.DrawClosedCurve($outline, $rightLeaf)
  $Graphics.DrawLine($outline, 312 + $OffsetX, 596 + $OffsetY, 520 + $OffsetX, 412 + $OffsetY)
  $Graphics.DrawLine($outline, 710 + $OffsetX, 600 + $OffsetY, 520 + $OffsetX, 412 + $OffsetY)
  $Graphics.FillEllipse($accentBrush, 446 + $OffsetX, 690 + $OffsetY, 132, 132)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-FlowerBasket([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $basketBrush = New-Brush $Palette.Primary $alpha
  $pinkBrush = New-Brush $Palette.Secondary $alpha
  $yellowBrush = New-Brush $Palette.Accent $alpha
  $leafBrush = New-Brush "#8EDB93" $alpha
  $skyBrush = New-Brush "#DFF8FF" ([Math]::Max(90, [int]($alpha * 0.72)))
  $smilePen = New-PenFromHex $Palette.Outline 14 $alpha
  $smilePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $smilePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

  $Graphics.FillEllipse($skyBrush, 246 + $OffsetX, 228 + $OffsetY, 536, 248)
  $Graphics.DrawArc($outline, 324 + $OffsetX, 334 + $OffsetY, 376, 332, 194, 152)
  $basketPath = New-RoundedPath (280 + $OffsetX) (506 + $OffsetY) 464 238 92
  $Graphics.FillPath($basketBrush, $basketPath)
  $Graphics.DrawPath($outline, $basketPath)

  foreach ($lineX in @(360, 438, 516, 594, 672)) {
    $Graphics.DrawLine($smilePen, $lineX + $OffsetX, 540 + $OffsetY, $lineX + $OffsetX, 730 + $OffsetY)
  }
  foreach ($lineY in @(566, 618, 670)) {
    $Graphics.DrawArc($smilePen, 314 + $OffsetX, $lineY + $OffsetY, 396, 96, 10, 160)
  }

  $Graphics.FillEllipse($leafBrush, 264 + $OffsetX, 422 + $OffsetY, 156, 108)
  $Graphics.FillEllipse($leafBrush, 612 + $OffsetX, 422 + $OffsetY, 156, 108)
  $Graphics.DrawEllipse($outline, 264 + $OffsetX, 422 + $OffsetY, 156, 108)
  $Graphics.DrawEllipse($outline, 612 + $OffsetX, 422 + $OffsetY, 156, 108)

  foreach ($stem in @(
    @{ X = 432; Top = 382; Bottom = 534 },
    @{ X = 512; Top = 308; Bottom = 536 },
    @{ X = 592; Top = 382; Bottom = 534 }
  )) {
    $Graphics.DrawLine($outline, $stem.X + $OffsetX, $stem.Top + $OffsetY, $stem.X + $OffsetX, $stem.Bottom + $OffsetY)
  }

  foreach ($petal in @(
    @{ X = 392; Y = 318; Brush = "pink"; Size = 134 },
    @{ X = 446; Y = 220; Brush = "yellow"; Size = 166 },
    @{ X = 566; Y = 320; Brush = "pink"; Size = 134 }
  )) {
    $brush = $pinkBrush
    if ($petal.Brush -eq "yellow") { $brush = $yellowBrush }
    $centerX = $petal.X + $OffsetX
    $centerY = $petal.Y + $OffsetY
    $size = $petal.Size
    foreach ($shift in @(
      @{ DX = 0; DY = -56 },
      @{ DX = 54; DY = 0 },
      @{ DX = 0; DY = 56 },
      @{ DX = -54; DY = 0 },
      @{ DX = 38; DY = -38 },
      @{ DX = -38; DY = -38 }
    )) {
      $Graphics.FillEllipse($brush, $centerX + $shift.DX - ($size / 2), $centerY + $shift.DY - ($size / 2), $size, $size)
      $Graphics.DrawEllipse($outline, $centerX + $shift.DX - ($size / 2), $centerY + $shift.DY - ($size / 2), $size, $size)
    }
    $Graphics.FillEllipse($skyBrush, $centerX - 42, $centerY - 42, 84, 84)
    $Graphics.DrawEllipse($outline, $centerX - 42, $centerY - 42, 84, 84)
  }

  $Graphics.FillEllipse($skyBrush, 412 + $OffsetX, 586 + $OffsetY, 44, 30)
  $Graphics.FillEllipse($skyBrush, 564 + $OffsetX, 586 + $OffsetY, 44, 30)
  $Graphics.DrawArc($smilePen, 446 + $OffsetX, 620 + $OffsetY, 132, 62, 10, 160)

  $basketPath.Dispose()
  $outline.Dispose()
  $basketBrush.Dispose()
  $pinkBrush.Dispose()
  $yellowBrush.Dispose()
  $leafBrush.Dispose()
  $skyBrush.Dispose()
  $smilePen.Dispose()
}

function Draw-Zombie([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $Graphics.FillEllipse($primaryBrush, 244 + $OffsetX, 256 + $OffsetY, 536, 536)
  $Graphics.DrawEllipse($outline, 244 + $OffsetX, 256 + $OffsetY, 536, 536)
  $Graphics.FillEllipse($secondaryBrush, 354 + $OffsetX, 418 + $OffsetY, 98, 118)
  $Graphics.FillEllipse($secondaryBrush, 572 + $OffsetX, 400 + $OffsetY, 118, 138)
  $Graphics.FillEllipse($accentBrush, 382 + $OffsetX, 450 + $OffsetY, 34, 34)
  $Graphics.FillEllipse($accentBrush, 616 + $OffsetX, 444 + $OffsetY, 40, 40)
  $Graphics.FillRectangle($secondaryBrush, 400 + $OffsetX, 628 + $OffsetY, 220, 92)
  foreach ($x in @(426, 478, 530, 582)) {
    $Graphics.DrawLine($outline, $x + $OffsetX, 630 + $OffsetY, $x + $OffsetX, 720 + $OffsetY)
  }
  $Graphics.DrawLine($outline, 310 + $OffsetX, 368 + $OffsetY, 358 + $OffsetX, 316 + $OffsetY)
  $Graphics.DrawLine($outline, 656 + $OffsetX, 298 + $OffsetY, 706 + $OffsetX, 240 + $OffsetY)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-Runner([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $Graphics.FillPolygon($secondaryBrush, @(
    (New-Point (336 + $OffsetX) (284 + $OffsetY)),
    (New-Point (604 + $OffsetX) (284 + $OffsetY)),
    (New-Point (430 + $OffsetX) (544 + $OffsetY)),
    (New-Point (640 + $OffsetX) (544 + $OffsetY)),
    (New-Point (386 + $OffsetX) (828 + $OffsetY)),
    (New-Point (470 + $OffsetX) (590 + $OffsetY)),
    (New-Point (304 + $OffsetX) (590 + $OffsetY))
  ))
  $Graphics.DrawPolygon($outline, @(
    (New-Point (336 + $OffsetX) (284 + $OffsetY)),
    (New-Point (604 + $OffsetX) (284 + $OffsetY)),
    (New-Point (430 + $OffsetX) (544 + $OffsetY)),
    (New-Point (640 + $OffsetX) (544 + $OffsetY)),
    (New-Point (386 + $OffsetX) (828 + $OffsetY)),
    (New-Point (470 + $OffsetX) (590 + $OffsetY)),
    (New-Point (304 + $OffsetX) (590 + $OffsetY))
  ))
  $Graphics.FillEllipse($primaryBrush, 586 + $OffsetX, 528 + $OffsetY, 188, 140)
  $Graphics.DrawEllipse($outline, 586 + $OffsetX, 528 + $OffsetY, 188, 140)
  $Graphics.FillRectangle($accentBrush, 248 + $OffsetX, 398 + $OffsetY, 72, 296)
  $Graphics.FillEllipse($accentBrush, 214 + $OffsetX, 360 + $OffsetY, 132, 132)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-Courier([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $Graphics.FillRectangle($primaryBrush, 286 + $OffsetX, 326 + $OffsetY, 452, 372)
  $Graphics.DrawRectangle($outline, 286 + $OffsetX, 326 + $OffsetY, 452, 372)
  $Graphics.DrawLine($outline, 286 + $OffsetX, 326 + $OffsetY, 512 + $OffsetX, 490 + $OffsetY)
  $Graphics.DrawLine($outline, 738 + $OffsetX, 326 + $OffsetY, 512 + $OffsetX, 490 + $OffsetY)
  $Graphics.FillPolygon($accentBrush, @(
    (New-Point (188 + $OffsetX) (432 + $OffsetY)),
    (New-Point (286 + $OffsetX) (382 + $OffsetY)),
    (New-Point (286 + $OffsetX) (482 + $OffsetY))
  ))
  $Graphics.FillPolygon($accentBrush, @(
    (New-Point (188 + $OffsetX) (572 + $OffsetY)),
    (New-Point (286 + $OffsetX) (522 + $OffsetY)),
    (New-Point (286 + $OffsetX) (622 + $OffsetY))
  ))
  $Graphics.FillRectangle($secondaryBrush, 448 + $OffsetX, 326 + $OffsetY, 30, 372)
  $Graphics.FillRectangle($secondaryBrush, 286 + $OffsetX, 474 + $OffsetY, 452, 30)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-WarPennant([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $stoneBrush = New-Brush $Palette.Primary $alpha
  $trimBrush = New-Brush $Palette.Secondary $alpha
  $sealBrush = New-Brush $Palette.Accent $alpha
  $gridPen = New-PenFromHex $Palette.Primary 10 ([Math]::Max(46, [int]($alpha * 0.42)))
  $gridPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $gridPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $flagBrush = New-Brush "#2A466F" $alpha
  $highlightBrush = New-Brush $Palette.Spot ([Math]::Max(72, [int]($alpha * 0.55)))
  foreach ($lineX in @(268, 382, 496, 610, 724)) {
    $Graphics.DrawLine($gridPen, $lineX + $OffsetX, 278 + $OffsetY, $lineX + $OffsetX, 786 + $OffsetY)
  }
  foreach ($lineY in @(302, 414, 526, 638, 750)) {
    $Graphics.DrawLine($gridPen, 224 + $OffsetX, $lineY + $OffsetY, 798 + $OffsetX, $lineY + $OffsetY)
  }
  $milestone = New-RoundedPath (328 + $OffsetX) (548 + $OffsetY) 360 248 72
  $Graphics.FillPath($stoneBrush, $milestone)
  $Graphics.DrawPath($outline, $milestone)
  $Graphics.FillRectangle($trimBrush, 482 + $OffsetX, 300 + $OffsetY, 36, 390)
  $Graphics.DrawLine($outline, 500 + $OffsetX, 280 + $OffsetY, 500 + $OffsetX, 704 + $OffsetY)
  $Graphics.FillPolygon($flagBrush, @(
    (New-Point (500 + $OffsetX) (280 + $OffsetY)),
    (New-Point (742 + $OffsetX) (330 + $OffsetY)),
    (New-Point (660 + $OffsetX) (454 + $OffsetY)),
    (New-Point (742 + $OffsetX) (566 + $OffsetY)),
    (New-Point (500 + $OffsetX) (514 + $OffsetY))
  ))
  $Graphics.DrawPolygon($outline, @(
    (New-Point (500 + $OffsetX) (280 + $OffsetY)),
    (New-Point (742 + $OffsetX) (330 + $OffsetY)),
    (New-Point (660 + $OffsetX) (454 + $OffsetY)),
    (New-Point (742 + $OffsetX) (566 + $OffsetY)),
    (New-Point (500 + $OffsetX) (514 + $OffsetY))
  ))
  $Graphics.FillPolygon($trimBrush, @(
    (New-Point (530 + $OffsetX) (326 + $OffsetY)),
    (New-Point (646 + $OffsetX) (350 + $OffsetY)),
    (New-Point (606 + $OffsetX) (438 + $OffsetY)),
    (New-Point (646 + $OffsetX) (518 + $OffsetY)),
    (New-Point (530 + $OffsetX) (492 + $OffsetY))
  ))
  $Graphics.FillRectangle($trimBrush, 438 + $OffsetX, 620 + $OffsetY, 140, 40)
  $Graphics.FillEllipse($sealBrush, 416 + $OffsetX, 660 + $OffsetY, 184, 112)
  $Graphics.FillEllipse($highlightBrush, 542 + $OffsetX, 342 + $OffsetY, 112, 94)
  $Graphics.FillRectangle($outline.Brush, 470 + $OffsetX, 404 + $OffsetY, 62, 26)
  $Graphics.FillRectangle($outline.Brush, 470 + $OffsetX, 458 + $OffsetY, 62, 26)
  $Graphics.FillRectangle($outline.Brush, 416 + $OffsetX, 712 + $OffsetY, 184, 24)
  $milestone.Dispose()
  $outline.Dispose()
  $stoneBrush.Dispose()
  $trimBrush.Dispose()
  $sealBrush.Dispose()
  $gridPen.Dispose()
  $flagBrush.Dispose()
  $highlightBrush.Dispose()
}

function Draw-SwordShield([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $shieldPoints = @(
    (New-Point (512 + $OffsetX) (184 + $OffsetY)),
    (New-Point (734 + $OffsetX) (304 + $OffsetY)),
    (New-Point (680 + $OffsetX) (706 + $OffsetY)),
    (New-Point (512 + $OffsetX) (844 + $OffsetY)),
    (New-Point (344 + $OffsetX) (706 + $OffsetY)),
    (New-Point (290 + $OffsetX) (304 + $OffsetY))
  )
  $Graphics.FillPolygon($primaryBrush, $shieldPoints)
  $Graphics.DrawPolygon($outline, $shieldPoints)
  $Graphics.FillPolygon($secondaryBrush, @(
    (New-Point (502 + $OffsetX) (238 + $OffsetY)),
    (New-Point (548 + $OffsetX) (238 + $OffsetY)),
    (New-Point (606 + $OffsetX) (628 + $OffsetY)),
    (New-Point (444 + $OffsetX) (628 + $OffsetY))
  ))
  $Graphics.FillRectangle($accentBrush, 378 + $OffsetX, 532 + $OffsetY, 268, 52)
  $Graphics.FillEllipse($accentBrush, 432 + $OffsetX, 356 + $OffsetY, 156, 156)
  $Graphics.FillEllipse($secondaryBrush, 476 + $OffsetX, 400 + $OffsetY, 68, 68)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-Plane([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 26 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $planePoints = @(
    (New-Point (190 + $OffsetX) (562 + $OffsetY)),
    (New-Point (470 + $OffsetX) (494 + $OffsetY)),
    (New-Point (728 + $OffsetX) (206 + $OffsetY)),
    (New-Point (812 + $OffsetX) (286 + $OffsetY)),
    (New-Point (600 + $OffsetX) (542 + $OffsetY)),
    (New-Point (824 + $OffsetX) (604 + $OffsetY)),
    (New-Point (746 + $OffsetX) (692 + $OffsetY)),
    (New-Point (510 + $OffsetX) (634 + $OffsetY)),
    (New-Point (374 + $OffsetX) (826 + $OffsetY)),
    (New-Point (296 + $OffsetX) (746 + $OffsetY)),
    (New-Point (370 + $OffsetX) (614 + $OffsetY)),
    (New-Point (190 + $OffsetX) (562 + $OffsetY))
  )
  $Graphics.FillPolygon($primaryBrush, $planePoints)
  $Graphics.DrawPolygon($outline, $planePoints)
  $Graphics.FillEllipse($secondaryBrush, 532 + $OffsetX, 320 + $OffsetY, 86, 86)
  $Graphics.FillEllipse($accentBrush, 230 + $OffsetX, 486 + $OffsetY, 120, 120)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-Hook([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 30 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $Graphics.DrawLine($outline, 520 + $OffsetX, 174 + $OffsetY, 520 + $OffsetX, 566 + $OffsetY)
  $Graphics.DrawArc($outline, 318 + $OffsetX, 458 + $OffsetY, 250, 250, 8, 220)
  $Graphics.FillPolygon($primaryBrush, @(
    (New-Point (552 + $OffsetX) (612 + $OffsetY)),
    (New-Point (642 + $OffsetX) (742 + $OffsetY)),
    (New-Point (532 + $OffsetX) (820 + $OffsetY)),
    (New-Point (438 + $OffsetX) (720 + $OffsetY))
  ))
  $Graphics.DrawPolygon($outline, @(
    (New-Point (552 + $OffsetX) (612 + $OffsetY)),
    (New-Point (642 + $OffsetX) (742 + $OffsetY)),
    (New-Point (532 + $OffsetX) (820 + $OffsetY)),
    (New-Point (438 + $OffsetX) (720 + $OffsetY))
  ))
  $Graphics.FillEllipse($secondaryBrush, 438 + $OffsetX, 150 + $OffsetY, 162, 118)
  $Graphics.FillEllipse($accentBrush, 474 + $OffsetX, 184 + $OffsetY, 48, 48)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
}

function Draw-ScrapRing([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $ringBrush = New-Brush $Palette.Primary $alpha
  $lampBrush = New-Brush $Palette.Secondary $alpha
  $glowBrush = New-Brush $Palette.Accent ([Math]::Max(80, [int]($alpha * 0.60)))
  $darkBrush = New-Brush $Palette.Outline $alpha
  $hazardPen = New-PenFromHex $Palette.Secondary 18 $alpha
  $hazardPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $coreBrush = New-Brush "#E8D5B0" ([Math]::Max(90, [int]($alpha * 0.75)))

  $Graphics.FillEllipse($ringBrush, 218 + $OffsetX, 244 + $OffsetY, 588, 588)
  $Graphics.DrawEllipse($outline, 218 + $OffsetX, 244 + $OffsetY, 588, 588)
  $Graphics.FillEllipse($darkBrush, 338 + $OffsetX, 364 + $OffsetY, 348, 348)
  $Graphics.DrawEllipse($outline, 338 + $OffsetX, 364 + $OffsetY, 348, 348)
  $Graphics.FillEllipse($coreBrush, 430 + $OffsetX, 456 + $OffsetY, 164, 164)

  foreach ($segment in @(
    @{ X = 336; Y = 250; W = 114; H = 72 },
    @{ X = 574; Y = 246; W = 110; H = 74 },
    @{ X = 696; Y = 430; W = 84; H = 126 },
    @{ X = 236; Y = 438; W = 82; H = 122 },
    @{ X = 332; Y = 720; W = 126; H = 72 },
    @{ X = 570; Y = 720; W = 124; H = 72 }
  )) {
    $plate = New-RoundedPath ($segment.X + $OffsetX) ($segment.Y + $OffsetY) $segment.W $segment.H 18
    $Graphics.FillPath($ringBrush, $plate)
    $Graphics.DrawPath($outline, $plate)
    $plate.Dispose()
  }

  $Graphics.DrawLine($hazardPen, 364 + $OffsetX, 286 + $OffsetY, 424 + $OffsetX, 286 + $OffsetY)
  $Graphics.DrawLine($hazardPen, 600 + $OffsetX, 286 + $OffsetY, 660 + $OffsetX, 286 + $OffsetY)
  $Graphics.DrawLine($hazardPen, 730 + $OffsetX, 458 + $OffsetY, 730 + $OffsetX, 520 + $OffsetY)
  $Graphics.DrawLine($hazardPen, 290 + $OffsetX, 460 + $OffsetY, 290 + $OffsetX, 520 + $OffsetY)
  $Graphics.DrawLine($hazardPen, 382 + $OffsetX, 754 + $OffsetY, 442 + $OffsetX, 754 + $OffsetY)
  $Graphics.DrawLine($hazardPen, 602 + $OffsetX, 754 + $OffsetY, 662 + $OffsetX, 754 + $OffsetY)

  $Graphics.FillRectangle($darkBrush, 676 + $OffsetX, 188 + $OffsetY, 42, 220)
  $Graphics.FillEllipse($lampBrush, 632 + $OffsetX, 132 + $OffsetY, 132, 132)
  $Graphics.FillEllipse($glowBrush, 600 + $OffsetX, 100 + $OffsetY, 196, 196)
  $Graphics.FillEllipse($coreBrush, 668 + $OffsetX, 168 + $OffsetY, 60, 60)

  foreach ($hand in @(
    @( (New-Point (160 + $OffsetX) (520 + $OffsetY)), (New-Point (198 + $OffsetX) (448 + $OffsetY)), (New-Point (252 + $OffsetX) (432 + $OffsetY)), (New-Point (236 + $OffsetX) (508 + $OffsetY)) ),
    @( (New-Point (848 + $OffsetX) (532 + $OffsetY)), (New-Point (826 + $OffsetX) (454 + $OffsetY)), (New-Point (768 + $OffsetX) (430 + $OffsetY)), (New-Point (786 + $OffsetX) (510 + $OffsetY)) ),
    @( (New-Point (398 + $OffsetX) (894 + $OffsetY)), (New-Point (430 + $OffsetX) (828 + $OffsetY)), (New-Point (494 + $OffsetX) (824 + $OffsetY)), (New-Point (476 + $OffsetX) (896 + $OffsetY)) )
  )) {
    $Graphics.FillPolygon($darkBrush, $hand)
    $Graphics.DrawPolygon($outline, $hand)
  }
  foreach ($finger in @(
    @{ X = 208; Y = 382; W = 30; H = 82 },
    @{ X = 242; Y = 366; W = 26; H = 88 },
    @{ X = 274; Y = 372; W = 24; H = 78 },
    @{ X = 734; Y = 370; W = 30; H = 84 },
    @{ X = 768; Y = 354; W = 26; H = 90 },
    @{ X = 800; Y = 364; W = 24; H = 78 },
    @{ X = 448; Y = 792; W = 30; H = 80 },
    @{ X = 484; Y = 786; W = 26; H = 82 },
    @{ X = 520; Y = 794; W = 24; H = 72 }
  )) {
    $fingerPath = New-RoundedPath ($finger.X + $OffsetX) ($finger.Y + $OffsetY) $finger.W $finger.H 12
    $Graphics.FillPath($darkBrush, $fingerPath)
    $Graphics.DrawPath($outline, $fingerPath)
    $fingerPath.Dispose()
  }

  $outline.Dispose()
  $ringBrush.Dispose()
  $lampBrush.Dispose()
  $glowBrush.Dispose()
  $darkBrush.Dispose()
  $hazardPen.Dispose()
  $coreBrush.Dispose()
}

function Draw-ShieldStar([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 28 $alpha
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $shieldPoints = @(
    (New-Point (512 + $OffsetX) (188 + $OffsetY)),
    (New-Point (730 + $OffsetX) (314 + $OffsetY)),
    (New-Point (666 + $OffsetX) (724 + $OffsetY)),
    (New-Point (512 + $OffsetX) (846 + $OffsetY)),
    (New-Point (358 + $OffsetX) (724 + $OffsetY)),
    (New-Point (294 + $OffsetX) (314 + $OffsetY))
  )
  $starPoints = @(
    (New-Point (512 + $OffsetX) (320 + $OffsetY)),
    (New-Point (564 + $OffsetX) (438 + $OffsetY)),
    (New-Point (694 + $OffsetX) (454 + $OffsetY)),
    (New-Point (598 + $OffsetX) (546 + $OffsetY)),
    (New-Point (624 + $OffsetX) (674 + $OffsetY)),
    (New-Point (512 + $OffsetX) (610 + $OffsetY)),
    (New-Point (400 + $OffsetX) (674 + $OffsetY)),
    (New-Point (426 + $OffsetX) (546 + $OffsetY)),
    (New-Point (330 + $OffsetX) (454 + $OffsetY)),
    (New-Point (460 + $OffsetX) (438 + $OffsetY))
  )
  $Graphics.FillPolygon($primaryBrush, $shieldPoints)
  $Graphics.DrawPolygon($outline, $shieldPoints)
  $Graphics.FillPolygon($secondaryBrush, $starPoints)
  $Graphics.DrawPolygon($outline, $starPoints)
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
}

function Draw-DragonFlame([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $darkBrush = New-Brush $Palette.Outline $alpha
  $spotBrush = New-Brush $Palette.Spot ([Math]::Max(110, [int]($alpha * 0.72)))
  $jawBrush = New-Brush "#F4C88A" $alpha
  $eyeBrush = New-Brush "#FFF8D6" $alpha

  $headPoints = @(
    (New-Point (260 + $OffsetX) (398 + $OffsetY)),
    (New-Point (354 + $OffsetX) (234 + $OffsetY)),
    (New-Point (528 + $OffsetX) (162 + $OffsetY)),
    (New-Point (722 + $OffsetX) (214 + $OffsetY)),
    (New-Point (824 + $OffsetX) (356 + $OffsetY)),
    (New-Point (786 + $OffsetX) (554 + $OffsetY)),
    (New-Point (648 + $OffsetX) (724 + $OffsetY)),
    (New-Point (428 + $OffsetX) (742 + $OffsetY)),
    (New-Point (286 + $OffsetX) (612 + $OffsetY))
  )
  $jawPoints = @(
    (New-Point (510 + $OffsetX) (484 + $OffsetY)),
    (New-Point (768 + $OffsetX) (576 + $OffsetY)),
    (New-Point (676 + $OffsetX) (686 + $OffsetY)),
    (New-Point (470 + $OffsetX) (620 + $OffsetY))
  )
  $manePoints = @(
    (New-Point (220 + $OffsetX) (502 + $OffsetY)),
    (New-Point (194 + $OffsetX) (350 + $OffsetY)),
    (New-Point (284 + $OffsetX) (200 + $OffsetY)),
    (New-Point (386 + $OffsetX) (168 + $OffsetY)),
    (New-Point (344 + $OffsetX) (332 + $OffsetY)),
    (New-Point (356 + $OffsetX) (538 + $OffsetY))
  )
  $hornMain = @(
    (New-Point (566 + $OffsetX) (176 + $OffsetY)),
    (New-Point (636 + $OffsetX) (14 + $OffsetY)),
    (New-Point (752 + $OffsetX) (188 + $OffsetY))
  )
  $hornSide = @(
    (New-Point (388 + $OffsetX) (244 + $OffsetY)),
    (New-Point (324 + $OffsetX) (88 + $OffsetY)),
    (New-Point (468 + $OffsetX) (198 + $OffsetY))
  )
  $flameArc = @(
    (New-Point (736 + $OffsetX) (506 + $OffsetY)),
    (New-Point (858 + $OffsetX) (414 + $OffsetY)),
    (New-Point (930 + $OffsetX) (530 + $OffsetY)),
    (New-Point (876 + $OffsetX) (660 + $OffsetY)),
    (New-Point (774 + $OffsetX) (612 + $OffsetY)),
    (New-Point (838 + $OffsetX) (542 + $OffsetY))
  )

  Fill-ClosedCurve $Graphics $spotBrush @(
    (New-Point (186 + $OffsetX) (608 + $OffsetY)),
    (New-Point (226 + $OffsetX) (446 + $OffsetY)),
    (New-Point (306 + $OffsetX) (308 + $OffsetY)),
    (New-Point (438 + $OffsetX) (212 + $OffsetY)),
    (New-Point (572 + $OffsetX) (196 + $OffsetY)),
    (New-Point (438 + $OffsetX) (658 + $OffsetY))
  )
  $Graphics.FillPolygon($secondaryBrush, $manePoints)
  $Graphics.DrawPolygon($outline, $manePoints)
  $Graphics.FillPolygon($primaryBrush, $headPoints)
  $Graphics.DrawPolygon($outline, $headPoints)
  $Graphics.FillPolygon($jawBrush, $jawPoints)
  $Graphics.DrawPolygon($outline, $jawPoints)
  $Graphics.FillPolygon($secondaryBrush, $hornMain)
  $Graphics.DrawPolygon($outline, $hornMain)
  $Graphics.FillPolygon($secondaryBrush, $hornSide)
  $Graphics.DrawPolygon($outline, $hornSide)
  $Graphics.FillPolygon($accentBrush, $flameArc)
  $Graphics.DrawPolygon($outline, $flameArc)
  $Graphics.FillEllipse($darkBrush, 470 + $OffsetX, 336 + $OffsetY, 160, 124)
  $Graphics.FillEllipse($eyeBrush, 512 + $OffsetX, 366 + $OffsetY, 76, 46)
  $Graphics.FillEllipse($darkBrush, 544 + $OffsetX, 380 + $OffsetY, 18, 22)
  $Graphics.DrawLine($outline, 560 + $OffsetX, 520 + $OffsetY, 690 + $OffsetX, 556 + $OffsetY)
  foreach ($tooth in @(
    @{ X = 598; Y = 520; W = 18; H = 34 },
    @{ X = 632; Y = 532; W = 16; H = 28 },
    @{ X = 664; Y = 544; W = 14; H = 24 }
  )) {
    $toothPoints = @(
      (New-Point ($tooth.X + $OffsetX) ($tooth.Y + $OffsetY)),
      (New-Point ($tooth.X + $tooth.W + $OffsetX) ($tooth.Y + $OffsetY)),
      (New-Point ($tooth.X + ($tooth.W / 2) + $OffsetX) ($tooth.Y + $tooth.H + $OffsetY))
    )
    $Graphics.FillPolygon($accentBrush, $toothPoints)
    $Graphics.DrawPolygon($outline, $toothPoints)
  }

  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
  $darkBrush.Dispose()
  $spotBrush.Dispose()
  $jawBrush.Dispose()
  $eyeBrush.Dispose()
}

function Draw-AtlasPin([System.Drawing.Graphics]$Graphics, [hashtable]$Palette, [int]$OffsetX, [int]$OffsetY, [int]$ShadowAlpha) {
  $alpha = Get-DrawAlpha $ShadowAlpha
  $outline = New-PenFromHex $Palette.Outline 24 $alpha
  $outline.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $primaryBrush = New-Brush $Palette.Primary $alpha
  $secondaryBrush = New-Brush $Palette.Secondary $alpha
  $accentBrush = New-Brush $Palette.Accent $alpha
  $spotBrush = New-Brush $Palette.Spot ([Math]::Max(110, [int]($alpha * 0.72)))
  $darkBrush = New-Brush $Palette.Outline $alpha
  $pinPath = New-Object System.Drawing.Drawing2D.GraphicsPath
  $pinPath.AddEllipse(284 + $OffsetX, 172 + $OffsetY, 456, 456)
  $pinPath.AddPolygon(@(
    (New-Point (512 + $OffsetX) (860 + $OffsetY)),
    (New-Point (372 + $OffsetX) (530 + $OffsetY)),
    (New-Point (652 + $OffsetX) (530 + $OffsetY))
  ))
  $Graphics.FillPath($primaryBrush, $pinPath)
  $Graphics.DrawPath($outline, $pinPath)
  $Graphics.FillEllipse($accentBrush, 386 + $OffsetX, 274 + $OffsetY, 250, 250)
  $Graphics.DrawEllipse($outline, 386 + $OffsetX, 274 + $OffsetY, 250, 250)
  $Graphics.FillEllipse($secondaryBrush, 436 + $OffsetX, 326 + $OffsetY, 54, 54)
  $Graphics.FillEllipse($secondaryBrush, 534 + $OffsetX, 326 + $OffsetY, 54, 54)
  $Graphics.DrawArc($outline, 434 + $OffsetX, 360 + $OffsetY, 160, 110, 12, 156)
  $ticketPath = New-RoundedPath (180 + $OffsetX) (562 + $OffsetY) 264 138 24
  $Graphics.FillPath($spotBrush, $ticketPath)
  $Graphics.DrawPath($outline, $ticketPath)
  $Graphics.DrawLine($outline, 218 + $OffsetX, 616 + $OffsetY, 406 + $OffsetX, 616 + $OffsetY)
  $Graphics.DrawLine($outline, 226 + $OffsetX, 650 + $OffsetY, 360 + $OffsetX, 650 + $OffsetY)
  $Graphics.FillEllipse($darkBrush, 204 + $OffsetX, 604 + $OffsetY, 18, 18)
  $Graphics.FillEllipse($darkBrush, 390 + $OffsetX, 604 + $OffsetY, 18, 18)
  $routePen = New-PenFromHex $Palette.Secondary 18 $alpha
  $routePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $routePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $Graphics.DrawBezier($routePen, 566 + $OffsetX, 598 + $OffsetY, 664 + $OffsetX, 530 + $OffsetY, 742 + $OffsetX, 642 + $OffsetY, 822 + $OffsetX, 574 + $OffsetY)
  $Graphics.FillEllipse($accentBrush, 792 + $OffsetX, 548 + $OffsetY, 56, 56)
  $Graphics.DrawEllipse($outline, 792 + $OffsetX, 548 + $OffsetY, 56, 56)
  $pinPath.Dispose()
  $ticketPath.Dispose()
  $outline.Dispose()
  $primaryBrush.Dispose()
  $secondaryBrush.Dispose()
  $accentBrush.Dispose()
  $spotBrush.Dispose()
  $darkBrush.Dispose()
  $routePen.Dispose()
}

function Draw-MotifBitmap([string]$Motif, [hashtable]$Palette) {
  $bitmap = New-Object System.Drawing.Bitmap 1024, 1024
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $graphics.Clear([System.Drawing.Color]::Transparent)

  switch ($Motif) {
    "atlaspin" {
      Draw-AtlasPin $graphics $Palette 18 20 90
      Draw-AtlasPin $graphics $Palette 0 0 0
    }
    "crownbridge" {
      Draw-CrownBridge $graphics $Palette 18 20 90
      Draw-CrownBridge $graphics $Palette 0 0 0
    }
    "scrapring" {
      Draw-ScrapRing $graphics $Palette 18 20 90
      Draw-ScrapRing $graphics $Palette 0 0 0
    }
    "commandcamp" {
      Draw-CommandCamp $graphics $Palette 18 22 90
      Draw-CommandCamp $graphics $Palette 0 0 0
    }
    "mountainbunker" {
      Draw-MountainBunker $graphics $Palette 18 22 90
      Draw-MountainBunker $graphics $Palette 0 0 0
    }
    "fistsign" {
      Draw-FistSign $graphics $Palette 20 22 90
      Draw-FistSign $graphics $Palette 0 0 0
    }
    "warpennant" {
      Draw-WarPennant $graphics $Palette 18 20 90
      Draw-WarPennant $graphics $Palette 0 0 0
    }
    "snowman" {
      Draw-Snowman $graphics $Palette 20 28 90
      Draw-Snowman $graphics $Palette 0 0 0
    }
    "dragonflame" {
      Draw-DragonFlame $graphics $Palette 20 24 90
      Draw-DragonFlame $graphics $Palette 0 0 0
    }
    "castle" {
      Draw-Castle $graphics $Palette 22 30 90
      Draw-Castle $graphics $Palette 0 0 0
    }
    "bomb" {
      Draw-Bomb $graphics $Palette 22 30 90
      Draw-Bomb $graphics $Palette 0 0 0
    }
    "leaf" {
      Draw-Leaf $graphics $Palette 20 26 90
      Draw-Leaf $graphics $Palette 0 0 0
    }
    "flowerbasket" {
      Draw-FlowerBasket $graphics $Palette 20 24 90
      Draw-FlowerBasket $graphics $Palette 0 0 0
    }
    "zombie" {
      Draw-Zombie $graphics $Palette 20 26 90
      Draw-Zombie $graphics $Palette 0 0 0
    }
    "runner" {
      Draw-Runner $graphics $Palette 20 24 90
      Draw-Runner $graphics $Palette 0 0 0
    }
    "courier" {
      Draw-Courier $graphics $Palette 20 24 90
      Draw-Courier $graphics $Palette 0 0 0
    }
    "swordshield" {
      Draw-SwordShield $graphics $Palette 18 24 90
      Draw-SwordShield $graphics $Palette 0 0 0
    }
    "plane" {
      Draw-Plane $graphics $Palette 22 26 90
      Draw-Plane $graphics $Palette 0 0 0
    }
    "hook" {
      Draw-Hook $graphics $Palette 20 26 90
      Draw-Hook $graphics $Palette 0 0 0
    }
    default {
      Draw-ShieldStar $graphics $Palette 18 24 90
      Draw-ShieldStar $graphics $Palette 0 0 0
    }
  }

  $graphics.Dispose()
  return $bitmap
}

function New-MasterBitmap([System.Drawing.Bitmap]$ForegroundBitmap, [hashtable]$Palette) {
  $bitmap = New-Object System.Drawing.Bitmap 1024, 1024
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $graphics.Clear([System.Drawing.Color]::Transparent)

  $path = New-RoundedPath 88 88 848 848 220
  $gradient = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    [System.Drawing.Point]::new(88, 88),
    [System.Drawing.Point]::new(936, 936),
    (New-Color $Palette.BgStart),
    (New-Color $Palette.BgEnd)
  )
  $outline = New-PenFromHex $Palette.Outline 28
  $spotBrush = New-Brush $Palette.Spot 150
  $glossBrush = New-Brush "#FFFFFF" 72
  $graphics.FillPath($gradient, $path)
  $graphics.FillEllipse($spotBrush, 182, 150, 260, 200)
  $graphics.FillEllipse($spotBrush, 700, 192, 110, 110)
  $graphics.FillEllipse($glossBrush, 204, 146, 520, 270)
  $graphics.DrawPath($outline, $path)
  $graphics.DrawImage($ForegroundBitmap, 0, 0, 1024, 1024)

  $path.Dispose()
  $gradient.Dispose()
  $outline.Dispose()
  $spotBrush.Dispose()
  $glossBrush.Dispose()
  $graphics.Dispose()
  return $bitmap
}

function Save-Png([System.Drawing.Image]$Image, [string]$PathValue, [int]$Width, [int]$Height) {
  $dir = Split-Path -Parent $PathValue
  if ($dir) { Ensure-Directory $dir }
  $output = New-Object System.Drawing.Bitmap $Width, $Height
  $graphics = [System.Drawing.Graphics]::FromImage($output)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $graphics.Clear([System.Drawing.Color]::Transparent)
  $graphics.DrawImage($Image, 0, 0, $Width, $Height)
  $output.Save($PathValue, [System.Drawing.Imaging.ImageFormat]::Png)
  $graphics.Dispose()
  $output.Dispose()
}

function Normalize-Manifest([string]$ManifestPath) {
  if (-not (Test-Path -LiteralPath $ManifestPath)) { return }
  $text = [System.IO.File]::ReadAllText($ManifestPath)
  $text = [regex]::Replace($text, 'android:icon\s*=\s*"[^"]+"', 'android:icon="@mipmap/app_icon"')
  $text = [regex]::Replace($text, '\s+android:roundIcon\s*=\s*"[^"]+"', '')
  [System.IO.File]::WriteAllText($ManifestPath, $text, [System.Text.UTF8Encoding]::new($false))
}

function Has-ColorResource([string]$ValuesDir, [string]$ColorName) {
  if (-not (Test-Path -LiteralPath $ValuesDir)) { return $false }
  $pattern = '<color\s+name="' + [regex]::Escape($ColorName) + '"'
  foreach ($file in Get-ChildItem -LiteralPath $ValuesDir -Filter "*.xml" -File -ErrorAction SilentlyContinue) {
    $text = [System.IO.File]::ReadAllText($file.FullName)
    if ([regex]::IsMatch($text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
      return $true
    }
  }
  return $false
}

if (-not (Test-Path -LiteralPath $Project)) {
  throw "Project path not found: $Project"
}

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$repoRoot = Get-RepoRoot $projectPath
$gameIdValue = $GameId
if ([string]::IsNullOrWhiteSpace($gameIdValue)) {
  $gameIdValue = Split-Path -Leaf $projectPath
}
$seedValue = $Seed
if ([string]::IsNullOrWhiteSpace($seedValue)) {
  $seedValue = $gameIdValue
}
$explicitSubjectValue = $Subject
if ([string]::IsNullOrWhiteSpace($explicitSubjectValue)) {
  $explicitSubjectValue = $gameIdValue.Replace("_", " ").Replace("-", " ")
}
$iconDirection = Get-IconDirection $repoRoot $gameIdValue $explicitSubjectValue
$subjectValue = $iconDirection.Subject
if ([string]::IsNullOrWhiteSpace($subjectValue)) {
  $subjectValue = $explicitSubjectValue
}
$motif = Get-Motif $subjectValue $iconDirection.Forbidden
$duplicateReview = Get-IconDuplicateReview $repoRoot $gameIdValue $subjectValue $motif $iconDirection.Silhouette
if ($duplicateReview.Risk -ne "low") {
  $blockedGames = @($duplicateReview.Matches | Select-Object -ExpandProperty game_id -Unique)
  throw ("Icon duplicate risk is '" + $duplicateReview.Risk + "' for game '" + $gameIdValue + "' against: " + ($blockedGames -join ", ") + ". Refine the icon subject or visual identity before regenerating.")
}
$palette = Get-Palette $motif

$exportBase = $ExportRoot
if ([string]::IsNullOrWhiteSpace($exportBase)) {
  $exportBase = Join-Path $repoRoot "artifacts\icons"
}
$exportDir = Join-Path $exportBase $gameIdValue

$resRoot = Join-Path $projectPath "app\src\main\res"
$drawableDir = Join-Path $resRoot "drawable"
$valuesDir = Join-Path $resRoot "values"
$mipmapAny = Join-Path $resRoot "mipmap-anydpi-v26"

Ensure-Directory $drawableDir
Ensure-Directory $valuesDir
Ensure-Directory $mipmapAny
Ensure-Directory $exportDir

Remove-IfExists (Join-Path $drawableDir "app_icon_fg.xml")
Remove-IfExists (Join-Path $drawableDir "app_icon_fg.png")
Remove-IfExists (Join-Path $mipmapAny "app_icon.xml")
Remove-IfExists (Join-Path $mipmapAny "app_icon_round.xml")
Remove-IfExists (Join-Path $valuesDir "app_icon_colors.xml")
Remove-IfExists (Join-Path $valuesDir "icon_colors.xml")

$foreground = Draw-MotifBitmap $motif $palette
$master = New-MasterBitmap $foreground $palette

$legacySizes = @(
  @{ Folder = "mipmap-mdpi"; Size = 48 },
  @{ Folder = "mipmap-hdpi"; Size = 72 },
  @{ Folder = "mipmap-xhdpi"; Size = 96 },
  @{ Folder = "mipmap-xxhdpi"; Size = 144 },
  @{ Folder = "mipmap-xxxhdpi"; Size = 192 }
)

foreach ($entry in $legacySizes) {
  $folderPath = Join-Path $resRoot $entry.Folder
  Ensure-Directory $folderPath
  Save-Png $master (Join-Path $folderPath "app_icon.png") $entry.Size $entry.Size
}

Save-Png $foreground (Join-Path $drawableDir "app_icon_fg.png") 432 432
Save-Png $master (Join-Path $exportDir "$gameIdValue-upload-1024.png") 1024 1024
Save-Png $master (Join-Path $exportDir "$gameIdValue-upload-512.png") 512 512

$primaryExportPath = Join-Path $exportDir "$gameIdValue-upload-1024.png"
$foregroundPath = Join-Path $drawableDir "app_icon_fg.png"
$foregroundHash = Get-FileSha256 $foregroundPath
$primaryExportHash = Get-FileSha256 $primaryExportPath
$otherExportFiles = Get-ChildItem -Path (Join-Path $repoRoot "artifacts\icons") -Recurse -File -Filter "*-upload-1024.png" -ErrorAction SilentlyContinue |
  Where-Object { $_.FullName -ne $primaryExportPath }
$reusedExportGames = @()
foreach ($otherExport in $otherExportFiles) {
  if ((Get-FileSha256 $otherExport.FullName) -eq $primaryExportHash) {
    $reusedExportGames += (Split-Path (Split-Path $otherExport.FullName -Parent) -Leaf)
  }
}
if ($reusedExportGames.Count -gt 0) {
  throw ("Icon export hash matches existing game export for: " + (($reusedExportGames | Select-Object -Unique) -join ", ") + ". Regeneration produced a reused icon and is blocked.")
}

$colorsXml = @"
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <color name="cst_app_icon_bg">$($palette.BgStart)</color>
</resources>
"@
if (-not (Has-ColorResource $valuesDir "cst_app_icon_bg")) {
  Write-TextUtf8 (Join-Path $valuesDir "icon_colors.xml") $colorsXml
}

$adaptiveXml = @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
  <background android:drawable="@color/cst_app_icon_bg" />
  <foreground android:drawable="@drawable/app_icon_fg" />
</adaptive-icon>
"@
Write-TextUtf8 (Join-Path $mipmapAny "app_icon.xml") $adaptiveXml

$metadata = [ordered]@{
  game_id = $gameIdValue
  subject = $subjectValue
  icon_subject = $subjectValue
  icon_silhouette = $iconDirection.Silhouette
  motif = $motif
  style = "cartoon"
  project_path = $projectPath
  generated_at = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssK")
  primary_export = $primaryExportPath
  visual_identity_source = $iconDirection.VisualIdentitySource
  icon_duplicate_risk = $duplicateReview.Risk
  generation_mode = "fresh_render"
  reuse_policy = "no_reuse"
  foreground_sha256 = $foregroundHash
  primary_export_sha256 = $primaryExportHash
  duplicate_review = [ordered]@{
    risk = $duplicateReview.Risk
    compared_games = @($duplicateReview.Matches)
  }
  seed = $seedValue
}
$metadataJson = $metadata | ConvertTo-Json -Depth 4
Write-TextUtf8 (Join-Path $exportDir "metadata.json") $metadataJson

Normalize-Manifest (Join-Path $projectPath "app\src\main\AndroidManifest.xml")

$foreground.Dispose()
$master.Dispose()

Write-Host "ICON_GENERATION_OK=true"
Write-Host "ICON_STYLE=cartoon"
Write-Host "ICON_MOTIF=$motif"
Write-Host "ICON_EXPORT_DIR=$exportDir"
