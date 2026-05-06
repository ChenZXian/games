# Minecart Bastion

Game ID: minecart_bastion
Direction: tower defense
Selected Concept: Minecart Bastion - mine rail defense where weapon carts patrol tracks and the player switches junctions to cover monster waves.
Recommended UI Skin: skin_post_apocalypse

## Game Positioning

Minecart Bastion is a compact Android Java tower-defense mini-game about defending a deep mine reactor from cave monsters. Instead of placing static towers beside a road, the player buys armed minecarts, assigns them to rail loops, and switches track junctions so moving firepower reaches the right threat lane at the right time.

## Target Feel And Player Fantasy

The game should feel tactical, mechanical, and readable. The player fantasy is being a mine operations commander who turns a broken rail network into a moving fortress, using timing, route control, and upgrades to make a small number of carts cover a large underground battlefield.

## Core Gameplay Loop

Each stage starts with a visible rail map, several monster tunnels, and one reactor base. The player spends ore to deploy weapon carts onto rail loops, starts the wave, watches enemies advance along cave paths, switches junctions to redirect carts, triggers limited support skills, collects ore from defeated enemies, and upgrades carts between waves. A stage is cleared after all scripted waves are defeated while the reactor still has durability.

## Controls And Input Model

Primary input is tap-based and mobile-friendly. Tap an empty depot to buy a cart, tap a cart to open upgrade and route priority actions, tap a junction lever to switch the active rail branch, and tap support skill buttons on the HUD to deploy short effects such as a rockfall stun or emergency repair. Dragging is optional only for panning the map if a stage is larger than the viewport.

## Failure And Win Conditions

The player loses when monster leaks reduce reactor durability to zero. The player wins a stage by surviving all waves. Star rating is based on remaining reactor durability, number of leaks, and clear speed. Endless mode can reuse the same rules with escalating waves and a score target instead of a fixed final wave.

## Progression Structure

Progression is stage-based across mine depths. Early stages teach one loop and one junction, mid stages add multiple entrances and armored enemies, and late stages add split loops, boss miners, and pressure events. Persistent unlocks can include new cart classes, higher upgrade caps, and passive reactor perks, but the first implementation should keep upgrades simple and local to a run.

## Level Or Run Structure

A standard level has 6 to 10 waves. Between waves, the player has a short planning window to build, upgrade, sell, and inspect enemy previews. During waves, building is still allowed but more expensive or limited to depot slots. Boss waves appear every third or final level and require route timing rather than raw damage alone.

## Economy Rewards And Upgrades

Ore is the main currency. Ore comes from wave start bonuses, enemy defeats, perfect wave bonuses, and optional ore crates that appear near track branches. Cart upgrades should use clear tiers: damage, fire rate, range arc, and special effect strength. Suggested cart classes are Gatling Cart for steady damage, Frost Cart for slow fields, Tesla Cart for chain damage, and Drill Bomber Cart for burst attacks with cooldown. Rewards after a stage include stars, best clear time, and unlocked depth nodes.

## Screen Map

Menu: title, Start, Continue, Stage Select, Help, Sound toggle.
Gameplay HUD: reactor durability, ore, wave number, next wave button, pause button, support skill buttons, selected cart panel.
Pause: Resume, Restart, Stage Select, Sound toggle.
Game Over: result title, waves survived, leaks, stars if applicable, Restart, Back to Menu.
Extra Screens: Stage Select with mine-depth nodes, Help screen explaining carts, junctions, ore, and leaks.

## UI Direction

Use exactly one UI skin: skin_post_apocalypse. The layout tone should be dusty metal, amber hazard lighting, worn panels, and high-contrast readable labels. HUD priority is reactor durability first, then ore, wave state, and selected cart actions. Key overlays are a bottom selected-cart panel, a compact next-wave preview card, and a centered result panel. UI assets should start with the repository UI Kit XML drawable contract; external UI assets are not required for the first draft unless the later UI workflow imports a license-clear pack into shared_assets/ui/.

## Gameplay Art Direction

Required gameplay art roles are rail tiles, depots, junction levers, reactor base, cave entrances, four cart classes, standard monster, fast monster, armored monster, boss monster, projectiles, hit sparks, ore pickups, rockfall effect, and cave background. Camera perspective should be top-down or slight 2.5D board view with rails as thick readable paths. Preferred source tier is repository-generated vector/XML or Canvas-drawn art first; if production-grade art is requested later, use the gameplay art workflow to import or generate license-clear assets under shared_assets/game_art/. Readability constraints: carts must be visually distinct by silhouette and color accent, enemies must not blend into rails, and junction state must be obvious at a glance. Fallback rule: if no suitable pack exists, keep art stylized and schematic rather than generic circles or rectangles.

## Icon Direction

The icon subject should be an armed minecart on a glowing rail in front of a cave reactor. The silhouette should be a chunky cart angled three-quarter view with a mounted cannon and bright ore sparks. Color direction should use warm amber, rust metal, dark cave brown, and a small cyan or green reactor glow. Tone should be cartoon-styled but rugged, matching the mine-defense theme clearly.

## Audio Direction

Menu BGM should be low, echoing mine ambience with slow mechanical percussion. Gameplay BGM should be rhythmic, tense, and loopable with clanking rails and muted drums. Boss or climax BGM should add heavier impacts and faster metal percussion without becoming noisy. SFX roles include cart deploy, junction switch, cart shot, frost hit, tesla chain, drill burst, enemy hit, enemy leak, ore pickup, wave start, reactor damage, win, lose, button tap, and pause.

## Technical Implementation Notes

Rendering should use a custom GameView or SurfaceView for the rail map, carts, enemies, projectiles, and effects, while HUD and menus use Android View or XML overlays. The state model must include MENU, PLAYING, PAUSED, GAME_OVER, and STAGE_CLEAR. Important entities are StageConfig, RailNode, RailEdge, Junction, Depot, Cart, Enemy, Projectile, WaveSpawner, Reactor, OrePickup, SupportSkill, and Effect. Systems to plan early are path traversal on rail loops, cart routing at junctions, enemy pathing to reactor, wave scripting, collision or range checks, deterministic update timing, and save data for unlocked stages.

## Differentiation Note

The registry already includes several defense-adjacent games: Castle Keep Defender uses central drag-aim shooting, Royal Line Defense uses static tower classes and a hero, Garden Siege uses plant-like grid placement, Bowling Barrage uses lane launches, and Neon Barricade Survival uses action barricades. Minecart Bastion is differentiated by moving tower carts, player-controlled rail switches, patrol coverage, and route timing as the main decision layer.

## Confirmation

Status: Draft
Initialization Gate: Blocked until explicit user confirmation.
Reviewer Action: Confirm this requirements draft or request specific revisions.