# Crown Frontier Tactics

Game ID: crown_frontier_tactics
Direction: Ancient Empires-inspired turn-based tactics with village capture, commander-led armies, a large frontier campaign, and classic unit-counter warfare on mobile.
Selected Concept: Crown Frontier
Recommended UI Skin: skin_military_tech

## Positioning

Crown Frontier Tactics is a square-grid turn-based war game built for players who want the classic handheld strategy feeling of marching armies, capturing villages, and winning through clean positioning instead of reaction speed. The project should feel close in spirit to older campaign tactics games, but larger in map scale, clearer in mobile readability, and more modern in onboarding, unit inspection, and commander progression.
The first delivery target is a fully offline single-player tactics game where the enemy side is controlled by AI rather than a second live player.

## Target Feel And Player Fantasy

The target feel is deliberate, readable, and satisfying. Every turn should feel like moving real formations across a contested frontier, where forests delay cavalry, bridges decide whole chapters, archers soften targets before a charge, and commanders turn local skirmishes into decisive breakthroughs. The player fantasy is being a young frontier marshal who rebuilds a fractured border kingdom through terrain control, careful recruitment, and smart use of a growing war roster.

## Core Gameplay Loop

1. Review the current chapter map, mission objective, starting funds, commander roster, and recruit structures.
2. Deploy the available commander and opening units around the allied keep or entry edge.
3. Capture nearby villages and strategic structures to increase income and deny enemy control.
4. Use terrain, formation order, and counter matchups to trade efficiently across the battlefield.
5. Recruit fresh units from captured keeps and forts, reinforce damaged lines, and rotate wounded troops to safer terrain.
6. Push toward the primary objective such as defeating the enemy commander, seizing a capital, surviving a siege, or escorting a prince across the map.
7. Earn chapter rewards, preserve veteran units when possible, unlock the next frontier region, and spend persistent commander points before the next battle.

## Controls And Input Model

- Tap a unit to select it and show movement range, attack range, terrain modifiers, and contextual actions.
- Tap a valid tile to move, then choose attack, capture, wait, recruit, use skill, or cancel from a compact contextual command ribbon.
- Drag the battlefield to pan the camera and use pinch or double-tap shortcuts only for optional zoom or recenter, not for required actions.
- Use dedicated buttons for end turn, cycle idle units, show enemy threat range, open objective panel, and toggle fast enemy turn playback.
- Long press a tile, unit, or structure to open a concise inspection card with terrain effects, unit stats, movement type, and matchup notes.
- The control flow should minimize accidental turn commits by requiring explicit confirmation before attacks, skills, or recruitment purchases are finalized.

## Win Conditions And Failure Conditions

### Win Conditions

- Defeat the enemy commander in standard conquest chapters.
- Capture and hold the target capital, gate, shrine, or fortress when the objective is territorial.
- Escort a required allied unit to an exit tile in travel chapters.
- Survive the required turn count in holdout or evacuation chapters.

### Failure Conditions

- The player commander is defeated in chapters where the marshal is deployed.
- The allied capital or primary keep is captured in defense chapters.
- A required ally, convoy, or heir unit is eliminated during escort objectives.
- The mission timer expires before the main victory condition is satisfied.
- All allied combat units are defeated and no recruit structure remains under player control.

## Progression Structure

- The campaign spans 30 hand-authored chapters across 6 frontier regions: Lowland March, Red Ford, Pinewatch, Iron Pass, Ashen Border, and Crown Approach.
- Each region introduces at least one new terrain pressure, enemy doctrine, and recruitable unit family.
- The player keeps a persistent marshal commander whose doctrine points unlock passive bonuses, limited active orders, and recruitment discounts.
- Veteran named heroes can join for specific regions and reappear later if they survive their original arc.
- Side objectives unlock extra gold, hero recruits, elite promotion tokens, and codex entries rather than filler cosmetics.

## Level Or Run Structure

- The game is chapter-based rather than endless, with each chapter using a large square-grid map.
- Standard map size range should run from roughly 16 x 12 tiles up to 26 x 18 tiles, with the late campaign leaning larger.
- Every region should contain 4 core maps and 1 optional side map, for a minimum of 30 total battles.
- Each map should contain multiple fronts or approach vectors rather than a single one-tile corridor.
- At least half of the campaign maps should include branching geography such as split rivers, alternate bridges, hill ridges, or interior fort lanes.
- A normal chapter should last roughly 12 to 22 minutes depending on objective complexity and unit count.

## Economy Rewards And Upgrade Model

