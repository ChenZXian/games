# Shared Game Art Library

This directory stores reusable gameplay art resources for Android Java mini-games.

Use this library for:

- characters
- enemies
- animals
- tilesets
- maps
- terrain
- props
- items
- projectiles
- pickups
- effects
- gameplay backgrounds

Do not use this library for:

- launcher icons
- menu panels
- HUD frames
- buttons
- fonts
- BGM or SFX

Recommended structure:

- `index.json`
- `packs/<pack_id>/manifest.json`
- `packs/<pack_id>/LICENSE`
- `packs/<pack_id>/NOTICE`
- `packs/<pack_id>/assets/`
- `packs/<pack_id>/preview/`

License rule:

- prefer CC0 or public-domain equivalent sources
- keep source URL and license URL in each manifest
- do not import unclear, personal-use-only, or mixed-license packs by default

Resolution order:

1. style-matched shared game art pack
2. imported official free and license-clear game art pack
3. project-local prototype art only when placeholders are explicitly acceptable

Current curated source family:

- Kenney official CC0 assets

Current curated packs:

- `kenney_platformer_characters` - cartoon platformer characters and zombies
- `kenney_platformer_art_deluxe` - cartoon platformer tiles, enemies, items, and backgrounds
- `kenney_roguelike_rpg_pack` - pixel RPG tilesheet and roguelike map elements
- `kenney_tiny_town` - pixel town, overworld, building, and map tiles
- `kenney_top_down_shooter` - top-down shooter characters, zombies, props, weapons, and tiles
- `kenney_tower_defense_top_down` - tower defense terrain, towers, enemies, and projectiles
- `kenney_animal_pack` - cartoon animals for farm, ranch, collection, and casual games
- `kenney_isometric_miniature_farm` - isometric farm terrain, buildings, props, and items
- `kenney_space_shooter_extension` - ships, projectiles, pickups, effects, and space shooter elements
- `kenney_simple_space` - cartoon space ships, planets, props, and backgrounds

Tooling:

- list packs with `tools/assets/assign_game_art.ps1 -ListPacks`
- resolve packs with `tools/assets/ensure_game_art_pack.ps1`
- assign project assets with `tools/assets/assign_game_art.ps1`
- import packs with `tools/assets/import_game_art_pack.ps1`
