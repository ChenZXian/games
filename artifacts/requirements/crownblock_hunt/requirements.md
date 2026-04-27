# Crownblock Hunt

Game ID: crownblock_hunt
Direction: Large-map side-scrolling street action inspired by flash-era district brawlers, with partner support, a layered weapon system, strong hit impact, and fluid combat.
Selected Concept: Old King District Hunt
Recommended UI Skin: skin_dark_arcade

## Positioning

Crownblock Hunt is a side-scrolling district-control action brawler built for players who want the old flash-era street-fight fantasy expanded into a much bigger and more physical game. Instead of one short linear stage, the player pushes block by block across a connected old-city district, brings partners into battle, picks up or equips multiple weapon classes, and fights with fast cancel windows, hard knockback, visible hitstop, and territory progression that changes the map over time.

## Target Feel And Player Fantasy

The target feel is aggressive, weighty, and stylish without becoming floaty. Every punch, bat swing, shotgun blast, guard break, launcher, and wall hit should feel expensive and readable. The player fantasy is leading a small street assault crew into a ruined district, smashing through gangs, rescuing allies, taking over blocks, and turning a dangerous urban maze into your own controlled territory.

## Core Gameplay Loop

1. Review the district board, current gang heat, partner availability, territory bonuses, and mission rewards.
2. Choose one route through the current district and equip one melee setup, one firearm loadout, one throwable slot, and two partner assists.
3. Enter connected street sectors and fight patrols, gate squads, rooftop shooters, and indoor holdouts.
4. Chain melee strings into launchers, weapon finishers, dodge cancels, guard breaks, and partner assists to maintain crowd control and combo income.
5. Pick up dropped weapons, recover medicine or cash stashes, rescue contacts, and capture local control points.
6. Beat the sector lieutenant, unlock the next route branch, and return to a safehouse or continue deeper with reduced resources.
7. Spend cash, weapon parts, and reputation on fighter moves, weapon upgrades, partner skills, and district support bonuses before the next push.

## Controls And Input Model

- Left virtual stick handles movement and short lane-depth repositioning inside the side-scrolling combat plane.
- Right-side buttons handle light attack, heavy attack, jump, dodge, firearm attack, and interact or pickup.
- Weapon swap uses one quick switch button and one hold popup for grounded pickups or reserve weapons.
- Partner support uses two direct assist buttons plus a hold command for follow, focus fire, or revive priority.
- Context actions cover door breach, stash pickup, execution on stunned elites, and control-point capture.
- Input buffering, cancel windows, and short hitstop should be planned early so the combat stays fluid instead of sticky.

## Win Conditions And Failure Conditions

### Win Conditions

- Capture all 9 territory districts in the Old King zone.
- Defeat the four major lieutenants and the final district ruler.
- Secure the main safehouse route and restore permanent control over the district board.

### Failure Conditions

- The player is fully defeated with no partner revive or recovery token remaining.
- A mandatory contact or escort target dies during critical rescue missions.
- A timed manhunt or defense mission expires before the control objective is completed.
- The team wipes during a district boss encounter and loses the active route attempt.

## Progression Structure

- Macro progression spans 9 named districts, each with its own faction pressure, landmark boss, and route network.
- Territory capture unlocks clinics, weapon shops, safehouses, shortcuts, and partner dispatch points on the district board.
- The player grows through move unlocks, launcher and grab branches, firearm mastery, passive combat traits, and combo economy bonuses.
- Partners grow through loyalty ranks, assist upgrades, rescue skills, and role specialization such as striker, gunner, tank, or medic.
- District control unlocks permanent support perks such as cheaper ammo, safer med drops, or backup reinforcements at captured blocks.

## Level Or Run Structure

- The full game uses one large connected district map with 9 macro zones.
- Each macro zone contains 4 to 6 combat sectors, for a minimum of 40 playable sector nodes.
- Sector types include market streets, alleys, rooftop walkways, stairwells, subway entrances, warehouses, arcades, pawn shops, and courthouse approaches.
- Runs can stop after one control point or continue deeper for better rewards at higher health and supply risk.
- Each district should contain at least one optional side route, one shortcut unlock, one rescue or stash objective, and one boss or elite gate.
- A standard route push should take 8 to 14 minutes depending on branch choice and boss depth.

## Economy Rewards And Upgrade Model

