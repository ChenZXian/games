# Pocket Landlord Classic

Game ID: pocket_landlord_classic
Direction: offline single-player landlord poker game
Selected Concept: Pocket Landlord Classic
Recommended UI Skin: skin_dark_arcade

## Positioning

Pocket Landlord Classic is an offline single-player Dou Dizhu card game for players who want familiar rules, clean card readability, strong turn feedback, and a long solo progression path without network dependency. It should feel like a polished mobile card room with campaign structure rather than a generic utility card app.

## Target Feel And Player Fantasy

The player should feel like a rising table master who reads the room, chooses when to grab landlord, preserves control cards, times bombs for swing turns, and steadily climbs through increasingly sharper AI tables. The mood should be calm, confident, and competitive, with satisfying card motion and table feedback.

## Core Gameplay Loop

Enter a table challenge, review stake and objective, deal cards, call score and determine landlord, play alternating tricks against two AI farmers or one AI landlord, use hint and card sorting tools to make decisions, finish the round, earn stars and coins, then unlock the next table or optional challenge stage. The primary loop is read hand state, choose a legal combination, manage tempo and bomb value, then close the round before the opponents unload their remaining cards.

## Controls And Input Model

Tap cards to select or deselect.
Tap action buttons for Call 1, Call 2, Call 3, Pass, Play, Hint, Reselect, Auto Sort, and Speed Toggle.
Drag across a run of cards to select consecutive cards quickly.
Long press a selected group to preview the recognized pattern name.
The game should prevent illegal plays and clearly explain the current legal response when the player attempts a bad move.

## Win And Failure Conditions

Win a round by emptying the player hand first if the player is landlord, or by helping either farmer side empty first when the player is a farmer.
Lose a round when the opposing side empties first.
Win a stage by reaching a target star result or by clearing the required number of table victories in the stage set.
Fail a stage when the player loses all allowed hearts in challenge mode or cannot reach the required stars within the local chapter objective.

## Progression Structure

The campaign is split into ranked halls: Bronze Hall, Silver Hall, Gold Hall, Jade Hall, and Crown Hall.
Each hall contains 12 staged tables with escalating AI quality and table rules.
Players earn up to 3 stars per table based on winning, remaining cards, and efficiency of high-value combinations.
Coins unlock cosmetic table themes, alternate card backs, and optional assist tools such as one extra hint refresh in challenge stages.
A side mode named Endgame Drills offers curated late-round card puzzles unlocked by chapter progress.

## Level Or Run Structure

The main structure is chapter-based progression, not a roguelike run.
Each table stage has a predefined AI style package, stake level, and optional modifier such as aggressive bidding, bomb-heavy deck evaluation, or conservative pass behavior.
Every hall ends with a boss table against an elite AI personality.
Optional daily practice can be local-rotating and seeded from packaged content rather than network data.

## Economy Rewards And Upgrades

Coins are awarded for stage clears, first wins, streak bonuses, and achievement milestones.
Stars unlock future halls and bonus endgame drill packs.
The economy is soft progression only and must never alter card fairness during a live round.
Unlockables include table felt variants, chip tray skins, announcer voice packs, win flourish variants, and practice assist toggles.
No pay-to-win systems and no online currencies.

## Screen Map

### Menu

Main menu with large central Start Table button, Hall Progress button, Endgame Drills button, Collection button, and Settings button.
A small profile card shows hall badge, total stars, and win rate.

### Gameplay HUD

Top bar shows chapter, table stake, round score delta, and pause.
Center table area is the active playfield with left AI, right AI, and bottom player hand.
A compact previous-play strip sits above the center of the table to show the current pattern to beat.
Bottom action dock contains Play, Pass, Hint, Reselect, Auto Sort, and Speed controls.
Landlord badge and multiplier marker remain near the active player seat, never over the hand area.

### Pause

Resume, Restart Table, Rules, Settings, and Exit To Menu.

### Game Over

Round result panel with win or loss stamp, stars earned, bombs used, biggest pattern played, coin reward, retry, and next table.

