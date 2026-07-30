# Lunar Salvage Miner Requirements Draft

## Game Positioning
Lunar Salvage Miner is an Android Java physics grabbing and resource survival mini-game. The player drives a compact lunar salvage cart across ruined moon outposts, scans debris pockets, launches an electromagnetic hook, retrieves ship parts, batteries, oxygen canisters, and repair crates, then uses those resources to restore a small moon base. The game keeps the readable timing appeal of hook mining but changes the decision pressure: every pull must balance salvage value, oxygen, battery charge, meteor debris, cart position, and module repair goals.

## Target Feel And Player Fantasy
The player fantasy is becoming a calm lunar recovery engineer working under pressure in a silent, damaged moon field. The target feel is tactical, tense, and satisfying. The hook should feel precise and metallic, the magnetic pull should snap onto metal parts with visible force, and every returned oxygen tank or battery should feel like buying time for one more risky grab.

## Core Gameplay Loop
1. A run begins in one of six lunar ruin regions with a damaged base module objective.
2. The player scans the debris field to reveal metal mass, oxygen caches, battery cells, meteor fragments, and key module parts.
3. The salvage hook swings from the cart mast or aims from a short arc depending on the current upgrade.
4. The player taps to fire the electromagnetic hook and can use left and right buttons to shift the cart before the next launch.
5. The hook magnetically attaches to valid metal targets, pulls loose resource objects, or bounces from non-metal stone debris.
6. Returning objects add repair parts, oxygen, power, credits, or hazards depending on object type.
7. Oxygen drains with time, battery drains with hook launches and scanner use, and meteor debris can damage the cart or waste a pull.
8. The stage is cleared when the required module repair goal is complete or the marked emergency part is recovered.
9. Between stages, the player spends parts and credits to repair base modules, unlock regions, and upgrade the cart, magnet, scanner, cable, and life support.

## Controls And Input Model
The main control is tap to fire the hook. Two compact left and right buttons move the cart along the top rail before launching so the player can line up difficult debris pockets. A scan button briefly highlights target roles and metal content. A battery boost button can overcharge the magnet for one stronger pull. All controls must stay outside the active debris field. The game must be playable with one thumb for firing and scanning, with optional second-thumb use for cart movement.

## Failure Conditions And Win Conditions
A stage is won by completing its repair target before oxygen reaches zero and before the cart hull is destroyed. The player fails if oxygen is depleted, battery charge is exhausted without enough recovered cells to continue, the cart takes too many meteor impacts, or a critical fragile part is destroyed. Failed stages are retryable, but a good run preserves earned credits and discovered region progress according to later implementation balance.

## Progression Structure
Progression is region-based. The launch scope contains six lunar ruin regions: Landing Pad Ruins, Solar Array Field, Cargo Crater, Habitation Wreck, Comms Dish Valley, and Reactor Shadow Basin. Each region introduces a new pressure pattern. Early regions focus on basic metal grabs and oxygen pickups. Middle regions add battery management, moving meteor fragments, shielded containers, and buried repair sets. Late regions add low-gravity drift, high-value reactor parts, damaged drones, and unstable debris fields.

## Level Or Run Structure
Each stage lasts about 75 to 120 seconds. The playfield is a side cutaway lunar surface with the salvage cart on a top rail or ridge and a debris field below. Targets are arranged across shallow, middle, and deep salvage layers. The cart can reposition horizontally along the top, while the hook fires downward into the field. Region layouts should vary by debris density, shielded pockets, meteor lanes, and required repair item placement. A short session should support two to four stages plus one base repair decision.

## Economy, Rewards, Drops, And Upgrades
Recovered credits buy cart upgrades. Recovered parts repair base modules and unlock new regions. Oxygen canisters extend the current run. Battery cells recharge hook and scanner use. Rare module cores unlock major base functions such as Power Relay, Air Recycler, Workshop, Signal Tower, Shield Dome, and Launch Dock. Upgrade paths include longer cable, stronger electromagnet, faster rewind, wider scan cone, larger oxygen reserve, battery efficiency, cart armor, and emergency reserve systems.