- Gold is earned primarily from controlled villages, trade posts, forts, and mission rewards.
- Recruitment costs vary by unit tier and mobility class to preserve meaningful army composition decisions.
- Between chapters, the player spends marshal points and region rewards on doctrine upgrades, limited hero promotions, and supply perks.
- Optional rewards include rescue bonuses, early-clear bonuses, survival bonuses, treasury captures, and hidden relic objectives.
- Recruitment inside a map should stay simple and readable, while between-map progression should deepen commander identity without turning the game into a spreadsheet simulator.

## Gameplay Diversity And Content Budget

### Genre Family And Concrete Sub-Archetype

- Genre family: turn-based tactics
- Concrete sub-archetype: kingdom-frontier square-grid capture-and-command war campaign

### Why It Is Not A Reskin Of An Existing Game

- It is not a lane or node defense game like `empire_wall_guard` or `crownroad_bastion`.
- It is not a physics puzzle like `siege_flock_tactics`.
- It is not a short skirmish generator with one repeated map template.
- It is built around large authored square-grid maps, village economy, commander objectives, terrain warfare, and recruitable army composition.

### Map Or Playfield Model

- One campaign flow across 6 frontier regions and 30 authored battle maps.
- Large square-grid battlefields with villages, keeps, bridges, rivers, ridgelines, forts, forests, ruins, and road networks.
- Multiple approach lines should exist on most maps, even when one route is safer and another is faster.
- Capturable structures should materially change tempo by adding income, healing access, or forward recruitment.

### Minimum Map And Environment Variety

- 30 maps minimum.
- At least 10 terrain families: plains, road, forest, hill, mountain, bridge, riverbank, fort wall, ruins, marsh, and shrine ground.
- At least 16 functional map elements: villages, keeps, forts, gates, towers, bridges, docks, healing springs, watchfires, ballista pads, barricades, supply wagons, ferry points, collapsible bridges, signal braziers, and treasure vaults.
- At least 18 landmarks across the campaign: border arch, red ford, pinewatch camp, ruined abbey, iron gate, stone viaduct, ash ridge, crown milestone, harbor tower, marsh shrine, canyon stairs, and royal pass beacon.

### Minimum Gameplay Roster Targets

- Allied roster minimum: militia, swordsman, spearman, archer, ranger, knight, lancer, healer, battle mage, scout rider, wyvern rider, ballista crew, and marshal commander.
- Enemy roster minimum: raider swordsman, pike guard, crossbowman, wolf rider, heavy knight, siege crew, war priest, rogue mage, wyvern raider, and elite commander.
- Neutral and support roles: villagers, caravans, prisoners, shrine wardens, supply wagons, allied garrisons, and map-triggered reinforcements.
- Boss or elite roster minimum: border baron, ford butcher, ash witch, iron pass marshal, wyvern captain, and crown pretender.
- Projectile and effect families: arrows, bolts, fire orbs, healing sigils, lance charge streaks, banner aura pulses, fort-shot impacts, and capture flag raises.

### Mechanic Variety And Progression Targets

- Core actions: move, attack, wait, capture, recruit, heal, use commander order, and inspect threat range.
- Secondary systems: terrain defense bonuses, movement penalties, unit counter matchups, village income timing, limited hero promotions, scripted reinforcements, and optional rescue or escort objectives.
- Upgrade families: marshal doctrine tree, hero class promotion, region support perks, commander aura improvements, and recruitment discount branches.
- Mission variants: conquest, holdout, escort, breakthrough, fortress assault, river crossing, shrine defense, fogless night ambush, and two-front split defense.
- Feedback expectations: clean movement previews, decisive combat forecast, distinct faction colors, banner capture animation, and readable death or retreat resolution.
- Enemy control model: enemy armies are AI-controlled by default, with at least easy, normal, and hard behavior presets for aggression, village priority, commander safety, and reinforcement timing.

### Forbidden Template Reuse

- No tower-defense wave lanes.
- No one-screen skirmish loop reused as the whole campaign.
- No auto-battler resolution replacing direct unit control.
- No tiny 8 x 8 test map structure used as production campaign content.
- No generic fantasy reskin with identical economy, roster, and objective structure from another project.

### Asset-Pack Variety Plan

- Search `shared_assets/game_art/` and `shared_assets/ui/` first for tactic-scale medieval packs with directional infantry and readable tiles.
- If the local shared library is too thin or too reused, import a free and license-clear tactics-friendly tileset or sprite pack into the shared library with provenance.
- Primary unit families, terrain tiles, commander portraits, and objective structures must be stylistically aligned rather than mixed from unrelated packs.
- If imported packs lack enough facing or action frames for the main units, use the approved sprite-pipeline workflow to extend from a matching source frame instead of settling for static placeholder pieces.

## Visual Identity Contract

### UI Layout Archetype

The UI should feel like a command table laid over a living battle map. The layout archetype is a left commander ledger, a bottom turn ribbon, and a right-side unit detail drawer that expands only when selection context matters.

