# Visual Identity Workflow

Version: 1.0
Last updated: 2026-04-23

This document defines the UI and icon visual-identity workflow for Android Java mini-games in this repository.

## 1. Purpose

This workflow prevents new games from sharing the same UI frame, HUD structure, button set, palette, and launcher icon concept.

It exists to make every delivery-ready game define:

- a distinct UI layout archetype
- a distinct HUD composition
- a style signature beyond `ui_skin`
- a clear UI asset strategy
- a distinct icon subject and silhouette
- forbidden visual reuse from earlier games

`ui_skin` is only the token foundation. It is not enough to make a game visually distinct.

## 2. Authoritative Contract

When a selected concept has a target `game_id`, the visual identity contract should be stored at:

- `artifacts/requirements/<game_id>/visual_identity.json`

Required top-level fields:

- `version`
- `game_id`
- `status`
- `visual_differentiation_axes`
- `forbidden_visual_reuse`
- `ui_identity`
- `icon_identity`

Allowed status values:

- `draft`
- `passed`
- `needs_revision`

`passed` means the UI and icon direction are specific enough to block repeated output.

## 3. UI Identity

`ui_identity` should include:

- `layout_archetype`
- `hud_composition`
- `navigation_model`
- `palette_signature`
- `material_language`
- `typography_style`
- `primary_ui_pack`
- `secondary_ui_assets`
- `unique_screen_motifs`
- `forbidden_ui_elements`

Examples of layout archetypes:

- top ribbon plus radial command wheel
- side inventory rail plus floating objective card
- bottom equipment dock plus map-corner minimeter
- split tactical board with right-side upgrade drawer
- diegetic dashboard with physical gauges and levers
- card hand plus battlefield HUD

The UI workflow should not repeatedly use the same top HUD pills plus bottom button strip unless the requirements explicitly demand it and at least two other visual axes differ.

## 4. Icon Identity

`icon_identity` should include:

- `subject`
- `silhouette`
- `composition`
- `palette`
- `background`
- `game_specific_motif`
- `forbidden_icon_reuse`

The icon subject should represent the chosen game, not a generic tower, shield, sword, crate, or reused asset.

The icon workflow should avoid:

- reusing the same foreground object
- reusing the same background badge
- reusing the same color pairing
- reusing another game's export file or unmodified source asset
- using an icon that could describe multiple existing projects equally well

## 5. Requirements Integration

The full requirements document must include a section named `Visual Identity Contract`.

That section should summarize:

- UI layout archetype
- HUD structure
- screen motifs
- palette and material language
- typography direction
- UI pack strategy
- icon subject
- icon silhouette
- icon palette and background
- forbidden visual reuse

The machine-readable form should be stored in `visual_identity.json`.

## 6. UI Workflow Expectations

Before assigning UI resources, the UI workflow should read `visual_identity.json` when it exists.

For delivery-ready output, the UI workflow should:

- preserve the selected layout archetype
- avoid generic reuse of the same HUD and bottom-bar composition
- choose a style-matched UI pack or import a new license-clear pack when the shared library lacks variety
- record the chosen pack in `ui_pack_assignment.json`
- implement screen-specific motifs instead of only changing colors

## 7. Icon Workflow Expectations

Before generating or updating icon assets, the icon workflow should read `visual_identity.json` when it exists.

For delivery-ready output, the icon workflow should:

- follow the icon subject and silhouette
- use a game-specific motif
- avoid copying another game's source icon or exported upload icon
- export upload-ready files under `artifacts/icons/<game_id>/`
- keep icon metadata clear enough for future duplicate review

## 8. Inspect Integration

The inspect workflow reports:

- `VISUAL_IDENTITY_STATUS=passed`
- `VISUAL_IDENTITY_STATUS=draft`
- `VISUAL_IDENTITY_STATUS=needs_revision`
- `VISUAL_IDENTITY_STATUS=missing`
- `VISUAL_IDENTITY_STATUS=invalid`

Delivery-ready output requires:

- requirements status is `confirmed`
- gameplay diversity status is `passed`
- visual identity status is `passed`
- icon and UI tracks are complete

