# Flame Dragon Onslaught

Game ID: flame_dragon_onslaught
Direction: Inspired by Yan Long action legends with a fiery dragon warrior, side-scrolling combat, flame powers, and boss-focused chapter progression.
Selected Concept: Side-scrolling action stage game where a fire dragon warrior clears enemy waves, builds heat energy, transforms into a half-dragon burst state, and defeats chapter bosses.
Recommended UI Skin: skin_dark_arcade

## Positioning

Flame Dragon Onslaught is a fast, arcade-driven side-scrolling action game for mobile. The player controls a flame dragon warrior who advances through hostile stages, chains melee hits, unleashes directional fire breath, and enters a half-dragon overdrive state to break elite encounters. The game should feel hotter, faster, and more theatrical than a grounded beat-em-up, with strong combat readability and distinct fire-powered momentum.

## Target Feel And Player Fantasy

The player fantasy is becoming a legendary dragon-blood warrior who pushes through enemy battalions with relentless aggression. Combat should feel explosive and heroic, with every combo feeding visible heat, every fire skill reshaping spacing, and every transformation moment acting like a crowd-clearing power spike. The tone is mythic, intense, and flashy rather than gritty or realistic.

## Core Gameplay Loop

Enter a stage, move right through combat arenas, defeat melee and ranged enemies, collect flame shards and energy orbs, build a heat gauge through clean offense, and spend that gauge on fire breath, launch strikes, and half-dragon overdrive. Clear all arena locks, survive hazards, and defeat the stage boss to unlock the next chapter. After each stage, claim medals and invest into permanent upgrades that strengthen combo sustain, survivability, and dragon skills.

## Controls And Input Model

Use a left virtual stick for movement and a right-side four-button cluster for light attack, heavy attack, jump, and flame skill. A separate overdrive button activates the half-dragon state when the heat gauge is full. Tap timing should support short grounded strings, air follow-ups, dodge repositioning, and directional fire breath. Inputs must remain responsive during hitstop and state transitions.

## Win And Failure Conditions

The player wins a stage by defeating all required enemies and the chapter boss. The player fails if health reaches zero or if a boss phase objective is missed during a limited survival segment. Bonus goals include no-revive clears, combo-score thresholds, and fast-clear ranks.

## Progression Structure

The main progression uses six story chapters. Each chapter contains three combat stages and one boss finale. Clearing chapters unlocks deeper enemy variants, new dragon-tech branches, and cosmetic aura colors. Persistent progression centers on a small but meaningful upgrade tree covering vitality, heat gain, fire-skill efficiency, and overdrive duration.

## Level Or Run Structure

The game is fully chapter-based rather than endless. The full content target is 24 stages total. Normal stages mix forward advance with short combat locks and environmental set pieces. Boss stages contain at least two combat phases and one pattern escalation. Optional challenge replays unlock after first completion for higher medals and difficulty modifiers.

## Economy Rewards And Upgrades

The primary currency is ember medals earned from stage completion, bonus ranks, and boss first clears. Secondary shard drops are spent on small chapter upgrades between runs. Upgrade categories are Flame Core, Dragon Body, and Sky Step. Flame Core boosts fire attack performance. Dragon Body improves resilience and overdrive durability. Sky Step improves movement options and combo routes.

## Screen Map

### Menu

Main menu with story start, chapter select, upgrade entry, help, settings, and profile summary.

### Gameplay HUD

Top-left health and heat bars. Top-right stage timer, score, and pause control. Bottom-right combat button cluster. Bottom-left movement stick. A small center-top boss health strip appears only during boss fights. The active combat lane remains visually protected from HUD overlap.

### Pause

Resume, restart, controls reminder, settings, and exit-to-menu actions in a compact overlay.

### Game Over

Failure or victory summary with medals earned, score, boss clear result, and next action buttons.

### Extra Screens

Chapter select, upgrade screen, skill summary, tutorial panels, and boss-intel cards.

## UI Direction

### Layout Tone

Aggressive mythic action presentation with ember glow, forged-metal panels, sharp highlights, and high-contrast combat readouts.

### HUD Priorities