### HUD Composition

- Left commander ledger for marshal portrait, chapter name, gold, and doctrine charges.
- Bottom turn ribbon for end turn, idle unit cycle, threat toggle, objective summary, and action prompts.
- Right context drawer for selected unit stats, terrain bonuses, forecast damage, and recruit options.
- Upper-right objective pennant for victory or failure condition reminders.

### Navigation Model

- Main progression moves from a frontier campaign atlas to chapter briefing cards to battlefield deployment.
- Between battles, the player navigates a war room that combines roster, doctrine tree, region map, and codex entries.
- Menus should feel like folded campaign charts, command ledgers, and unit registry books rather than generic centered panels.

### Palette Signature

- indigo command cloth
- burnished brass
- chalk white
- muted parchment tan
- frontier red wax
- moss green terrain accents

### Material Language

- lacquered map table edges
- brass pins
- cloth pennants
- wax seals
- inked route lines
- carved command plaques

### Typography Style

- Strong condensed serif or disciplined military headline style for titles.
- Clean readable body type for stats and terrain data.
- Large high-contrast numeric styling for gold, HP, and forecast values.

### UI Pack Strategy

- Recommended `ui_skin` is `skin_military_tech`.
- The UI workflow should pair that skin with a command-table or strategy-map pack from `shared_assets/ui/` if one is available and not overused.
- If no suitable pack exists, the workflow may import a free and license-clear war-room UI pack from the web into `shared_assets/ui/` before project assignment.

### Icon Subject

- A royal war pennant planted into a cracked frontier milestone with a square battle-map tile pattern behind it.

### Icon Silhouette And Composition

- Tall angled pennant as the main silhouette.
- Stone milestone and crown notch forming the lower anchor.
- Subtle square-tile grid behind the flag to signal turn-based tactics rather than generic action combat.
- One diagonal spear-shadow or banner pole line for directional energy.

### Icon Palette And Background

- Deep indigo field background.
- Brass gold pennant trim.
- Off-white milestone engraving.
- Red wax or crimson seal accent.
- Faded map texture behind the flag rather than a flat badge.

### Forbidden Visual Reuse

- Do not reuse the common top-pill HUD with a full-width bottom action bar.
- Do not reuse wall-defense battlement framing from `empire_wall_guard`.
- Do not reuse neon glass, sci-fi chip panels, or post-apocalypse hazard framing.
- Do not reuse another game's exported icon silhouette, shield badge, zombie head, or helmet motif.
- Do not use a plain banner-only icon with no milestone and no tactical-map signal.

## Screen Map

### Menu

- Title screen
- Continue
- New campaign
- Region select
- Codex
- Settings
- Help

### Gameplay HUD

- Left commander ledger
- Bottom turn ribbon
- Right context drawer
- Upper-right objective pennant

### Pause

- Battle summary
- Objective recap
- Turn count
- Roster status
- Restart or retreat controls

### Game Over

- Defeat reason
- surviving units
- region reached
- retry or chapter select actions

### Extra Screens

- Campaign atlas
- Chapter briefing
- Deployment screen
- War room roster view
- Doctrine tree
- Unit codex
- Victory results

## UI Direction

### Layout Tone

Measured, martial, and tactical. The interface should feel like a polished command instrument for reading battlefields, not an arcade HUD and not a decorative fantasy quest frame.

### HUD Priorities

- Gold, turn state, and current objective must be readable without opening panels.
- Unit stats and combat forecast should appear only when a selection exists.
- The board center must remain visually clean so terrain and threat ranges dominate.
- Recruitment and commander skills should feel anchored to structures and commanders, not float as generic global buttons.

### Gameplay Safe Area

- Reserve the left edge for a narrow commander rail only.
- Reserve the lower edge for the turn ribbon and context prompts.
- Keep the board center and upper-middle bands free from decorative framing.
- Right-side context panels should collapse automatically when not needed.

### Frame And Border Policy

- Decorative map-table edges may exist outside the active board bounds only.
- No thick bezel, crest, or carved frame may overlap movement tiles or touch-critical grid cells.
- Screen motifs should live in side rails, panels, and briefing surfaces rather than on top of the battlefield.

### Key Overlay Panels

- Objective pennant
- recruit structure panel
- combat forecast card
- chapter briefing card
- doctrine unlock card

### UI Asset Strategy

- Reuse a style-matched shared UI pack first.
- If the shared library is weak or overused, import a free and license-clear tactics or war-room UI pack into `shared_assets/ui/` with manifest and license tracking.
- Final menu item `10` output should not ship with plain shape-only placeholder buttons or stock anonymous fantasy frames.

## Gameplay Art Direction

### Required Art Roles

