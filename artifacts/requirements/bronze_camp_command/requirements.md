# Bronze Camp Command

Game ID: bronze_camp_command
Direction: Age of Empires style war economy strategy mini-game
Selected Concept: Bronze Camp Command
Recommended UI Skin: skin_military_tech
Requirements Status: Draft

## Positioning

Bronze Camp Command is a compact offline Android Java real-time strategy mini-game about growing a Bronze Age camp from a few villagers into a strike force. It preserves the fantasy of gathering resources, placing core buildings, training counters, scouting, and choosing the right attack timing, while keeping each battle short enough for mobile sessions.

## Target Feel And Player Fantasy

The player should feel like a field commander making fast economic choices under pressure: assign workers, decide whether to boom or rush, read the enemy camp, then launch a decisive squad attack before the rival economy overwhelms them. The tone is ancient frontier warfare with readable tactical clarity rather than heavy simulation.

## Core Gameplay Loop

1. Start with a Town Hut, three villagers, a small food patch, a wood grove, and a stone outcrop.
2. Assign villagers to gather food, wood, or stone by tapping a resource node.
3. Spend resources on Barracks, Range Yard, Stone Workshop, Watch Post, and worker production.
4. Train militia, spearmen, slingers, shield guards, and ram crews.
5. Scout fogged map zones with a small scout banner to reveal enemy buildings and raids.
6. Defend against light enemy probes while growing the economy.
7. Form squads and attack the enemy landmark before the timer or enemy final assault.
8. Earn stars from clear time, landmark health, and economy efficiency.

## Controls And Input Model

The game uses mobile-friendly tap and drag controls. Tap buildings to open a contextual command radial. Tap resource nodes to assign the selected villager group. Drag a box on the playfield to select combat units. Tap ground to move, tap enemies or buildings to attack, and double-tap the Town Hut to recall idle villagers. A right-side squad rail provides quick selection for villagers, infantry, ranged units, and siege units.

## Win And Failure Conditions

The player wins by destroying the enemy Chief Hall or by capturing and holding both neutral Obelisks for 45 seconds after the third minute. The player loses if their own Town Hut is destroyed, if all villagers are defeated with no food to train replacements, or if the enemy launches its final assault at 8 minutes and destroys the camp landmark.

## Progression Structure

The first release should include a three-map campaign. Map 1 teaches resource assignment and basic militia attacks. Map 2 adds scouting, spearmen, and slingers. Map 3 adds stone economy, ram crews, Watch Posts, and a stronger enemy landmark. Persistent progression unlocks cosmetic banner colors, challenge medals, and one civilization perk slot for replay.

## Level Or Run Structure

Each match is a bounded 6 to 8 minute skirmish. The map begins partially visible around the player camp. Enemy raids occur on a schedule with variation based on player scouting and aggression. Neutral Obelisks and resource-rich outer nodes encourage leaving the base. The run ends at victory, defeat, or the enemy final assault.

## Economy Rewards And Upgrades

Resources are food, wood, and stone. Food trains villagers and infantry. Wood builds production structures and ranged units. Stone builds Watch Posts, upgrades the Town Hut, and produces ram crews. Upgrades include Worker Tools, Hardened Shields, Sling Pouches, Spear Drill, Reinforced Rams, and Signal Fires. Rewards are stars, map medals, and civilization perk unlock progress.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

The genre family is real-time strategy. The concrete sub-archetype is micro base-building skirmish with worker economy, fog scouting, squad control, and landmark assault. It is not a lane push game, not a tower defense game, and not a turn-based grid tactics game.

### Why It Is Not A Reskin Of An Existing Game

The design differs from Camp Conquest by requiring worker resource assignment, building placement, and production queues instead of pure camp capture. It differs from Lane Warfront and Epic Campaign War by removing fixed lanes and energy-only unit spawning. It differs from Crown Frontier Tactics by using real-time economy and direct map control rather than turn-based grid actions.

### Map Or Playfield Budget

The playfield is a top-down free movement map with a player camp, enemy camp, two neutral Obelisks, three resource regions, two choke paths, and one flanking path. Each map must include at least four interactive regions, three terrain types, three functional resource families, and landmarks that change tactical decisions. The camera is fixed to a full-map or soft-panning top-down board sized for phone landscape play.

### Entity Roster Budget

The first playable must include villagers, scouts, militia, spearmen, slingers, shield guards, ram crews, enemy raiders, enemy spearmen, enemy slingers, enemy guards, an elite chieftain, resource nodes, buildings, projectiles, hit sparks, dust puffs, and rally markers. Primary humanoid units need directional facing and alternating walk poses when moving.

### Mechanic Variety Budget

The required mechanics are worker assignment, building placement, production queues, fog scouting, squad selection, attack move, defensive recall, tech upgrades, enemy raids, neutral objective capture, and landmark destruction. At least three enemy wave variants and two viable player strategies must exist: fast militia rush and delayed stone-tech assault.

### Forbidden Template Reuse

Do not reuse the three horizontal lane push layout, the bottom-only unit strip, the fixed wall-defense template, the pure node-number territory spread model, or the turn-based square-grid tactics loop. The map must support free unit movement and economic base decisions.

## Visual Identity Contract

### UI Layout Archetype

Use a battlefield command map layout with a compact top-left resource ledger, a right-side squad rail, and a bottom-left contextual command radial. The UI should feel like a bronze-and-verdigris field tablet with tactical overlays, not a generic top HUD plus bottom button strip.