### Extra Screens

Hall map screen.
Endgame drill selection screen.
Rules and pattern glossary screen.
Collection and cosmetics screen.
Statistics screen with role win rates and favorite patterns.

## UI Direction

### Layout Tone

Premium green felt table framed by dark lacquer wood, brass highlights, and crisp ivory card faces. The presentation should feel like a focused mobile card lounge rather than a casino overload.

### HUD Priorities

Current trick to beat, remaining card counts for all three players, landlord identity, active turn state, and legal action buttons must be readable at a glance.

### Gameplay Safe Area

Top HUD is reserved above the felt table.
Left and right AI hand zones sit outside the central trick reveal lane.
Bottom player hand zone occupies a fixed reserved band and must not be overlapped by result banners or decorative trim.
The center of the table remains open for card play animations.

### Frame And Border Policy

Use side bezels and top header outside the active card lanes.
Decorative wood frame may surround the felt table but must not intrude into the card fan zones or the center trick reveal region.

### Key Overlay Panels

Round start bid panel.
Pause panel.
Pattern glossary popover.
Result panel.
Assist explanation tooltip.

### UI Asset Strategy

Use one `skin_dark_arcade` foundation, then customize it into a card-table identity with bespoke felt textures, brass counters, dark panels, and high-contrast ivory labels. Buttons should feel tactile and rectangular, not neon sci-fi or candy-like.

## Gameplay Art Direction

Required roles are standard playing cards, landlord badge, multiplier token, hall badges, boss portraits, result stamps, table themes, chip or coin reward icons, assist icons, and turn indicators.
Camera perspective is top-down card table with slight depth on panels.
Cards must remain crisp and realistic enough for instant suit and rank recognition.
Default facing for all seat indicators points inward toward the table center.
Moving or highlighted entities such as turn indicators, bid markers, and win flourish tokens should animate with glow, scale, and slide transitions.
Minimum visual states include idle, active turn, selected cards, valid play, invalid attempt, hint highlight, landlord assigned, win, loss, and boss table presentation.
Animation tier for primary interactive pieces is polished card-motion with lift, snap, fan spread, trick throw arc, and settlement burst.
Anchor and hitbox assumptions should prioritize wide touch targets on player cards and buttons with no overlap ambiguity.
If no suitable shared gameplay art pack exists, create project-local polished table assets rather than falling back to plain prototype rectangles.

## Icon Direction

### Subject

A confident landlord crown emblem resting above a fan of three premium cards, centered around a red joker and black ace pairing.

### Silhouette

Wide fan silhouette with a distinct crown peak above it, readable at small size.

### Color Direction

Deep emerald, antique gold, ivory, and a single red accent from the joker.

### Tone

Classic, prestigious, and tabletop-focused.

## Audio Direction

### Menu BGM

Warm lounge-inspired instrumental with light strings and soft percussion, calm but confident.

### Gameplay BGM

Subtle rhythmic table theme with restrained tension that does not distract from thinking.

### Boss Or Climax BGM

Slightly faster and grander arrangement with sharper plucks and stronger percussion.

### SFX Families

Card tap, card fan, card throw, trick confirm, illegal move warning, pass, bid call, landlord assign flourish, bomb impact, rocket burst, win stamp, loss sting, menu click, and reward chime.

## Technical Implementation Notes

### Rendering Style

Android Java custom table view for the live card layout and card animation layer, with XML-based HUD and overlay panels around it.

### State Model

Need clear round state phases: dealing, bidding, landlord reveal, player turn, AI turn, trick resolution, round result, stage result.
Need rule evaluator for singles, pairs, triples, triple-with-one, triple-with-pair, straight, pair-straight, airplane, airplane-with-wings, four-with-two, bomb, and rocket.
Need AI strategy profiles ranging from beginner to elite with bidding heuristics and play heuristics.

### Important Entities

Card model.
Player seat model.
Round controller.
Pattern evaluator.
Hint generator.
AI decision engine.
Campaign progression model.
Statistics tracker.

