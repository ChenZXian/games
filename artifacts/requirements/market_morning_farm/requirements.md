# Market Morning Farm

Game ID: market_morning_farm
Direction: A casual farm management game inspired by the social farming feel of classic crop games, focused on growing produce and running a morning market stall.
Selected Concept: Market Morning Farm
Recommended UI Skin: skin_cartoon_light

## Positioning

Market Morning Farm is a short-session Android Java mini-game where the player grows crops before sunrise, stocks a roadside market stall, adjusts prices, and serves different customer types before the market closes. The game keeps the friendly farming fantasy while shifting the main decision pressure from simple crop timers to supply, pricing, and customer flow.

## Target Feel And Player Fantasy

The player should feel like a clever small-town farmer who turns a modest garden into a busy morning market. Sessions should feel bright, readable, and satisfying: plant quickly, harvest at the right time, arrange attractive stall displays, and react to customer demand without needing online services or real-time social systems.

## Core Gameplay Loop

1. Start each market day with limited coins, seed choices, and several empty garden plots.
2. Plant fast, medium, or premium crops based on expected customer demand.
3. Water and boost selected plots to control growth timing.
4. Harvest produce into a small storage crate.
5. Place harvested goods on three stall shelves with different visibility bonuses.
6. Adjust each product price between bargain, fair, and premium.
7. Serve customer waves, earn coins and reputation, and avoid stockouts or overpriced stalls.
8. Spend profits on seeds, shelf upgrades, storage expansion, or garden plot upgrades.
9. Clear the daily target to unlock the next market day and new product or customer rules.

## Controls And Input Model

- Tap an empty plot to open the seed picker.
- Tap a planted plot to water, boost, or harvest when ready.
- Drag produce from storage to a stall shelf slot.
- Tap a shelf product to cycle price tier.
- Tap customer thought bubbles to inspect preferred goods and price tolerance.
- Tap upgrade buttons between days.
- Use a pause button in the HUD; all controls must be single-touch friendly.

## Win And Failure Conditions

- Win condition: reach the daily revenue target and minimum reputation target before the market timer ends.
- Star rating: one star for revenue, one for reputation, one for serving a daily special order.
- Failure condition: market timer ends before reaching the minimum revenue target, or reputation drops to zero from repeated stockouts and price complaints.
- Soft failure: a day can be replayed without permanent loss.

## Progression Structure

- The game uses a chapter-like sequence of market days.
- Early days teach planting, harvesting, stocking, and fair pricing.
- Midgame introduces customer types, perishable produce, and shelf placement bonuses.
- Later days add daily trends, bulk buyers, and premium crops with higher risk.
- Persistent upgrades include plot quality, crate size, shelf appeal, watering speed, and price insight.

## Level Or Run Structure

- A run is one market day lasting about three to five minutes.
- Each day has a fixed demand forecast, customer mix, revenue target, reputation target, and one optional special order.
- The garden and stall share one screen, with the stall at the lower half and crop plots above it.
- Difficulty increases by adding more customer preferences and tighter timing, not by making controls faster than mobile players can handle.

## Economy Rewards And Upgrades

- Coins are earned from sales.
- Reputation is earned from fair pricing, correct product matches, and serving special orders.
- Seeds cost coins; premium seeds require more planning but sell for more.
- Upgrades:
  - Garden Plot: faster growth and better yield chance.
  - Watering Can: shorter action cooldown.
  - Storage Crate: more inventory slots.
  - Shelf Appeal: increases customer patience and product visibility.
  - Price Tag Insight: reveals price tolerance for one customer type.
- Rewards include new seed packs, new market decorations, new shelf layouts, and customer profile entries.

## Screen Map

### Menu

- Shows the game title, start button, day select button, upgrades button, and sound toggle.
- Uses bright farm-market motifs: awning stripe, small crates, and seed packet cards.

### Gameplay HUD

- Top-left: day number and remaining market time.
- Top-center: revenue target meter.
- Top-right: reputation hearts and pause button.
- Left edge: compact seed tray drawer.
- Bottom strip: storage crate and stall shelf area.
- Customer queue appears beside the stall but must not cover active crop plots.

### Pause

- Resume, restart day, sound toggle, and return to menu.

### Game Over

- Shows revenue, reputation, missed customers, best sale, and retry button.

### Extra Screens

- Upgrade screen for plot, crate, shelf, and tool improvements.
- Customer book screen showing known preferences and price tolerance.
- Day select screen with earned star ratings.

## UI Direction

### Layout Tone

