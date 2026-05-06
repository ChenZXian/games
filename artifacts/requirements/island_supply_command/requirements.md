# Island Supply Command

Game ID: island_supply_command
Direction: strategy
Selected Concept: Island Supply Command
Recommended UI Skin: skin_cartoon_light

## Positioning

Island Supply Command is a compact real-time logistics strategy game for short mobile sessions. The player commands a cartoon island fleet, builds convoy routes across linked ports, keeps forward harbors supplied, and captures the enemy flagship harbor through superior stock flow rather than raw tapping speed.

## Target Feel And Player Fantasy

The game should feel bright, readable, and tactical. The player fantasy is not commanding a giant army directly, but acting as a sharp admiral who wins by making the map work harder: every route matters, every supply burst changes the front, and a well-fed harbor can hold off larger forces.

## Core Gameplay Loop

1. Start a stage with one main player harbor, several neutral islands, and one enemy flagship harbor.
2. Tap an owned harbor and a connected target harbor to establish or remove a supply route.
3. Owned harbors generate stock over time and send convoy boats along active routes when stocked.
4. Friendly harbors use stock to auto-fire at enemy convoys and to strengthen their own capture pressure.
5. Neutral and enemy harbors lose defense when hostile convoys arrive and flip ownership when defense reaches zero.
6. Earn command points from successful deliveries and captures.
7. Spend command points on instant boost skills or port upgrades to stabilize the network.
8. Collapse the enemy flagship harbor before the stage timer expires or before the player flagship falls.

## Controls And Input Model

- Primary control: tap an owned harbor, then tap a connected harbor to toggle a route.
- Secondary control: tap a harbor to inspect its stock, defense, and upgrade state.
- Skill buttons:
  - Route: default interaction mode for route assignment.
  - Surge: tap an owned harbor to inject bonus stock immediately.
  - Cannon: tap an owned harbor to grant a temporary convoy-defense aura.
- Pause button in the top HUD.
- Result overlay buttons for restart or return to menu.

## Win And Failure Conditions

- Win:
  - capture the enemy flagship harbor, or
  - own every strategic harbor on the map.
- Fail:
  - lose the player flagship harbor, or
  - let the timer expire while the enemy flagship still stands.

## Progression Structure

- Five handcrafted campaign stages with increasing node count and more aggressive enemy route behavior.
- Stage clear grants a star rank based on time remaining, convoy losses, and player harbor integrity.
- Highest cleared stage and best star total should persist locally.

## Level Or Run Structure

- Stage 1: tutorial-grade map with one neutral route fork.
- Stage 2: two-lane island split that teaches route prioritization.
- Stage 3: central neutral fortress that can swing the whole map.
- Stage 4: enemy side harbors constantly pressure both flanks.
- Stage 5: long flagship siege with multiple neutral choke points.

Each stage should last roughly three to five minutes on a clean run.

## Economy Rewards And Upgrades

- Passive economy: each owned harbor generates stock over time.
- Command points: earned from deliveries, captures, and convoy interceptions.
- Harbor upgrades:
  - Dock Boost: faster convoy launch cadence.
  - Hull Shield: higher defense and convoy durability.
  - Watch Cannon: stronger interception radius against enemy convoys.
- Active skills:
  - Surge: instant stock refill on one owned harbor.
  - Cannon: short defensive aura that improves interception.

## Screen Map

### Menu

- Game title
- one-paragraph pitch
- start campaign button
- how to play button
- stage progress summary

### Gameplay HUD

- command points
- stage and star target
- timer
- player harbor count
- pause button
- context hint text above the bottom action bar

### Pause

- resume
- restart stage
- return to menu

### Game Over

- victory or defeat title
- stage summary with time, stars, and convoy losses
- restart
- return to menu

### Extra Screens

- lightweight how-to-play dialog inside the menu flow
- compact harbor info card when a node is selected

## UI Direction

### Layout Tone

Bright nautical command table with clean rounded panels, toy-like harbor markers, blue water gradients, and orange-gold accent highlights for routes and cargo.

### HUD Priorities

- make route state and harbor ownership readable at a glance
- keep command points and time highly visible
- keep active tool mode obvious so taps are predictable

### Key Overlay Panels

- central menu card
- pause card
- result card
- selected harbor info card near the bottom center

### UI Asset Strategy

Use `skin_cartoon_light` and assign the shared `kenney_ui_pack` preset for primary and secondary buttons plus the narrow headline font. Keep gameplay rendering in `GameView`, while menu, pause, help, and result surfaces stay in XML overlays.

## Icon Direction

### Subject

A bright cargo crate boat crossing between two islands with a bold signal arrow.

### Silhouette

Rounded boat hull, oversized crate, and one sweeping route arrow that reads clearly at small sizes.

### Color Direction

Turquoise water, white hull highlight, sunny orange cargo accent, and a deep navy outline.

### Tone

Cartoon, readable, upbeat, and strategy-forward rather than realistic.

## Audio Direction

### Menu BGM

Light naval planning mood with optimistic motion and clean rhythmic pulses.

### Gameplay BGM

Steady tactical loop with buoyant percussion and rising momentum as routes multiply.

### Boss Or Climax BGM

Tighter, more urgent variation with stronger drums and sharper melodic peaks for the flagship push.

### SFX Families

- soft UI clicks for panel and button actions
- brighter confirm tone for route creation
- short warning stinger when the flagship is under attack
- small pop on convoy delivery
- heavier impact on harbor capture

## Technical Implementation Notes

### Rendering Style

Use a single `SurfaceView` to draw the water map, linked sea lanes, harbors, convoy boats, capture rings, and route highlights. Use simple shape-based art and particle pings instead of bitmap scene art.

### State Model

- `MENU`
- `PLAYING`
- `PAUSED`
- `GAME_OVER`

The gameplay session should also track stage index, star result, selected harbor, active tool mode, and active route graph.

### Important Entities

- HarborNode
- SeaLink
- Convoy
- StageDefinition
- SessionProgress
- FloatingText

### Systems To Plan Early

- route assignment and removal rules
- convoy spawning cadence and stock transfer rules
- capture math for neutral and enemy harbors
- enemy AI route selection priorities
- persistent campaign progress storage
- lightweight audio playback for looping BGM plus a few SFX events

## Differentiation Note

This concept stays separate from `territory_swarm` because battles are won through stock routing and convoy interception rather than direct unit floods between map nodes. It stays separate from `lane_warfront` because there are no side-view lanes or manual unit spawning, and it stays separate from `switchyard_courier` because the player manages a living multi-route war economy instead of guiding a single courier vehicle.

## Confirmation

Status: Confirmed by automatic workflow authorization for menu item 10.
