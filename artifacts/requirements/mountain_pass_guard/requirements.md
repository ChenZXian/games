# Mountain Pass Guard

Game ID: mountain_pass_guard
Direction: military war flag mountain pass defense
Selected Concept: Mountain Pass Guard - defend a forked mountain pass with hidden garrisons, reveal ambush units at the right moment, and stop enemy strike groups from seizing the command flag bunker.
Recommended UI Skin: skin_military_tech

## Game Positioning

Mountain Pass Guard is a compact Android Java defense strategy game built around hidden deployment, choke-point control, and mountain terrain pressure. The player commands a border detachment defending a command flag bunker inside a narrow pass where timing reveals, terrain tools, and reserve management matter more than building a large tower grid.

## Target Feel And Player Fantasy

The game should feel like commanding a tense frontline holdout where every ridge, bunker, and demolition charge matters. The player fantasy is being the pass commander who lays traps, hides elite squads in rock cover, reads enemy approach patterns, and wins by triggering disciplined counteractions instead of brute-force unit spam.

## Core Gameplay Loop

Each mission begins with a mountain pass map containing one command bunker, two to four entry routes, hidden deployment slots, barricade points, and support nodes. During a short preparation phase, the player assigns concealed defenders, chooses a limited support loadout, and upgrades fixed positions. Once waves begin, the player reveals ambush squads, rotates reserves, triggers landslides or bridge demolitions, calls flare reconnaissance, and preserves command bunker durability until all enemy waves are defeated.

## Controls And Input Model

Primary input is tap-based with drag support for fast reassignment. Tap a hidden slot to assign or reveal a squad, tap a route marker to set rally focus, drag reserve squads between bunker sectors, tap support buttons for flare scan, demolition trigger, suppressive barrage, and emergency retreat, and tap paused enemies or squads to inspect their role cards. The control model is designed for landscape single-thumb-plus-tap play, with the command bar anchored outside the protected gameplay pass.

## Win And Failure Conditions

The player wins by surviving all scripted assault waves while keeping the command flag bunker intact. The player loses when bunker durability reaches zero, when a special infiltrator unit steals the command flag and escapes through an extraction edge, or when the defense morale meter collapses after too many unrecovered breaches. Bonus stars depend on bunker durability, number of unrevealed ambushes preserved, and successful route-denial actions.

## Progression Structure

Progression is campaign-based across mountain border sectors. Early stages teach hidden rifle squads, flare scouting, and barricade timing. Mid-campaign adds armored trucks, mortar teams, fake feint routes, and destructible bridges. Late chapters introduce night operations, split-route boss assaults, elite saboteurs, and weather modifiers such as fog or snow gusts. Meta progression stays light and practical: new squad classes, extra support choices, and additional pre-battle slot capacity.

## Level Or Run Structure

A standard mission lasts 6 to 9 waves with a preparation phase before wave one and short regroup windows after every second wave. Each map must include at least one primary route and one alternative flank or cliff path so route reading remains meaningful. Chapter finales use multi-phase assault commanders with escorts and decoy pushes. Optional challenge missions can remix the same map shell with night visibility limits, strict support caps, or convoy protection side goals.

## Economy, Rewards, Drops, Or Upgrade Model

Supply points are the main combat currency. The player earns supply points from wave starts, enemy eliminations, survival bonuses, recovered cargo crates, and successful capture of temporary supply mules. Supplies are spent on pre-wave upgrades, barricade repairs, emergency reveals, and one-use support actions. Core upgrade paths are Hidden Infantry, Machine Gun Nest, Sniper Ridge, Combat Engineers, Mortar Pit, and Signal Post. Each path should have three meaningful tiers with tactical identity rather than flat stat inflation.

## Gameplay Diversity And Content Budget

Genre family and sub-archetype: defense strategy with hidden-garrison choke-point warfare rather than open-lane tower defense.

Why it is not a reskin of an existing game: the game does not use a bottom tower strip, a build-anywhere maze, or a single static base race. Its identity comes from concealed defenders, route denial, morale pressure, infiltration loss conditions, and forked mountain pass maps with terrain-trigger actions.