## Gameplay Diversity And Content Budget
The genre family is physics grabbing and resource survival. The concrete sub-archetype is electromagnetic lunar salvage with base repair progression. It is not a reskin of a classic hook miner because the player must manage oxygen, battery, cart position, scan information, magnetic material rules, and base repair objectives instead of only aiming at valuable loot.

The playfield model is a fixed side-view lunar salvage field with a movable top cart, layered debris pockets, and reserved survival HUD zones. Minimum interactive regions are cart rail, hook launch arc, shallow scrap layer, deep salvage layer, oxygen cache pockets, battery pockets, meteor hazard lanes, buried module part pockets, scanner overlay zone, and base repair objective rail. Terrain and map elements include crater ridges, broken solar panels, hull wreckage, cargo containers, moon rocks, regolith mounds, cable snags, meteor streak lanes, and shielded wreck shells.

The minimum entity roster includes six salvage resource families, six hazard families, five repair part families, four cart upgrade systems, six base module objectives, and at least ten feedback effects. Required mechanics include cart repositioning, timed hook launch, magnetic attach rules, object weight and rewind speed, oxygen drain, battery drain, scanner reveal, overcharge pull, meteor collision, fragile part protection, stage repair goals, region unlocks, and base module progression.

Forbidden template reuse: do not reduce the game to aim hook pull coins only; do not reuse the crystal cavern prism path as the main hook; do not copy farm, ranch, card, runner, or tower defense loops; do not place a bottom command strip over the salvage field; do not represent salvage only as generic circles without material, weight, survival, or repair purpose.

## Visual Identity Contract
The UI layout archetype is a lunar control dashboard. The salvage field fills the center, the cart rail sits at the top edge of the playfield, oxygen and battery gauges sit as compact life-support instruments in the upper corners, and the base repair objective appears as a narrow module schematic strip along one side. Buttons should look like hardened EVA controls, not fantasy buttons or farm cards.

The HUD composition prioritizes oxygen, battery, repair objective, recovered parts, cart hull, scan cooldown, and magnet boost charge. Oxygen and battery must be readable at a glance and should use gauge arcs or segmented meters rather than generic score pills. Navigation should move from menu to region select, then gameplay, then base repair, then result.

The palette signature is moon gray, dark vacuum navy, oxygen teal, solar amber, warning red, and clean instrument white. Material language should combine brushed dark panels, frosted glass readouts, cable metal, lunar dust, caution stripes, and blueprint module lines. Typography should be compact, technical, and readable in English.

Playfield safety is strict. The hook path, debris layers, meteor lanes, and cart movement rail must remain unobstructed. HUD instruments may occupy only reserved top corner zones and a narrow side objective rail. Decorative bezels must never cover collectible targets, meteor lanes, or the hook return path.

The icon subject is an electromagnetic salvage claw lifting a broken satellite panel and oxygen canister above a crescent moon crater. The silhouette should read as a hooked magnet cable, angular spacecraft scrap, round oxygen tank, and lunar horizon. It must not reuse the brass crystal hook icon, generic gold miner icon, farm crate, animal face, medical kit, tower, sword, or shield.

## Screen Map
- Menu: title, start button, sound toggle, help button, and a small animated salvage claw above a moon crater.
- Region Select: six lunar regions, base module progress, recommended oxygen and battery level, and unlock requirements.
- Gameplay HUD: oxygen gauge, battery gauge, hull meter, repair objective, cart rail, hook, debris field, scan button, magnet boost, left and right cart buttons, and pause.
- Base Repair: six repair modules with part requirements, module benefits, and next region unlock status.
- Upgrade Workshop: cable, magnet, scanner, battery, oxygen reserve, armor, and rewind speed upgrades.
- Pause: resume, restart, sound toggle, and return to menu.
- Result: recovered parts, oxygen left, battery left, repair progress, region stars, and next action.
- Help: one-page diagram explaining metal targets, oxygen, battery, scan highlights, meteor debris, and base repairs.

