# Crownroad Bastion

Game ID: crownroad_bastion
Direction: complex kingdom tower defense with forked road maps
Selected Concept: Crownroad Bastion - a classic kingdom road-defense game inspired by multi-route tower defense structure, with multiple entrances, merge-split paths, fixed build pads, barracks interception, hero movement, and area skills. The map must not be a simple horizontal three-lane structure.
Recommended UI Skin: skin_cartoon_light

## Game Positioning

Crownroad Bastion is a compact Android Java tower-defense mini-game about defending a royal crossroads fortress. It should reference the structure of classic premium tower defense games at the system level: winding roads, fixed build pads, tower specialization, barracks soldiers, hero repositioning, enemy wave previews, and tactical spell timing. It must not copy original maps, characters, names, icons, or proprietary assets from any commercial game.

## Target Feel And Player Fantasy

The player should feel like a royal battlefield commander reading a curved battle map rather than managing straight lanes. The fantasy is placing towers around a living crossroads, moving a hero to block weak points, using barracks to hold enemies at bends, and timing spells when enemies merge into danger zones.

## Core Gameplay Loop

Each stage starts on a non-linear road map with at least two enemy entrances, at least one merge node, at least one split node, and one fortress exit. The player spends gold on fixed build pads, chooses tower types, upgrades towers, moves a hero, deploys temporary reinforcements, and casts area skills. Enemies follow node-graph paths instead of lane rows. The player wins by clearing all waves before too many enemies reach the bastion gate.

## Controls And Input Model

Primary input is tap-based. Tap an empty build pad to open tower choices. Tap a built tower to upgrade or sell. Tap the hero button then tap a reachable road node to move the hero. Tap spell icons then tap a map area. Tap wave preview to start early. The player should be able to pan slightly or view the whole stage in landscape without hiding the route network.

## Failure And Win Conditions

The player has a life counter for enemies that escape through the bastion gate. Different enemy classes cost different lives when they escape. The stage is cleared when all scripted waves are defeated. Star rating depends on remaining lives, early-wave bonuses, and whether the hero survives boss waves.

## Progression Structure

The first playable version should include a campaign map stub with 5 locked stage nodes, with Stage 1 playable and later nodes visually present. Stage progression unlocks tower upgrade caps and enemy types. Persistent upgrades are light: tower path unlocks, hero skill levels, and encyclopedia entries.

## Level Or Run Structure

Stage 1 must not be a simple three-lane horizontal map. It should use a royal crossroads layout: north forest entrance and east road entrance merge at a market square, then split around a hill before rejoining near the bastion gate. Build pads sit near bends, bridge chokepoints, and the final approach. A future Stage 2 can add a south ford shortcut and a flying enemy route.

## Economy Rewards And Upgrades

Gold comes from enemy defeats, wave completion, early wave starts, and bonus crates. Tower classes are Archer Tower, Militia Barracks, Cannon Tower, Mage Tower, and Banner Shrine. Each tower has three tiers in the first implementation. Barracks spawn soldiers that physically block enemies on the road. Banner Shrine buffs nearby towers and soldiers instead of attacking directly.

## Enemies And Units

Enemy classes include Foot Raider, Fast Runner, Shield Bearer, Wolf Rider, Shaman Healer, Armored Ogre, Bat Swarm, Battering Ram, and Warlord Captain. Player-side units include Hero Knight, Militia Soldier, Reinforcement Spearman, and temporary Royal Guard from a skill. Moving and attacking units must have facing behavior, idle or walk animation, attack timing, hit feedback, and death or retreat feedback.

## Screen Map

Menu: title, Start, Stage Select, Encyclopedia, Help, Sound toggle.
Gameplay HUD: lives, gold, wave number, next wave preview, hero portrait, spell buttons, pause.
Pause: Resume, Restart, Stage Select, Sound toggle.
Game Over: result title, escaped enemies, remaining lives, stars, Restart, Back to Map.
Extra Screens: Stage Select map, Tower Encyclopedia, Enemy Encyclopedia, Help overlay.

## UI Direction

Use exactly one UI skin: skin_cartoon_light. The UI should feel like a royal campaign board with parchment panels, readable tower cards, bright fantasy buttons, and clear wave preview badges. HUD priority is lives first, then gold, wave count, hero status, and spells. Key overlays are build radial, tower upgrade card, wave preview panel, hero command marker, and result panel.

## UI Asset Strategy

Use repository UI Kit foundation plus a tracked shared UI pack when available. If the current shared UI library is insufficient, import a license-clear free UI pack through the UI workflow into shared_assets/ui/ with manifest, license, source URL, and project assignment record. Do not use untracked downloaded UI art directly in the project.

## Gameplay Art Direction

Required roles are curved road tiles, grass terrain, bridge, hill, forest edge, build pad, bastion gate, archer tower, barracks, cannon, mage tower, shrine, hero, soldiers, reinforcements, enemies, projectiles, spell effects, wave flags, and supply crates. Camera perspective should be top-down or slight angled board view. Preferred assets should come from shared_assets/game_art/ first, especially tower-defense, fantasy, RPG, or medieval packs.

If assets are missing, the implementation may search the web only for official free and license-clear assets, preferably CC0 or public-domain equivalent, and must import them through shared_assets/game_art/ with provenance before project use. It must not use unclear, personal-use-only, ripped, fan-extracted, or commercial game assets.

## Animation And Facing Requirements

Enemies follow road tangent direction and rotate or flip so their face/front points along movement. Barracks soldiers face their blocked target. Hero movement must show walking or stepped motion rather than smooth sliding. Towers should animate firing with recoil, muzzle flash, projectile spawn, or spell glow. Projectiles rotate toward their travel vector. Primary unit states must include idle, move, attack, hit, and death or escape.

## Icon Direction

The icon should show a royal crossroads fortress gate with a crown banner, a curved road arrow, and a small tower silhouette. Palette should use bright parchment gold, royal blue, grass green, and warm stone. Tone should be cartoon fantasy, readable at small size, and clearly about kingdom road defense.

## Audio Direction

Menu BGM should be light royal campaign music with strings, flute, and soft drums. Gameplay BGM should be upbeat tactical fantasy with marching percussion. Boss or climax BGM should add heavier drums and brass hits. SFX roles include button tap, build tower, upgrade, sell, archer shot, cannon shot, magic bolt, soldier block, hero move, hero attack, reinforcement summon, enemy hit, enemy death, gold pickup, wave start, warning horn, spell cast, victory, and defeat.

## Technical Implementation Notes

Rendering should use a custom GameView or SurfaceView for the map, paths, units, towers, projectiles, effects, and hero. HUD and non-real-time screens should use Android View or XML overlays. Core systems are NodeGraphPath, PathSegment, BuildPad, Tower, BarracksSquad, Hero, Enemy, WaveDefinition, Projectile, Spell, Effect, StageConfig, and SaveState. The path system must use node graph polylines with merge and split nodes, not a laneY array or simple three-row movement.

## Differentiation Note

The registry already contains Royal Line Defense, Minecart Bastion, Empire Wall Guard, Castle Keep Defender, and other defense-adjacent games. Crownroad Bastion is differentiated by its explicit curved road node graph, merge-split map layout, fixed build pads around bends, hero repositioning, barracks interception, spell timing, and campaign-map presentation. The design must avoid the previous simple horizontal three-lane structure.

## Confirmation

Status: Draft
Initialization Gate: Blocked until explicit user confirmation.
Reviewer Action: Confirm this requirements draft or request specific revisions.
