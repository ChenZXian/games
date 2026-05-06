# Game Art Workflow

Version: 1.4
Last updated: 2026-04-23

This document defines the repository workflow for reusable gameplay art assets.

## 1. Purpose

The game art workflow exists to:

- avoid bare circle or rectangle placeholder gameplay visuals in delivery-ready games
- keep character, map, prop, item, background, projectile, and effect assets separate from UI assets
- make licensed free and open-source asset provenance inspectable
- allow future games to reuse compatible visual packs by theme and game type
- keep different generated games from collapsing into the same asset layout, same map scale, and same roster

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
- `shared_assets/game_art/animation_catalog.json`
- `shared_assets/game_art/source_catalog.json`
- `shared_assets/game_art/packs/<pack_id>/manifest.json`
- `shared_assets/game_art/packs/<pack_id>/LICENSE`
- `shared_assets/game_art/packs/<pack_id>/NOTICE`
- `shared_assets/game_art/packs/<pack_id>/assets/`
- `shared_assets/game_art/packs/<pack_id>/preview/`

Animation-capable packs should be recorded in `animation_catalog.json` with animation tier, supported states, camera perspective, default facing, and runtime integration notes.

Project-local copies should be derived from this shared library when possible.

Recommended project path:

- `app/src/main/assets/game_art/<pack_id>/`
- `app/src/main/assets/game_art/game_art_assignment.json`
- `app/src/main/assets/game_art/runtime_art_map.json`

## 5. Required Brief

Before selecting or assigning gameplay art, define:

- game theme
- camera perspective
- game type
- gameplay diversity contract or equivalent content budget
- required art roles
- style tags
- target visual tone
- license tier
- whether placeholder art is acceptable for the current stage
- runtime orientation and animation needs

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
2. global source-catalog search across official free and license-clear packs
3. official free and license-clear source import into `shared_assets/game_art/`
4. project-local custom drawing only when the project is still prototype-grade

Delivery-ready projects should not silently fall back to shape-only gameplay placeholders.

If the best local pack is weak, overused, or style-mismatched for the requested roles, production-grade flows must escalate to source-catalog or online import instead of forcing a poor local match.

Selection should apply a reuse penalty when the same pack has already been assigned to multiple projects, so similar prompts do not keep collapsing to the same small subset of packs.

Low-intelligence, auto, or speed-priority runs may reduce the number of evaluated candidates, but they must not skip:

- reuse-penalty evaluation
- style-match scoring
- source-catalog escalation when local packs are weak
- license checks for imported packs

An assignment record is not enough by itself.

- `project_local_canvas_art` means prototype-only unless the user explicitly accepts placeholder quality
- delivery-ready gameplay art must come from a tracked shared pack or an imported license-clear pack
- copied gameplay art must be consumed by the running game, not only stored under project assets
- if the runtime still draws only circles, rectangles, or text labels for primary gameplay objects, inspection should report placeholder quality
- if an entity can move, attack, turn, take damage, die, or idle, the runtime should map that entity to explicit visual states instead of drawing one static bitmap
- if an asset faces a fixed direction, the runtime must rotate, flip, or choose a direction-specific frame so the entity's face, weapon, or front side matches its movement or attack direction
- smooth position interpolation alone is not enough for production-grade gameplay art

## 7. Runtime Art Map

Delivery-ready gameplay art should keep a runtime mapping file at:

- `app/src/main/assets/game_art/runtime_art_map.json`

This file records how assigned assets are used by gameplay code.

Recommended top-level fields:

- `version`
- `game_id`
- `status`
- `camera_perspective`
- `default_coordinate_space`
- `entities`

Allowed `status` values:

- `draft`: generated by asset assignment and not yet integrated
- `integrated`: runtime code uses the mapped assets, states, facing, anchors, and hitboxes
- `needs_revision`: implementation exists but has known visual mismatch

Each entity entry should include:

- `entity_key`
- `role`
- `asset_keys`
- `default_facing`
- `facing_rule`
- `anchor`
- `hitbox`
- `render_size`
- `z_order`
- `states`
- `animation_rule`
- `movement_rule`

Minimum delivery expectations:

