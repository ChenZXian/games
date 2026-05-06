# Ashen Frontier

Game ID: ashen_frontier
Direction: Post-apocalypse large-map survival against zombie hordes with a full weapon system and safehouse expansion.
Selected Concept: Ashen Frontier
Recommended UI Skin: skin_post_apocalypse

## Positioning

Ashen Frontier is a top-down post-apocalypse survival action game built around large regional exploration instead of a small arena. The player rides between ruined districts, clears infected hot zones, scavenges scarce resources, rescues survivors, and returns to a growing safehouse network. The core identity is long-route survival under pressure, with a readable weapon system and a large connected world map that feels materially bigger than a wave room shooter.

## Target Feel And Player Fantasy

The target feel is tense, dusty, dangerous, and earned. The player fantasy is being a hard-to-kill frontier scavenger who knows when to push deeper into dead territory, when to burn ammunition, when to switch weapons, and when to abort and race home with a full bike rack and one more rescued civilian. Combat should feel decisive but costly, and the world should reward route knowledge, preparation, and weapon mastery more than twitch speed alone.

## Core Gameplay Loop

1. Review the regional map, threat heat, fuel, ammo, and survivor requests at the safehouse.
2. Choose one destination region and load a primary weapon, a sidearm, one throwable slot, one healing slot, and one utility slot.
3. Ride into the chosen field region, explore roads, ruins, checkpoints, gas stations, depots, and landmarks while collecting scrap, fuel, medicine, ammo, and upgrade parts.
4. Fight or evade zombie groups using weapon range, reload timing, stagger windows, choke points, and terrain.
5. Secure temporary extraction points, escort survivors, or loot a high-risk landmark objective.
6. Return to a safehouse before bike durability, fuel, medicine, or ammo collapses.
7. Spend resources on weapon upgrades, safehouse modules, map intel, and survivor roles that unlock deeper runs.

## Controls And Input Model

- Left virtual stick moves the character in 8 directions.
- Right-side gesture aim pad handles free aim, snap-turn, and sustained firing.
- Tap buttons trigger reload, melee shove, dodge roll, throwable, heal, and interact.
- A dedicated bike mount button switches between on-foot exploration and fast regional traversal in allowed road sections.
- Weapon swap uses two quick weapon buttons plus a hold radial for special gear.
- Map view supports pinch zoom and tap-to-place route markers before entering a region.

## Win And Failure Conditions

### Win Conditions

- Complete the chapter objective chain for all 12 frontier regions and secure the final transmission relay.
- Fully connect the safehouse network across the frontier map.
- Survive the final siege night after restoring long-range power.

### Failure Conditions

- Player health reaches zero in the field without a revive kit.
- Infection meter reaches the terminal threshold during a run.
- Escort target dies during a mandatory survivor extraction.
- The safehouse reactor is starved of fuel during late-stage siege events.

## Progression Structure

- The macro progression spans 12 named frontier regions grouped into 4 belts: outskirts, industrial scar, burnt suburbs, and dead core.
- Each region unlocks new route hazards, zombie families, landmark objectives, and material drops.
- Safehouse progression unlocks workshop, infirmary, watchtower, fuel still, radio room, armory, and survivor barracks.
- Player growth comes from weapon mastery ranks, passive scavenger perks, bike modules, and survivor specialist bonuses.
- Story progression is light and environmental, delivered through radio chatter, survivor requests, and recovered route logs.

## Level Or Run Structure

- The game uses a large macro map with 12 regions.
- Each region contains 3 to 5 field subzones connected by roads, alleys, broken bridges, drainage cuts, or service tunnels.
- A field run begins at a safehouse gate or field relay, then flows through open exploration, hotspot combat, and one major landmark objective.
- Some regions support multiple entry points, letting the player choose stealthier but longer routes or direct high-threat routes.
- Night pressure, weather shifts, and roaming horde migration vary the route between repeat visits.
- A completed run should take 8 to 18 minutes depending on objective depth.

## Economy Rewards And Upgrades

- Core resources: scrap, weapon parts, fuel, medicine, cloth, batteries, relay chips, and canned food.
- Temporary run loot includes ammo types, throwables, armor plates, and field attachments.
- Weapon progression supports quality tiers, attachment slots, and mastery unlocks rather than random stat inflation.
- Bike progression supports fuel tank size, cargo rack, shock frame, noise dampening, and headlight upgrades.
- Safehouse upgrades unlock new vendor stock, higher survivor capacity, field crafting, and secondary extraction routes.
- Optional bounty contracts reward aggressive zombie clearing while civilian rescue chains reward cautious route play.

## Screen Map

### Menu

- Main title screen with weathered radio mast backdrop.
- Continue, new run, loadout, records, settings, and help.

### Gameplay HUD

- Left vertical survival rig for health, stamina, infection, and fuel.
- Top-center slim route compass with objective distance and horde warning pulses.
- Right vertical weapon tower for primary, sidearm, reload, and throwable slots.
- Bottom-center compact interaction strip for bike mount, heal, shove, and contextual actions.

