# Ring Prefecture Clash

Game ID: ring_prefecture_clash
Direction: Inspired by prefecture and city conquest strategy games with round battle units and bright tabletop-style territorial warfare.
Selected Concept: Nationwide node-link conquest across prefectures and major cities with auto-growing forces, visible front-line streams, and supply-cut tactics.
Recommended UI Skin: skin_cartoon_light

## Positioning

Ring Prefecture Clash is a short-session real-time territorial conquest game for mobile. The player commands a faction represented by round battle units that pour between prefecture capitals and city nodes across stylized Japan-inspired regional maps. The game should feel readable, playful, and tactical rather than realistic, with satisfying swings created by route pressure, reinforcement timing, and supply denial.

## Target Feel And Player Fantasy

The player fantasy is becoming a nimble campaign commander who wins by reading the map faster than the opponent. The feel should combine toy-soldier charm, map-table clarity, and constant territorial momentum. Every line the player draws should cause visible waves of round troops to surge, collide, stall, and break through.

## Core Gameplay Loop

Start a match on a regional conquest map with multiple prefecture and city nodes already divided between factions. Friendly nodes generate round units over time up to a capacity limit. The player taps and drags from one owned node to one or more connected targets to send a percentage of stationed forces. Attacking an enemy node drains defenders through unit attrition at the target. Capturing neutral logistics nodes boosts growth or mobility. Cutting enemy reinforcement routes creates isolated fronts that are easier to collapse. Winning a stage requires capturing every enemy stronghold or forcing the rival command node to fall.

## Controls And Input Model

Primary input uses single-finger tap and drag. Tap a node to inspect its growth rate, capacity, and route options. Drag from a selected owned node to a connected node to dispatch a troop stream. Drag across multiple owned nodes in sequence to issue chained pressure routes. Double tap a selected node to send an all-in surge. A bottom-right command toggle switches between standard send ratio and reserve-safe send ratio. Pause, speed control, and tactical skill buttons remain outside the protected map space.

## Win And Failure Conditions

The player wins by capturing all hostile command nodes or achieving full territorial control before the enemy recovers. The player loses if all owned production nodes are captured or if the home command node falls with no remaining active reinforcements. Bonus objectives include fast-clear time, low-loss clears, and full regional bonus capture.

## Progression Structure

Progression uses a chapter map divided into six macro regions inspired by different parts of Japan. Each chapter unlocks new node rules, environmental modifiers, and enemy commander behaviors. Between stages the player invests campaign medals into three persistent branches: logistics, morale, and command. Logistics improves movement speed and route durability. Morale improves unit growth and comeback bonuses. Command improves active tactical skills and reserve efficiency.

## Level Or Run Structure

The game is stage-based rather than endless. The full launch target is 40 campaign stages split across six regions, with each region introducing a different map shape and tactical pressure profile. Early stages teach growth, pathing, and timing. Mid-game introduces chokepoints, ferries, mountain passes, split capitals, and temporary neutral forts. Late stages add elite enemy commanders, event modifiers, and asymmetrical starting positions.

## Economy Rewards And Upgrades

The main reward currency is campaign medals earned from stage completion, side goals, and chapter clears. Stages also grant region stamps used to unlock optional maps and cosmetic unit variants. In-battle temporary bonuses come from capturing specialty nodes such as rail junctions, watchtowers, or supply depots. Persistent upgrades stay broad and low-complexity so the match decision-making remains the star.

## Screen Map

### Menu

Main menu with chapter entry, progression summary, medal totals, commander profile, settings, and help.

### Gameplay HUD

Top-left region card with stage title, objective, and current front pressure rating. Top-right compact controls for pause, speed, and audio. Left-side vertical commander skill pips. Bottom-left selected node information card. Bottom-right command pad for send ratio mode and surge button. Center map area remains structurally reserved for active nodes, routes, and unit traffic.

### Pause

Resume, restart, settings, help, and leave-stage options in a floating panel outside the gameplay-safe center.

### Game Over

Result panel with victory or defeat banner, medals earned, objective breakdown, map replay, and next-stage shortcut when unlocked.

### Extra Screens

Chapter map, upgrades screen, unit codex, regional bonus checklist, and tutorial overlays.

## UI Direction

### Layout Tone

Bright command-table presentation with paper-map softness, chunky tactical widgets, and toy-commander readability.

### HUD Priorities

