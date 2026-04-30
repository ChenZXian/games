# Iron Banner Realms

Game ID: iron_banner_realms
Direction: city-state conquest inspired by classic territory expansion warfare with elegant medieval presentation and smooth marching combat animation
Selected Concept: Iron Banner Realms
Recommended UI Skin: skin_military_tech

## Positioning

Iron Banner Realms is a premium-feeling Android Java mini-game about conquering a fractured medieval frontier by routing armies between cities, forts, mills, and mines. It takes the immediate readability of classic city-link conquest games, then deepens it with supply lines, terrain chokepoints, unit classes, and stronger audiovisual identity so the result feels like a polished realm-war campaign instead of a thin clone.

## Target Feel And Player Fantasy

The player should feel like a sharp battlefield sovereign directing banners across a living war map. The tone is confident, elegant, and strategic: rivers matter, roads matter, cavalry flanks matter, and every captured town visibly strengthens the realm. The art should feel refined and storybook-medieval rather than muddy or cheap, while troop motion should feel smooth, elastic, and legible at phone scale.

## Core Gameplay Loop

1. Start each battle with one main citadel and a few nearby villages under player control.
2. Drag from one owned settlement to another node to dispatch troops along roads or open terrain paths.
3. Capture neutral farms, watchtowers, quarries, and barracks to raise income, visibility, reinforcement speed, and unit quality.
4. Cut enemy supply chains so isolated cities lose reinforcement efficiency.
5. Unlock or spend command points on tactical actions such as rally march, shield wall, scout flare, and bridge repair.
6. Overwhelm the enemy capital or force a surrender by controlling the required share of strategic nodes.
7. Earn stars, campaign medals, and realm upgrades that unlock new commanders, map modifiers, and unit perks.

## Controls And Input Model

- Primary input is tap-and-drag from one owned node to a target node to send troops.
- Tap a node to inspect owner, troop count, production role, and linked routes.
- Double-tap an owned capital or barracks to send a rally wave to the nearest contested front.
- Tap tactical ability buttons on the lower-right command cluster for time-limited battlefield actions.
- Pinch to zoom slightly on large maps and drag to pan when the battlefield exceeds one screen.
- Long-press a road or bridge to preview route safety, travel time, and supply status.

## Win And Failure Conditions

### Win Conditions
- Capture the enemy capital.
- Or complete scenario objectives such as holding all river forts for a set duration.

### Failure Conditions
- Lose the player capital.
- Or lose all troop-producing settlements.
- Or fail scenario timers in challenge maps.

## Progression Structure

- World map campaign with 5 frontier provinces.
- Each province contains 4 standard battles and 1 climax battle.
- Three-star rating per stage based on victory, speed, and casualty efficiency.
- Realm progression unlocks commander banners, passive realm edicts, and tactical skill upgrades.
- Optional elite challenge remixes add altered terrain, weather, or enemy doctrines.

## Level Or Run Structure

- Total launch target: 25 core campaign stages.
- Early stages teach node capture, supply lines, and terrain control.
- Mid-game introduces rivers, bridges, barracks, and mixed enemy unit doctrines.
- Late stages add multi-front sieges, fortress rings, and elite enemy counterattacks.
- Climax stages use larger maps with 2 enemy capitals or fortified road networks.

## Economy Rewards And Upgrades

- Node income is abstracted as troop reinforcement rate plus command point gain.
- Farms increase manpower recovery.
- Mines and quarries accelerate fortification repair and heavier unit unlock timers.
- Barracks upgrade nearby troop streams from levy militia to trained arms.
- Watchtowers extend fog-of-war reveal and increase route warning time.
- Between stages, medals can unlock one of three upgrade branches: Logistics, War Council, and Iron Discipline.

## Screen Map

### Menu
- Animated parchment campaign map with active provinces, current banner, and progression summary.
- Quick buttons for Campaign, Elite Trials, Upgrades, Settings, and Help.