- Core resources are cash, weapon parts, medicine, ammo crates, reputation, and district intel.
- Combat rewards scale with combo quality, elite finishers, rescue success, and route depth.
- Weapon progression covers melee categories, sidearms, SMGs, shotguns, rifles, and thrown tools.
- Upgrade paths include attack speed, launch height, guard break force, reload speed, ammo reserve, recoil control, assist cooldown, and revive efficiency.
- Shops and safehouses offer consumables, backup weapons, route hints, and partner contract upgrades.
- Optional risk contracts reward no-down clears, full block capture chains, and minimum-damage boss kills.

## Gameplay Diversity And Content Budget

### Genre Family And Concrete Sub-Archetype

- Genre family: action brawler
- Concrete sub-archetype: large-district side-scrolling street-control beat-em-up shooter

### Why It Is Not A Reskin Of An Existing Game

- It is not a short single-stage lock-gate brawler like `ashen_strike_trial`.
- It is not a boss-farm action loop like `relic_blade_trials`.
- It is not a compact arena or survival-wave game.
- It is built around district conquest, partner orchestration, route replay, and mixed melee plus firearm combat inside a side-scrolling urban map.

### Map Or Playfield Model

- One connected district board with 9 macro territories.
- Minimum 40 sector nodes across streets, interiors, rooftops, and transit spaces.
- Side-scrolling stages with lane-depth movement and branch exits.
- Captured zones remain changed on later visits through safehouses, shops, or reduced patrol pressure.

### Minimum Map And Environment Variety

- 9 macro districts.
- Minimum 40 sector nodes.
- At least 8 terrain families: market street, alley, subway entrance, rooftop line, warehouse floor, old arcade interior, courthouse stairs, riverside road.
- At least 18 landmarks: tram depot, fish market gate, old station tunnel, church yard, canal bridge, pawn district sign, courthouse arch, warehouse crane.
- At least 12 functional map elements: breakable doors, chain gates, med vending points, weapon lockers, stash crates, improvised barricades, drop bridges, elevator lifts, rooftop jump links, alarm switches, control banners, rescue cages.

### Minimum Gameplay Roster Targets

- Player leader plus at least 8 recruitable partners.
- Enemy roster minimum: street brawler, knife rusher, pipe bruiser, shield enforcer, pistol thug, SMG gunner, rooftop sniper, biker rusher, heavy brute.
- Elite and boss roster minimum: riot captain, blade twin, shotgun lieutenant, bike chief, district ruler.
- Neutral and support roles: civilians, informants, shopkeepers, med staff, captured allies.
- Pickup and support objects: melee pickups, firearm pickups, ammo crates, medicine bags, money stashes, throwable caches.

### Mechanic Variety And Progression Targets

- Core player actions: move, light combo, heavy strike, launcher, juggle, dodge, jump attack, firearm attack, reload, pickup, execution, partner assist.
- Secondary systems: revive, rescue, control-point capture, route choice, combo economy, guard break, crowd knockback, wall stun.
- Upgrade families: player move tree, firearm mods, partner assist tree, territory support perks, consumable capacity.
- Mission variants: route sweep, escort rescue, hold point, boss raid, manhunt chase, stash breach, last-block defense.
- Strong hit feedback is mandatory through hitstop, recoil, attack trails, enemy stagger, screen shake bursts on heavy finishers, and distinct audio layers.

### Forbidden Template Reuse

- No one-room arena survival loop.
- No tiny lock-gate corridor repeated as the entire game structure.
- No pure melee-only brawler with cosmetic firearms.
- No top-down scavenging map.
- No generic three-wave then boss template reused across all districts.

### Asset-Pack Variety Plan

- Shared assets should be searched first under `shared_assets/game_art/` and `shared_assets/ui/`.
- If the shared library is too weak or stylistically wrong, the workflow may import free and license-clear online packs into shared shared-asset libraries with provenance recorded.
- Fallback online sources must stay free and license-clear, with preference for CC0 or clearly reusable packs before any project-local sprite generation.

## Visual Identity Contract

### UI Layout Archetype

The UI should feel like a district warrant board and strike dossier rather than a generic arcade overlay. The layout archetype is a left dossier rail for player status, a right pager column for partners, and a lower-right combat slab for weapon state and combo pressure.

### HUD Composition

- Left vertical rail: health, stamina, armor, and heat.
- Right portrait pager: two active partners, cooldown rings, revive state, and loyalty spark.
- Lower-right slab: current weapon, ammo, pickup weapon icon, combo rank, and finisher readiness.
- Upper-left district stamp: current block name, control percentage, and route objective.