Primary HUD priority is map clarity and route readability. Secondary priority is selected-node detail. Tertiary priority is commander skills and speed control. Nothing may sit directly over contested nodes or route intersections.

### Gameplay Safe Area

The active playfield occupies the center band with protected margins around dense route clusters. Top bars stay shallow. Side skill rail uses reserved dead space. Bottom information cards anchor below the route mesh rather than floating over it.

### Frame And Border Policy

No decorative bezel may overlap nodes, route lines, or unit streams. Any map frame must be baked into surrounding dead space. Large battle-state controls must live in dedicated side or bottom containers.

### Key Overlay Panels

Selected node card, stage objective card, commander skill rail, pause dialog, tutorial spotlight cards, and end-of-stage result panel.

### UI Asset Strategy

Use skin_cartoon_light as the token base. The downstream UI workflow should target a board-game travel atlas motif with soft blue route labels, cream paper cards, rounded tactical chips, and small stamp-like badges. Avoid neon lines, bottom-only command strips, or military steel panels already common in other projects.

## Gameplay Art Direction

### Required Art Roles

Regional map backgrounds, prefecture command nodes, neutral towns, supply hubs, rail hubs, ferry ports, route overlays, round player units, round enemy units, elite commander tokens, capture burst effects, route pulse effects, morale aura effects, and region-specific landmark props.

### Camera Perspective

Top-down stylized campaign map with slightly tilted tabletop readability, but unit motion remains on a flat 2D plane.

### Suggested Shared Pack Family Or Source Tier

Prefer a shared gameplay art pack built around cartoon map icons and round chibi war tokens. If the shared library lacks a suitable pack, import or generate a license-clear pack specifically for circular battle units and clean regional node icons before final delivery. The round battle units are mandatory presentation, not optional polish.

### Default Facing And Facing Behavior For Moving Or Attacking Entities

Round battle units face their travel direction using eye position, tiny helmet tilt, flag angle, or weapon side bias. When streams reverse or retarget, their facing updates immediately. Elite commander tokens should show a stronger directional silhouette. Ferry and rail transfer units should rotate toward route exit direction.

### Minimum Visual States And Animation Expectations

Basic round units need idle bob, move bounce, attack bump on contact, hit recoil, capture cheer, and defeat pop-out. Elite commander tokens need idle, move, pressure pulse, and defeat states. Node ownership changes need flag flip and color burst feedback.

### Animation Quality Tier For Primary Entities

Medium. Primary round battle units should use at least two-frame locomotion bounce plus directional facing cues. Contact attrition should visibly pulse rather than rely on invisible number subtraction.

### Anchor, Hitbox, And Z-Order Assumptions For Primary Entities

Units use centered anchors, circular collision footprints, and route-lane offsets so streams stay visually legible. Nodes sit below unit sprites but above the map base. Effects render above units only for short bursts.

### Visual Readability Constraints

Node ownership, route direction, and stream density must remain legible on small phones. Unit colors need strong faction contrast without losing the cheerful toy-war tone. Overlapping streams require lane offsets or micro-banding to avoid becoming a single blob.

### Fallback Rule If No Suitable Pack Exists

If no suitable shared or imported pack exists, the gameplay art workflow must generate a project-specific round-battle-unit pack and supporting map icons with tracked provenance. Placeholder circles alone are not acceptable for delivery-ready output.

## Icon Direction

### Subject

A determined round prefecture atlas ticket token over a regional map pin.

### Silhouette

Large circular face-token in the foreground with a diagonal flag or baton, backed by a bold map-pin or prefecture crest shape.

### Color Direction

Sky blue, warm red, cream white, and gold accent with a map-table paper backdrop.

### Tone

Friendly, competitive, and tactical rather than grim or realistic.

## Audio Direction

### Menu BGM

Light strategic march with plucked strings, whistle motifs, and upbeat campaign-table energy.

### Gameplay BGM

Rhythmic tactical pulse with snare, pizzicato motion, and rising brass accents as fronts intensify.

### Boss Or Climax BGM

Faster regional showdown arrangement with stronger percussion and urgent call-and-response motifs.

### SFX Families

Soft route draw ticks, troop launch puffs, node capture pops, stream collision taps, morale surge stingers, medal reward chimes, and paper-stamp UI confirms.

## Technical Implementation Notes

### Rendering Style

Use a custom GameView or SurfaceView for the map, routes, node ownership, and moving unit streams. Menus and HUD should use Android Views and XML overlays aligned around the map container.

### State Model