### Pause

- Large folded field map overlay with mission log, survivor roster summary, and audio toggles.

### Game Over

- Cause-of-death recap, region reached, loot recovered, survivors saved, and retry options.

### Extra Screens

- Safehouse management screen.
- Weapon bench and attachment tuning screen.
- Survivor assignment screen.
- Regional world map screen.
- Trader and request board screen.

## UI Direction

### Layout Tone

The interface should feel like a field survival kit mounted over the screen instead of floating arcade pills. It should read as scavenged metal plates, fabric tags, scratched acrylic windows, and hazard-marked mechanical locks.

### HUD Priorities

- Health, ammo, and infection are always visible.
- Fuel and bike status surface when mounted or far from extraction.
- Objective distance and horde pressure stay near the top compass.
- Noise level and stealth visibility appear only in alert-sensitive zones.

### Key Overlay Panels

- Fold-out region map panel.
- Bench-style weapon comparison drawer.
- Survivor dispatch board with role tags.
- Contract board for rescue, supply, and purge tasks.

### UI Asset Strategy

- Primary skin is `skin_post_apocalypse`.
- Base UI structure comes from the repository UI Kit token system.
- The UI workflow should pair the skin with a rugged industrial pack staged under `shared_assets/ui/` if the shared library has one that matches dusty metal, warning paint, and analog instrument motifs.
- Do not reuse the common top-pill plus bottom-strip composition seen in generic mobile shooters.

## Gameplay Art Direction

### Required Art Roles

- Player scavenger with on-foot and bike-mounted silhouettes.
- Civilian survivor variants.
- Standard infected, runner infected, bloater infected, crawler infected, armored infected, and elite howler infected.
- Highway debris, gas pumps, barricades, wrecks, safehouse gates, relay towers, and infected nests.
- Projectiles, muzzle flashes, blood mist, dust kick-up, shell ejection, explosion, and fire effects.

### Camera Perspective

- 3/4 top-down perspective with readable road geometry and directional cover.

### Suggested Shared Pack Family Or Source Tier

- Primary target is a gritty top-down post-apocalypse pack from `shared_assets/game_art/`.
- Secondary target is a license-clear modern ruins or industrial wasteland pack for props and terrain.
- If the shared library is weak, import a CC0 or clearly licensed zombie-survival pack before resorting to project-local placeholder art.

### Default Facing And Facing Behavior

- The player and all main zombie types must support 8-direction facing selection or mirrored directional sprites.
- Aim direction should control torso or full-body facing during firing.
- Bike travel uses forward-facing travel poses with left and right lean variants.

### Minimum Visual States And Animation Expectations

- Player: idle, walk, run, fire, reload, shove, dodge, hurt, downed, bike mount, bike ride.
- Common infected: idle, shamble, sprint, lunge, hurt, knockback, death.
- Elite infected: idle, patrol, attack windup, attack action, stagger, death.
- Survivors: idle, run, panic, interact.

### Animation Quality Tier For Primary Entities

- Medium for most props and effects.
- Medium-high for player, common infected, runner infected, and elite infected.

### Anchor Hitbox And Z-Order Assumptions

- Feet anchors define movement and sorting.
- Weapon muzzle origins should be exposed per weapon family.
- Zombie hitboxes should separate body center and head weak-point zones.
- Vehicles and barricades should own larger low-body collision boxes than their visible silhouettes.

### Visual Readability Constraints

- Player silhouette must separate from terrain through rim value contrast, not only color.
- Hazard pools, gas clouds, and explosive props need distinct outline logic.
- Ammo pickups and medicine pickups need different color temperature and icon shape.

### Fallback Rule If No Suitable Pack Exists

- Use a repository-approved sprite generation pipeline from one chosen seed frame per major actor family and normalize anchors, facing, and scale before assignment.

## Weapon System Direction

### Weapon Families

- Sidearms: semi-auto pistol, heavy revolver.
- Primaries: pump shotgun, compact SMG, burst rifle, lever rifle.
- Special slot unlocks later: crossbow, homemade flamethrower, improvised grenade launcher.
- Throwables: molotov, pipe bomb, noise lure, flash flare.

### Weapon Roles

- Pistol is the low-cost fallback with stable accuracy.
- Revolver is high-stagger and elite-finishing.
- Shotgun dominates close corridor clear.
- SMG manages mobile crowd trimming.
- Burst rifle handles general mid-range control.
- Lever rifle rewards precision and weak-point play.
- Crossbow supports quiet infiltration and ammo recovery.
- Flamethrower is area denial with fuel tension.

### Weapon Progression

- Each weapon family gains mastery through kills, critical hits, rescue completions, and landmark clears.
- Upgrades include magazine size, reload speed, recoil stability, pellet spread tuning, weak-point bonus, fire ignition chance, and noise reduction.
- Attachments are practical and limited: sights, stocks, extended mags, barrel mods, sling hooks, and underbarrel tools when appropriate.

### Combat Rules