- frontline infantry families
- ranged units
- cavalry units
- flying units
- healer and mage units
- commanders and heroes
- villages and keeps
- forts and gates
- terrain tiles
- recruit structures
- capture banners
- projectile and spell effects

### Camera Perspective

- Top-down orthographic or slightly angled square-grid tactics presentation with clear tile ownership and facing readability.

### Suggested Shared Pack Family Or Source Tier

- Prefer a cohesive tactics-scale medieval sprite and tileset family from `shared_assets/game_art/` first.
- If needed, import a free and license-clear strategy tileset and unit pack from the web into `shared_assets/game_art/` before project wiring.

### Default Facing And Facing Behavior For Moving Or Attacking Entities

- Ground units should face their movement direction while marching and their target direction while attacking.
- Cavalry, flying units, and siege pieces should visibly reorient before attack resolution.
- Captured villages, keeps, and banners should change faction colors immediately after capture completes.

### Minimum Visual States And Animation Expectations

- Primary units need idle, march, attack, hurt, and defeat states.
- Commanders and heroes need more expressive attack and victory or command states than basic troops.
- Ranged units need projectile release frames and visible projectile travel.
- Capture actions need a banner raise or flag-flip moment, not just an instant color swap.

### Animation Quality Tier For Primary Entities

- Medium-high for commanders, cavalry, flyers, and elite units.
- Medium for core infantry, ranged troops, and healers.
- Medium for siege pieces with strong recoil and impact feedback.

### Anchor Hitbox And Z-Order Assumptions For Primary Entities

- Unit anchors should sit at tile center with slight forward bias for tall sprites.
- Tile occupancy should remain one-cell readable even for larger commander or wyvern sprites.
- Projectiles and spell effects render above terrain but below top-level UI rails.
- Structures render below units while banners, damage numbers, and forecast highlights render above them.

### Visual Readability Constraints

- Faction palette contrast must remain strong on grass, road, and fortress tiles.
- Grid and movement overlays must stay readable without drowning the terrain art.
- Large late-game maps cannot rely on muddy low-contrast palettes or tiny illegible units.

### Fallback Rule If No Suitable Pack Exists

- If no coherent free and license-clear pack exists, generate or refine project-local tactics sprites only as a last resort, and still preserve directional states and runtime-art-map coverage for all primary units.

## Icon Direction

### Subject

A frontier war pennant planted into a cracked crown milestone over a square tactics map.

### Silhouette

Tall flag, angled pole, chipped milestone base, and a visible tiled backdrop.

### Color Direction

Indigo, brass gold, off-white stone, muted crimson seal, and dark map shadowing.

### Tone

Classic, strategic, and authoritative rather than cute or hyper-dark.

## Audio Direction

### Menu BGM

Measured and nostalgic, with restrained snare rolls, low strings, and a handheld-era strategy melody shape.

### Gameplay BGM

Steady marching rhythm with clear melodic phrasing, blending retro war-campaign energy with cleaner modern layering.

### Boss Or Climax BGM

Higher brass tension, stronger percussion, and a sense of border-break or final assault urgency.

### SFX Families

- unit march steps
- sword and spear impacts
- arrow releases and landings
- spell chimes and burst hits
- banner capture cues
- recruitment chimes
- turn transition drums
- commander order stingers

## Technical Implementation Notes

### Rendering Style

- Use a Java `SurfaceView` or equivalent real-time board renderer for the grid, units, movement overlays, and attack previews.
- Use XML or Android View overlays for menus, battle HUD, pause, codex, and results.
- Favor sprite batching by atlas family to keep larger maps practical on mid-range devices.

### State Model

- Core states include menu, atlas, briefing, deployment, player turn, enemy turn, resolution, victory, defeat, and pause.
- Each unit should track faction, class, HP, movement type, action consumed state, capture ability, promotion flag, and commander affiliation.
- Campaign persistence should store chapter progress, doctrine unlocks, unlocked heroes, options, and basic battle stats.

### Important Entities

- battlefield grid and terrain tiles
- allied and enemy units
- commanders and heroes
- capturable structures
- villages and keeps
- neutral caravans and escort targets
- projectiles and combat effects
- objective triggers and reinforcement markers

### Systems To Plan Early

- movement and path cost rules
- terrain defense and mobility tables
- recruit structure ownership and income timing
- combat forecast and resolution order
- enemy AI prioritization for capture, kill pressure, commander safety, wounded-unit retreat logic, and behavior differences by difficulty
- campaign save and chapter unlock flow
- runtime art map for unit facing, states, anchors, and tile occupancy

## Differentiation Note

This project should read as a true turn-based frontier war campaign inside the repository, not as a defense game, slingshot game, or action reskin. Its identity depends on large square-grid chapter maps, village-income tempo, commander-led recruitment, terrain warfare, and a command-table UI that keeps the active battlefield readable.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