### Gameplay HUD
- Top resource spine for troop reserve, command points, objective, and stage timer.
- Left commander card with current doctrine and tactical ability cooldowns.
- Right mini event rail for bridge alerts, node under attack, and supply cut warnings.
- Bottom-right command cluster for tactical skills.

### Pause
- Compact overlay with Resume, Restart, Help, Audio, and Exit.

### Game Over
- Victory or defeat report with map overlay, star result, casualties, stage medals, and next action buttons.

### Extra Screens
- Province selection screen.
- Realm upgrades screen.
- Commander banner selection screen.
- Battlefield help codex for node roles and unit interactions.

## UI Direction

### Layout Tone
- Clean war-table presentation with refined brass markers, parchment overlays, and crisp military readability.

### HUD Priorities
- Objective state.
- Troop reserve and command points.
- Tactical ability readiness.
- Node attack alerts.
- Supply-line warnings.

### Gameplay Safe Area
- Reserve top 12 percent for the command ribbon.
- Reserve left 14 percent for commander card and doctrine badge.
- Reserve right 14 percent for event rail and alert stack.
- Reserve bottom-right 22 percent by 24 percent for tactical skill cluster.
- The central battlefield, all capturable nodes, roads, bridges, and troop movement routes must remain unobstructed.

### Frame And Border Policy
- Decorative parchment corners and brass trims must sit only in reserved dead space.
- No heavy frame, bottom bar, or top pill cluster may cover roads, node labels, or moving troop lines.

### Key Overlay Panels
- Node detail popover.
- Tactical ability tooltip.
- Province briefing panel.
- Realm upgrade card drawer.

### UI Asset Strategy
- Use skin_military_tech tokens as the structural base.
- Refine with parchment map insets, heraldic crests, wax seal dividers, brass command buttons, and animated route glow.
- Candidate reference packs and sources to evaluate in the later UI and art workflows:
  - Kenney Medieval RTS for clean structural map and settlement pieces: https://kenney.nl/assets/medieval-rts
  - Toen's Medieval Strategy Sprite Pack for compact strategy-style world objects and units: https://opengameart.org/content/toens-medieval-strategy-sprite-pack-v10-16x16
  - CitrusGames medieval unit sprites for smoother unit marching and attack loops if licensing and scale fit the project: https://citrusgames.itch.io/mediaval-units

## Gameplay Art Direction

### Required Art Roles
- Player capital, enemy capital, neutral villages, farms, quarries, mines, barracks, watchtowers, bridges, forts, roads, rivers, forests, hills, banners, troop streams, commander markers, tactical FX, and siege objective markers.

### Camera Perspective
- Top-down strategy view with a slight painterly tilt in props, but movement and route readability remain planar.

### Suggested Shared Pack Family Or Source Tier
- Primary source tier: license-clear medieval strategy tiles and settlement props from Kenney and OpenGameArt.
- Secondary source tier: high-quality paid animated unit sprites from itch.io if the pack license supports project use and integration.
- Preferred visual blend: bright but grounded medieval palette, readable roads, saturated banners, and soft terrain shadows.

### Default Facing And Facing Behavior For Moving Or Attacking Entities
- Infantry and cavalry banners should face route direction while moving.
- Ranged volleys should orient toward the contested target node.
- Commander marker and rally VFX should rotate toward the active route origin-to-target vector.

### Minimum Visual States And Animation Expectations
- Levy infantry: idle, march, hit, collapse.
- Spearmen: idle, march, brace, hit, collapse.
- Swordsmen: idle, march, strike, hit, collapse.
- Cavalry: idle, gallop, charge impact, collapse.
- Archers: idle, march, loose shot, hit, collapse.
- Node ownership states: neutral, player, enemy, contested, recently captured.
- Bridges: intact, damaged, repaired.

### Animation Quality Tier For Primary Entities
- Medium-plus 2D strategy animation tier.
- Troop streams should not be static dots; they should visibly pulse or animate in marching packets.
- Primary battle clashes need timed impact flashes, dust, flag sway, and casualty feedback.