- Reload timing matters and can be interrupted by melee shoves or dodge rolls.
- Weapon noise attracts nearby infected and can trigger roaming hordes.
- Headshots, fire, and explosive chain reactions create different crowd-control patterns.
- Ammo economy must make weapon choice meaningful instead of letting one gun solve every encounter.

## Icon Direction

### Subject

- A masked scavenger helmet with cracked visor and a rifle silhouette crossing a route marker.

### Silhouette

- Strong triangular helmet mass over a diagonal long-gun line and one glowing warning eye.

### Color Direction

- Dusty amber, rust orange, charcoal black, and one acid-green contamination accent.

### Tone

- Dangerous, survivalist, readable at small size, and clearly tied to zombie frontier combat rather than generic shooter branding.

## Audio Direction

### Menu BGM

- Sparse radio-static guitar drone with low percussion and distant wind.

### Gameplay BGM

- Low industrial pulse, dry percussion, and tension swells that intensify when horde pressure rises.

### Boss Or Climax BGM

- Aggressive metallic rhythm with alarm pulses and distorted bass.

### SFX Families

- Distinct gun tails by weapon family.
- Punchy zombie impact and stagger layers.
- Bike engine, idle rattle, and skid effects.
- Radio chirps, loot rustle, and safehouse machinery loops.

## Technical Implementation Notes

### Rendering Style

- Android Java `SurfaceView` gameplay renderer with region-sized tile chunks and pooled sprite actors.
- XML or Android View overlays for HUD, map, loadout, and safehouse screens.
- Landscape orientation is preferred for route readability.

### State Model

- `MENU`
- `WORLD_MAP`
- `LOADOUT`
- `SAFEHOUSE`
- `PLAYING`
- `PAUSED`
- `GAME_OVER`

### Important Entities

- Player scavenger.
- Bike mount.
- Zombie actor families.
- Survivor escorts.
- Loot containers.
- Explosive props.
- Safehouse modules.
- Route beacons and extraction markers.

### Systems To Plan Early

- Weapon data definitions and ammo classes.
- Zone streaming and region transition rules.
- Zombie perception, sound attraction, and migration groups.
- Bike mount and dismount transitions.
- Survivor escort behaviors.
- Save model for region completion, safehouse upgrades, and mastery progress.

## Differentiation Note

Ashen Frontier is not a reskin of the repository's existing survival shooters. It replaces small arena survival with a large regional exploration structure, introduces bike-supported traversal, ties combat to an explicit multi-family weapon system, uses persistent safehouse growth, and makes route planning and extraction pressure equal to shooting skill.

## Gameplay Diversity And Content Budget

- Genre family and concrete sub-archetype:
  top-down survival action scavenger with regional extraction structure.
- Why it is not a reskin of an existing game:
  the core decision is route depth versus extraction safety across a macro frontier map, not stationary defense or arena wave endurance.
- Map or playfield model:
  12 macro regions, each with 3 to 5 connected field subzones and one signature landmark objective.
- Minimum map regions routes lanes zones rooms or screens:
  12 macro regions, at least 40 field subzones, at least 18 major landmark encounters, and 5 safehouse or relay hubs.
- Minimum terrain obstacle landmark and functional map element variety:
  highways, suburbs, refinery yards, drainage cuts, overpasses, trailer camps, burned forests, depots, hospitals, and relay towers.
- Player enemy neutral item projectile and effect roster targets:
  1 player, 1 bike mount mode, 6 zombie families, 4 survivor roles, 8 loot node types, 7 weapon families, 4 throwable families, and 10 major VFX families.
- Mechanic variety and progression targets:
  stealth approach, noisy assault, rescue escort, timed extraction, relay activation, nest purge, roadblock breaching, and safehouse defense events.
- Forbidden template reuse:
  no fixed small arena loop, no three-lane defense board, no endless same-room waves, no generic bottom-only weapon bar as the main identity.

## Visual Identity Contract

- UI layout archetype:
  left survival rig plus top route compass plus right weapon tower plus bottom utility strip.
- HUD composition:
  asymmetrical field-kit arrangement with stacked gauges and a tall weapon column.
- Navigation model:
  hub-to-region map routing with fold-out map overlays and bench-style upgrade drawers.
- Palette signature:
  ash beige, rust orange, soot black, oxidized brown, and controlled hazard amber.
- Material language:
  worn metal, scratched acrylic, fabric tags, field tape, and hazard paint.
- Typography style:
  condensed military sans for labels paired with wider distressed numerals for gauges.
- UI pack strategy:
  `skin_post_apocalypse` plus one rugged industrial support pack if the shared library has a suitable candidate.
- Icon subject:
  masked scavenger helmet and rifle crossing a route marker.
- Icon silhouette and composition:
  helmet dominant shape with one diagonal gun line and contaminated glow accent.
- Icon palette and background:
  ember orange foreground over dark charcoal badge with ash dust texture.
- Forbidden visual reuse:
  do not reuse the common rounded top-pill HUD, generic blue sci-fi palette, or generic crate or tower icon concepts from other projects.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
