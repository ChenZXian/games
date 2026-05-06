# Gameplay Diversity Workflow

Version: 1.0
Last updated: 2026-04-23

This document defines the gameplay diversity and content-scale workflow for Android Java mini-games in this repository.

## 1. Purpose

This workflow prevents new games from becoming thin reskins of earlier projects.

It exists to make every new game specify:

- a clear genre family and concrete sub-archetype
- a distinct control model and core loop signature
- a map or playfield content budget
- an entity and mechanic content budget
- an asset variety budget that matches the gameplay
- template-reuse rules that block repeated layouts, rosters, and loops

The tower-defense case is only one example. The same rules apply to runners, puzzle games, action games, farming games, shooters, survival games, and other mini-game types.

## 2. Authoritative Contract

When a selected concept has a target `game_id`, the gameplay diversity contract should be stored at:

- `artifacts/requirements/<game_id>/gameplay_diversity.json`

The file is part of the requirements trace and should be created with the full requirements draft.

Required top-level fields:

- `version`
- `game_id`
- `status`
- `genre_family`
- `genre_archetype`
- `camera_perspective`
- `control_model`
- `core_loop_signature`
- `differentiation_axes`
- `forbidden_template_reuse`
- `map_content_budget`
- `entity_content_budget`
- `mechanic_content_budget`
- `asset_variety_budget`

Allowed status values:

- `draft`
- `passed`
- `needs_revision`

Meaning:

- `draft`: the contract exists but is not ready to gate delivery
- `passed`: the contract is specific enough to guide initialization and inspection
- `needs_revision`: the contract exists but is known to be too generic, too small, or too close to an existing game

`passed` means the contract is complete enough for downstream workflows. The contract becomes authoritative only when the requirements trace itself is confirmed.

## 3. Candidate Diversity Rules

The 10-candidate stage must not produce ten variations of the same game.

The candidate list should vary at least these dimensions:

- genre family
- game objective
- control model
- camera or playfield perspective
- content scale
- primary player decision
- failure pressure
- asset family

If the user asks for one broad genre such as tower defense, the 10 candidates should still vary by sub-archetype, for example:

- lane defense
- maze-building defense
- route switching defense
- hero defense
- trap grid defense
- convoy escort defense
- multi-entry siege defense
- deck-driven defense
- resource logistics defense
- rotating-base defense

## 4. Full Requirements Diversity Rules

The full requirements document must include a section named `Gameplay Diversity And Content Budget`.

That section should summarize the machine-readable contract in human-readable form:

- genre family and sub-archetype
- why it is not a reskin of an existing game
- map or playfield model
- minimum map regions, lanes, rooms, routes, zones, or screens
- minimum terrain or map element variety
- minimum player action variety
- minimum enemy, obstacle, unit, item, or prop variety
- progression and upgrade variety
- animation and feedback expectations for primary actors
- asset-pack variety plan
- explicitly forbidden reuse from previous generated games

## 5. Map Content Budget

`map_content_budget` should be specific to the game type.

Recommended fields:

- `play_area_model`
- `route_or_region_count`
- `interactive_regions`
- `terrain_types`
- `decorative_prop_count`
- `functional_map_elements`
- `landmarks`
- `camera_or_scroll_model`

Examples:

- A tower-defense game should not default to a tiny three-lane map unless that exact layout is required and differentiated.
- A runner should specify segment types, obstacle families, elevation or lane rules, and scroll rhythm.
- A farming or management game should specify zones, production buildings, resource nodes, and upgrade locations.
- A puzzle game should specify board size, piece families, special tiles, blockers, and level variation.
- An action game should specify arenas, cover, hazards, enemy spawn regions, and movement constraints.

## 6. Entity Content Budget

`entity_content_budget` should specify the minimum gameplay roster.

Recommended fields:

- `player_unit_types`
- `enemy_types`
- `neutral_or_resource_entities`
- `boss_or_elite_types`
- `projectile_or_effect_types`
- `item_or_powerup_types`

The budget should be implementable in a Java Android mini-game, but it must be large enough to avoid one-unit prototypes.

## 7. Mechanic Content Budget

`mechanic_content_budget` should specify the minimum interaction variety.

Recommended fields:

- `primary_player_actions`
- `secondary_player_actions`
- `upgrade_paths`
- `wave_or_level_variants`
- `special_rules`
- `risk_reward_systems`
- `failure_pressure_variants`

The first playable must preserve the confirmed mechanics instead of simplifying the game into a generic movement or tap loop.

## 8. Asset Variety Budget

`asset_variety_budget` should define how gameplay assets support the design.

Recommended fields:

- `primary_game_art_packs`
- `secondary_game_art_packs`
- `ui_pack`
- `animation_tier`
- `required_animation_states`
- `minimum_distinct_sprite_families`
- `asset_reuse_note`

The art workflow should select assets by role and mechanic, not copy the same pack into every similar game.

## 9. Tower Defense Example

For tower-defense games, the requirements must choose a sub-archetype before initialization.

The following repeated template is not acceptable for delivery-ready output unless the requirements explicitly justify it and change at least two major axes:

- three horizontal lanes
- left-side base
- bottom-only tower bar
- one small map
- same tower asset family
- same enemy march behavior
- same next-wave loop

Possible differentiation axes:

- multi-entry routes instead of fixed lanes
- player-built maze routes
- moving convoy objective instead of static base
- hero-controlled anchor unit
- route switching between waves
- trap-combo tiles
- vertical or isometric map layout
- resource logistics between defense nodes
- weather or terrain modifiers
- deck or draft based tower availability

## 10. Initialization Expectations

The initialization workflow should read the confirmed requirements trace and the gameplay diversity contract before writing a new project.

If the contract is missing, still draft, or too generic, initialization should stop and route back to requirements planning.

If implementation cannot meet the contract within the repository constraints, it should stop and report the missing content rather than producing a simpler unrelated game.

## 11. Inspect Integration

The inspect workflow reports:

- `GAMEPLAY_DIVERSITY_STATUS=passed`
- `GAMEPLAY_DIVERSITY_STATUS=draft`
- `GAMEPLAY_DIVERSITY_STATUS=needs_revision`
- `GAMEPLAY_DIVERSITY_STATUS=missing`
- `GAMEPLAY_DIVERSITY_STATUS=invalid`

Delivery-ready output requires:

- requirements status is `confirmed`
- gameplay diversity status is `passed`
- implementation fidelity status is `passed`
- icon, UI, gameplay art, runtime art map, and audio are complete