### Anchor Hitbox And Z Order Assumptions For Primary Entities
- Troop packets anchor at route centerlines with lane offsets near nodes to prevent overlap soup.
- Capturable structures remain below troop layers and above terrain.
- Tactical FX sit above units but below permanent HUD overlays.

### Visual Readability Constraints
- Node owners must be readable by banner shape and color, not by tiny text alone.
- Roads, bridges, and chokepoints must remain visible under troop pressure.
- Forest and hill decoration must never hide ownership rings or attack arrows.

### Fallback Rule If No Suitable Pack Exists
- Prioritize shared medieval structure assets plus custom refined route and banner effects over low-quality mismatched unit packs.
- If no suitable full animation pack exists, use a small number of well-drawn troop classes with clean directional march loops instead of many static placeholder sprites.

## Icon Direction

### Subject
- A crowned iron war banner planted between two fortified city towers with marching troop pennants crossing below.

### Silhouette
- Tall central banner spike flanked by two asymmetrical castle-tower roofs and a curved troop-flow arc near the bottom.

### Color Direction
- Deep royal blue, iron gray, gold trim, parchment glow, and a sharp crimson enemy accent.

### Tone
- Noble, tactical, and polished rather than grimdark or cartoon-silly.

## Audio Direction

### Menu BGM
- Noble medieval strategy overture with warm strings, restrained drums, and a sense of dynastic ambition.

### Gameplay BGM
- Rhythmic but controlled war-table pulse with marching percussion, plucked strings, and rising tension during frontline swings.

### Boss Or Climax BGM
- Heavier drums, brass swells, and more urgent cadence for fortress assaults and final capital sieges.

### SFX Families
- Banner dispatch swish.
- Footstep march pulse.
- Cavalry thump.
- Arrow volley release.
- Capture chime.
- Alarm horn.
- Bridge repair hammer.
- Victory fanfare.

## Technical Implementation Notes

### Rendering Style
- Custom battlefield view for map, routes, troop streams, and node animation.
- Android View or XML overlays for HUD, pause, result, and upgrade surfaces.

### State Model
- MENU
- PROVINCE_SELECT
- UPGRADES
- PLAYING
- PAUSED
- VICTORY
- DEFEAT

### Important Entities
- SettlementNode
- RouteLink
- TroopPacket
- CommanderDoctrine
- TacticalAbility
- SupplyChainState
- BattleScenario

### Systems To Plan Early
- Path and route ownership graph.
- Supply-line propagation.
- Smooth troop packet interpolation and merge logic.
- Node production timers and capture contest rules.
- Tactical ability cooldown and targeting rules.
- AI aggression profiles for expansion, defense, and flank raids.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype
- Genre family: strategy.
- Concrete sub-archetype: real-time city-link conquest with supply-line warfare and unit-class pressure.

### Why It Is Not A Reskin Of Existing Registry Entries
- It differs from territory_swarm by replacing abstract prefecture routing with a medieval battlefield graph, terrain bottlenecks, supply cuts, mixed troop classes, and structured campaign provinces.
- It differs from RTS base-building games in the registry by removing worker-economy construction and focusing on route conquest tempo.

### Map Or Playfield Model
- Province battlefields built from linked city-state nodes, roads, river crossings, forts, and economic points.
- Stage targets: 14 to 28 capturable nodes depending on chapter.
- At least 5 map archetypes: open plains, river forks, forest road maze, hill fort ring, and twin-capital siege.

### Terrain Obstacle Landmark And Functional Map Element Variety
- Terrain types: plains, forest, hills, riverbanks, stone roads, mud fields.
- Functional map elements: bridges, forts, barracks, farms, mines, watchtowers, choke gates.
- Landmarks: monasteries, ruined aqueducts, giant oaks, cliff shrines, field camps.

### Player Enemy Neutral Item Projectile And Effect Roster Targets
- Player troop classes: levy infantry, swordsmen, spearmen, archers, light cavalry.
- Enemy troop classes: same base roster plus scenario-specific elite guards and raiders.
- Neutral entities: villages, forts, mercenary camps, holy shrines, supply wagons.
- Effect families: arrow volleys, shield wall glow, rally banner pulse, bridge repair sparks, capture ring flare.