Use skin_cartoon_light with a market-board layout: the playfield is split into an upper garden grid and lower stall workbench. The interface should feel cheerful and organized, with bold readable labels and tactile produce cards.

### HUD Priorities

The HUD must prioritize market time, revenue progress, reputation, active customer patience, storage count, and current price tier. Avoid generic top-pill repetition by making the key target meter look like a chalkboard sign above the stall.

### Gameplay Safe Area

Reserve the upper 62 percent of the screen for garden plots and customer movement. Reserve the bottom 26 percent for storage and stall shelves. Keep the top HUD within a shallow non-interactive banner. No decorative frame may overlap crop cells, customer bubbles, or stall slots.

### Frame And Border Policy

Decorative awning stripes and wooden trim may sit outside the active playfield only. Borders should define the stall workbench and chalkboard HUD, not cover playable elements.

### Key Overlay Panels

- Seed picker: small popover anchored to the seed tray.
- Customer detail bubble: compact card beside the active customer.
- Upgrade drawer: full-width panel between days, never during active play.

### UI Asset Strategy

Start with the repository UI Kit using skin_cartoon_light. The UI workflow should first search shared_assets/ui for a bright market, farm, or wooden shop pack with license metadata. If no suitable pack exists, use UI Kit XML drawables with custom market motifs and record the fallback as project-local custom refinement.

## Gameplay Art Direction

- Required art roles: crop plots, seed packets, growth stages, harvested produce, storage crate, stall shelves, customer sprites, coin/reputation effects, watering effect, sold-out marker, special order marker.
- Camera perspective: orthographic top-down for plots, front-facing stall shelf for goods, small side/front customer sprites.
- Suggested source tier: style-matched shared game art pack first; then license-clear farm or market pack; then project-local generated bitmap through the gameplay art workflow if needed.
- Default facing: customers face toward the stall when waiting and face away when leaving.
- Facing behavior: customer sprites use horizontal flip when entering from left or right queue lanes.
- Minimum states:
  - Crops: empty, sprout, growing, ready, withered.
  - Customers: walking, waiting, happy, annoyed, leaving.
  - Produce: storage icon, shelf icon, sold pop effect.
- Animation expectations: customers need at least two-frame walking loops; crop growth can use discrete stage changes; coin and reputation feedback should animate by scale or movement.
- Animation quality tier: medium for customers, simple staged animation for crops and goods.
- Anchors and hitboxes: crop plot hitbox equals visible plot tile; customer hitbox centered on body; stall slot hitbox equals shelf slot bounds.
- Z-order: background ground, plots, crops, customer queue, stall, produce icons, effects, HUD.
- Readability constraints: each product family must have a distinct silhouette and color; price tags must remain legible on small screens.
- Fallback rule: generic circles, rectangles, or unassigned canvas-only produce placeholders are not acceptable for delivery-ready output.

## Icon Direction

### Subject

A smiling market crate overflowing with carrots, tomatoes, and a small price tag.

### Silhouette

Large wooden crate in the foreground, diagonal price tag, small striped awning shape behind it.

### Color Direction

Fresh green, tomato red, carrot orange, warm wood, and sky-blue background accents.

### Tone

Cartoon, friendly, high-contrast, clearly readable at launcher size.

## Audio Direction

### Menu BGM

Gentle sunny acoustic loop with light percussion and a relaxed small-town market mood.

### Gameplay BGM

Upbeat but soft farm-market loop with plucked strings, hand percussion, and subtle tempo lift during busy customer waves.

### Boss Or Climax BGM

No boss track is required. Use a short busy-market intensity layer or jingle during final rush seconds.

### SFX Families

Tap, plant, water, harvest, stock shelf, price change, customer happy, customer complaint, coin sale, reputation gain, upgrade purchase, day clear, day failed.

## Technical Implementation Notes

### Rendering Style

Use a custom GameView or SurfaceView for active gameplay rendering, with Android View or XML overlays for menu, pause, result, and upgrade screens. Gameplay should run in landscape orientation.

### State Model

Required states: MENU, PLAYING, PAUSED, GAME_OVER. Add DAY_RESULT and UPGRADE as internal screen states if useful.

### Important Entities

- Plot
- CropDefinition
- CropInstance
- StorageStack
- StallSlot
- CustomerDefinition
- CustomerInstance
- DemandForecast
- UpgradeState
- MarketDayDefinition
- FloatingFeedback

### Systems To Plan Early

