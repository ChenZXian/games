# Stormbeak Siege

Game ID: stormbeak_siege
Direction: landscape slingshot destruction game with strong icon alignment to the playable bird hero
Selected Concept: Stormbeak Siege
Recommended UI Skin: skin_dark_arcade

## Positioning

Stormbeak Siege is a landscape-first physics destruction game built around elemental stormbirds, dramatic fortress collapses, and tactile slingshot control. It should satisfy the immediate appeal of a bird-flinging siege game while clearly differentiating itself through weather-driven stage rules, floating fortress layouts, and stronger hero-skill identity.

## Target Feel And Player Fantasy

The player should feel like the commander of a stormbird assault flock, reading weakpoints in airborne fortresses, timing mid-flight powers for maximum collapse, and smashing enemy core chambers with precision and style. The tone should be bold, adventurous, and satisfying rather than goofy or disposable.

## Core Gameplay Loop

Choose a stage, review its objective and weather rule, pull back the slingshot on the left launch perch, release a selected stormbird, trigger its mid-flight skill at the right moment, collapse support beams and devices, destroy enemies or overload the target core, earn stars and medals, then unlock the next fortress chapter. The core loop is read structure, predict chain reactions, launch a bird with intent, and convert one impact into a full siege cascade.

## Controls And Input Model

Landscape-only presentation.
Tap or drag the current bird to the slingshot pouch.
Drag backward to aim and set force.
Release to fire.
Tap once during flight to trigger the bird's unique skill.
Tap retry for instant stage restart.
Tap bird queue icons to preview the next squad order when allowed.

## Win And Failure Conditions

Win a stage by completing its objective within the allotted bird count.
Primary stage objectives include core destruction, enemy sweep, relay overload, and rescue completion.
Fail when all birds are spent and the stage objective remains unresolved.
Boss stages may also fail if a retaliation device fully charges or a protected convoy target is destroyed.

## Progression Structure

The campaign is split into six chapters: Cliff Nest, Storm Bridge, Iron Cloud Bastion, Crystal Tempest, Ember Draft Citadel, and Skyfall Keep.
Each chapter contains 10 standard stages and 1 boss stage.
Players unlock additional bird classes and chapter utility tools as they advance.
Stars unlock side challenge constellations and harder weather-modifier stages.

## Level Or Run Structure

The main structure is chapter-based progression with handcrafted stages.
Each stage is a self-contained siege sandbox with one major visual theme, one target structure logic, and one weather modifier.
Bonus challenge stages remix prior fortress parts with stricter bird limits or stronger crosswinds.
Boss stages introduce larger multi-core air fortresses with phase-based collapse logic.

## Economy Rewards And Upgrades

Players earn stars, feathers, and storm medals.
Feathers are used for bird skill upgrades such as larger split arcs or stronger chain lightning.
Storm medals unlock optional cosmetics like sling skins, banner styles, and launch impact flares.
The upgrade model must reinforce planning and spectacle without trivializing structural puzzle solving.

## Screen Map

### Menu

Main menu with chapter entry, squad screen, challenge constellation, settings, and icon-rich stage progress.

### Gameplay HUD

Top chapter ribbon with objective text and remaining birds.
Left launch frame containing current bird portrait, next queue, and wind indicator.
Right objective frame containing fortress core health, target checklist, and weather icon.
Bottom command rail containing retry, pause, and utility actions outside the active launch arc.

### Pause

Resume, restart, stage briefing, settings, and exit to chapter map.

### Game Over

Victory or defeat panel with stars earned, structures collapsed, best chain, rewards, and next-stage access.

### Extra Screens

Chapter map.
Bird squad roster and upgrade screen.
Challenge constellation screen.
How-to-play and bird skill glossary.

## UI Direction

### Layout Tone

A dramatic landscape siege console framed like a wind-forged field command deck, with the slingshot perch on the left and the target fortress dominating the right half of the screen.

### HUD Priorities

Remaining birds, current objective, weather modifier, and live core state must be instantly readable.

### Gameplay Safe Area

The slingshot pull arc must remain completely unobstructed on the left.
The projectile flight corridor through the center must stay clear.
The fortress collapse zone on the right must not be covered by decorative HUD chrome.
All permanent UI rails belong outside the active launch-and-impact field.

### Frame And Border Policy

Use outer bronze frames and storm-crystal plates only along the top, bottom, and far edges.
Do not place ornamentation over the slingshot band, airborne travel lane, or fortress weakpoint region.

### Key Overlay Panels

Stage briefing card.
Bird skill glossary panel.
Boss phase warning panel.
Victory and defeat result panel.

### UI Asset Strategy

Use one `skin_dark_arcade` foundation and custom-refine it into a storm siege identity with brass plates, teal lightning meters, wind-carved borders, and landscape-first HUD framing. This should not look like a reactor console, candy board, or card table.

## Gameplay Art Direction

Required art roles include stormbird squad sprites, slingshot rig, fortress materials, enemy crew, weather devices, explosive props, storm relay cores, and collapse effects.
Camera perspective is fixed side-view in landscape.
Primary stormbirds need clear silhouette differences by element and skill role.
Default bird facing is rightward toward the fortress.
Birds must visibly animate idle, pulled-back launch, mid-flight skill, impact, and victory flare states.
Enemy sentries and turret units need idle, alarm, hit, and defeated states.
Fortress materials should clearly differentiate wood, chain metal, crystal supports, and armored cloudstone.
Impact effects must sell force with debris shards, dust plumes, lightning arcs, or ember bursts depending on bird type.
If no suitable shared pack exists, create project-local storm siege art rather than falling back to generic circles or rectangles.