- moving entities define `idle` and `move` or an equivalent state
- attacking entities define `attack` or `fire`
- damageable entities define `hit` or `damage_feedback`
- removed or defeated entities define `destroy`, `death`, or an equivalent effect
- entities with directional meaning define `default_facing` and `facing_rule`
- top-down entities should align facing with velocity, target direction, or attack vector
- side-view entities should flip on horizontal direction changes when source art has one facing
- projectiles and weapons should rotate or select frames toward their travel or firing vector
- anchors and hitboxes should match gameplay collision and visual contact points

## 8. Animation Quality Tiers

Gameplay art should classify primary entity animation with one of these tiers:

- `static`: one image or one pose, acceptable for terrain, props, buildings, simple pickups, or prototype-only entities
- `directional_static`: one image with runtime rotation or flipping, acceptable for ships, turrets, projectiles, carts, and simple machines
- `pose_set`: separate idle, move, attack, hit, and death or equivalent poses
- `frame_animation`: multi-frame sequences for locomotion or actions
- `advanced_sprite_animation`: multi-frame movement plus action timing, facing, hit feedback, and removal effects
- `generated_strip`: generated or edited full animation strips normalized from an approved seed frame

Delivery-ready humanoid, animal, zombie, soldier, or creature entities should target `advanced_sprite_animation` whenever they are primary gameplay objects.

Minimum animation behavior for primary characters:

- walking or running must alternate leg or body poses instead of sliding a single bitmap
- attack must include at least windup and action poses, and should include recovery when frames exist
- ranged attacks should align the weapon or hand toward the target or projectile vector
- melee attacks should move the arm, weapon, body, or hit shape through a visible contact phase
- hit feedback should change pose, tint, flash, squash, recoil, or frame for a short duration
- death or destroy should use a pose, fade, burst, particles, or removal effect instead of instant disappearance
- animation frame rate should be tied to action timing or distance traveled, not only wall-clock time

When an imported pack lacks enough frames for a primary entity:

1. prefer a different license-clear animated pack
2. if no suitable pack exists, generate a whole-strip animation from an approved seed frame through the sprite pipeline
3. normalize frames to one size, one scale, and one anchor
4. inspect the preview before marking runtime art as integrated

The sprite pipeline should generate or normalize whole strips in one pass. Do not generate unrelated individual frames that drift in silhouette, palette, or proportions.

## 9. Tool Entry Points

Current entry points:

- `tools/assets/import_game_art_pack.ps1`
- `tools/assets/ensure_game_art_pack.ps1`
- `tools/assets/assign_game_art.ps1`

Recommended usage:

- list shared packs with `tools/assets/assign_game_art.ps1 -ListPacks`
- resolve a matching pack with `tools/assets/ensure_game_art_pack.ps1`
- inspect source-catalog candidates through `GAME_ART_SOURCE_CANDIDATES` output when local packs are weak or overused
- assign a selected pack to a project with `tools/assets/assign_game_art.ps1`
- import a license-clear pack with `tools/assets/import_game_art_pack.ps1`

`assign_game_art` should create `runtime_art_map.json` as `draft`.
Implementation or optimization must update it to `integrated` only after runtime code applies the mapping.

## 10. Requirements Integration

Full requirements should include a gameplay art direction section covering:

- required gameplay art roles
- suggested shared pack or source family
- visual readability constraints
- camera and scale assumptions
- facing and animation expectations for each moving or attacking entity type
- minimum runtime state list such as idle, move, attack, hit, death, spawn, or collect
- animation quality tier for primary entities
- fallback rule if no suitable pack exists
- asset-variety expectations from the gameplay diversity contract

## 11. Inspection Integration

Inspection should report `GAME_ART_STATUS`.

Allowed statuses:

- `complete`
- `placeholder_only`
- `deferred`

Meaning:

- `complete`: project has a tracked assignment, the shared library records the pack usage, runtime code uses assigned gameplay assets for primary objects, and `runtime_art_map.json` is `integrated`
- `placeholder_only`: project has local gameplay visuals but no tracked shared asset assignment, or the project stores a pack but still renders primary objects as shape-only placeholders
- `deferred`: no gameplay art workflow evidence exists

Inspection should also report `GAME_ART_RUNTIME_STATUS`.

Allowed runtime statuses:

- `integrated`
- `draft`
- `needs_revision`
- `missing`
- `invalid`

## 12. Current Preferred Source

The preferred first-party source for the initial shared library is Kenney because its official asset pages clearly publish CC0 licensing for many 2D game asset packs.

Other sources may be added later only after license review.