Core states are MENU, STAGE_SELECT, PLAYING, PAUSED, RESULT, and UPGRADES. Match state needs node ownership, unit counts, growth timers, active streams, active skills, and AI intent queues.

### Important Entities

CommandNode, CityNode, NeutralNode, SupplyHubNode, RailHubNode, FerryPortNode, TroopStream, CommanderSkill, RegionalModifier, and FactionProfile.

### Systems To Plan Early

Graph pathing between connected nodes, stream merging and splitting, route rendering under congestion, AI pressure heuristics, node capacity rules, chapter progression persistence, and adaptive touch targeting for dense route clusters.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Real-time territorial conquest with graph-node reinforcement warfare and supply-cut tactics. This is not a side-view lane pusher, not a transport puzzle, and not a pure logistics game.

### Why It Is Not A Reskin Of An Existing Game

The project differs from territory_swarm by shifting from neon geography conquest into bright tabletop campaign presentation, denser route-pressure gameplay, explicit supply isolation, multi-node chain commands, and mandatory round battle unit identity. It also differs from lane-based war games because there are no fixed horizontal lanes, only a dynamic region graph.

### Map Or Playfield Budget

The campaign requires at least 40 authored stage graphs across six regional themes. The graph library must include compact urban clusters, coastal ferry chains, mountain choke webs, twin-capital maps, ring-road maps, and branch-heavy inland maps. At least 10 landmark families should appear across the map set, including castles, ports, towers, depots, bridges, tunnels, stations, shrines, dams, and industrial centers.

### Entity Roster Budget

The roster includes at least three friendly round unit visual families, three enemy round unit visual families, six enemy commander token identities, five node function types, four region modifier types, six effect families, and three active commander skill families.

### Mechanic Variety Budget

Core mechanics include growth pressure, route timing, isolation capture, specialty node bonuses, reserve-safe sending, all-in surge, chapter modifiers, and commander skills. At least eight map rule variants are required across the full campaign, such as fogged intel nodes, temporary neutral revolts, ferry-only links, rail speed routes, fortified capitals, morale bleed zones, storm-slowed routes, and reversible bridge control.

### Forbidden Template Reuse

Do not reuse the exact top-pill plus bottom-strip HUD pattern used by other strategy projects. Do not reuse a single tiny three-path national map for all chapters. Do not reuse generic static circles with no facing, no bounce, and no contact feedback. Do not collapse the design into one-unit auto-capture taps or a lane war reskin.

## Visual Identity Contract

### UI Layout Archetype

Top corners for light command metadata, left vertical tactical rail, bottom anchored node card, and a broad unobstructed central atlas.

### HUD Composition

Objective card at upper left, compact utility cluster at upper right, slim commander rail on the left edge, contextual node panel at lower left, and command-mode controls at lower right.

### Navigation Model

Chapter map progression with region cards, stage nodes, and post-stage result ladders. In-match UI is minimal and context-driven.

### Playfield Safe Area

The center 70 percent of the landscape screen is protected for nodes and unit routes. Corners and outer bands are reserved for HUD. No floating action button may sit over major node clusters.

### Frame Overlay Policy

Use structural gutters and panel containers instead of decorative overlays. Map borders may be ornamental only in external margins.

### Palette And Material Language

Cartoon-light base with sky, cream, and ink-blue travel-atlas tones, paper-card panels, stamp badges, route ribbons, and polished enamel chips.

### Typography Direction

Rounded, compact, travel-poster-inspired sans serif styling with bold HUD numerals and softer chapter labels.

### UI Pack Strategy

Start from skin_cartoon_light tokens, then refine with a custom atlas-board motif pack. Prefer shared UI resources that support paper tabs, transit-map labels, and stamp seals. Avoid sci-fi glow and heavy metal framing.

### Icon Subject And Silhouette

Foreground round prefecture atlas ticket token, backed by a regional map pin and territorial burst accents.

### Forbidden Visual Reuse

Do not reuse the neon black-magenta palette of territory_swarm. Do not reuse a military steel dashboard. Do not reuse generic swords, shields, towers, or castles as the icon subject. Do not use the same HUD pill chain and bottom command strip seen in previous war projects.

## Differentiation Note

Ring Prefecture Clash should read as a cheerful tabletop campaign game built around route pressure and round troop flow. Its identity relies on visible unit streams, regional graph diversity, and a bright atlas-like presentation rather than on realistic war themes or pure map skinning.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
