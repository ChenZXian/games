# Castaway Clan

Game ID: castaway_clan
Direction: Robinson Crusoe style island building survival with group camp management
Selected Concept: Castaway Clan
Recommended UI Skin: skin_cartoon_light

## Positioning

Castaway Clan is a top-down island survival and camp-building mini-game focused on turning one shipwreck survivor into the leader of a small stranded community. The game combines direct exploration, resource gathering, task assignment, and camp expansion in short mobile-friendly sessions. The intended identity is a bright but hazardous castaway adventure rather than a grim zombie or post-war survival game.

## Target Feel And Player Fantasy

The player should feel like a practical island leader who survives by organizing people, not only by personally farming every resource. The tone should be adventurous, readable, and resilient. The fantasy is to rescue scattered survivors, assign meaningful roles, stabilize daily needs, and watch a fragile beach camp evolve into a functioning coastal colony.

## Core Gameplay Loop

1. Leave camp and explore nearby island regions for wood, stone, fiber, fruit, fish, herbs, and stranded cargo.
2. Rescue or recruit survivors from events, wreck sites, and inland hazards.
3. Return to camp and assign survivors to gathering, cooking, building, guarding, healing, or fishing roles.
4. Construct and upgrade shelters, storage, workshops, water systems, cooking stations, fences, watch posts, and recovery structures.
5. Prepare for daily risk spikes such as storms, hunger drains, illness, morale drops, and animal raids.
6. Complete chapter objectives that unlock new sectors, technologies, and colony capacity.

## Controls And Input Model

- Top-down touch movement using a virtual joystick on the left side
- Context action button for gather, rescue, fight, inspect, and interact
- Quick tool button for axe, spear, hammer, or torch use
- Tap-select camp structures and survivors in management mode
- Tap task cards to assign or reassign survivor jobs
- Drag-and-place building ghosts inside valid camp zones
- Pause button and time-rate toggle for normal or fast-forward camp simulation outside danger alerts

## Win And Failure Conditions

Win conditions:
- Complete the final rescue signal objective by building a long-range beacon and sustaining the colony through the final storm night
- Reach all required colony milestones across food, water, shelter, morale, and signal infrastructure

Failure conditions:
- The player character collapses while no active healer or stocked recovery bed exists
- Colony population drops below the minimum viable count after critical losses
- Food and water remain at zero long enough to trigger irreversible camp abandonment
- The main camp fire and shelter core are destroyed during a severe storm or raid event

## Progression Structure

Progression is chapter-based with colony expansion gates.

- Chapter 1: First Shelter
  - establish fire, bedroll, crate storage, and water boiling
- Chapter 2: Working Camp
  - unlock work roles, fishing rack, tool bench, and perimeter fence
- Chapter 3: Inland Push
  - open jungle trail, herbal medicine, guard duty, and scouting
- Chapter 4: Coastal Industry
  - build kiln, advanced workshop, smokehouse, larger storage, and rain collector array
- Chapter 5: Signal And Survival
  - gather rare parts, build beacon tower, withstand final multi-phase storm

Persistent progression inside the run includes:

- survivor count and skill growth
- camp tech unlocks
- facility tier upgrades
- expanded carrying capacity
- morale and trust bonuses that improve work output

## Level Or Run Structure

The game uses one persistent island map divided into unlockable zones with day-night cycles.

- Beachhead camp zone
- Palm shore and drift line
- Freshwater grove
- Jungle interior
- Cliff pass
- Reef edge and fishing coast
- Ruined wreck site
- Beacon hill summit

Each in-game day is a run-sized session:

- Dawn planning
- Day exploration and gathering
- Dusk return and assignment
- Night survival resolution

Major chapter gates unlock new zones and systems instead of separate disconnected levels.

## Economy Rewards And Upgrades

Primary resources:
- wood
- stone
- fiber
- food
- fresh water
- medicinal herbs
- scrap metal
- rope
- resin

Soft progression values:
- morale
- colony trust
- camp safety
- technology points from discoveries

Upgrade model:
- tool upgrades improve harvesting speed and yield
- structure upgrades improve storage, output, safety, and recovery
- role upgrades improve survivor efficiency in assigned jobs
- expedition upgrades improve stamina, scouting range, and transport capacity

Reward structure:
- daily objective rewards
- chapter milestone unlocks
- rescue rewards from saving survivors
- rare blueprint rewards from wreckage or hidden landmarks

## Screen Map

### Menu

- title banner
- continue or new game
- chapter progress summary
- settings
- help

### Gameplay HUD