### Navigation Model

- Main menu opens into a district evidence wall instead of a plain button list.
- Safehouse screens use pinned route cards, crew roster tabs, and confiscated-gear trays.
- Mission entry flows from district board to route card to combat scene.

### Palette Signature

- Asphalt navy
- sodium amber
- bruised crimson
- faded police white
- dirty brass
- cold fluorescent mint for route traces

### Material Language

- scratched enamel signs
- taped warrants
- acetate evidence sleeves
- chipped station metal
- spray-painted route marks
- dented gear trays

### Typography Style

- Bold compressed action sans for titles
- narrow dossier labels for system text
- thick impact numerals for combo and ammo readouts

### UI Pack Strategy

- Primary foundation is `skin_dark_arcade`.
- The UI workflow should combine that skin with a gritty urban dossier or law-board style pack if a fitting shared pack exists.
- If the shared pack library does not fit, the workflow may download a free and license-clear pack from the web and stage it under `shared_assets/ui/` before assignment.

### Icon Subject

- A taped fist raised in front of a cracked Old King district street sign with a small sawn-off silhouette along the lower edge.

### Icon Silhouette And Composition

- Strong upright fist shape.
- Broken rectangular street-sign plate behind it.
- Short diagonal weapon line near the bottom.
- One bright route-mark slash cutting across the sign.

### Icon Palette And Background

- Deep asphalt blue background with sodium amber edge light.
- Skin or glove tones muted and bruised, not cartoon bright.
- Brass, rust, and off-white sign details.
- Small crimson accent for danger and conflict.

### Forbidden Visual Reuse

- Do not reuse the common top-pill HUD with a full-width bottom action bar.
- Do not reuse fantasy parchment, clean sci-fi neon chips, or post-apocalypse rust field-kit framing.
- Do not reuse another game's exported icon silhouette, shield badge, zombie head, or helmet motif.
- Do not use a plain fist-only icon with no district-sign identity.

## Screen Map

### Menu

- Title screen with district skyline, evidence-wall menu, continue, new campaign, crew, district board, settings, and help.

### Gameplay HUD

- Left dossier rail for player survival values.
- Right partner pager stack.
- Lower-right combat slab for combo, weapon, and ammo.
- Upper-left block header with mission objective.

### Pause

- District card overlay with current route progress, partner status, loot summary, and retry or retreat actions.

### Game Over

- Defeat recap with district reached, blocks controlled, partner saves, weapon usage, and retry options.

### Extra Screens

- Safehouse hub
- Crew roster
- Weapon bench
- District board
- Shop and clinic
- Route result screen

## UI Direction

### Layout Tone

Heavy, urban, compressed, and utilitarian. The screen should feel like a violent city crackdown board, not a bright cartoon interface and not a fantasy quest UI.

### HUD Priorities

- Health, combo pressure, and current crowd danger must stay readable at all times.
- Partner availability must be visible without covering the fight.
- Ammo and pickup-weapon state should appear only when relevant but remain fast to scan.
- District objective and control progress should stay compact, not dominate the top edge.

### Key Overlay Panels

- District evidence board
- Crew pager
- Weapon tray comparison drawer
- Route card mission summary
- Block capture result stamp

### UI Asset Strategy

- Start from `skin_dark_arcade` tokens.
- Prefer a tracked shared UI pack that looks like urban law-board gear, worn neon signage, or industrial folder hardware.
- If the shared UI library is insufficient, download a free and license-clear pack from the web, store it under `shared_assets/ui/`, and record provenance before project assignment.

## Gameplay Art Direction

### Required Art Roles

- Player street enforcer
- 8 partner silhouettes with role differentiation
- Gang enemies across melee, ranged, shield, bike, and heavy categories
- District bosses and elite lieutenants
- Street props, market stalls, subway props, rooftop props, control banners, safehouse props
- Weapon pickups, ammo cases, medicine pickups, throwable effects, muzzle flashes, hit sparks, dust, debris, wall-hit effects

### Camera Perspective

- Side-scrolling 2.5D brawler view with limited lane depth

### Suggested Shared Pack Family Or Source Tier

- First choice is a gritty urban side-scrolling action or brawler pack already staged in `shared_assets/game_art/`.
- Second choice is a clearly licensed modern gang, city, or police-raid style pack for characters and props.
- If the shared library does not fit, the workflow may download free and license-clear packs from the web, import them into `shared_assets/game_art/`, and record provenance before project use.

