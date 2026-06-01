# Petal Isle Farm

## Game Positioning

Petal Isle Farm is a cozy and decorative farm management mini-game inspired by social farming games, but centered on flower beauty, garden styling, and short-session order fulfillment. The player restores a flower island estate by planting visually rich crops and blossoms, maintaining them through simple care actions, and shaping a high-value ornamental farm that feels lively and rewarding.

## Target Feel And Player Fantasy

The target feel is bright, soft, tidy, and satisfying. The player fantasy is becoming the owner of a dream flower farm where every plot can turn into a pretty, profitable showcase. The game should feel welcoming for casual players, with a strong visual payoff whenever plants bloom, paths are decorated, or visitor orders are completed.

## Core Gameplay Loop

1. Plant flowers, herbs, and boutique fruit crops on prepared plots.
2. Check growth timers and perform care actions such as watering, fertilizing, and pest clearing.
3. Harvest produce and beauty-rated blossoms.
4. Fulfill rotating visitor and market board orders.
5. Earn coins, petals, and reputation stars.
6. Unlock new seed families, decorative structures, and additional farm zones.
7. Reorganize the farm to improve both productivity and beauty score.

## Controls And Input Model

- Tap a plot to inspect, plant, or harvest.
- Drag a seed packet onto a valid plot to plant quickly.
- Tap contextual care buttons for water, fertilizer, or pest spray.
- Drag decorative items from an inventory tray onto decoration anchors.
- Swipe left or right to pan between island zones.
- Tap visitor order cards to view requirements and submit items.

## Failure Conditions And Win Conditions

There is no hard fail for the overall estate. A session can have soft failure through missed premium orders, wilted flowers losing beauty rank, or an event day ending before target reputation is reached. Success comes from clearing progression goals, restoring all island zones, unlocking the full flower collection, and reaching high estate beauty milestones.

## Progression Structure

Progression is chapter based and estate driven.

- Chapter 1: Starter meadow and basic flowers.
- Chapter 2: Pond garden and pollinator unlocks.
- Chapter 3: Orchard corner and premium bouquet orders.
- Chapter 4: Hillside greenhouse terrace and rare flowers.
- Chapter 5: Festival plaza and late-game prestige beautification.

Each chapter unlocks:

- new plant families
- new decorative sets
- higher-value visitor types
- more advanced order board requests
- additional passive bonuses from beauty score thresholds

## Level Structure Or Run Structure

The game uses persistent farm management instead of isolated runs.

- Main mode is a persistent island estate.
- Sessions are designed for 2 to 8 minutes of meaningful progress.
- Timed event tasks provide short goals such as bloom chains, beauty targets, or rush deliveries.
- The order board refreshes on a predictable cadence to create recurring objectives.

## Economy, Rewards, Drops, Or Upgrade Model

Primary currencies:

- Coins for seeds, tools, and expansions.
- Petals for decorative unlocks and premium beautification upgrades.
- Reputation stars for chapter progression and visitor unlocks.

Reward sources:

- crop harvests
- beauty bonuses on perfect blooms
- order board completions
- chapter objectives
- daily sign-in style milestone rewards

Upgrade model:

- watering can efficiency
- fertilizer storage
- pest control capacity
- greenhouse bonus slots
- plot beauty amplifiers such as archways, fences, and flower beds

## Gameplay Diversity And Content Budget

- Genre family: casual management
- Concrete sub-archetype: decorative flower farm with beauty-rated harvests
- Why it is not a reskin: the primary value loop mixes crop growth, visual beauty scoring, decorative placement, and rotating boutique orders rather than plain timed orchard harvesting
- Map model: a persistent side-scroll island estate composed of themed zones
- Minimum map regions: 5 themed zones with unique plot layouts and decoration anchors
- Terrain and landmark variety: meadow, pond edge, orchard rise, greenhouse terrace, festival square, bridges, stone paths, pergolas, water wheels
- Player action variety: plant, water, fertilize, clear pests, harvest, submit orders, place d茅cor, reposition d茅cor
- Roster targets: 20 plant types, 8 decorative structure families, 6 visitor order archetypes, 5 event task patterns
- Progression targets: 5 chapters, 3 beauty milestone tiers per zone, 4 tool upgrade tracks
- Animation expectations: bloom opening, leaf sway, watering sparkle, pest pop, harvest bounce, visitor arrival feedback
- Asset-pack variety plan: a dedicated pastel botanical set for crops and flowers plus a separate garden d茅cor family for structures and props
- Forbidden template reuse: no small orchard-only grid, no plain produce-only economy, no generic barn-centric ranch loop, no reuse of Bloom Orchard map pacing or order presentation

## Visual Identity Contract

