# Scrap Ring Outpost

Game ID: scrap_ring_outpost
Direction: Zombie base defense
Selected Concept: Scrap Ring Outpost
Recommended UI Skin: skin_post_apocalypse
Requirements Status: Draft

## Positioning

Scrap Ring Outpost is an offline Android Java zombie defense mini-game built around a circular survivor base instead of fixed lanes. The player manages four defensive sectors around a scrap-built central hub, spending short daytime prep windows to place barricades, towers, traps, and utility modules before a night assault tests the weak side of the ring. The experience should feel tense, readable, and replayable within mobile-length sessions.

## Target Feel And Player Fantasy

The player should feel like a desperate but capable outpost commander holding the last livable yard in a dead industrial zone. The fantasy is not only killing zombies, but reading pressure across the entire perimeter, reinforcing the most exposed sector, and turning scavenged junk into a working defense web before the next breach wave lands.

## Core Gameplay Loop

1. Start the day at a central command yard with four connected ring sectors around it.
2. Collect scrap, fuel, and parts from completed waves, salvage drops, and sector harvest rigs.
3. Spend the prep phase placing or upgrading barricades, gun nests, arc lamps, spike traps, and repair stations on specific sector slots.
4. Assign one active sector focus, which changes where rapid-reload support, spotlight power, and manual skill bonuses are strongest.
5. Survive the night wave as zombies attack one or multiple sectors with distinct enemy mixes.
6. Rotate focus, trigger emergency skills, repair critical structures, and stabilize any breach before zombies reach the command yard.
7. Earn rating stars, unlock new module options, and push into higher threat nights with stronger but more complex defense patterns.

## Controls And Input Model

The game uses direct tap controls designed for landscape mobile play. The player taps a sector to inspect it, taps empty defense slots to build, taps built modules to upgrade or sell, and taps skill buttons for emergency actions. A radial or compact contextual command cluster appears only for the selected defense slot or structure. Swipe or tap the outer ring sectors to rotate focus quickly. No twin-stick movement is required.

## Failure Conditions And Win Conditions

The player wins a stage by surviving the required number of nights or by completing a scenario objective such as holding until evac, charging a broadcast beacon, or repairing an armored train. The player loses if zombies fully breach the inner command yard, if all four sectors collapse simultaneously for more than a short grace timer, or if a scenario-critical structure is destroyed.

## Progression Structure

The first release should ship with a three-zone progression arc. Zone 1 introduces single-sector pressure and basic barricade play. Zone 2 introduces split-attacks, utility power routing, and armored infected. Zone 3 introduces elite siege enemies, hazard weather, and multi-night endurance maps. Meta progression unlocks new module blueprints, sector perks, and passive command traits, but each run still requires tactical rebuilding inside the mission.

## Level Structure Or Run Structure

Each mission lasts 5 to 8 minutes and is divided into alternating prep and assault phases. Shorter maps use three nights with increasing attack pressure. Larger maps use four or five nights and may add special event waves. Each stage defines:

- one circular outpost with four sectors
- one inner command yard
- 2 to 4 scavenging or utility side objectives
- escalating wave tables
- one map modifier such as fog, acid rain, blackout, or explosive barrels

## Economy, Rewards, Drops, And Upgrades

Core resources are scrap, fuel, and parts. Scrap builds most barricades and traps. Fuel powers arc lamps, flamers, and emergency burn lines. Parts upgrade gun nests, repair stations, and command modules. Night rewards include resource crates, elite drops, and rating bonuses based on breach control, command yard damage, and sector survival. Upgrade categories include:

- barricade durability
- ballistic damage
- trap reset speed
- spotlight stun strength
- repair throughput
- command focus cooldown

## Gameplay Diversity And Content Budget

### Genre Family And Concrete Sub Archetype

The genre family is strategy defense. The concrete sub-archetype is ring-sector perimeter defense with rotating command focus and wave-based base management. It is not a horizontal lane push game, not a fixed tower-slot castle wall template, and not an open-world survival shooter.

### Why It Is Not A Reskin Of An Existing Game

The design differs from Empire Wall Guard by defending a circular multi-sector perimeter instead of a front-facing wall siege. It differs from Crownroad Bastion and Minecart Bastion by removing path-centric tower placement and replacing it with sector reinforcement and breach stabilization. It differs from Ashen Frontier by not using avatar movement, scavenging exploration, or shooter combat as the main loop.

### Map Or Playfield Model

The playfield is a top-down circular base board with four named sectors arranged around an inner command yard. Each sector contains a short outer approach, 4 to 6 defense slots, one breach gate, and one utility node. The inner yard holds the command core and emergency support systems. The map must visually support sector identity rather than feeling like four mirrored lanes.

### Minimum Map Regions, Routes, Zones, And Screens