### HUD Composition

The HUD shows food, wood, stone, population, idle villagers, selected squad count, current objective, and wave warning. Command options appear only for the current selection. The right-side rail has icon slots for villagers, infantry, ranged units, siege, and all-army selection.

### Navigation Model

Menu, campaign map, gameplay, pause, result, and codex screens use direct buttons and tabs. During gameplay, all tactical actions are available without leaving the playfield.

### Playfield Safe Area

Reserve the top 12 percent for the resource ledger and objective strip, the right 15 percent for squad rail controls, and the bottom-left 22 percent by 26 percent for contextual commands. Active building placement and unit movement must avoid these HUD zones.

### Frame Overlay Policy

Decorative frames may sit only in reserved HUD edges. No border, bezel, or corner ornament may cover active map paths, resource nodes, building footprints, or selection gestures.

### Palette And Material Language

The selected skin is skin_military_tech, tuned toward dark olive, aged bronze, pale mint tactical text, warm amber alerts, and stone-gray panels. Materials should suggest carved tablets, bronze rivets, map ink, and low-glow tactical lines.

### Typography Direction

Use compact uppercase labels for resources and commands, larger numeric resource values, and short objective text. Avoid ornate fantasy type; readability is more important than decoration.

### UI Pack Strategy

Prefer a style-matched shared UI pack if available. If the shared library lacks a suitable bronze tactical pack, create project-local XML UI refinements through the UI workflow, then track the assignment. UI Kit tokens remain the structural foundation.

### Icon Subject And Silhouette

The launcher icon should show a cartoon bronze villager hand setting a granary marker beside wheat bundles on a clay ledger. The silhouette should read as a compact economy seal with a hut block and wheat cluster, not a generic shield, tower, or sword.

### Forbidden Visual Reuse

Do not reuse the top HUD pill plus bottom command strip composition from lane defense projects. Do not reuse wall-defense icon subjects, generic castle silhouettes, neon lane palettes, or the same icon shield foreground used by other strategy games.

## Screen Map

### Menu

Shows title, Start Campaign, Skirmish, Codex, Settings, and a small animated camp map background.

### Campaign Map

Shows three battlefield nodes, star medals, unlocked perk slot, and selected challenge modifiers.

### Gameplay HUD

Shows resources, population, objective, selected squad, command radial, quick squad rail, pause, and wave warning.

### Pause

Shows Resume, Restart, Settings, Controls, and Quit To Menu.

### Game Over

Shows Victory or Defeat, stars, clear time, economy efficiency, units lost, landmark damage, Restart, Next Map, and Menu.

### Extra Screens

Codex explains unit counters, resources, buildings, and objective rules in short English text.

## UI Direction

The chosen ui_skin is skin_military_tech. The layout tone is tactical and compact. HUD priorities are resources first, selected squad second, objective third, and alerts fourth. The playfield safe area must prevent the command radial and squad rail from blocking building placement. Key overlay panels are building queues, upgrade choices, unit counters, and result scoring.

## Gameplay Art Direction

Required roles are villagers, scout, militia, spearman, slinger, shield guard, ram crew, enemy variants, chieftain elite, Town Hut, Barracks, Range Yard, Stone Workshop, Watch Post, Chief Hall, food patch, wood grove, stone outcrop, Obelisk, projectiles, dust, hit sparks, rally flags, fog edge, and selection rings. The camera is top-down with slight three-quarter readability. Moving humanoids should face movement direction by directional frames or horizontal flipping and use at least two-frame walk alternation. Attack-capable units need windup, action, and recovery timing through pose changes or sprite swaps. Anchors use feet center for humanoids, footprint center for buildings, and front axle center for rams. Hitboxes should be smaller than visual bounds for units and exact footprint rectangles for buildings. Z-order sorts by y-position for units and fixed layer for terrain, buildings, effects, and HUD. If no suitable pack exists, the gameplay art workflow may create project-local stylized sprites, but delivery-ready output cannot rely on generic circles or rectangles.

## Icon Direction

The icon subject is a bronze villager hand placing a granary marker beside wheat bundles on a clay ledger. The silhouette is a compact economy seal with a hut block and wheat cluster. The color direction is bronze, olive, stone, wheat gold, and verdigris. The tone is cartoon strategy, clear at small sizes, and strongly tied to base-building warfare.

## Audio Direction

Menu BGM should be calm ancient camp ambience with low drums and plucked strings. Gameplay BGM should be steady tactical percussion that intensifies after enemy raids begin. Climax BGM should add heavier drums during the final assault or landmark attack. SFX families include resource gather taps, building placement, train complete, rally horn, unit attack, projectile hit, ram impact, tech upgrade, warning horn, victory sting, and defeat sting.

## Technical Implementation Notes

Rendering should use a custom GameView or SurfaceView for map, entities, fog, selection, and effects, with Android View or XML overlays for HUD and menus. The state model must include MENU, CAMPAIGN, PLAYING, PAUSED, RESULT, and CODEX. Important systems to plan early are resource economy ticks, command selection, pathing around simple circular blockers, building footprint validation, production queues, enemy raid director, fog reveal, capture zone timers, and save data for medals.

## Differentiation Note

This project should be positioned as a tiny real-time economy war game. It must preserve worker economy, building placement, free map movement, fog scouting, and landmark assault so it does not collapse into existing lane battle, tower defense, territory swarm, or turn-based tactics templates.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm the requirements or request revisions.
