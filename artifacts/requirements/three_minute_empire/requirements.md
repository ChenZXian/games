# Three Minute Empire Requirements Draft

## Game Positioning

Three Minute Empire is a short-session mobile strategy game inspired by lightweight WeChat mini game pacing. Each match lasts exactly three minutes. The player captures neutral resource points, turns them from gray to the player color, upgrades buildings, dispatches troop streams, and tries to destroy the enemy main base before the timer ends.

The game must feel fast, readable, and tactically satisfying. The player should understand the board within seconds, but repeated play should reward better route timing, resource-point prioritization, and final assault planning.

## Target Feel And Player Fantasy

The player is a small-screen battlefield commander making quick strategic decisions under pressure. The fantasy is not a slow grand strategy empire. It is a compact command duel where every captured resource point visibly changes the map and increases momentum.

## Core Gameplay Loop

The match starts with the player base on the left, the enemy base on the right, and multiple neutral resource points between them. Neutral resource points are gray. When the player captures one, its visual owner changes immediately to the player color. When the enemy captures one, it changes to the enemy color. Contested points pulse between the current owner color and a warning tint until capture resolves.

The player taps friendly buildings or resource points, drags a route to a target point, and releases to send a troop stream. Resource points generate supply while owned. Supply is spent on upgrading controlled points, upgrading the main base, and launching tactical surge commands. Capturing more points increases income but stretches defense. The player wins by destroying the enemy main base or holding a higher score when the three-minute timer expires.

## Controls And Input Model

- Tap a controlled node to select it.
- Drag from a controlled node to another connected node to dispatch troops.
- Tap a controlled resource point to open a compact upgrade radial.
- Tap the surge button to send a temporary reinforced stream from all selected front-line nodes.
- Tap pause to pause the match.

All controls must be touch-first and work on small phones without requiring precise multi-touch gestures.

## Failure Conditions And Win Conditions

Win conditions:

- Destroy the enemy main base before the timer ends.
- If time expires, win by having the higher conquest score.

Failure conditions:

- The enemy destroys the player main base.
- If time expires, lose by having a lower conquest score.

Conquest score is based on owned resource points, enemy damage, upgrade level, and remaining base health.

## Progression Structure

The game uses a stage ladder. Early stages introduce one resource point type and simple links. Later stages add branching routes, high-value mines, repair outposts, and enemy surge patterns. Between stages, the player unlocks persistent commander perks that slightly alter starting supply, dispatch speed, point defense, or surge cooldown.

## Level Or Run Structure

Each stage is a single three-minute match. The map fits on one screen and uses 8 to 12 capturable nodes:

- 1 player base
- 1 enemy base
- 6 to 10 resource or tactical points

Stages should vary by route graph shape rather than reusing one symmetric lane layout. Examples include triangle center race, two-front fork, ring road, bridge choke, and resource-rich risky flank.

## Economy, Rewards, Drops, And Upgrade Model

Owned resource points generate supply every few seconds. Supply can be spent on:

- Resource point upgrade: increases income and local defense.
- Barracks upgrade: increases troop dispatch size.
- Command surge: temporary mass dispatch and speed boost.
- Repair pulse: restores a small amount of base or point durability.

Stage rewards include commander experience, upgrade currency, and map medals. Rewards must be deterministic enough for a lightweight strategy game and not require ads, network services, or backend features.

## Gameplay Diversity And Content Budget

The genre family is real-time strategy. The concrete sub-archetype is a three-minute node-capture resource war with timed score pressure. It is not a reskin of the existing territory, RTS, or lane-push projects because it centers on short fixed-duration matches, visible point ownership color conversion, resource-point income, building upgrades, and a one-screen route graph rather than large conquest maps, worker economies, or lane bases.

Map model:

- One-screen node graph map.
- 8 to 12 nodes per stage.
- 2 to 4 route branches.
- At least 5 route graph archetypes across the first stage set.
- Resource point ownership must be visually explicit: gray for neutral, player color for owned by the player, enemy color for owned by the enemy, and contested pulse for active capture.

Entity roster:

- Player troop stream
- Enemy troop stream
- Reinforced surge troop stream
- Main base
- Resource point
- Mine point
- Repair outpost
- Watch post
- Command beacon

Mechanic targets:

- Dispatch troop streams between linked nodes.
- Capture neutral and enemy points through troop pressure.
- Upgrade controlled points.
- Trigger timed surge.
- Defend against enemy counter-surge.
- Win by base destruction or timer score.

Forbidden template reuse:

- Do not use three horizontal lanes.
- Do not use a bottom-only unit spawn strip.
- Do not use worker-gather base building.
- Do not use prefecture map conquest.
- Do not use tower placement along a path.

## Visual Identity Contract

UI layout archetype: one-screen command board with a left-side vertical command rail and compact top timer ribbon. The playfield is a route graph map with colored ownership nodes. The HUD must not sit on top of active nodes or route lines.

HUD composition:

- Top center timer and score ribbon.
- Left command rail with pause, surge, and repair buttons.
- Bottom-left selected-node stat plate only when a node is selected.
- No bottom full-width command strip.

Navigation model:

- Menu screen -> stage select -> gameplay -> result screen.
- Pause overlay as a compact side drawer.

Playfield safe area:

- Top timer band reserved.
- Left command rail reserved.
- Main graph nodes must be placed in the remaining board area.
- Decorative frame overlays must not cover graph nodes, routes, or drag paths.

Palette signature:

- Base skin: skin_military_tech.
- Neutral resource points: gray.
- Player ownership: bright teal-green.
- Enemy ownership: warm red-orange.
- Contested state: pulsing amber edge with owner-fill interpolation.