- 4 outer sectors
- 1 inner command yard
- 4 breach gates
- at least 3 side objective or utility zones across the full map
- at least 2 distinct outer approach shapes across the first release
- menu, gameplay, pause, result, and blueprint screens

### Terrain, Obstacle, Landmark, And Functional Map Element Variety

The first release needs junk walls, oil puddles, power posts, wreck piles, spotlight towers, breach gates, repair pads, and beacon or train landmarks depending on scenario. At least three terrain identities should appear across the first release, such as scrapyard asphalt, muddy checkpoint ground, and frozen industrial yard edges.

### Player, Enemy, Neutral, Item, Projectile, And Effect Roster Targets

Player-facing defense roles must include barricade wall, gun nest, spike trap, flame pipe, arc lamp, repair station, and command skill effects. Enemy roles must include shambler, runner, brute, spitter, crawler, armored riot zombie, and one elite siege class. Neutral and utility roles must include power nodes, salvage crates, explosive barrels, and scenario devices. Primary moving zombies need directional facing and moving animation.

### Mechanic Variety And Progression Targets

Required mechanics include sector selection, defense slot building, upgrade branching, split-wave pressure, emergency command focus, breach repair, utility power decisions, and elite encounter handling. At least two viable playstyles should exist in early content: trap-heavy attrition and gun-nest reactive defense.

### Forbidden Template Reuse

Do not reuse:

- three horizontal lanes with left-versus-right march
- bottom-only command strip plus top resource pills
- static castle wall with tower pads only
- single chokepoint tower maze loop
- generic survivor shooter avatar combat loop

## Visual Identity Contract

### UI Layout Archetype

Use a tactical perimeter dashboard layout with a compact top resource spine, a right-side vertical sector focus rail, and a bottom-left contextual command cluster. The visual identity should resemble a rugged scrap command console overlooking a perimeter schematic, not a generic fantasy tower defense HUD.

### HUD Composition

The HUD shows scrap, fuel, parts, current night, breach alert level, selected sector integrity, focus cooldown, and objective status. Sector cards on the right show all four sectors and flash independently under pressure. Contextual build and upgrade controls should appear only when a slot or module is selected.

### Navigation Model

The game flows through menu, zone select, blueprint loadout, gameplay, pause, and result screens. During gameplay, the player never leaves the board for ordinary actions; overlays stay light and contextual.

### Playfield Safe Area

Reserve the top 11 percent of the screen for resources and mission status, the right 16 percent for sector cards and focus controls, and the bottom-left 24 percent by 24 percent for contextual build actions. The circular playable ring and inner yard must remain fully visible inside the remaining safe viewport and must not be hidden behind decorative bezel art.

### Frame Overlay Policy

Scrap-metal frames, warning stripes, and console chrome may sit only in reserved dead-space edges. They must not overlap the circular defense ring, breach gates, active zombie approach routes, tap-critical defense slots, or emergency command interactions.

### Palette Signature

The selected skin is skin_post_apocalypse, tuned toward rust brown, soot black, oxidized steel, warning amber, sickly green alert glows, and pale sand text for readability.

### Material Language

UI panels should look welded, bolted, and field-repaired. Shapes should use heavy edges, hazard stencil accents, cracked glass meters, and patchwork metal rather than smooth sci-fi panels.

### Typography Style

Use blocky condensed uppercase labels with large numeric readouts and short actionable verbs. The look should feel improvised military-industrial, but still stay readable on phones.

### UI Pack Strategy

Prefer a shared post-apocalypse tactical UI pack if one exists. If not, the UI workflow should produce tracked project-local refinements on top of UI Kit using hazard stripes, welded frames, and sector iconography while still respecting safe-area rules.

### Icon Subject, Silhouette, And Composition

The launcher icon should show a cartoon improvised outpost ring made from scrap barricades with one bright warning lamp and zombie hands pressing in from the outside. The silhouette should read as a circular fortified yard with one strong lamp tower, not a generic shield, bunker, or gun.

### Forbidden UI And Icon Reuse

Do not reuse neon arcade HUD language, top pill plus bottom strip composition, generic castle wall iconography, or a plain skull-on-shield badge. The icon must be specific to a circular scrap outpost under siege.

## Screen Map

### Menu

Title, Start, Zone Select, Blueprint Bay, How To Play, Settings, and Credits over a lightly animated perimeter schematic background.

### Zone Select

A short mission chain map showing threat zones, unlock order, star goals, and scenario modifiers.

### Blueprint Bay

A lightweight pre-mission screen where the player chooses unlockable module variants, passive command traits, and preview stats.

### Gameplay HUD

Resources, current objective, sector rail, selected sector status, breach alerts, contextual build controls, pause, and active skill buttons.

### Pause

Resume, Restart, Settings, Controls, and Return To Menu.

### Result

Victory or defeat banner, night survived, breach count, command yard damage, reward breakdown, stars, and next-step buttons.

### Extra Screens