## UI Direction
Use exactly one UI skin: `skin_military_tech`. The screen should feel like a clean lunar utility console, with instrument gauges, compact module cards, and a quiet technical palette. HUD priorities are oxygen first, battery second, repair objective third, then hull and recovered parts. Reserve the top 14 percent for life-support instruments and cart rail, the right 16 percent for repair objective and scan information, and the bottom 12 percent for cart movement and boost controls. The central hook and debris field must remain clear.

## Gameplay Art Direction
The camera perspective is a fixed 2D side cutaway of the lunar surface. Required art roles are salvage cart, rail, mast, cable, electromagnetic hook, oxygen canister, battery cell, solar panel scrap, hull plate, circuit core, engine nozzle, satellite dish fragment, module crate, meteor fragment, moon rock, cable snag, unstable container, repair module icon, scan pulse, magnet spark, dust puff, impact flash, and base module schematic.

The cart faces left or right based on movement. The hook rotates along the cable angle and should visually charge when magnet boost is active. Metal objects need idle, highlighted, hooked, pulled, delivered, and damaged states. Oxygen and battery pickups need clear readable silhouettes. Meteor fragments need at least two drift poses or rotation states. Primary moving elements should use lightweight medium-tier animation: cart wheel or tread movement, hook cable extension, magnet pulse expansion, meteor drift, and dust impacts.

Anchors are cable tip for the hook, visual center for floating debris, lower center for heavy grounded scrap, and module socket for repair parts. Hitboxes should be slightly smaller than visual bounds for fair near misses. Z-order should place background crater silhouettes behind debris, debris behind cable, cable behind the hook head, and HUD above only reserved non-playfield zones.

If the shared gameplay art library lacks suitable lunar salvage assets, the gameplay art workflow may create project-local stylized canvas art for the first playable, but delivery-ready output must define runtime art roles and avoid generic circles or rectangles.

## Icon Direction
The icon should show a glowing electromagnetic claw pulling a broken satellite panel and oxygen canister out of a moon crater. The color direction is oxygen teal, solar amber, moon gray, dark navy, white glow, and small red hazard accents. The tone is sci-fi survival, clean, readable, and distinct from a gold or crystal miner icon.

## BGM Direction
Menu music should be quiet lunar ambience with soft pads, distant radio pulses, and sparse metallic tones. Gameplay music should use low-pressure sci-fi percussion, oxygen-warning pulses, and steady mechanical rhythm. Result music should use short repair-confirmation stings. SFX roles include button tap, cart move, scan pulse, hook fire, cable extend, magnet lock, metal attach, non-metal bounce, rewind strain, oxygen pickup, battery pickup, part delivery, meteor impact, hull warning, boost activate, repair complete, region unlock, victory, and failure.

## Technical Implementation Notes
Implementation should be a single Android Java project under `games/lunar_salvage_miner` after confirmation. Use `com.android.boot.MainActivity`, a custom Java game view for hook physics and drawing, and Android View or XML overlays for menu, region select, base repair, upgrades, pause, help, and result screens. Required state model includes MENU, REGION_SELECT, PLAYING, PAUSED, BASE_REPAIR, UPGRADE, RESULT, and GAME_OVER. Important systems are cart horizontal movement, hook launch and return states, magnetic material filtering, object weight, oxygen timer, battery resource, scanner reveal, meteor hazard movement, repair goal tracking, region unlock persistence, and runtime art mapping.

## Differentiation Note Against Registry
Compared with `crystal_cavern_hook`, this design shifts from jewel order collection and prism bends to survival resource pressure, cart repositioning, magnetic material rules, and base repair progression. Compared with the older `gold_miner`, it adds oxygen, power, scanner, moving cart, lunar debris roles, and module repair goals. Compared with ranch, farm, card, runner, action, and tower defense projects, it has no care stations, crops, card shedding, lane running, brawler combat, or tower placement.

## Confirmation Gate
This requirements trace is a draft. Project initialization must not begin until the user explicitly confirms these requirements.
