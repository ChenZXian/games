# Black Spade Alliance

Game ID: black_spade_alliance
Direction: Custom four-seat shedding card game with alliance cards, one human player, three AI players, five-round score match
Selected Concept: Black Spade Alliance
Recommended UI Skin: skin_dark_arcade

## Positioning

Black Spade Alliance is a four-seat custom card duel for Android built around a hidden-alliance twist, a one-versus-three edge case, and a fixed five-round score match. It translates a table card game into a fast, readable mobile format with strong rule clarity and short competitive sessions.

## Target Feel And Player Fantasy

The player should feel like a sharp card-table reader who can detect alliances, protect a partner, control lead tempo, and steal points through timing. The match should feel tense but readable, with each round clearly swinging the five-round scoreboard.

## Core Gameplay Loop

Start a five-round match, deal a full no-joker deck, resolve alliance ownership from the spade 3 and spade A, take turns playing legal sets or passing, let the last uncontested play reopen the lead, continue until finish order determines the round score, then advance to the next round and repeat until the fifth-round result.

## Controls And Input Model

The player taps cards to select them, taps Play to submit a legal combination, taps Pass when trailing a live table combination, taps card groups or the rule button for rule hints, and uses menu buttons for pause, restart, and return to the menu. Selected cards lift visually and legal plays enable the Play button automatically.

## Win And Failure Conditions

A match lasts exactly five rounds. The player wins the match by ending with a higher total than their starting score baseline or by reaching the best score tier. A single round is favorable when the player side goes out ahead of the opposing side, or when the player wins a solo one-versus-three round. A round is unfavorable when opponents take the lead order and the player gains no points.

## Progression Structure

The initial release uses fixed rules and no persistent economy. Progression comes from five-round scoreboard swings, a light AI difficulty ramp across rounds, post-match best-score tracking, and a later-ready hook for harder AI table personalities.

## Level Or Run Structure

Each match contains five rounds. Each round has dealing, alliance reveal, turn sequence, round result, and match score update. The fifth round escalates presentation and scoreboard tension without changing the core rules.

## Economy Rewards And Upgrades

There is no currency or upgrade tree in the first version. Rewards are round points, match-total score, best-score memory, and a clear rematch call to action.

## Screen Map

### Menu

Title, start match, rules, difficulty, best score.

### Gameplay HUD

Round index, total score, alliance status, current table play, remaining card counts, player hand, Play button, Pass button, pause, mute.

### Pause

Resume, restart match, return to menu.

### Game Over

Final total score, five-round breakdown, solo-round result badge, rematch.

### Extra Screens

Rules overlay and round result overlay.

## UI Direction

### Layout Tone

A competitive felt-table presentation with dark glass panels, warm score accents, ivory cards, and clear red-black suit contrast.

### HUD Priorities

Current live table combination first, player hand readability second, total score and round index third, alliance indicator fourth.

### Gameplay Safe Area

The center playfield remains reserved for played cards and lead resolution. HUD elements stay on the corners and lower safe edge. Decorative frame treatment stays outside touch-critical card zones.

### Frame And Border Policy

Allow a subtle metallic or wooden outer rim around the table only in the dead edge space. Do not place heavy bezels or bottom strips on top of the active card table.

### Key Overlay Panels

Rules dialog, round-result board, final-result board, alliance marker card, pause panel.

### UI Asset Strategy

Use one dark-arcade UI skin with a dedicated tracked shared UI pack assignment, then refine the playfield with project-local card-table visuals so the layout stays distinct from existing action HUD templates.

## Icon Direction

### Subject

Spade 3 and spade A crossing above a green felt center.

### Silhouette

Two tilted cards forming a clean X shape with a circular score ring behind them.

### Color Direction

Deep green, ivory white, black suit marks, and a restrained gold accent.

### Tone

Tactical, sharp, readable, and card-table competitive rather than casino flashy.

## Audio Direction

### Menu BGM

Low-key suspense with light jazz pulse and soft percussion.

### Gameplay BGM

Steady, thoughtful rhythm that supports card reading without crowding the play state.

### Boss Or Climax BGM

A slightly tenser loop for the fifth round and solo one-versus-three pressure moments.

### SFX Families

Deal flick, card select, card slap, pass cue, score gain pulse, round clear, final tally.

## Technical Implementation Notes

### Rendering Style

Custom card-table rendering in a dedicated view with Android View overlays for menu and result panels.

### State Model

Menu, dealing, player_turn, ai_turn, round_result, match_result, paused.

### Important Entities

Card, card suit, card rank, play combination, player seat, alliance resolver, trick controller, score controller, AI policy set, match controller.

### Systems To Plan Early

Custom rank comparator with 3 high and 4 low, legal combination recognition for single pair triple flush and full-house style three-with-two, turn reopening rules, alliance detection, finish-order scoring, AI cooperation logic, five-round flow, and visible rule explanation.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Genre family: card strategy.
Sub archetype: four-seat partnership shedding game with alliance-anchor cards and solo-versus-team edge case.

### Map Or Playfield Budget

One circular four-seat felt table with four distinct seat zones, one central live-play zone, one scoreboard zone, one rules zone, and three presentation modes for normal round, pressure round, and final tally.

### Entity Roster Budget

One human seat, three AI seats with distinct play tendencies, one full 52-card deck, one alliance resolver, one scoreboard controller, one result timeline, and one set of card and HUD presentation assets.

### Mechanic Variety Budget

Legal singles, pairs, triples, flush, three-with-two, pass and reopen flow, alliance cooperation, solo one-versus-three branch, score outcome variants for over one, over two, and solo win, AI tempo shift between early and late rounds, and final-round pressure emphasis.

### Forbidden Template Reuse

Do not simplify the game into a generic race-to-empty-hand mode, do not remove the spade 3 plus spade A alliance logic, do not replace flush and three-with-two with unrelated standard rule sets, and do not reuse the repository top-pill-plus-bottom-strip combat HUD pattern.

## Visual Identity Contract

### UI Layout Archetype

Four-edge table ring layout with a centered live-play pool and an arced bottom hand fan for the player.

### HUD Composition

Round and total score on the upper left, alliance and lead marker on the upper right, seat counts around the rim, main card action row on the lower edge, and transient result cards in the center after each round.

### Playfield Safe Area

The center sixty percent stays clear for active cards and finish-order feedback. The lower hand fan gets its own reserved zone. Corner HUD remains compact and non-occluding.

### Frame Overlay Policy

Outer trim may decorate only the dead outer boundary. No opaque overlays may cover the card center while turns are live.

### Palette And Material Language

Dark felt, glassy dark panels, gold score tabs, ivory cards, black and crimson suit symbols, and narrow scoreboard text.

### Typography Direction

Condensed sport-scoreboard styled sans with strong numeric readability.

### UI Pack Strategy

Track the selected shared UI pack in project and library metadata, then use project-local card-table styling and token tuning to avoid looking like an action or tower-defense screen.

### Icon Subject And Silhouette

Crossed spade 3 and spade A over a circular score medallion on deep green felt.

### Forbidden Visual Reuse

Do not use sword, tower, shield, zombie, convoy, or generic fantasy motifs. Do not use bottom command strips or combat meters from other repository games. Do not use a generic fan of random playing cards as the icon.

## Differentiation Note

This project is distinct from the existing registry because its core loop is turn-based shedding and alliance reading rather than movement, aiming, building, routing, or wave survival. Its identity rests on custom rank order, anchor-card team rules, finish-order scoring, and a five-round card-table scoreboard.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