Map or playfield model: angled top-down canyon battlefield with one central bunker zone, elevated ridges, narrow roads, tunnel mouths, bridge spans, and fallback barricade rings.

Minimum map regions, routes, lanes, zones, rooms, or screens: one bunker core zone, two to four active assault routes, at least two elevated firing positions, one reserve transfer corridor, one destructible terrain element, and one extraction edge for infiltrator threats.

Terrain, obstacle, landmark, and functional map element variety: cliff walls, ridge nests, sandbag barricades, timber bridge, rockslide trigger, tunnel entrance, signal tower, ammo cache, and medical post.

Player, enemy, neutral, item, projectile, and effect roster targets: six player defense roles, eight enemy archetypes, two neutral support entities, six projectile families, and eight combat or terrain effect families.

Mechanic variety and progression targets: hidden deployment, reveal timing, route focus orders, temporary retreat, recon scan, demolition trigger, support cooldown management, morale recovery between waves, and chapter modifiers such as fog or snow.

Forbidden template reuse: no three-horizontal-lane layout, no bottom-only summon strip, no generic endless wave loop without regroup windows, no fixed left-base-right-enemy march structure, and no reuse of another defense game's exact HUD, tower lineup, or chapter rhythm.

## Visual Identity Contract

UI layout archetype: tactical field dossier with a left stacked status rail, top route-pressure strip, and right-side collapsible command tray.

HUD composition: bunker durability and morale on the left rail, wave and threat forecast across the top, support skills on the right, and squad detail cards rising from the lower right without covering route junctions.

Navigation model: menu to sector map to mission briefing to gameplay, with post-stage debrief cards summarizing revealed ambush efficiency and breach history.

Playfield safe area: the entire mountain pass center and route junctions remain fully clear of decorative chrome; HUD only touches reserved left, top, and far-right margins.

Frame overlay policy: tactical framing may appear only on dead-space rails outside the canyon playfield and must never sit on active road tiles, ridge nests, or reveal slots.

Palette signature: deep forest black-green, desaturated steel, radar cyan, warning amber, and bunker red for breach emergencies.

Material language: matte command plastics, thin radar glass, military stencil plates, and restrained illuminated indicators rather than fantasy ornament or glossy arcade chrome.

Typography style: compact uppercase labels for tactical data, wider numeric readouts for durability and wave timing, and clean briefing text with strong contrast.

UI pack strategy: start from the repository UI Kit with skin_military_tech tokens, then refine through the UI workflow using a tactical instrumentation pack or custom XML treatments that emphasize route readouts and dossier cards.

Icon subject: a bunker-mounted command flag behind crossed barricades and a spotlight flare over a mountain notch.

Icon silhouette and composition: triangular mountain notch framing a square bunker door, a bold flag pole at center, and one diagonal flare trail for instant recognition.

Forbidden visual reuse: no top pill HUD with bottom command strip, no fantasy castle icon, no rail-cart silhouette, no ancient wall motif, and no copied defense roster badges from existing military or tower projects.

## Screen Map

Menu: title, campaign continue, sector select, challenge missions, help, sound toggle.
Gameplay HUD: bunker durability, morale, current wave, next route forecast, supply points, reserve count, support skill tray, pause button.
Pause: resume, restart mission, sector map, help, sound toggle.
Game Over: mission failed or bunker lost, waves survived, routes breached, ambush efficiency, retry, back to sector map.
Extra Screens: sector map, mission briefing, squad unlock panel, support loadout panel, help glossary, stage clear debrief.

## UI Direction

Recommended ui_skin: skin_military_tech.

Layout tone: disciplined tactical dashboard with dossier cards and route status instruments rather than a toy-like defense board.

HUD priorities: bunker durability first, then morale and route threat, then supply points and support cooldowns, then selected squad details.

Playfield safe area: reserve the left margin for the durability rail, top band for wave forecast, and right margin for support controls; the pass center, route forks, bunker approach, and ridge slots remain protected gameplay zones.

Frame and border policy: any frame treatment must live on the outer rails only, with no decorative corners intruding into road or bunker interaction spaces.

Key overlay panels: selected squad card, mission briefing sheet, support loadout panel, post-wave summary card, and result debrief panel.