How To Play and enemy glossary screens should be present because the defense vocabulary is more complex than a one-button arcade loop.

## UI Direction

Recommended ui_skin: skin_post_apocalypse.

Layout tone: rugged, high-pressure, readable under motion.

HUD priorities:

- objective and breach alerts first
- sector integrity second
- resources third
- contextual action fourth

Key overlay panels:

- build and upgrade cluster
- blueprint loadout panel
- pause panel
- result summary

The UI must visibly preserve the circular playfield and inner yard. Decorative chrome should frame the safe area rather than float over it.

## Gameplay Art Direction

### Required Art Roles

- four distinct sector gate modules
- central command core
- barricade wall
- gun nest
- spike trap
- flame pipe
- arc lamp
- repair station
- shambler
- runner
- brute
- spitter
- crawler
- armored riot zombie
- siege elite
- salvage crate
- power node
- explosive barrel
- smoke, sparks, blood-lite hit cues, breach warning flashes

### Camera Perspective

Top-down with slight depth readability so sectors and inner yard remain easy to parse on mobile.

### Suggested Shared Pack Family Or Source Tier

Prefer license-clear top-down post-apocalypse enemy and defense packs with gritty industrial props. If a single pack is too repetitive, combine one zombie pack with one scrap-fortification environment pack through the gameplay art workflow.

### Default Facing And Facing Behavior

Primary zombies should face movement direction using directional sprites or horizontal flipping. Spitters should turn toward the attacked sector target before firing. Brutes and siege elites should visibly face the breached gate or struck barricade.

### Minimum Visual States And Animation Expectations

All primary enemies need idle, walk, hit, and defeat states. Attack-capable enemies need windup, action, and recovery. Defenses need at least active, damaged, and destroyed states where relevant. Arc lamps and flame pipes need powered-on feedback.

### Animation Quality Tier For Primary Entities

Delivery-ready top-down action tier with alternating walk cycles, readable attack timing, and strong damage feedback for primary enemies and active defenses.

### Anchor, Hitbox, And Z-Order Assumptions

Enemy anchors use feet-center. Barricades and defenses use footprint-center. Projectiles sort above ground props and below HUD. Z-order should preserve ring readability without enemies vanishing behind decorative map clutter.

### Visual Readability Constraints

Each sector must remain distinguishable by lighting, prop accents, or gate shape. Enemy silhouettes must stay readable against dark scrap terrain. Alert effects must be visible without becoming full-screen clutter.

### Fallback Rule If No Suitable Pack Exists

The gameplay art workflow may create project-local prototype sprites only as a short-lived fallback, but menu item 10 delivery cannot stop at circles, rectangles, or non-directional placeholder zombies.

## Icon Direction

Subject: scrap barricade ring with warning lamp and feral hands outside the perimeter.

Silhouette: circular fortified yard with one tall lamp tower offset to one side.

Color direction: rust orange, iron gray, acid green glow, and hazard amber.

Tone: cartoon post-apocalypse strategy, readable at app-icon scale, urgent but not horror-gore heavy.

## BGM Direction

Menu music mood: low industrial ambience with distant radio hiss, restrained percussion, and survivor tension.

Gameplay loop mood: pulsing scrapyard rhythm with anxious bass, improvised percussion, and rising pressure when sectors are near breach.

Boss or climax mood: heavier metal percussion and alarm pulses when elite siege waves hit or the inner yard is exposed.

## SFX Direction

- barricade placement
- weld and repair ticks
- gun nest burst fire
- trap snap
- flamethrower ignition
- arc lamp zap
- breach siren
- sector warning ping
- salvage pickup
- zombie hit and collapse
- elite stomp
- victory sting
- defeat alarm

## Technical Implementation Notes

### Rendering Style

Use a custom GameView or SurfaceView for the board, enemies, defenses, and effects. Use Android View or XML overlays for HUD, blueprint loadout, pause, and result screens.

### State Model

MENU, ZONE_SELECT, LOADOUT, PLAYING, PAUSED, RESULT, and HOW_TO_PLAY.

### Important Entities

SectorModel, DefenseSlot, DefenseModule, ZombieUnit, WaveDirector, ResourceWallet, FocusController, CommandCore, ScenarioModifier, and RewardSummary.

### Special Systems To Plan Early

- sector threat routing
- circular board layout and tap mapping
- split-wave scheduling
- contextual build controls
- safe-area-aware HUD layout
- power and fuel dependency rules
- breach recovery timing
- lightweight save data for unlocks and stars

## Differentiation Note Against The Current Registry

This project should stand out as a perimeter management defense game where the main decision is which sector to reinforce and when to rotate command focus. It must preserve the circular base identity, split pressure, salvage economy, and contextual defense management so it does not collapse into a standard lane tower defense, wall siege, or character-controlled zombie shooter.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm the requirements or request revisions.