## Icon Direction

### Subject

A fierce stormbird head lunging forward from a slingshot arc with lightning feathers.

### Silhouette

A rounded aggressive bird face with a sharp beak crest and a curved launch trail behind it.

### Color Direction

Storm teal, white lightning, orange beak, and brass highlight.

### Tone

Dynamic, heroic, and immediately tied to the in-game projectile character.

## Audio Direction

### Menu BGM

A light but adventurous sky-war theme with drums, strings, and airy motifs.

### Gameplay BGM

Rhythmic siege music with strong momentum and clear build-up during collapse moments.

### Boss Or Climax BGM

Heavier percussion, stronger brass, and more urgent storm accents.

### SFX Families

Slingshot stretch, launch snap, wing burst, lightning crack, ember flare, crystal shatter, chain collapse, enemy panic, core overload, victory flare, and retry click.

## Technical Implementation Notes

### Rendering Style

Use Android Java with a custom `GameView` for the siege field and physics updates, plus XML overlay panels for menu, pause, results, and chapter UI.

### State Model

Need explicit states for menu, stage briefing, aiming, bird in flight, impact resolution, debris settle, stage result, and chapter progression.
Need deterministic per-stage structure setup with reusable material behavior rules.
Need a lightweight 2D physics approximation focused on sling launch, gravity, debris collapse, and triggered devices rather than a full heavyweight physics engine.

### Important Entities

Stormbird projectile classes.
Slingshot launcher.
Fortress block materials.
Enemy sentries.
Relay cores.
Weather controllers.
Debris fragments.
Chapter progression model.

### Systems To Plan Early

Landscape coordinate system and safe area handling.
Projectile trajectory simulation.
Mid-flight ability activation framework.
Structure stability and collapse propagation.
Stage objective evaluation.
Bird roster upgrade data.
Chapter and star save system.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Genre family is physics destruction puzzle action.
Concrete sub-archetype is a landscape slingshot elemental bird siege with fortress-core targeting.
It is not a reskin of existing registry games because its primary play is ballistic launch and collapse timing across landscape fortress stages, not lane control, board matching, cluster tapping, side-scrolling melee, or card-table strategy.
It also differs from the earlier tactical bird fortress project by centering on storm-element bird classes, floating fortress geometry, weather-driven stage rules, and hero-projectile identity rather than a generic tactical bird barrage setup.

### Map Or Playfield Budget

At least 6 chapter environment families.
At least 10 fortress silhouette families plus 4 boss fortress archetypes.
Every chapter must include multiple material combinations, weather logic, and at least one suspended or floating structure type.
Interactive playfield regions are launch perch, air lane, structure body, core chamber, device pockets, and debris floor.

### Entity Roster Budget

At least 5 player bird classes.
At least 6 enemy or target unit types.
At least 7 structural or device gameplay roles.
At least 4 boss fortress variants.
At least 12 effect families across impact, weather, collapse, and overload events.

### Mechanic Variety Budget

Primary player actions are aim, launch, skill trigger, target selection, and retry.
Stage rule variety includes crosswind, shield cores, chain relays, burning stores, freezing supports, and hostage rescue.
Bird progression includes squad expansion and targeted skill upgrades.
Forbidden template reuse includes making every stage a flat ground castle with only wood crates and one bird behavior.

### Forbidden Template Reuse

Do not reuse portrait puzzle shells.
Do not reuse reactor console framing from `prism_sector_collapse`.
Do not reuse card-table dark felt UI from `pocket_landlord_classic`.
Do not use a generic bird-without-element icon or unrelated emblem icon.

## Visual Identity Contract

### UI Layout Archetype

Landscape siege frame with a left launch perch panel and a right fortress objective stack.

### HUD Composition

Top chapter ribbon, left bird queue, right objective and core frame, bottom command rail.

### Navigation Model

Chapter map to stage briefing to siege play to result and reward flow.

### Playfield Safe Area

Protected zones are the slingshot arc, central flight lane, and target fortress collapse field.

### Frame Overlay Policy

All heavy UI chrome stays outside active launch and impact spaces.

### Palette And Material Language

Storm teal, ember orange, brass gold, slate blue, cloud gray, and white lightning accents over wind-forged bronze and crystal materials.

### Typography Direction

Bold adventure headings with sharp counters and readable action labels.

### UI Pack Strategy

Use `skin_dark_arcade` only as structural base, then push it into a custom storm siege layout with chapter-specific refinement.

### Icon Subject And Silhouette

The icon must feature the playable stormbird and its launch arc, not a generic crest or unrelated object.

### Forbidden Visual Reuse

No portrait board framing, no candy UI, no reactor HUD, no tabletop card motifs, and no generic shieldstar icon motif.

## Differentiation Note

Stormbeak Siege stands apart by combining horizontal slingshot play, elemental bird skills, floating fortress layouts, and weather-modified destruction puzzles. It should feel much closer to a purpose-built landscape storm assault game than to a generic reskin of any prior puzzle or siege project in the repository.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