UI asset strategy: use one tactical UI direction only, based on skin_military_tech plus repository-compliant XML drawables first; if the shared UI library has a suitable military instrumentation pack, use that instead of generic placeholder panels.

## Gameplay Art Direction

Required art roles: command bunker, flag mast, rifle squad, machine gun team, sniper pair, engineer squad, mortar pit crew, signal operator, enemy scouts, rifle raiders, shield truck, mortar crew, saboteur infiltrator, elite commander, supply mule, flare projectile, mortar shell, tracer bursts, smoke, rockslide debris, bridge collapse effect, bunker hit effect, snow or fog overlays, canyon background set, and route markers.

Camera perspective: slight angled top-down battlefield with readable elevation, narrow pass lanes, and clear route junctions.

Suggested shared pack family or source tier: military or frontier warfare packs with clear squad silhouettes and environment props, sourced from the shared game art library first, then a license-clear free pack import if the library is weak.

Default facing and facing behavior for moving or attacking entities: all squads and enemy units must face movement or attack direction; infantry use left-right and forward-diagonal facings where practical, trucks rotate toward route direction, mortar crews pivot toward target lane, and infiltrators visibly turn during retreat or escape.

Minimum visual states and animation expectations: idle alert, move, attack, hit, retreat, and defeated for primary infantry; move, fire, damaged, and destroyed for vehicles; armed, triggered, and cooldown for interactive defenses and terrain devices.

Animation quality tier for primary entities: mid-tier sprite animation with alternating walk or run poses for moving squads and visible windup plus recovery timing for attack-capable primary units.

Anchor, hitbox, and z-order assumptions for primary entities: infantry anchor near feet center, vehicles slightly rear-biased, bunker and cliffs below combat overlays, projectiles above units, and smoke or flare effects above projectile arcs but below HUD.

Visual readability constraints: enemy silhouettes must separate infiltrators, heavy assault troops, and mortar crews immediately; reveal slots and barricade points must remain readable over terrain; bunker damage state should be obvious at a glance.

Fallback rule if no suitable pack exists: import a new license-clear military pack into shared_assets/game_art/ with provenance, then produce project-local refinements only where the shared pack lacks required states.

## Icon Direction

Subject: mountain bunker with a command flag and emergency flare.
Silhouette: peaked canyon cutout around a squat bunker block and centered flag pole.
Color direction: dark olive, cool steel, warning amber flare, and a sharp red flag accent.
Tone: cartoon-styled but disciplined, defensive, and high-stakes.

## BGM Direction

Menu music mood: restrained command-room tension with low percussion, soft radio pulses, and distant brass.
Gameplay loop mood: steady war-drum pulse with urgent tactical rhythm, climbing slightly when route pressure rises.
Boss or climax mood if applicable: heavier percussion, alarm pulses, and forceful brass hits for multi-route assault finales.

## Technical Implementation Notes

Rendering style: custom GameView or SurfaceView for battlefield simulation, squad animation, terrain events, and projectile logic, with Android View or XML overlays for HUD and non-real-time screens.

State model: MENU, BRIEFING, LOADOUT, PLAYING, PAUSED, STAGE_CLEAR, GAME_OVER.

Important entities: MissionConfig, RouteLane, RevealSlot, DefenseSquad, EnemyWaveGroup, SupportSkill, BunkerState, MoraleSystem, TerrainTrigger, SupplyDrop, EffectActor, SaveState.

Special systems to plan early: hidden-slot assignment, reveal timing rules, enemy route decision tables, morale pressure and recovery, destructible terrain triggers, support cooldown pipeline, multi-route wave scripting, squad facing rules, animation state timing, and persistence for unlocked squads and challenge medals.

## Differentiation Note Against The Current Registry

The current registry already includes multiple defense games such as Castle Keep Defender, Royal Line Defense, Garden Siege, Minecart Bastion, Empire Wall Guard, and Crownroad Bastion. Mountain Pass Guard is differentiated by its hidden-garrison reveal loop, bunker morale pressure, infiltrator theft loss condition, multi-entry canyon route denial, and field-dossier tactical presentation instead of a standard tower-strip defense layout.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