- top-left colony condition cluster for food, water, morale, population
- top-right day timer, weather alert, and pause
- left joystick area reserved for movement
- right action and tool buttons
- bottom expandable task rail for build, assign, craft, map, and objectives

### Pause

- resume
- controls
- audio
- restart day
- return to menu

### Game Over

- failure reason card
- colony summary
- rescued survivors count
- chapter reached
- restart or return to menu

### Extra Screens

- survivor roster and job assignment screen
- build and craft drawer
- discovery journal
- chapter objective panel
- end-of-day report overlay

## UI Direction

### Layout Tone

The UI should feel like a practical shipwreck field journal mixed with clean seaside expedition equipment. It should stay bright and readable, avoiding military panels or neon sci-fi framing.

### HUD Priorities

- colony survival values must be readable in less than one second
- weather warnings must escalate clearly before storms
- survivor assignment pressure must be visible without opening a deep menu
- build and craft access must remain one tap away

### Gameplay Safe Area

- reserve the full center playfield for navigation, gathering nodes, animals, and hazard reactions
- use a thin top ribbon and a retractable bottom command rail
- keep left and right lower corners for touch controls only
- avoid decorative framing on the central island terrain

### Frame And Border Policy

- no heavy frame overlay on the live playfield
- panels may use rope, canvas, and driftwood motifs only in dedicated HUD containers
- screen chrome must sit outside active traversal and interaction zones

### Key Overlay Panels

- compact colony condition ribbon
- weather alert chip
- survivor shortage alert card
- build drawer
- end-of-day report card

### UI Asset Strategy

- primary ui_skin is skin_cartoon_light
- use one style-matched coastal expedition UI treatment built on the repository UI Kit
- motifs should emphasize canvas tags, tied rope corners, washed wood headers, and map-pin style markers
- avoid the common top HUD pills plus thick bottom command strip reuse; the bottom rail should behave like a fold-out field notebook tab bar

## Gameplay Art Direction

Required art roles:
- player leader
- rescued survivor civilians
- worker variants for builder, fisher, gatherer, guard, healer
- wild animals including boar, crab, gull, and snake
- island resource nodes including driftwood, berry bush, rock pile, herb patch, fishing spot
- camp structures including tents, fire pit, drying rack, fence, workshop, beacon tower
- environment props including wreckage, palms, crates, barrels, bones, rope bundles, tide pools

Camera perspective:
- top-down with slight angled readability, consistent across map and camp

Default facing and facing behavior:
- humanoid survivors default facing down-right in idle sheets if the pack requires it
- moving survivors must visually face movement direction through directional frames or horizontal flips
- guards and spear users must face target direction before attack release
- animals must rotate or swap directional frames based on chase or flee direction

Minimum visual states and animation expectations:
- survivors: idle, walk, gather, build, hurt, collapse
- guards: idle, walk, attack, hurt
- boar: idle, charge, hurt, defeat
- crab: scuttle, pinch, defeat
- structures: intact, damaged, upgraded visual tier when applicable
- effects: chop hit, hammer spark, rain splash, campfire glow, storm gust, morale rise, warning pulse

Animation quality tier:
- primary humanoids should meet a medium sprite animation tier with alternating walk poses and clear work-action poses

Anchor, hitbox, and z-order assumptions:
- humanoids anchor near feet center
- resource nodes use lower-third anchors for ground contact
- fences and tents occupy wider static footprints
- interactive entities require readable collision boxes smaller than their full silhouette
- tall props such as palms and towers should render canopy or head sections above characters

Visual readability constraints:
- resources must be distinguishable from decorative props at a glance
- hazards must have stronger saturation or motion cues than passive scenery
- survivors need role-coded accents without becoming uniform clones

Fallback rule if no suitable pack exists:
- prefer a license-clear shared pack family with island survival fit
- if role coverage is incomplete, extend via approved sprite pipeline from matching seed frames instead of shipping placeholder circles or static blocks

## Icon Direction

### Subject

A determined castaway leader raising a torch or hammer in front of a half-built beach camp and signal fire.

### Silhouette

Large foreground character with one raised tool arm, backed by a triangular tent and angled beacon flame.

### Color Direction

Warm sand, sea blue, sunlit orange fire, and leafy green accents.

### Tone

Adventurous, resilient, and character-driven rather than desperate or grim.

## Audio Direction

### Menu BGM

Light island adventure theme with wood percussion, soft plucks, and a hopeful castaway mood.

### Gameplay BGM

Rhythmic survival loop with hand drums, marimba-like plucks, and moderate tension that can layer extra percussion during night danger.

