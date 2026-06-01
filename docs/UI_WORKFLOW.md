# UI Workflow

Version: 1.7
Last updated: 2026-06-01

This document defines the repository UI workflow for Android Java mini-games.

## 1. Purpose

The UI workflow exists to:

- define screen structure before implementation
- reserve a protected gameplay safe area before decorative framing
- require adaptive layout behavior across common phone and tablet aspect ratios
- keep exactly one `ui_skin`
- support practical, higher-fidelity game UI instead of token-only placeholder UI
- allow licensed binary UI assets, fonts, and open-source UI resource packs
- keep reusable external UI resources in a shared library first when possible
- prevent repeated UI layouts, HUD composition, and pack/preset reuse from becoming the default look
- make adaptive gameplay-state UI measurable before inspection or packaging

## 2. Repository UI Model

General Android app UI may use Jetpack Compose, but this repository is Java-only.

The repository standard is therefore:

- gameplay rendering in `GameView` or `SurfaceView`
- HUD, menu, help, pause, reward, and result screens in Android View or XML layers
- token-driven styling plus licensed binary UI assets when needed

UI Kit is the mandatory foundation, not the final visual ceiling.

- every project must still keep the UI Kit token contract, logical `ui_*` resources, and exactly one `ui_skin`
- UI Kit-only XML drawables may be used for first prototypes and structural scaffolding
- `project_local_xml_ui` means placeholder-only for production-grade, delivery-ready, or menu item `10` output
- delivery-ready UI must apply a style-matched shared UI pack, an imported license-clear UI pack, or an equivalent custom asset layer with provenance
- inspection should not mark UI as complete when the only evidence is local XML/token scaffolding
- delivery-ready UI should follow the visual identity contract in `artifacts/requirements/<game_id>/visual_identity.json` when it exists

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
- playfield safe area
- aspect-ratio adaptation plan
- frame and border policy
- chosen `ui_skin`
- style tags
- asset strategy
- visual identity contract alignment

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
- `shared_assets/ui/source_catalog.json`

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
2. Read the visual identity contract when it exists.
3. Define the UI structure, state map, visual layout archetype, and protected gameplay safe area.
4. Define the adaptation plan for narrow phones, tall phones, and tablet-like aspect ratios.
5. Choose one `ui_skin`.
6. Decide whether token-only resources are enough for the current stage or whether binary assets are required.
7. Resolve assets in this order:
   - style-matched shared UI pack
   - global source-catalog search for official or license-clear UI sources
   - imported licensed open-source UI pack
   - project-local custom refinement
8. Reserve active gameplay space before styling:
   - themed borders, bezels, or rails should stay outside the active playfield when possible
   - if a frame touches the gameplay plane, it may only consume dead space that is explicitly reserved in the UI brief
   - HUD overlays must not cover active board cells, lanes, routes, spawn points, tower pads, combat lanes, units, drag paths, or touch-critical action zones
   - gameplay-state command rows should avoid packing more than six direct buttons into one horizontal row
   - menu, help, pause, and result panels should avoid fixed widths that can exceed small phone landscape widths
   - the UI brief must be translated into actual `activity_main.xml` bounds, margins, padding, or equivalent viewport constraints before polish is considered complete
   - do not ship the anti-pattern `full-screen GameView + edge-anchored HUD stack` unless the gameplay renderer itself reserves the same dead space and inspection can verify that reserve
9. Verify the adaptive behavior:
   - narrow-phone layouts should not collapse or overlap
   - tall-phone layouts should not leave battle controls detached from their reserved zones
   - tablet-like layouts should not leave the gameplay area undersized or squeezed by fixed side rails
   - runtime UI checks should cover small-phone portrait, small-phone landscape, narrow landscape, phone landscape, tablet landscape, and tablet-square viewports
   - direct battle-control touch targets should stay at or above the repository minimum touch size
   - button text should fit within the estimated target width after resolving `@string` values
10. Do not reuse the same HUD composition, top metric bar, bottom command strip, or pack preset across projects unless the visual identity contract allows it.
11. Do not silently fall back to generic shape-only placeholder UI for production-grade, menu item `10`, or delivery-ready requests.
12. Reuse or import shared UI resources under `shared_assets/ui/` when possible.
13. Assign project-local resources and implement layouts or overlays.
14. Validate required UI foundation files and resource naming.

## 8. Visual Variety Rules

For production-grade or delivery-ready UI, vary at least these axes against recent projects:

- HUD geometry
- command location
- button shape language
- panel material
- meter style
- palette contrast
- typography direction
- screen motifs

The following repeated template should not be reused by default:

- top row of rounded metric pills
- full-width center objective bar
- bottom row of identical rectangular command buttons
- same blue-gray button pack
- same pause button treatment

Pack selection should apply a reuse penalty so one matching pack does not become the default answer for every similar project.

## 9. Validation Goals

The final UI result should satisfy all of the following:

- one `ui_skin`
- complete values foundation
- required logical UI resources present
- gameplay rendering kept separate from non-real-time UI where practical
- decorative borders, HUD chrome, and thematic frames do not occlude active gameplay space
- any framed presentation preserves a documented safe area for movement, combat, interaction, and touch-critical regions
- battle-state controls, HUD bars, and tactical panels stay outside the active gameplay viewport instead of floating over it
- battle-state layouts remain readable and non-overlapping on narrow phones, tall phones, and tablet-style aspect ratios
- battle-state controls preserve minimum touch targets and do not rely on cramped one-row command bars
- visible button text fits its estimated adaptive width after string resolution
- delivery-ready UI should fail validation when a full-span gameplay view is combined with anchored overlays and no reserved safe area is detectable
- `styles.xml` should define resolvable base styles for `Widget`, `Widget.Game`, and `Widget.Game.Button` when nested widget styles need them
- style parents should use known Material Components button or platform progress styles instead of unsupported parents such as `Widget.MaterialComponents.ImageButton`
- SurfaceView canvas rendering should protect `lockCanvas` and `unlockCanvasAndPost` with null guards, `catch`, and `finally` so backgrounding, rotation, and surface destruction do not crash the app
- provenance recorded for imported open-source assets
- visual identity contract preserved when one exists