### Default Facing And Facing Behavior

- Player, partners, and all main enemies must support left and right facing with proper attack-side switching.
- Firearms must align muzzle direction with facing and attack pose.
- Bikers or rush units may use stronger directional lean or motion exaggeration.

### Minimum Visual States And Animation Expectations

- Player: idle, walk, run, light attack chain, heavy attack, launcher, jump, fall, dodge, shoot, reload, hurt, knockdown, pickup, assist call.
- Partner: idle, move, attack, assist, hurt, downed, revive, finisher pose.
- Standard enemies: idle, patrol, attack, hurt, knockback, knockdown, get-up, death.
- Elites and bosses: intro, patrol, windup, attack, stagger, armor break, downed, death.
- Pickups and interactives: idle, highlight, collect or activate.

### Animation Quality Tier For Primary Entities

- High for player, partners, elites, and bosses.
- Medium-high for standard gang enemies.
- Medium for props, pickups, and support effects.

### Anchor Hitbox And Z-Order Assumptions

- Feet anchors drive placement and lane-depth sorting.
- Separate hitboxes should exist for body hurtbox, head or upper body hurtbox, and active attack boxes where needed.
- Firearms should define muzzle origin anchors.
- Pickup objects should sit below actors but above floor debris.

### Visual Readability Constraints

- The player and active partner must stand out from the background through value contrast, outline logic, or controlled rim lighting.
- Enemy attack windups need visible silhouette differences, not only color.
- Heavy hits need readable screen-space feedback through sparks, recoil, and impact displacement.
- Pickup weapons, medicine, and ammo must not share the same color coding.

### Fallback Rule If No Suitable Pack Exists

- Import a free and license-clear web pack into `shared_assets/game_art/` first.
- If imported packs still miss critical frames, use the repository sprite extension pipeline from one approved seed frame per actor family and normalize anchors, scale, and facing before assignment.

## Icon Direction

### Subject

- Taped fist in front of a cracked Old King district street sign with a short shotgun silhouette.

### Silhouette

- Tall fist plus broken sign plate plus short diagonal weapon edge.

### Color Direction

- Asphalt blue
- amber route light
- dirty off-white sign paint
- brass metal
- restrained crimson accent

### Tone

Hard urban action, physical, territorial, and dangerous.

## Audio Direction

### Menu BGM

- Low-fi street tension with distant siren wash, cassette hiss, and restrained percussion.

### Gameplay BGM

- Punchy breakbeat percussion, dirty bass, tense synth stabs, and rhythmic pauses that leave room for impact sounds.

### Boss Or Climax BGM

- Faster drum layers, aggressive bass, metal scrape accents, and district-alarm energy.

### SFX Families

- Thick punch and kick impacts
- metal bat and pipe contact layers
- distinct pistol, SMG, shotgun, and rifle tails
- reload foley
- dodge whoosh
- partner assist call cues
- crowd hit reactions
- control-point capture stamp and siren stingers

## Technical Implementation Notes

### Rendering Style

- Real-time combat should render in a custom `SurfaceView` or equivalent `GameView`.
- HUD, menu, pause, result, shop, and crew screens should use XML and Android View overlays.

### State Model

- Required states should include `MENU`, `DISTRICT_BOARD`, `SAFEHOUSE`, `LOADOUT`, `PLAYING`, `PAUSED`, `RESULT`, and `GAME_OVER`.

### Important Entities

- Player fighter
- partner fighters
- gang enemies
- elite lieutenants
- bosses
- pickup weapons
- item drops
- breakable props
- block control markers

### Special Systems To Plan Early

- combo, hitstop, cancel, stagger, and launch system
- partner AI and assist command system
- district board data and territory ownership
- weapon pickup and reserve loadout system
- lane-depth movement and camera zone rules
- boss armor break and crowd-control balance

## Differentiation Note Against The Current Registry

Crownblock Hunt is closest in spirit to the repository's action projects, but it is materially different from `ashen_strike_trial`, `relic_blade_trials`, and `overtime_street_fighter`. It is broader in map scale, built around district capture instead of one short stage or boss loop, and relies on partner orchestration plus mixed melee and firearms rather than a single-character combat funnel.

Project initialization should begin only after this draft is explicitly confirmed.
