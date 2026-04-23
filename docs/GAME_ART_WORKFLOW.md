# Game Art Workflow

Version: 1.0
Last updated: 2026-04-23

This document defines the repository workflow for reusable gameplay art assets.

## 1. Purpose

The game art workflow exists to:

- avoid bare circle or rectangle placeholder gameplay visuals in delivery-ready games
- keep character, map, prop, item, background, projectile, and effect assets separate from UI assets
- make licensed free and open-source asset provenance inspectable
- allow future games to reuse compatible visual packs by theme and game type

## 2. Scope

This workflow covers gameplay-facing art resources such as:

- player characters
- enemies
- NPCs
- animals
- tilesets
- terrain
- buildings
- props
- items
- projectiles
- pickups
- environmental effects
- gameplay backgrounds

This workflow does not replace:

- launcher icon workflow
- UI workflow for menus, buttons, panels, HUD frames, or fonts
- audio workflow

## 3. License Policy

Only use assets when the license is clear and compatible with repository use.

Default allowed source tier:

- CC0 or public-domain equivalent
- free for commercial use with no attribution requirement
- official source URL available

Conditional source tier:

- permissive licenses with attribution requirements, only when attribution and redistribution obligations are explicitly tracked

Default blocked source tier:

- unclear license
- personal-use-only license
- no redistribution permission
- AI-generated or third-party pack with unknown training or ownership status
- mixed-license collection without per-file provenance

## 4. Shared Library Location

Reusable game art assets should enter the shared library first:

- `shared_assets/game_art/index.json`
- `shared_assets/game_art/packs/<pack_id>/manifest.json`
- `shared_assets/game_art/packs/<pack_id>/LICENSE`
- `shared_assets/game_art/packs/<pack_id>/NOTICE`
- `shared_assets/game_art/packs/<pack_id>/assets/`
- `shared_assets/game_art/packs/<pack_id>/preview/`

Project-local copies should be derived from this shared library when possible.

Recommended project path:

- `app/src/main/assets/game_art/<pack_id>/`
- `app/src/main/assets/game_art/game_art_assignment.json`

## 5. Required Brief

Before selecting or assigning gameplay art, define:

- game theme
- camera perspective
- game type
- required art roles
- style tags
- target visual tone
- license tier
- whether placeholder art is acceptable for the current stage

Common art roles:

- `character`
- `enemy`
- `npc`
- `animal`
- `tileset`
- `terrain`
- `building`
- `prop`
- `item`
- `projectile`
- `pickup`
- `effect`
- `background`

## 6. Resolution Order

Default resolution order:

1. style-matched shared game art pack
2. official free and license-clear source import into `shared_assets/game_art/`
3. project-local custom drawing only when the project is still prototype-grade

Delivery-ready projects should not silently fall back to shape-only gameplay placeholders.

## 7. Tool Entry Points

Current entry points:

- `tools/assets/import_game_art_pack.ps1`
- `tools/assets/ensure_game_art_pack.ps1`
- `tools/assets/assign_game_art.ps1`

Recommended usage:

- list shared packs with `tools/assets/assign_game_art.ps1 -ListPacks`
- resolve a matching pack with `tools/assets/ensure_game_art_pack.ps1`
- assign a selected pack to a project with `tools/assets/assign_game_art.ps1`
- import a license-clear pack with `tools/assets/import_game_art_pack.ps1`

## 8. Requirements Integration

Full requirements should include a gameplay art direction section covering:

- required gameplay art roles
- suggested shared pack or source family
- visual readability constraints
- camera and scale assumptions
- fallback rule if no suitable pack exists

## 9. Inspection Integration

Inspection should report `GAME_ART_STATUS`.

Allowed statuses:

- `complete`
- `placeholder_only`
- `deferred`

Meaning:

- `complete`: project has a tracked assignment and the shared library records the pack usage
- `placeholder_only`: project has local gameplay visuals but no tracked shared asset assignment
- `deferred`: no gameplay art workflow evidence exists

## 10. Current Preferred Source

The preferred first-party source for the initial shared library is Kenney because its official asset pages clearly publish CC0 licensing for many 2D game asset packs.

Other sources may be added later only after license review.