### Boss Or Climax BGM

Storm-night climax track with urgent drums, low toms, and faster melodic pulse during the final beacon defense.

### SFX Families

- wood chop
- rock hit
- hammer build
- water fill
- campfire crackle
- gull and surf ambience
- boar charge warning
- storm wind and thunder
- UI notebook flip and rope-tension click
- morale boost chime

## Technical Implementation Notes

### Rendering Style

- SurfaceView or custom GameView for island simulation and moving actors
- Android XML and View overlays for HUD, menu, pause, assignment, and end-of-day panels
- pathfinding can stay lightweight with zone-based worker routing and short local obstacle avoidance

### State Model

- MENU
- PLAYING
- PAUSED
- GAME_OVER
- DAY_REPORT
- ASSIGNMENT_OVERLAY

### Important Entities

- player leader
- survivor agents
- hostile animals
- resource nodes
- camp structures
- weather event controller
- objective controller

### Systems To Plan Early

- survivor task assignment and simple AI scheduling
- day-night cycle and camp resolution tick
- hunger, thirst, morale, and health simulation
- structure placement and camp footprint validation
- chapter objective gating
- weather escalation and raid spawning

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

- genre family: survival management
- concrete sub-archetype: castaway colony builder with direct exploration and survivor job assignment
- why it is not a reskin: the primary decision space is staffing and colony stability on a persistent island map, not farming orders, zombie combat extraction, or naval route control

### Map Or Playfield Budget

- one persistent island divided into at least 8 named regions
- minimum 4 terrain families: beach, grove, jungle, cliff, reef
- at least 10 functional map elements including fishing spots, water source, wreckage caches, herb groves, animal dens, beacon platform, choke paths, shelter pads, rope bridges, and storage sites
- at least 8 landmark props that support orientation and progression

### Entity Roster Budget

- 1 player leader archetype
- at least 5 survivor work roles
- at least 4 hostile or pressure creature types
- at least 6 resource node families
- at least 10 camp structure families
- at least 8 effect or pickup families

### Mechanic Variety Budget

- direct exploration
- gathering
- rescue encounters
- task assignment
- structure building
- crafting
- weather preparation
- night defense
- morale management
- chapter objective completion

### Forbidden Template Reuse

- no three-lane defense map
- no chapter flow built around stage-clear combat arenas
- no pure farming grid as the core playfield
- no harbor route-capture command loop
- no generic top HUD pill row with bottom static action strip

## Visual Identity Contract

### UI Layout Archetype

Top survival ribbon plus fold-out field notebook rail, with floating alert cards and a dedicated survivor roster drawer.

### HUD Composition

- compact ribbon for colony vitals
- right-side weather and day chip stack
- bottom notebook tab rail for build, assign, craft, map, objectives
- contextual survivor issue cards appearing near the upper center edge

### Playfield Safe Area

- center 70 percent of the screen remains open for actor movement and environment reading
- top bar stays shallow
- bottom rail retracts when not in use
- no lateral decorative rails allowed over the island terrain

### Frame Overlay Policy

Use motif-rich HUD containers only outside active play space. Canvas, rope, and driftwood ornamentation may decorate panels but must never mask resources, hazards, or touch-critical ground.

### Palette And Material Language

- seafoam blue
- sun-bleached canvas
- warm driftwood tan
- coral orange
- leaf green
- materials should feel stitched, knotted, and weathered but still optimistic

### Typography Direction

Friendly expedition signage with bold readable headers and clean body text; avoid techno, gothic, or military stencil vibes.

### UI Pack Strategy

Build from the repository UI Kit using skin_cartoon_light tokens plus a coastal expedition motif layer. Prefer one coherent pack language rather than mixing rugged survival metal with playful cartoon candy elements.

### Icon Subject And Silhouette

Leader character with raised tool and beacon glow over a beach camp silhouette, clearly readable at small size.

### Forbidden Visual Reuse

- do not reuse military dashboard layouts
- do not reuse neon magenta-cyan sci-fi palette structures
- do not reuse dark throne-room or fortress framing
- do not reuse generic crate, sword, shield, or tower icon subjects
- do not use the same top-pill and bottom-strip HUD composition seen in repeated casual prototypes

## Differentiation Note

Castaway Clan differentiates itself from the current registry by centering a rescued-survivor labor economy on a single persistent island. It is not an orchard timer game, not a ranch combo manager, not a zombie combat scavenger, and not a harbor logistics war. Its strongest unique axis is the conversion of every found survivor into a functional colony role that reshapes camp output and survival odds.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
