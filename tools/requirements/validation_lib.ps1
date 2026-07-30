function Test-RequirementNonEmpty($value) {
  if ($null -eq $value) { return $false }
  if ($value -is [string]) { return -not [string]::IsNullOrWhiteSpace($value) }
  if (($value -is [System.Array]) -or ($value -is [System.Collections.IEnumerable] -and -not ($value -is [string]) -and -not ($value.PSObject.Properties.Count -gt 0))) {
    foreach ($item in $value) {
      if (Test-RequirementNonEmpty $item) { return $true }
    }
    return $false
  }
  if ($value.PSObject -and $value.PSObject.Properties.Count -gt 0) {
    foreach ($prop in $value.PSObject.Properties) {
      if (Test-RequirementNonEmpty $prop.Value) { return $true }
    }
    return $false
  }
  return $true
}

function Get-RequirementNonEmptyFieldCount($data, $keys) {
  $count = 0
  foreach ($key in $keys) {
    $prop = $data.PSObject.Properties[$key]
    $value = if ($prop) { $prop.Value } else { $null }
    if (Test-RequirementNonEmpty $value) { $count++ }
  }
  return $count
}

function Get-GameplayDiversityValidationErrors($data) {
  $errors = New-Object System.Collections.Generic.List[string]
  $requiredTopLevel = @(
    "version",
    "game_id",
    "status",
    "genre_family",
    "genre_archetype",
    "camera_perspective",
    "control_model",
    "core_loop_signature",
    "differentiation_axes",
    "forbidden_template_reuse",
    "map_content_budget",
    "entity_content_budget",
    "mechanic_content_budget",
    "asset_variety_budget"
  )

  foreach ($key in $requiredTopLevel) {
    if (-not ($data.PSObject.Properties.Name -contains $key)) {
      $errors.Add("missing top-level field: $key") | Out-Null
    }
  }

  foreach ($key in @("game_id", "status", "genre_family", "genre_archetype", "camera_perspective", "control_model", "core_loop_signature")) {
    if (-not (Test-RequirementNonEmpty $data.$key)) {
      $errors.Add("empty required field: $key") | Out-Null
    }
  }

  if ($data.status -notin @("draft", "passed", "needs_revision")) {
    $errors.Add("status must be draft, passed, or needs_revision") | Out-Null
  }

  foreach ($key in @("differentiation_axes", "forbidden_template_reuse")) {
    if (-not ($data.$key -is [System.Array]) -or -not (Test-RequirementNonEmpty $data.$key)) {
      $errors.Add("$key must be a non-empty list") | Out-Null
    }
  }

  $mapBudget = $data.map_content_budget
  if ($null -eq $mapBudget -or -not ($mapBudget.PSObject.Properties.Count -gt 0)) {
    $errors.Add("map_content_budget must be an object") | Out-Null
  } else {
    $requiredMapKeys = @("play_area_model", "route_or_region_count", "interactive_regions", "terrain_types", "functional_map_elements")
    if ((Get-RequirementNonEmptyFieldCount $mapBudget $requiredMapKeys) -lt 4) {
      $errors.Add("map_content_budget is too generic") | Out-Null
    }
  }

  $entityBudget = $data.entity_content_budget
  if ($null -eq $entityBudget -or -not ($entityBudget.PSObject.Properties.Count -gt 0)) {
    $errors.Add("entity_content_budget must be an object") | Out-Null
  } else {
    if ((Get-RequirementNonEmptyFieldCount $entityBudget $entityBudget.PSObject.Properties.Name) -lt 3) {
      $errors.Add("entity_content_budget needs at least three non-empty role fields") | Out-Null
    }
  }

  $mechanicBudget = $data.mechanic_content_budget
  if ($null -eq $mechanicBudget -or -not ($mechanicBudget.PSObject.Properties.Count -gt 0)) {
    $errors.Add("mechanic_content_budget must be an object") | Out-Null
  } else {
    if ((Get-RequirementNonEmptyFieldCount $mechanicBudget $mechanicBudget.PSObject.Properties.Name) -lt 4) {
      $errors.Add("mechanic_content_budget needs at least four non-empty fields") | Out-Null
    }
  }

  $assetBudget = $data.asset_variety_budget
  if ($null -eq $assetBudget -or -not ($assetBudget.PSObject.Properties.Count -gt 0)) {
    $errors.Add("asset_variety_budget must be an object") | Out-Null
  } else {
    $requiredAssetKeys = @("primary_game_art_packs", "animation_tier", "required_animation_states", "minimum_distinct_sprite_families", "asset_reuse_note")
    if ((Get-RequirementNonEmptyFieldCount $assetBudget $requiredAssetKeys) -lt 4) {
      $errors.Add("asset_variety_budget is too generic") | Out-Null
    }
  }

  return $errors
}

