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
  if (($value -match 'fist|knuckle|glove') -and ($value -match 'sign|street|district|warrant|route|block')) { return "fistsign" }
  if ($value -match 'snow|ice|frost') { return "snowman" }
  if ((-not $banCastle) -and $value -match 'castle|royal|keep|king') { return "castle" }
  if ($value -match 'bomb|bomber|blast') { return "bomb" }
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
    "fistsign" {
      return @{
        BgStart = "#1B2433"; BgEnd = "#C67B32"; Primary = "#D0B79C"; Secondary = "#E7E0D6"; Accent = "#ED9E3D"; Outline = "#6B5846"; Spot = "#FFD6B4"
      }
    }
    "snowman" {
      return @{
        BgStart = "#72D7FF"; BgEnd = "#2F8BFF"; Primary = "#FFFFFF"; Secondary = "#FF8B38"; Accent = "#1D4ED8"; Outline = "#15315C"; Spot = "#FFF1B8"
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

function Draw-MotifBitmap([string]$Motif, [hashtable]$Palette) {
  $bitmap = New-Object System.Drawing.Bitmap 1024, 1024
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $graphics.Clear([System.Drawing.Color]::Transparent)

  switch ($Motif) {
    "fistsign" {
      Draw-FistSign $graphics $Palette 20 22 90
      Draw-FistSign $graphics $Palette 0 0 0
    }
    "snowman" {
      Draw-Snowman $graphics $Palette 20 28 90
      Draw-Snowman $graphics $Palette 0 0 0
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
  primary_export = (Join-Path $exportDir "$gameIdValue-upload-1024.png")
  visual_identity_source = $iconDirection.VisualIdentitySource
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