Health, heat gauge, and boss state are first priority. Stage threats and player positioning remain more important than decorative framing. Combo, score, and timer are secondary. Tutorial prompts and skill readiness are tertiary.

### Gameplay Safe Area

The center band of the landscape playfield must remain free for movement, juggling, projectiles, and boss telegraphs. Top HUD stays shallow. Control buttons stay in lower corners only. No dialog card may cover the player or the current enemy cluster during live combat.

### Frame And Border Policy

No ornamental panel edge may intrude into the active platform lane. Decorative dragon-metal framing belongs in reserved outer gutters and overlay screens only.

### Key Overlay Panels

Pause dialog, chapter card, upgrade panel, tutorial prompt, boss warning banner, and result card.

### UI Asset Strategy

Use skin_dark_arcade as the structural base, then route the downstream UI workflow toward a molten-forge action style with red-orange heat accents, steel-black panels, angular glyphs, and ember-lit skill buttons. Avoid neon sci-fi grids, cute rounded badges, or tactical strategy HUD patterns from other projects.

## Gameplay Art Direction

### Required Art Roles

Player warrior, half-dragon overdrive form, melee soldiers, shield soldiers, archers, flame cultists, flying drakes, elite captains, chapter bosses, weapon trails, fire breath effects, explosion bursts, ground hazards, stage props, layered backgrounds, pickups, and chapter transition banners.

### Camera Perspective

Side-scrolling action view with a single combat lane plus controlled jump arcs and layered parallax backgrounds.

### Suggested Shared Pack Family Or Source Tier

Prefer a style-matched fantasy action character pack plus a fire-focused effect pack in the shared gameplay-art library. If the shared library cannot satisfy both combat readability and required animation states, import or generate a license-clear pack with humanoid action poses and flame effects before final delivery.

### Default Facing And Facing Behavior For Moving Or Attacking Entities

The player and enemy fighters must face movement or target direction and flip cleanly without ambiguous silhouettes. Fire breath, launch attacks, and boss lunges must visibly commit direction before impact.

### Minimum Visual States And Animation Expectations

The player needs idle, run, jump, land, light attack chain, heavy attack, fire breath, hit, knockdown, recover, overdrive transform, and victory states. Standard enemies need idle, move, attack, hit, and defeat states. Bosses need at least idle, move, windup, action, hit, stagger, and phase-shift states.

### Animation Quality Tier For Primary Entities

Medium to high. The player and bosses should not rely on one static sprite. Movement must alternate poses, attacks need windup and recovery, and overdrive should feel visually distinct from the base form.

### Anchor, Hitbox, And Z-Order Assumptions For Primary Entities

Primary characters use ground-line foot anchors, torso-centered hurtboxes, directional attack boxes, and foreground priority over small props. Bosses require separate body and weapon hit regions. Fire effects may render above actors only during attack windows.

### Visual Readability Constraints

Combat silhouettes must stay readable against bright fire backgrounds. Player flame effects cannot fully hide enemy telegraphs. Hit flashes and heat gauge feedback should be strong without drowning out active hazards.

### Fallback Rule If No Suitable Pack Exists

If no suitable shared or imported pack exists, the gameplay-art workflow must generate or assemble a project-specific action pack with tracked provenance. Placeholder rectangles or static silhouettes are not acceptable for delivery-ready output.

## Icon Direction

### Subject

A roaring half-dragon warrior head with a blazing horn crest.

### Silhouette

Forward-leaning dragon-warrior face with one raised flaming horn and a wide open jaw flame arc.

### Color Direction

Hot orange, crimson, black steel, and gold ember highlights.

### Tone

Explosive, heroic, and boss-rush focused.

## Audio Direction

### Menu BGM

A dramatic fire-temple theme with restrained percussion and heroic anticipation.

### Gameplay BGM

Fast action rhythm with heavy drums, molten synth-brass, and rising pressure during arena locks.

### Boss Or Climax BGM

A more intense arrangement with stronger percussion, dramatic accents, and transformation payoff energy.

### SFX Families

Sword swipes, flame burst layers, breath ignition, jump and land hits, enemy defeat pops, boss slam impacts, medal reward chimes, and overdrive activation stingers.

