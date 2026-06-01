# Minecart Hook Express

Game ID: minecart_hook_express
Direction: auto-scrolling minecart hook runner
Selected Concept: Minecart Hook Express
Recommended UI Skin: skin_post_apocalypse

## Positioning

Minecart Hook Express is a side-view auto-scrolling hook runner where the cart never stops. The player fires left and right side hooks to grab ore, pull loose barricades, trigger rail switches, and create a safe route before the cart reaches each station.

## Target Feel And Player Fantasy

The player should feel like a fast rail operator in a dangerous mining canyon: reading tracks ahead, snapping hooks into side walls, yanking resources and hazards at speed, and arriving at stations with a loaded cart instead of a wreck.

## Core Gameplay Loop

Cart rolls forward, upcoming rail segments and side targets enter view, player fires a left or right hook, hook either collects ore or drags an obstacle or switch lever, player swipes up or down to change rail lanes, cart passes hazards, reaches a station, receives score and upgrade currency, then unlocks a tougher route segment.

## Controls And Input Model

Left half tap fires left hook. Right half tap fires right hook. Up swipe moves to the upper track when available. Down swipe moves to the lower track when available. Tap-and-hold charges a stronger pull for heavy barricades but increases cable heat. Pause and restart use normal UI buttons.

## Win And Failure Conditions

Win a run by reaching the end station with the required shipment value or route objective complete. Fail if cart durability reaches zero, the cart hits a hard barricade without clearing it, falls onto a broken rail, or misses a mandatory station target. Partial success awards currency but blocks chapter advancement when core cargo targets are missed.

## Progression Structure

Progression is chapter based. Each chapter has five station routes and a final long express route. Upgrades improve hook range, rewind speed, cable heat capacity, cart armor, ore magnet priority, and switch reaction time. New chapters add split rails, moving gates, ore wagons, crushers, unstable bridges, and timed switch chains.

## Level Or Run Structure

Each route lasts about 90 to 150 seconds. It is built from scrolling rail segments: straight lanes, fork lanes, tunnel gates, overhead ore pockets, side-wall ore seams, blocked rails, switch yards, bridge gaps, station approach lanes, and bonus depot lanes. Route difficulty rises by mixing faster scroll speed, shorter warning distance, and heavier hook targets.

## Economy Rewards And Upgrades

Ore, gems, cargo crates, and station bonuses convert into coins. Perfect station arrival gives a route stamp. Three stamps unlock a chapter shortcut. Upgrade costs are paid with coins and rare bolts. Optional contracts ask the player to collect certain ore colors, clear a number of barricades, or avoid damage for extra rewards.

## Screen Map

Menu shows cart, current chapter map, last best route, and start button. Gameplay HUD shows cart durability, cargo value, cable heat, station target, lane indicator, and route distance. Pause shows resume, restart, sound, and help. Station result shows cargo value, damage taken, targets completed, and upgrade suggestions. Garage screen shows cart and hook upgrades.

## UI Direction

Use skin_post_apocalypse with a rail-yard dashboard identity: riveted metal panels, amber station lamps, steel-gray track diagrams, coal-dark background, hazard red warnings, and green signal lights. HUD should sit in a top slim rail panel and a compact right-side route meter, keeping the active hook lanes clear. Frames may decorate outer rails only and must not cover side hook targets or upcoming hazards.

## Gameplay Art Direction

Required roles include minecart, wheels, left hook, right hook, hook cable, ore seams, gem clusters, ore wagons, switch levers, breakable barricades, heavy gates, broken rails, bridge gaps, station platforms, sparks, dust, cable heat glow, and impact flashes. Camera is side-view with forward scroll. The cart faces right and animates wheels while moving. Hooks face toward their travel direction and show launch, latch, pull, release, and rewind states. Obstacles need idle, pulled, broken, and impact states. If no suitable shared pack exists, use project-local runtime canvas art and record it as a project internal shared runtime pack.

## Icon Direction

Icon subject is a speeding minecart with two side hooks, one grabbing amber ore and one pulling a red rail barricade. Silhouette should be diagonal cart motion, crossed side cables, ore sparkle, and rail track arc. Palette uses coal black, steel gray, hazard red, signal green, and glowing amber. Tone is chunky cartoon industrial action, clearly different from static gold miner hooks and lunar salvage icons.

## Audio Direction

Menu BGM should feel like clanking station prep with a slow rail rhythm. Gameplay BGM should be fast mechanical percussion with mine tunnel pulse. Climax music should add warning bells and faster drums for final express routes. SFX include hook launch, cable latch, ore collect, barricade pull, switch flip, rail change, cart hit, station arrival, upgrade, win, and fail.

## Technical Implementation Notes

Use a custom Java GameView with deterministic segment spawning. State model: MENU, PLAYING, PAUSED, GAME_OVER, STATION_RESULT, GARAGE. Important entities are cart, hook, cable, rail segment, obstacle, ore target, switch target, station goal, and pickup effect. Plan early for lane collision, hook collision, side-target ownership, cable heat, segment pacing, and station scoring.

## Gameplay Diversity And Content Budget

Genre family is arcade action runner. Sub-archetype is auto-scrolling rail runner with side hooks and lane switching. It is not a reskin of gold_miner or crystal_cavern_hook because the player is not firing from a fixed turret; the cart moves continuously, hazards approach on rails, hooks fire sideways, and rail switching is equally important as collecting.

Minimum content budget: 4 chapters, 20 station routes, 3 rail lanes, 8 segment families, 7 obstacle families, 6 resource target families, 5 upgrade paths, 6 contract variants, and 3 station objective types. Forbidden reuse includes static claw pendulum, fixed minefield, ranch/farm management panels, tower defense lane spawning, and lunar oxygen survival repair loop.

## Visual Identity Contract

UI layout archetype is an express route dashboard with a top signal rail, right route meter, bottom compact garage controls, and central unobstructed scrolling track. HUD composition prioritizes durability, cargo, cable heat, station target, lane, and distance. Playfield safe area reserves center and side hook corridors; all status chrome stays above or outside the hook lanes. Icon and UI must emphasize speed, rails, side hooks, barricades, and cargo, not generic gold nuggets or static claw machines.

## Differentiation Note

Compared with existing hook games, this design changes the camera, movement pressure, aiming direction, hazard model, and route structure. Compared with rail games like Switchyard Courier or Minecart Bastion, this is not route planning or tower patrol; it is live auto-scrolling reflex play with hooks used as both collection and obstacle manipulation tools.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm the requirements or request revisions.
