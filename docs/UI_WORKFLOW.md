# UI Workflow

Version: 1.1
Last updated: 2026-04-22

This document defines the repository UI workflow for Android Java mini-games.

## 1. Purpose

The UI workflow exists to:

- define screen structure before implementation
- keep exactly one `ui_skin`
- support practical, higher-fidelity game UI instead of token-only placeholder UI
- allow licensed binary UI assets, fonts, and open-source UI resource packs
- keep reusable external UI resources in a shared library first when possible

## 2. Repository UI Model

General Android app UI may use Jetpack Compose, but this repository is Java-only.

The repository standard is therefore:

- gameplay rendering in `GameView` or `SurfaceView`
- HUD, menu, help, pause, reward, and result screens in Android View or XML layers
- token-driven styling plus licensed binary UI assets when needed

## 3. Scope

The UI workflow may cover:

- menu
- gameplay HUD
- help or tutorial
- pause
- result or game over
- reward or upgrade choice
- level select
- shop or loadout

Every UI workflow run should define:

- screen list
- HUD metrics
- state map
- chosen `ui_skin`
- style tags
- asset strategy

The UI brief is mandatory before pack selection or implementation.

## 4. Allowed UI Assets

The UI workflow may use:

- XML drawables
- PNG
- WEBP
- 9-patch
- vector drawables
- TTF or OTF fonts
- licensed open-source UI packs
- user-provided UI assets

The UI workflow must not use closed or unknown-license assets unless the user explicitly provides or authorizes them.

## 5. Shared UI Library

Reusable external UI resources should be stored under:

- `shared_assets/ui/`

Recommended pack layout:

- `shared_assets/ui/index.json`
- `shared_assets/ui/packs/<pack_id>/manifest.json`
- `shared_assets/ui/packs/<pack_id>/assignments/<preset_id>.json`
- `shared_assets/ui/packs/<pack_id>/LICENSE`
- `shared_assets/ui/packs/<pack_id>/NOTICE`
- `shared_assets/ui/packs/<pack_id>/drawable/`
- `shared_assets/ui/packs/<pack_id>/font/`
- `shared_assets/ui/packs/<pack_id>/preview/`

Recommended manifest fields:

- `pack_id`
- `title`
- `source_url`
- `license`
- `license_url`
- `recommended_ui_skin`
- `default_assignment_preset`
- `assignment_presets`
- `asset_types`
- `style_tags`
- `notes`

Recommended shared library index fields:

- `version`
- `packs`
- `pack_id`
- `title`
- `license`
- `source_url`
- `recommended_ui_skin`
- `default_assignment_preset`
- `asset_types`
- `style_tags`
- `quality_tags`
- `used_by`

Recommended assignment preset fields:

- `preset_id`
- `title`
- `ui_skin`
- `assignments`
- `notes`

## 6. Project Assignment

Project-local UI resources may be assigned into:

- `app/src/main/res/layout/`
- `app/src/main/res/drawable*/`
- `app/src/main/res/font/`
- `app/src/main/assets/ui/`

UI resource naming should stay explicit and reusable:

- `ui_*` for generic panels, buttons, overlays, and backgrounds
- `hud_*` for HUD-specific assets
- `menu_*` for menu assets
- `fx_ui_*` for UI-only effect assets

When a shared UI pack is applied, the project should keep a record at:

- `app/src/main/assets/ui/ui_pack_assignment.json`

Current assignment tool entry point:

- `tools/assets/assign_ui.ps1`
- `tools/assets/ensure_ui_pack.ps1`
- `tools/assets/import_ui_pack.ps1`

Discovery examples:

- `tools/assets/assign_ui.ps1 -ListPacks`
- `tools/assets/assign_ui.ps1 -ListPresets -PackId <pack_id>`

## 7. Workflow Steps

1. Identify the target game and the requested UI scope.
2. Define the UI structure and state map.
3. Choose one `ui_skin`.
4. Decide whether token-only resources are enough or whether binary assets are required.
5. Resolve assets in this order:
   - style-matched shared UI pack
   - imported licensed open-source UI pack
   - project-local custom refinement
6. Do not silently fall back to generic shape-only placeholder UI for production-grade or delivery-ready requests.
7. Reuse or import shared UI resources under `shared_assets/ui/` when possible.
8. Assign project-local resources and implement layouts or overlays.
9. Validate required UI foundation files and resource naming.

## 8. Validation Goals

The final UI result should satisfy all of the following:

- one `ui_skin`
- complete values foundation
- required logical UI resources present
- gameplay rendering kept separate from non-real-time UI where practical
- provenance recorded for imported open-source assets
