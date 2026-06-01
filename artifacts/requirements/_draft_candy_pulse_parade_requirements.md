# Candy Pulse Parade

## Game Positioning
Candy Pulse Parade is a premium-feeling mobile match-3 puzzle game built around bright candy spectacle, satisfying chain reactions, and polished stage-show presentation. The game should feel immediately readable to casual players while delivering unusually rich feedback through layered explosion timing, syrup wave propagation, sparkle trails, and candy shard motion that make every large combo feel celebratory instead of routine.

## Target Feel And Player Fantasy
The target feel is joyful, glossy, and energetic. The player fantasy is that every smart swap triggers a candy parade of chain reactions, color surges, and stage-clearing flourishes. Moment-to-moment play should feel smooth and snackable, while bigger cascades should feel dramatic enough to encourage replay and chase perfect clears.

## Core Gameplay Loop
1. Enter a handcrafted puzzle stage with a clear goal, move budget, and board modifiers.
2. Swap adjacent candy pieces to form horizontal or vertical matches of three or more.
3. Trigger cascades, charge special candy effects, and break blockers that gate board flow.
4. Build striped, wrapped, and rainbow special pieces to solve layered goals more efficiently.
5. Clear the target before running out of moves to earn stars, bonus coins, and progression unlocks.
6. Advance through a chapter map with escalating blocker combinations and spectacle moments.

## Controls And Input Model
- Single-finger input only.
- Tap a candy once to select it.
- Swipe toward an adjacent tile to attempt the swap.
- Invalid swaps animate with a soft bounce-back rather than a hard reject.
- Long idle state may show a subtle hint pulse on one valid move.

## Failure Conditions And Win Conditions
- Win by completing the stage objective before moves reach zero.
- Fail when the move budget ends and the objective remains incomplete.
- Soft fail pressure comes from layered blockers, awkward board partitions, and objective routing rather than countdown stress.

## Progression Structure
- Chapter-based world map.
- First release target: 6 chapters with 20 stages each for 120 total stages.
- Early chapters teach core match, blocker, and special-candy interactions.
- Mid chapters combine blockers and introduce split-board routing.
- Late chapters emphasize chain planning, objective sequencing, and combo efficiency.
- Star ratings gate optional reward chests and chapter-perfect milestones.

## Level Structure
- Stage types rotate between score targets, jelly clear, candy order, ingredient drop, and blocker purge.
- Every chapter contains:
  - 8 standard score or jelly stages
  - 5 order stages
  - 4 blocker-heavy puzzle stages
  - 2 ingredient-drop stages
  - 1 spectacle finale stage with denser chain potential
- Board size starts at 8x8 and expands up to 9x9 for advanced layouts.

## Economy And Rewards
- Coins from stage clears, first-clear bonuses, and chapter chests.
- Stars accumulate on chapter map for milestone rewards.
- Optional reserve system:
  - pre-run boosters earned from gameplay, not required for the first implementation
  - no backend dependency
- Cosmetic retention comes from chapter completion, perfect-clear badges, and candy collection panels rather than monetization systems.

## Gameplay Diversity And Content Budget
- Genre family: puzzle
- Concrete sub-archetype: handcrafted objective-driven match-3 with spectacle-first combo presentation
- Camera perspective: fixed flat board with layered 2D candy animation and depth-stacked particle overlays
- Control model: single-finger adjacent swap interaction
- Core loop signature: swap, cascade, manufacture specials, break blockers, complete objective under move pressure
- Why it is not a reskin:
  - the repository currently has no match-3 board puzzle
  - the entire game is built around grid routing, blocker layering, and combo spectacle rather than action, farming, defense, or runner loops
  - the board must support deliberate cascade choreography and rich effect timing
- Playfield model:
  - portrait-first rectangular puzzle board
  - top objective ribbon
  - side-free board space with HUD outside the active candy grid
- Minimum map and board variety:
  - 120 stages in the initial target plan
  - at least 12 distinct board silhouettes across the full set
  - at least 6 split-board or lane-partition layouts
  - at least 5 ingredient-drop path layouts
  - at least 8 blocker combination recipes
- Terrain and functional element variety:
  - jelly layers
  - frosting blocks
  - licorice locks
  - chocolate spreaders
  - candy crates
  - ingredient lanes
  - one-way portals
  - conveyor rows in late chapters
- Player action variety:
  - basic swap
  - striped special creation
  - wrapped special creation
  - rainbow special creation
  - special plus special combination
  - chain routing through blockers
- Enemy or obstacle equivalent roster targets:
  - 5 standard candy colors at launch
  - 3 core special candy families
  - 8 blocker or board modifier families
  - 4 objective families
  - 6 combo-event effect families
- Progression and upgrade variety:
  - chapter unlocks
  - star chest rewards
  - optional earned boosters
  - chapter finale spectacle boards
- Animation and feedback expectations:
  - every match pops with squash, flash, and shard motion
  - cascades trigger ripple lighting across nearby tiles
  - special candy activation must feel visually distinct and layered
  - large chain clears should escalate camera-free screen feedback through pulse overlays and celebratory confetti bursts
- Asset variety plan:
  - candy families must include distinct shapes, frosting, and highlight treatment
  - blockers require readable silhouettes and damage states
  - special candies need unique charge and trigger silhouettes
  - particle language must differentiate ordinary clears from premium chain events
- Forbidden template reuse:
  - no reuse of action-game HUD layouts from existing repository projects
  - no landscape combat shell
  - no generic top-pill plus bottom-command-strip layout
  - no tiny single-board prototype with only one blocker and one objective