## Technical Implementation Notes

### Rendering Style

Use a custom GameView or SurfaceView for the combat lane, actors, hit effects, and background scrolling. Menus and HUD should use Android View and XML overlays around a reserved gameplay viewport.

### State Model

Core states are MENU, CHAPTER_SELECT, PLAYING, PAUSED, GAME_OVER, and UPGRADES. Combat state needs actor states, hitstop windows, combo count, heat gauge, overdrive timer, enemy wave controllers, boss phase state, and reward resolution.

### Important Entities

PlayerActor, EnemyActor, BossActor, Projectile, HitEffect, Pickup, HazardZone, StageTrigger, ComboTracker, and HeatGauge.

### Systems To Plan Early

Combat state machine, enemy wave scripting, boss phase scripting, invulnerability windows, hit detection, overdrive transformation swap, platform and arena boundaries, and stage reward persistence.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Side-scrolling action brawler with combo pressure, fire-resource skills, and stage-boss escalation. This is not a generic endless runner, not a tactics game, and not a plain melee stage clear loop without transformation.

### Why It Is Not A Reskin Of An Existing Game

The project differs from existing side-action entries by making heat management and half-dragon overdrive the core combat signature. It also requires six full chapters, dedicated boss finale stages, directional fire breath, and more overt mythic flame identity than the repo's grounded melee projects.

### Map Or Playfield Budget

The campaign requires 24 stages across six themes such as burning fortress, volcanic pass, ruined shrine, dragon forge, ash forest, and sky citadel. The stage set must include at least six hazard families including falling magma, flame vents, collapsing ledges, spike cages, rolling siege fireballs, and temporary inferno zones.

### Entity Roster Budget

Minimum roster includes one player base form, one overdrive form, six standard enemy families, three elite enemy families, six bosses, five pickup types, and six effect families.

### Mechanic Variety Budget

Required mechanics include light and heavy combo chains, jump combat, fire breath spacing, dodge repositioning, overdrive transformation, arena locks, environmental hazards, and multi-phase boss scripting. At least six boss gimmick patterns are required across the full campaign.

### Forbidden Template Reuse

Do not collapse the design into a one-button auto-combat loop. Do not reduce overdrive into a small damage buff with no visual distinction. Do not reuse a generic top HUD plus bottom menu strip from other projects. Do not ship static, non-animated combat actors as delivery-ready output.

## Visual Identity Contract

### UI Layout Archetype

Top combat ribbon with lower-corner controls and centered boss alert overlays.

### HUD Composition

Health and heat at upper left, score and time at upper right, contextual boss strip at upper center, movement at lower left, action buttons at lower right.

### Navigation Model

Menu to chapter map to stage run, with separate upgrades and boss-intel flow.

### Playfield Safe Area

The center 65 percent of the landscape gameplay viewport is protected for action movement and boss telegraphs. Upper corners and lower corners are reserved for HUD and controls only.

### Frame Overlay Policy

Decorative forge-metal frames must stay outside the reserved combat lane and may not cover platforms, enemies, projectiles, or dodge routes.

### Palette And Material Language

Black steel, ember orange, molten gold, deep red, and sharp cyan-white highlight sparks.

### Typography Direction

Bold arcade action lettering with tall condensed labels and sharp combat numerals.

### UI Pack Strategy

Use skin_dark_arcade tokens, then refine through an action-forge overlay language rather than fantasy parchment or neon tech. Prefer a shared or imported pack that supports molten action accents and strong skill-button readability.

### Icon Subject And Silhouette

A roaring half-dragon warrior head with a blazing horn crest and flame arc.

### Forbidden Visual Reuse

Do not reuse city-map strategy iconography, tactical dashboards, or cute cartoon-light buttons. Do not reuse the same dark-neon combat HUD pattern from prior action entries without strong forge-specific differentiation.

## Differentiation Note

Flame Dragon Onslaught should read as a fire-myth action showcase built around heat, transformation, and stage-boss escalation. Its identity depends on aggressive combat flow, flame-resource pacing, and a loud dragon-warrior presentation rather than on generic side-scrolling melee.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