- Crop growth timers and yield calculation.
- Customer preference and price tolerance.
- Shelf placement visibility bonuses.
- Revenue and reputation scoring.
- Day configuration data.
- Touch routing between garden, storage, stall, and HUD.
- Runtime art map for gameplay assets.
- Audio role assignment for BGM and SFX.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Genre family: casual management. Concrete sub-archetype: farm-to-market stall pricing and customer-flow management. This is not a plain timed orchard or flower order game because the primary pressure is stocking shelves, setting prices, and serving customer preferences under a market timer.

### Map Or Playfield Budget

- One integrated market screen split into garden zone, stall workbench, customer queue, and HUD banner.
- Minimum interactive regions: 12 crop plots, 3 stall shelves, 1 storage crate, 1 seed tray, 1 customer queue lane, 1 forecast sign.
- Terrain and props: soil plots, wooden stall counter, awning, crate area, path lane, chalkboard sign, daily special marker.
- Camera: fixed orthographic screen without scrolling for first delivery; later expansion may add alternate stall layouts.

### Entity Roster Budget

- Crops: carrot, tomato, corn, strawberry, pumpkin, herb.
- Customers: bargain shopper, chef, family buyer, premium collector, impatient commuter.
- Neutral/resource entities: seed packets, storage crates, shelf slots, price tags, trend signs.
- Effects: water splash, harvest sparkle, coin burst, reputation heart, complaint puff, sold-out marker.
- Items/powerups: quick water, display polish, bulk basket, trend hint.

### Mechanic Variety Budget

- Primary actions: plant, water, harvest, stock, price, serve customer demand.
- Secondary actions: inspect preference, use limited boost, upgrade between days, choose day route.
- Upgrade paths: plot speed, crate capacity, shelf appeal, watering cooldown, price insight.
- Day variants: crop trend day, bargain crowd day, chef order day, low-stock challenge, premium rush.
- Special rules: perishable goods lose value if held too long; shelf placement changes attention; price tiers affect conversion and reputation.
- Risk reward: premium pricing gives more coins but risks complaints; fast crops are safe but lower profit; premium crops may miss timing.
- Failure pressure: market timer, stockouts, customer patience, reputation loss.

### Forbidden Template Reuse

- Do not reuse the simple timed orchard order loop from Bloom Orchard.
- Do not reuse the flower beauty-order structure from Petal Isle Farm.
- Do not reuse the animal pen collection and cleanliness loop from Pasture Parade Deluxe.
- Do not implement as a generic crop timer with a static order board only.
- Do not use the repeated top HUD pill plus bottom command strip layout without the market stall workbench identity.

## Visual Identity Contract

### UI Layout Archetype

Split garden-and-stall workspace with upper plot grid, lower market counter, left seed tray drawer, and chalkboard target meter.

### HUD Composition

Chalkboard sign for revenue progress, small day/time plaque, reputation hearts as stamp icons, pause as an icon button. Storage and shelf controls live in the bottom workbench instead of a generic command bar.

### Playfield Safe Area

Active plot cells, shelf slots, and customer queue must remain unobstructed. HUD banner occupies reserved non-interactive space only.

### Frame Overlay Policy

Awning and wooden trim are structural background or reserved-edge decorations. No frame overlay may cover gameplay cells or touch regions.

### Palette And Material Language

Bright farm-market palette: sky blue, leaf green, tomato red, carrot orange, cream paper, and warm light wood. Materials should resemble painted wood, paper tags, chalkboard signs, and soft fabric awnings.

### Typography Direction

Rounded friendly sans-serif through platform-safe text styles. Use strong hierarchy with clear numbers for price, revenue, and time.

### UI Pack Strategy

Use skin_cartoon_light as the required foundation. Prefer a license-clear market or farm shop UI pack if available in shared_assets/ui; otherwise use UI Kit drawables with custom XML chalkboard, paper-tag, and awning motifs.

### Icon Subject And Silhouette

Overflowing produce crate with a price tag and awning backdrop. Avoid generic single crop icons.

### Forbidden Visual Reuse

- No generic flower basket icon like Petal Isle Farm.
- No orchard-only tree or beauty meter as the primary icon.
- No pasture pen or animal collection motif.
- No dark arcade or neon dashboard layout.
- No full-width bottom button strip as the primary interaction identity.

## Differentiation Note

The current registry already contains Bloom Orchard, Pasture Parade Deluxe, and Petal Isle Farm. Market Morning Farm is differentiated by making the market stall, price setting, customer tolerance, shelf visibility, and timed stock management the core loop rather than pure crop care, animal collection, or decorative order fulfillment.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm the requirements or request revisions.