- UI layout archetype: left-side vertical estate task rail with top-right floating resource badges and a bottom-right contextual tool bloom wheel
- HUD composition: beauty score flower badge, coins, petals, reputation stars, active order shortcut, chapter goal card
- Navigation model: persistent estate screen with slideable zones and lightweight overlay panels
- Playfield safe area: gameplay plots occupy the center and lower-middle field while badges remain on the top corners and task rail stays on the far left gutter
- Frame overlay policy: no decorative border may overlap live crop plots or decoration anchors
- Palette signature: cream white, leaf green, coral pink, sky aqua, and sunflower yellow accents
- Material language: rounded painted wood signs, ceramic tags, glassy dew chips, and floral cutout panels
- Typography style: playful rounded display headings with clean readable body labels
- UI pack strategy: use one primary cartoon-light floral UI direction with petal motifs and garden signage
- Icon subject: a smiling flower basket with a bloom burst
- Icon silhouette: rounded basket body with three large blossom heads forming a crown
- Icon palette and background: pink, yellow, and mint blossoms over a bright sky-blue circular background
- Forbidden visual reuse: no repeated top-pill-plus-bottom-strip HUD, no military or neon frame logic, no reused orchard badge layout, no generic crate or fruit-only icon

## Screen Map

- Main Menu
- Estate Gameplay HUD
- Order Board Overlay
- Seed Shop Overlay
- Decoration Inventory Overlay
- Chapter Goals Panel
- Pause Panel
- Event Summary Panel
- Result Or Milestone Celebration Panel
- Collection Almanac Screen

## UI Direction

- Recommended ui_skin: skin_cartoon_light
- Layout tone: airy, floral, layered, and cheerful
- HUD priorities: beauty score first, order access second, core currencies third
- Playfield safe area: top corners reserved for compact floating chips, left gutter for tasks, bottom-right for context tools, center and lower center protected for farm interaction
- Frame and border policy: use soft signage panels outside the plot grid and avoid thick borders on the live garden
- Key overlay panels: order board, seed catalog, d茅cor drawer, milestone celebration card, chapter objective panel

## Gameplay Art Direction

- Required art roles: flower crops, herb crops, boutique fruit crops, tilled plots, watered plots, decorative fences, archways, benches, fountains, bridges, butterflies, bees, visitors, tool effects, milestone celebration props
- Camera perspective: angled top-down with a soft toy-like farm presentation
- Suggested shared pack family or source tier: pastel cartoon botanical pack for crops and a matching whimsical garden d茅cor pack for structures
- Default facing and facing behavior: visitors and pollinators use left-right facing based on travel direction; static crops do not require facing; ambient butterflies should mirror direction shifts
- Minimum visual states: seed, sprout, mid-growth, bloom-ready, bloom-perfect, wilted, harvested, watered, fertilized sparkle, pest-attacked indicator
- Movement animation expectation: visitors walk with alternating steps, bees hover with bobbing loops, butterflies flap while drifting, petals float during milestone celebrations
- Animation quality tier: medium for crops and high for hero props such as rare flowers, visitors, and celebration moments
- Anchor, hitbox, and z-order assumptions: crops anchor at soil center, tall d茅cor anchors at rear tile edge, visitors render above path props and below foreground canopy trims
- Visual readability constraints: harvestable states must read clearly without obscuring plot ownership, decoration silhouettes must not hide crop state icons, flower rarity should be visible by petal shape and glow cues
- Fallback rule if no suitable pack exists: use project-local stylized prototype art only as temporary draft support and replace before delivery-ready inspection

## Icon Direction

- Subject: smiling woven flower basket with three hero blossoms and one leaf ribbon
- Silhouette: wide rounded basket base with blossoms rising in a triangular crown
- Color direction: coral pink, butter yellow, mint green, and sky blue
- Tone: premium cute and polished rather than toy-flat

## BGM Direction

- Menu music mood: gentle morning garden melody with light bells and soft acoustic plucks
- Gameplay loop mood: breezy upbeat pastoral loop with warm rhythm and calm charm
- Boss or climax mood if applicable: festival celebration variation with brighter percussion and a fuller melody for milestone completions

## Technical Implementation Notes

- Rendering style: farm playfield rendered in a custom Java view with separate XML overlay HUD and panels
- State model: MENU, PLAYING, PAUSED, ORDER_BOARD, SHOP, MILESTONE, GAME_OVER or DAY_END style summary
- Important entities: plot, plant definition, planted crop instance, order card, visitor, decoration anchor, placed d茅cor item, event objective
- Special systems to plan early: growth timer simulation, beauty score calculation, order refresh logic, zone scrolling, decoration placement validation, save data schema, flower state visualization pipeline

## Differentiation Note Against Current Registry

Petal Isle Farm differs from Bloom Orchard by centering flower beauty score, decorative estate composition, and boutique visitor orders instead of a mostly productivity-driven orchard grid. It also differs from Pasture Parade Deluxe by using plant care and ornamental land design rather than animal pen management.