### Systems To Plan Early

Legal pattern parser.
Card sorting and grouping system.
Hint generation and fallback.
Deterministic shuffle seeding for stage reproducibility where needed.
Animation queue for dealing and trick resolution.
AI difficulty parameter sets.
Local save for chapter progress, stars, unlockables, and stats.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Genre family is card strategy.
Concrete sub-archetype is offline Dou Dizhu campaign battler with full-round trick-taking and solo chapter progression.
It is not a reskin of any existing repository game because the primary decision is legal pattern timing and hand management, not movement, matching, aiming, lane placement, or real-time action.

### Map Or Playfield Budget

Single card-table playfield model with 5 hall themes.
At least 5 distinct table environments, 5 hall background treatments, 10 boss table decorations, and 6 result or ceremony backdrops.
Interactive regions include player hand band, center trick reveal lane, left and right opponent seats, top status band, and bottom action dock.
Functional elements include bid panel, multiplier token, remaining-card counters, active-turn marker, and last-play strip.

### Entity Roster Budget

1 player seat role.
2 opponent seat roles active every round.
5 AI personality families: cautious, greedy, bomb-hunter, tempo-control, and deceptive saver.
15 pattern categories fully supported.
8 assist and feedback token types.
10 achievement badge types.

### Mechanic Variety Budget

Primary player actions are call score, pass, play cards, request hint, sort hand, and adjust speed.
Secondary actions are inspect rules, review last trick, retry table, and enter drill mode.
Progression variety includes hall unlocks, boss tables, endgame drills, cosmetic unlocks, and achievement goals.
Rule modifiers across stages include aggressive bidding opponents, bomb-preference opponents, low-tolerance race tables, and landlord-pressure tables.
Forbidden template reuse includes turning the game into a simple card matching app, poker solitaire, or a generic casino lobby shell.

### Forbidden Template Reuse

Do not reuse the neon reactor board layout from Prism Sector Collapse.
Do not reuse candy HUD composition from Candy Pulse Parade.
Do not use top HUD pills plus bottom narrow strip with arcade action styling.
Do not replace full Dou Dizhu rules with simplified shedding-only rules.

## Visual Identity Contract

### UI Layout Archetype

Centered premium felt table with three-seat radial card staging and a bottom command dock shaped like a lacquer control rail.

### HUD Composition

Top status header, center previous-trick lane, left and right seat counters, bottom player hand band, and a wide bottom action rail.

### Navigation Model

Hub-style front menu to hall map to table encounter to result to next table or menu.

### Playfield Safe Area

Protected zones are the full player hand band, the center trick lane, and the two AI seat card stacks. HUD headers and decorative frame pieces must stay outside these play zones.

### Frame Overlay Policy

Frame visuals may wrap around the felt table perimeter only. No ornamental elements may sit on top of card stacks, selected cards, or active trick cards.

### Palette And Material Language

Emerald felt, walnut brown, antique gold, ivory card stock, charcoal overlays, and subtle red accents.
Material language is polished tabletop wood and felt rather than sci-fi glass or candy plastic.

### Typography Direction

Refined slab-serif inspired headings with clean readable body labels. Number counters should look like engraved scoreboard digits.

### UI Pack Strategy

Use `skin_dark_arcade` tokens only as baseline spacing and state system, then refine with custom table-specific panels and buttons stored in project-local assets and tracked UI assignment.

### Icon Subject And Silhouette

Crown over a three-card fan with one red joker focal point.

### Forbidden Visual Reuse

Do not use neon blue reactor motifs, candy gradients, glossy bubble buttons, or split-board sci-fi framing.
Do not reuse another game's icon subject such as gem cluster, bird, tank, or defense tower.

## Differentiation Note

Pocket Landlord Classic differs from the current registry by being a turn-based legal-pattern card battler with campaign halls, deterministic AI personalities, and card-table presentation. Its interaction model is deliberate selection and rule evaluation rather than touch movement, aiming, matching, route control, or wave defense.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
