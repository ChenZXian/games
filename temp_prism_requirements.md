# Prism Sector Collapse

## Game Positioning
Prism Sector Collapse is a large-board, high-clarity color-collapse puzzle game built for players who want longer board planning, stronger spatial control, and a more strategic rhythm than a swap-based match-3. The game should feel clean, sharp, and deliberate, with giant fields of color that invite route reading, setup play, and satisfying chain detonations across a much bigger board footprint.

## Target Feel And Player Fantasy
The target feel is crisp, tactical, and electrically satisfying. The player fantasy is that they are stabilizing a giant prism reactor grid by carving precise color-collapse routes, charging spectrum devices, and setting off wide-area energy clears across an oversized board. The game should reward foresight more than twitch response, while still delivering strong visual payoff when a carefully prepared sector collapses in sequence.

## Core Gameplay Loop
1. Enter a large reactor board with a defined objective, move budget, and device layout.
2. Tap a connected cluster of four or more same-color prism cells to collapse it.
3. Let gravity, lateral slide rules, and energy relays reshape the oversized board.
4. Build large cascades to overcharge line blasters, color converters, and pulse cores.
5. Complete the target before moves expire to earn stars, chapter progress, and unlock more complex board systems.
6. Advance through sector maps that introduce larger boards, harder blockers, and more multi-step routing goals.

## Controls And Input Model
- Single-finger input only.
- Tap a valid connected same-color cluster to clear it.
- Hold on a cluster briefly to preview collapse size and projected charge gain.
- Invalid taps should trigger a short outline shimmer instead of a harsh failure bounce.
- Long idle state may show one subtle route suggestion around a high-value cluster.

## Failure Conditions And Win Conditions
- Win by completing the board objective before moves reach zero.
- Fail when the move budget ends and the sector objective remains incomplete.
- Pressure comes from board fragmentation, armored blockers, color isolation, and inefficient collapse timing rather than speed countdown stress.

## Progression Structure
- Sector-based chapter map.
- First release target: 5 sectors with 18 stages each for 90 total stages.
- Early sectors teach core cluster collapse, gravity reading, and board opening.
- Mid sectors introduce split chambers, relay gates, and converter devices.
- Late sectors emphasize oversized board management, multi-objective sequencing, and color economy.
- Stars unlock optional analyzer rewards and sector milestone devices.

## Level Structure Or Run Structure
- Stage types rotate between energy quota, relay activation, armored blocker purge, color collection, and core rescue.
- Every sector contains:
  - 6 standard collapse quota stages
  - 4 color collection stages
  - 4 blocker-routing stages
  - 3 relay or converter puzzle stages
  - 1 oversized finale board with stacked objectives
- Board size starts at 10x14 and expands to 12x16 in advanced sectors.

## Economy, Rewards, Drops, Or Upgrade Model
- Stars and credits from stage clears, first clears, and analyzer milestones.
- Credits unlock cosmetic board themes and pre-stage utility charges.
- Optional reserve tools:
  - one column pulse
  - one color scanner
  - one reshuffle token
- No backend dependency.
- Retention comes from high-score routing, large-board mastery, and perfect clear pursuit rather than collection-heavy meta systems.

## Gameplay Diversity And Content Budget
- Genre family: puzzle
- Concrete sub-archetype: oversized same-color cluster collapse puzzle with device-driven chain routing
- Camera perspective: fixed portrait top-down grid with large visible board coverage and layered effect overlays
- Control model: tap-to-collapse connected color clusters with hold preview
- Core loop signature: read giant board, collapse large color regions, reshape columns, charge devices, finish multi-step goals under move pressure
- Why it is not a reskin of an existing game:
  - the first game is adjacent-swap match-3, while this game is cluster-tap collapse with no swap mechanic
  - the board is materially larger and planning revolves around region carving instead of local swap optimization
  - the visual fantasy is a prism reactor console, not candy spectacle or parade celebration
- Playfield model:
  - portrait-first oversized rectangular reactor board
  - board occupies the dominant central field with minimal chrome
  - compact status rails stay outside the active collapse area
- Minimum map and board variety:
  - 90 stages in the initial target plan
  - at least 10 distinct board silhouettes across the full set
  - at least 6 split-chamber or partitioned sector layouts
  - at least 5 wide-board finale layouts at 12x16 scale
  - at least 7 blocker and device combination recipes
- Terrain, obstacle, landmark, and functional map element variety:
  - armored prism blocks
  - locked cells
  - relay nodes
  - color converters
  - one-way slide channels
  - static void gaps
  - charge cores
  - pulse emitters
- Player action variety:
  - basic cluster collapse
  - hold preview
  - relay charging
  - device-triggered line clear
  - converter setup play
  - reserve tool activation
- Enemy, obstacle, unit, item, or prop roster targets:
  - 6 standard colors at launch
  - 4 device families
  - 7 blocker or board modifier families
  - 5 objective families
  - 6 effect families
- Progression and upgrade variety:
  - sector unlocks
  - analyzer star milestones
  - optional reserve utilities
  - oversized sector finale boards
- Animation and feedback expectations:
  - every cluster clear should ripple through adjacent cells with directional energy pull
  - larger clears should escalate screen-wide sector pulse feedback
  - device activations must have distinct charge, release, and aftermath phases
  - high-value collapses should feel more surgical and electric than candy-like or bouncy
- Asset variety plan:
  - each color family must remain sharply distinguishable under fast cascades
  - blockers require readable armor damage staging
  - devices need unique silhouettes and charge states
  - effect language must distinguish normal collapses, relay triggers, and sector burst events
- Forbidden template reuse:
  - no adjacent swap match-3 interaction
  - no candy parade board framing, top marquee, or bottom utility tray composition
  - no pastel confection palette
  - no tiny-board prototype layouts