function Get-VisualIdentityValidationErrors($data) {
  $errors = New-Object System.Collections.Generic.List[string]
  $requiredTopLevel = @(
    "version",
    "game_id",
    "status",
    "visual_differentiation_axes",
    "forbidden_visual_reuse",
    "ui_identity",
    "icon_identity"
  )
  foreach ($key in $requiredTopLevel) {
    if (-not ($data.PSObject.Properties.Name -contains $key)) {
      $errors.Add("missing top-level field: $key") | Out-Null
    }
  }

  foreach ($key in @("game_id", "status")) {
    if (-not (Test-RequirementNonEmpty $data.$key)) {
      $errors.Add("empty required field: $key") | Out-Null
    }
  }

  if ($data.status -notin @("draft", "passed", "needs_revision")) {
    $errors.Add("status must be draft, passed, or needs_revision") | Out-Null
  }

  foreach ($key in @("visual_differentiation_axes", "forbidden_visual_reuse")) {
    if (-not ($data.$key -is [System.Array]) -or -not (Test-RequirementNonEmpty $data.$key)) {
      $errors.Add("$key must be a non-empty list") | Out-Null
    }
  }

  $uiIdentity = $data.ui_identity
  if ($null -eq $uiIdentity -or -not ($uiIdentity.PSObject.Properties.Count -gt 0)) {
    $errors.Add("ui_identity must be an object") | Out-Null
  } else {
    foreach ($key in @("layout_archetype", "hud_composition", "navigation_model", "palette_signature", "material_language", "typography_style", "primary_ui_pack", "playfield_safety", "unique_screen_motifs", "forbidden_ui_elements")) {
      if (-not ($uiIdentity.PSObject.Properties.Name -contains $key)) {
        $errors.Add("ui_identity missing field: $key") | Out-Null
      } elseif (-not (Test-RequirementNonEmpty $uiIdentity.$key)) {
        $errors.Add("ui_identity empty field: $key") | Out-Null
      }
    }
    if (-not ($uiIdentity.PSObject.Properties.Name -contains "secondary_ui_assets")) {
      $errors.Add("ui_identity missing field: secondary_ui_assets") | Out-Null
    } elseif (-not ($uiIdentity.secondary_ui_assets -is [System.Array])) {
      $errors.Add("ui_identity.secondary_ui_assets must be a list") | Out-Null
    }

    $playfieldSafety = $uiIdentity.playfield_safety
    if ($null -eq $playfieldSafety -or -not ($playfieldSafety.PSObject.Properties.Count -gt 0)) {
      $errors.Add("ui_identity.playfield_safety must be an object") | Out-Null
    } else {
      foreach ($key in @("reserved_edges", "hud_anchor_zones", "protected_gameplay_zones", "frame_overlay_policy")) {
        if (-not ($playfieldSafety.PSObject.Properties.Name -contains $key)) {
          $errors.Add("ui_identity.playfield_safety missing field: $key") | Out-Null
        } elseif (-not (Test-RequirementNonEmpty $playfieldSafety.$key)) {
          $errors.Add("ui_identity.playfield_safety empty field: $key") | Out-Null
        }
      }
    }
  }

  $iconIdentity = $data.icon_identity
  if ($null -eq $iconIdentity -or -not ($iconIdentity.PSObject.Properties.Count -gt 0)) {
    $errors.Add("icon_identity must be an object") | Out-Null
  } else {
    foreach ($key in @("subject", "silhouette", "composition", "palette", "background", "game_specific_motif", "forbidden_icon_reuse")) {
      if (-not ($iconIdentity.PSObject.Properties.Name -contains $key)) {
        $errors.Add("icon_identity missing field: $key") | Out-Null
      } elseif (-not (Test-RequirementNonEmpty $iconIdentity.$key)) {
        $errors.Add("icon_identity empty field: $key") | Out-Null
      }
    }
  }

  return $errors
}