Material language:

- Tactical glass panels, thin map lines, small command icons, crisp node rings.

Typography:

- Compact military display style using the repository text appearance styles.

UI pack strategy:

- Start from the repository UI Kit.
- Prefer a style-matched tactical shared UI pack if available.
- If no suitable pack exists, use XML drawables and token tuning without placeholder-only bare shapes for delivery-ready output.

Icon identity:

- Subject: a small tactical map with a gray point flipping into a teal captured point under a command flag.
- Silhouette: circular map node cluster with one raised flag.
- Composition: large central captured node, two smaller gray nodes, diagonal route line.
- Palette: dark military green background, teal captured point, gray neutral point, amber command highlight.
- Forbidden icon reuse: no generic shield, castle, sword, tower, or soldier-only icon.

## Screen Map

Menu:

- Game title
- Start button
- Stage select button
- Settings button

Stage select:

- Stage ladder
- Medal status
- Commander perk preview

Gameplay HUD:

- Three-minute timer
- Player score and enemy score
- Supply count
- Selected node stats
- Pause, surge, and repair controls

Pause:

- Resume
- Restart
- Exit to menu

Game over:

- Win or lose result
- Conquest score
- Owned point count
- Enemy base damage
- Upgrade rewards
- Restart and next stage buttons

## UI Direction

Recommended ui_skin: skin_military_tech.

The interface should feel like a compact command console, not a fantasy war panel. The most important readability rule is node ownership color. Neutral resource points must be gray, player-owned points must be the player color, and enemy-owned points must be the enemy color. This color change must be instant and obvious when capture completes.

HUD priorities:

- Timer
- Supply
- Ownership and capture state
- Selected node upgrade affordance
- Surge cooldown

Frame and border policy:

- Decorative borders are allowed only outside the reserved playfield.
- Route lines and node hit areas must remain uncovered.
- Drag paths must remain clear from the selected node to its neighbors.

## Gameplay Art Direction

Camera perspective: top-down tactical board.

Required art roles:

- Player base
- Enemy base
- Neutral resource point
- Player-owned resource point
- Enemy-owned resource point
- Contested capture state
- Troop stream particles
- Mine point
- Repair outpost
- Watch post
- Command beacon
- Surge effect
- Capture burst

Default facing:

- Troop stream markers face or stretch along the active route direction.
- Bases and nodes are top-down and do not need directional facing.

Animation expectations:

- Troop streams animate along graph route lines.
- Resource points animate ownership transition with fill color and ring pulse.
- Neutral-to-player capture must visibly change from gray to the player color.
- Enemy capture must visibly change to the enemy color.
- Contested points use pulsing edge animation.
- Surge streams use brighter trails and faster movement.

Animation quality tier:

- Medium. Primary feedback is route movement, ownership color transition, capture burst, and surge trails.

Anchor and hitbox assumptions:

- Nodes use center anchors.
- Hitboxes should be larger than the visible node circles for finger comfort.
- Route lines render below nodes and troop streams.
- HUD controls render outside route graph safe area.

Fallback rule:

- If no suitable gameplay art pack exists, use custom XML/vector/canvas tactical map art with clear ownership colors. Do not use generic unstyled circles for delivery-ready output.

## Icon Direction

The icon should be cartoon-tactical rather than realistic military. It should show a three-minute countdown core shaped like a compact bomb clock, with one gray half flipping into a teal player-controlled half and a small amber spark at the conversion edge. The subject must communicate short-session pressure, capture, and ownership conversion without using a generic castle, shield, soldier, flag, or map badge.

## BGM Direction

Menu music:

- Calm tactical planning loop with light electronic percussion.

Gameplay loop:

- Fast but clean command rhythm, short three-minute pressure, no heavy orchestration.

Climax cue:

- Subtle intensity layer during the final 30 seconds.

## SFX Direction

Required SFX roles:

- Node select
- Dispatch
- Capture neutral point
- Capture enemy point
- Lose point
- Upgrade
- Surge ready
- Surge activate
- Timer final warning
- Win
- Lose

Capture SFX should distinguish neutral capture from enemy-point takeover.

## Technical Implementation Notes

Rendering style:

- Android Java custom GameView for graph, nodes, route lines, troop streams, and capture animation.
- Android View or XML overlays for menu, HUD, pause, and result screens.

State model:

- MENU
- STAGE_SELECT
- PLAYING
- PAUSED
- GAME_OVER

Important entities:

- Node
- Route
- TroopStream
- MatchTimer
- SupplyEconomy
- UpgradeModel
- EnemyCommander
- CaptureController
- ScoreModel

Systems to plan early:

- Node ownership state with NEUTRAL, PLAYER, ENEMY, CONTESTED_PLAYER, CONTESTED_ENEMY.
- Ownership color resolver.
- Touch-safe node hit testing.
- Drag route validation.
- Fixed three-minute timer.
- AI dispatch and upgrade behavior.

## Differentiation Note Against Current Registry

Three Minute Empire differs from territory_swarm and ring_prefecture_clash by using a fixed three-minute match, small one-screen tactical maps, building upgrades, direct resource-point income, and explicit gray-to-owner color conversion. It differs from bronze_camp_command because it has no worker economy, fog scouting, or base-building placement. It differs from lane push games because it uses graph capture, not continuous lanes or unit spawning from a bottom command strip.

## Confirmation Gate

This requirements trace is draft only. Project initialization, optimization, icon work, UI work, gameplay art work, audio work, inspection, and APK export must begin only after explicit user confirmation.