## Visual Identity Contract
- UI layout archetype:
  - portrait candy theater layout with a rounded top objective marquee, centered board stage, and bottom tray for moves, boosters, and stage actions
- HUD composition:
  - top marquee for target summary and progress icons
  - left floating chapter badge and score ribbon
  - right floating moves counter tower
  - bottom candy-glass utility tray
- Navigation model:
  - chapter map to stage card to gameplay to result panel
  - modal overlays for pause, booster select, and results
- Playfield safe area:
  - active board remains fully unobstructed inside a centered portrait-safe rectangle
  - decorative chrome can live above and below the board but never over the candy cells
  - no persistent side rails inside the board bounds
- Frame overlay policy:
  - use a candy cabinet frame outside the board bounds only
  - no ornaments, meters, or floating buttons over touch-critical candy cells
- Palette signature:
  - taffy cyan, berry pink, lemon yellow, cream white, mint green accents on a bright pastel backdrop
- Material language:
  - glossy candy shell, syrup glass, soft jelly translucency, thick cream borders
- Typography style:
  - playful rounded display type with clean readable body labels
- UI pack strategy:
  - recommended ui_skin: skin_cartoon_light
  - custom candy-themed refinement layered on top of the cartoon-light token base
- Icon subject:
  - a striped rainbow candy bursting out of a parade ribbon arc
- Icon silhouette and composition:
  - large center candy with explosive shard halo and ribbon sweep, readable at small size
- Icon palette and background:
  - warm pink and cyan candy on a creamy sky-blue or pale yellow background burst
- Forbidden visual reuse:
  - no military, dark arcade, or post-apocalypse visual language
  - no reused fist, weapon, fortress, animal-ranch, or tactic-map icon motifs
  - no reuse of existing repository HUD archetypes built for action combat

## Screen Map
- Main Menu
- Chapter Map
- Stage Start Panel
- Gameplay HUD
- Pause Overlay
- Goal Reminder Overlay
- Stage Clear Result
- Stage Fail Result
- Collection Or Candy Guide Panel

## UI Direction
- Recommended ui_skin: skin_cartoon_light
- Layout tone: deluxe candy shop parade cabinet with soft depth and glossy highlights
- HUD priorities:
  - target objective first
  - moves remaining second
  - score and combo celebration third
- Playfield safe area:
  - the board occupies the central largest rectangle with at least 24dp equivalent visual breathing room around it
- Frame and border policy:
  - rounded candy frame stays outside board cells
  - blocker and effect overlays stay cell-contained
- Key overlay panels:
  - pre-level target card
  - pause sheet
  - success burst panel with stars and candy showers
  - fail panel with retry emphasis

## Gameplay Art Direction
- Required art roles:
  - standard candy pieces
  - special candy pieces
  - blocker pieces
  - objective items
  - board cell backgrounds
  - match particles
  - cascade effects
  - UI celebration effects
- Camera perspective:
  - fixed top-down board presentation with slight faux depth from highlights and shadows
- Suggested source tier:
  - primarily project-local custom puzzle art direction guided by shared puzzle-friendly UI assets
- Default facing behavior:
  - not character-based; pieces rely on highlight direction and animated charge states instead
- Minimum visual states:
  - idle
  - selected
  - swap motion
  - match pulse
  - destruction
  - cascade landing
  - charged special
  - blocker damaged
- Animation expectations:
  - standard candies should wobble subtly on landing
  - matches should pop with a two-step scale burst and fragment release
  - striped and wrapped activations require their own pre-flash and release timing
  - rainbow specials require a board-scan light sweep
- Animation quality tier:
  - high for puzzle feedback, even if actor count is low
- Anchor, hitbox, and z-order assumptions:
  - candies are cell-centered
  - particles render above pieces
  - blockers remain below active blast effects but above board background
- Readability constraints:
  - colors must remain distinct under quick cascades
  - blockers need clear crack or damage staging
  - selected piece highlight must be visible without obscuring candy color
- Fallback rule:
  - if high-quality candy assets are not sourced, use polished vector or shape-driven custom art rather than placeholder circles

## Icon Direction
- Subject: striped rainbow candy burst
- Silhouette: rounded candy core with diagonal ribbon slash and shard halo
- Color direction: pink, cyan, yellow, and cream
- Tone: premium cheerful spectacle

## BGM Direction
- Menu music mood:
  - light candy parade, toybox whimsy, bright and welcoming
- Gameplay loop mood:
  - bouncy, sparkling, rhythmic, and slightly more energetic than menu
- Climax mood:
  - stage-clear fanfare should feel glossy, triumphant, and sweet rather than epic

## Technical Implementation Notes
- Rendering style:
  - Android View-based board renderer with polished tweened tile animation
- State model:
  - menu, map, stage_start, playing, resolving, paused, win, fail
- Important systems:
  - deterministic board generation and refill
  - swap validation
  - cascade resolver
  - special candy generation rules
  - layered blocker health logic
  - move budget and objective tracking
  - effect queue for attractive staged animation timing
- Special systems to plan early:
  - animation sequencing model for cascade readability
  - board lock input during resolve windows
  - objective-specific stage scripting
  - hint system

## Differentiation Note Against Current Registry
Candy Pulse Parade stands apart from the current repository by introducing the first grid-based adjacent-swap puzzle structure, the first portrait-first board-centric game shell, and the first project where animation spectacle is centered on chain resolution instead of combat, farming, or navigation pressure.