## Visual Identity Contract
- UI layout archetype:
  - reactor survey console with a giant central board, thin top telemetry strip, left sector index spine, and right objective stack
- HUD composition:
  - thin top telemetry strip for score and charge
  - left vertical sector and chain meter spine
  - right compact move and objective modules
  - no bottom command tray during core play
- Navigation model:
  - sector console menu to stage node grid to gameplay to debrief panel
  - slide-in side sheets for help and pause
- Playfield safe area:
  - board owns the vast center of the portrait screen and remains untouched by decorative chrome
  - hud modules must remain outside board bounds on top, left, and right margins only
  - no persistent overlays over active prism cells
- Frame overlay policy:
  - use minimal neon contour framing outside the board rectangle only
  - effects may flare above the board temporarily but controls and metrics never sit on top of cells
- Palette signature:
  - graphite black, cyan grid glow, lime signal green, amber warning, magenta pulse, white highlights
- Material language:
  - glassy reactor panels, luminous contour lines, matte industrial backdrop, hard-edged prism cells
- Typography style:
  - squared futuristic display numerals with clean condensed labels
- UI pack strategy:
  - recommended ui_skin: skin_neon_future
  - custom reactor-console refinement layered on top of the neon-future token base
- Icon subject:
  - a fractured prism cube releasing a cross-shaped energy burst
- Icon silhouette and composition:
  - dominant tilted prism block with sharp shard corners and a bright central core flare
- Icon palette and background:
  - cyan, lime, magenta, and white energy over a dark graphite or deep navy reactor field
- Forbidden visual reuse:
  - no candy, syrup, cream, or toybox motifs
  - no rounded candy cabinet composition
  - no reused ribbon-burst icon silhouette
  - no soft pastel objective marquee or bottom booster tray layout

## Screen Map
- Main Menu
- Sector Map
- Stage Brief Panel
- Gameplay HUD
- Pause Side Sheet
- Goal And Device Guide Sheet
- Stage Clear Debrief
- Stage Fail Debrief
- Utility Loadout Sheet

## UI Direction
- Recommended ui_skin: skin_neon_future
- Layout tone: sharp reactor dashboard with high board ownership and minimal ornamental mass
- HUD priorities:
  - move budget and objective state first
  - charge and sector chain state second
  - score and efficiency metrics third
- Playfield safe area:
  - board should visually dominate the screen and preserve large contiguous tap space with only narrow left and right support rails
- Frame and border policy:
  - thin contour frame outside the board rectangle only
  - no heavy bezels, rounded candy borders, or decorative panels near tap-critical cells
- Key overlay panels:
  - stage brief card
  - pause side sheet
  - result debrief panel with chain statistics
  - utility loadout sheet before advanced stages

## Gameplay Art Direction
- Required art roles:
  - standard prism cells
  - device cells
  - blocker cells
  - objective cores
  - board backgrounds
  - collapse particles
  - chain pulse effects
  - UI telemetry accents
- Camera perspective:
  - fixed top-down reactor board presentation
- Suggested shared pack family or source tier:
  - primarily project-local custom geometric puzzle art with UI support from sci-fi style shared assets
- Default facing and facing behavior for moving or attacking entities:
  - not character-based; cells use edge-light direction, charge lines, and activation beams instead of actor facing
- Minimum visual states and animation expectations:
  - idle
  - highlighted
  - hold-preview
  - collapse pull
  - impact flash
  - device charged
  - device firing
  - blocker damaged
  - column landing
- Animation quality tier for primary entities:
  - high for collapse readability and chain payoff
- Anchor, hitbox, and z-order assumptions for primary entities:
  - cells are centered to grid bounds
  - touch hitbox should stay slightly inset from cell borders for precision
  - particles render above cells and below HUD modules
- Visual readability constraints:
  - color families must remain readable for color-weak players through shape marks and internal line motifs
  - blockers and devices must not be confused with standard cells
  - the largest board state must remain legible on mobile portrait screens
- Fallback rule if no suitable pack exists:
  - prefer polished geometric runtime rendering with explicit color and symbol separation over generic soft circles or candy-style pieces

## Icon Direction
- Subject: fractured prism reactor cube
- Silhouette: sharp tilted cube with shard wings and a bright cross-core burst
- Color direction: cyan, lime, magenta, white, graphite
- Tone: tactical, clean, high-energy, futuristic

## BGM Direction
- Menu music mood:
  - cool, synthetic, scanning, confident
- Gameplay loop mood:
  - focused electronic pulse with a steady planning rhythm and rising intensity for larger chain setups
- Boss or climax mood if applicable:
  - sector finale tracks should feel charged, expansive, and high-voltage rather than sweet or playful

## Technical Implementation Notes
- Rendering style:
  - Android View-based large-board renderer with efficient tap-cluster detection, collapse sequencing, and low-overdraw geometric effects
- State model:
  - menu, sector_map, stage_brief, playing, resolving, paused, win, fail
- Important entities:
  - prism cells
  - armored blocks
  - relay nodes
  - converter devices
  - pulse emitters
  - rescue cores
- Special systems to plan early:
  - large-board readability and touch targeting
  - cluster detection on oversized grids
  - collapse and side-slide rules
  - color-accessibility symbol overlay system
  - device charge and trigger sequencing
  - late-stage partition board support

## Differentiation Note Against The Current Registry
Prism Sector Collapse is distinct from the repository and from Candy Pulse Parade because it replaces swap-based local puzzle play with large-field cluster planning, uses a much larger board footprint, adopts a dark neon reactor visual language, and prioritizes clarity and tactical region carving over glossy candy celebration.