### Mechanic Variety And Progression Targets
- Primary actions: send troops, split reinforcements, defend choke, commit tactical skill.
- Secondary actions: scout map sectors, repair bridges, trigger rally march, lock a node into shield wall stance.
- Upgrade paths: Logistics, War Council, Iron Discipline.
- Scenario variants: holdout, race-to-capital, dual-front invasion, bridge-control, supply-denial, encirclement.
- Failure pressure: attrition, split fronts, timed objectives, elite counterattacks, route collapse after bridge loss.

### Animation And Feedback Expectations For Primary Actors
- Troop packets visibly march with directional movement.
- Captured nodes swap banner animation, flare, and ownership ring.
- Battle collisions create brief dust, sparks, and casualty falloff rather than silent number subtraction.

### Asset Pack Variety Plan
- Use one primary medieval environment pack family plus one animated unit pack family.
- Keep capital structures, farms, forts, and bridges visually distinct rather than recolored duplicates.
- Ensure each province introduces at least one landmark set and one route-hazard visual theme.

### Forbidden Template Reuse
- No abstract prefecture circles with plain lines.
- No identical node graph reused across multiple provinces with recolor only.
- No top-pill plus bottom-strip HUD borrowed from earlier projects.
- No tiny three-lane defense layout disguised as conquest.
- No static dot armies standing in for marching medieval troops.

## Visual Identity Contract

### UI Layout Archetype
- Regal war-table dashboard with top command ribbon, left doctrine card, right alert rail, and bottom-right tactical cluster.

### HUD Composition
- Troop reserve, command points, province objective, timer, current doctrine, alert queue, tactical cooldowns, and selected-node summary.

### Navigation Model
- Menu to province map to battle briefing to gameplay, with compact pause and battle result overlays and a separate realm-upgrade screen.

### Playfield Safe Area
- Reserve top, left, right, and bottom-right zones exactly as listed above so no live route, node, or troop stream sits under HUD chrome.

### Frame Overlay Policy
- Brass trims, heraldic corners, and parchment insets may decorate dead space only.
- No large decorative frame may overlap capturable nodes, roads, bridges, or combat packets.

### Palette And Material Language
- Royal blue, parchment tan, iron steel, old gold, muted forest green, and enemy crimson.
- Material language is heraldic military tablecraft: wax seals, brass bezels, stitched map leather, enamel badges, and engraved route markers.

### Typography Direction
- High-contrast serif titles for campaign surfaces and clean condensed tactical labels for gameplay HUD.

### UI Pack Strategy
- skin_military_tech as token base, then custom heraldic map-war refinement pack tracked through the UI workflow.

### Icon Subject And Silhouette
- Crowned iron banner between city towers with troop-flow arc; no generic shield medallion or plain castle front.

### Forbidden Visual Reuse
- No neon-future HUD language.
- No zombie or post-apocalypse scrap motifs.
- No repeated top pill cluster with bottom command strip.
- No generic castle-only icon.
- No plain colored dots as armies.
- No heavy decorative frame over active map routes.

## Differentiation Note

Iron Banner Realms aims to occupy a gap in the current repository: a polished medieval city-state conquest game with stronger route tactics, better terrain identity, richer presentation, and smoother troop animation than the existing abstract territory and base-war projects.

## External Asset Research Notes

The art workflow should prioritize license-clear or commercially licensed sources that support a refined medieval strategy look:

- Kenney Medieval RTS: CC0 and structurally excellent for settlements, map props, and strategy readability. Source: https://kenney.nl/assets/medieval-rts
- Toen's Medieval Strategy Sprite Pack: OpenGameArt listing with clear attribution model, useful for compact top-down strategy units and environment pieces. Source: https://opengameart.org/content/toens-medieval-strategy-sprite-pack-v10-16x16
- CitrusGames Medieval Units: paid animated unit pack candidate for smoother marching and combat cycles if style fit and license verification pass during the gameplay art workflow. Source: https://citrusgames.itch.io/mediaval-units

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
