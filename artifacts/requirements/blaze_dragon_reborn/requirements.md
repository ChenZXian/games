# Blaze Dragon Reborn

Game ID: blaze_dragon_reborn
Direction: Side-scrolling fantasy brawler inspired by classic combo-driven action games with fluid chaining, beautiful chi blasts, attractive heroes, and premium UI presentation
Selected Concept: Blaze Dragon Reborn
Recommended UI Skin: skin_dark_arcade

## Positioning

Blaze Dragon Reborn is a chapter-based side-scrolling fantasy beat-em-up built for short but high-intensity sessions on Android. The game targets players who want the feel of classic arcade lane-clearing action with smoother combo routing, stronger chi-blast spectacle, handsome hero presentation, and a polished premium interface instead of a plain prototype HUD.

## Target Feel And Player Fantasy

The player should feel like a fast elite martial hero cutting through enemy formations with uninterrupted chains, air juggles, launchers, and radiant chi finishers. Hits should feel adhesive rather than slippery, with enough hit-stop, pull, lift, and cancel windows to make attack strings look intentional. The fantasy is not survival horror or loot grind first; it is stylish battlefield dominance with attractive heroes and explosive martial arts energy.

## Core Gameplay Loop

Enter a chapter stage, advance through a side-scrolling combat route, engage compact enemy packs, build chi through clean combo execution, spend chi on crowd-clearing martial blasts or launcher follow-ups, survive a mid-stage elite encounter, then defeat a chapter boss to earn rating rewards, unlock move upgrades, and open the next route. A typical minute-to-minute loop is move into a combat gate, open with grounded chain attacks, convert to launcher or dash follow-up, cash out with a chi art, gather drops, then push to the next encounter.

## Controls And Input Model

The game uses touch-first landscape controls with a left virtual stick and right-side action cluster. Primary actions are light attack, heavy attack, jump, dodge, and chi art. Light attack forms the base combo string. Heavy attack acts as launcher, knockback, or armor-break depending on timing. Jump supports aerial continuation. Dodge provides invulnerability frames and animation cancel utility. Chi art consumes one or more stored chi segments. Long-press heavy or chi art may unlock variant finishers later in progression. Inputs must support buffering and combo chaining so that attack flow remains continuous on mobile.

## Win And Failure Conditions

The player wins a stage by clearing all gated encounters and defeating the stage boss or final elite target. Bonus star goals come from clear time, combo rating, and remaining life. Failure occurs when the hero life bar reaches zero or when the player fails a chapter-specific protection objective in special stages such as escort shrine carts or defending a seal pillar. Continues are allowed at checkpoint cost in normal mode, while challenge mode requires full clean clears.

## Progression Structure

Progression is chapter based with persistent hero growth. The launch scope uses one main hero with a defined upgrade tree and two additional heroes planned as post-core content targets, not required for first implementation. The player earns gold, crest shards, and technique scrolls. Gold upgrades baseline attack, vitality, and chi capacity. Scrolls unlock combo branches, aerial extenders, dodge cancels, and new chi arts. Crest shards unlock cosmetic portrait frames, menu art variants, and eventual hero roster expansion. Stage ratings unlock side challenge stages and mastery objectives.

## Level Or Run Structure

The first delivery target should include six chapters. Each chapter contains three combat stages and one boss stage, for a minimum of twenty-four playable stage nodes if challenge variants are counted, or eighteen core combat scenes plus six boss scenes in the strict base path. Stage spaces should vary between temple roads, cliff bridges, burning courtyards, ruined fort halls, moonlit shrine terraces, and volcanic gate approaches. Each stage uses several short combat arenas linked by traversal strips so the game feels like forward momentum rather than a single static arena.

## Economy Rewards And Upgrades

Normal enemies drop small spirit motes and occasional gold. Combo finish ratings award bonus chi essence at encounter end. Elites drop technique scroll fragments. Bosses drop guaranteed crest caches and one move upgrade token. The upgrade model should branch into three combat lanes: combo extension, chi mastery, and survivability. Combo extension increases cancel windows and unlocks air chain routes. Chi mastery increases segment gain, blast variety, and visual finisher scale. Survivability improves guard break resistance, recovery speed, and emergency burst availability. Shops are not diegetic merchants; upgrades are applied in a chapter preparation screen between stages.

## Screen Map

### Menu

The main menu presents the featured hero on a dramatic angled character stage with a right-side chapter panel, a lower-left start card, and compact secondary buttons for training, upgrades, codex, and settings. The menu should feel premium and character-forward, not like a generic list.

### Gameplay HUD

Gameplay HUD includes hero life, chi segments, combo count, encounter objective text, pause button, and contextual boss indicator. HUD must leave the active combat lane visibly open in the center.

### Pause

Pause includes resume, restart, controls reference, audio toggles, and stage objective reminder in a framed modal panel outside the critical combat viewport.

### Game Over

Game over shows chapter art, performance summary, rewards earned, retry, and return to chapter hub.

### Extra Screens

Required extra screens are chapter select, hero upgrade screen, move list and training room, boss intro splash, results screen, and collection codex for enemies and techniques.

## UI Direction

### Layout Tone

The layout tone is premium fantasy arcade with dark lacquer panels, sharp glowing cuts, ornate dragon-line dividers, and bright cyan-gold action highlights against a deep midnight base. The overall presentation should feel more like a stylish action game lobby than a utilitarian mobile template.

### HUD Priorities

Primary HUD priority is preserving combat readability. Life and chi sit at the upper left in a stacked crest cluster. Combo count and style rank bloom at upper right. Objective text sits near top center during combat start then fades to a smaller anchor. Touch buttons stay in lower left and lower right dead zones with generous spacing.

### Gameplay Safe Area

The active combat lane occupies the central horizontal band and must remain free from decorative borders and thick HUD chrome. Reserve slim top space for bars, bottom corners for controls, and keep the center sixty percent width and middle fifty percent height protected for enemies, launch arcs, and chi projectiles.

### Frame And Border Policy

Frames and ornamental dragon motifs belong in side gutters, menu panels, and result cards only. No heavy bezel or frame overlay may sit over moving enemies, jump arcs, or touch-critical dodge space. Any ornate border must be structurally outside the playfield.

### Key Overlay Panels

Key overlay panels are chapter intro cards, boss warning banner, pause modal, results breakdown card, and upgrade selection cards. Panels use the chosen skin tokens with a game-specific dragon crest header treatment.

### UI Asset Strategy

The base token system uses skin_dark_arcade. The UI workflow should pair it with one fantasy martial asset pack staged through shared_assets/ui if the local shared library has a suitable pack with metallic trims and slash-shaped accents. If shared assets are weak, import one license-clear pack into shared_assets/ui and refine it project-locally. The UI must not end as token-only plain rectangles.

## Gameplay Art Direction

### Required Art Roles

Required art roles are hero body sprites, weapon trails, chi projectiles, enemy infantry families, enemy heavy units, agile aerial enemies, elite minibosses, chapter bosses, breakable props, stage hazards, shrine devices, and chapter backgrounds.

### Camera Perspective

The game uses a side-scrolling side-view perspective with a slightly elevated lane read so enemies can stack in depth without losing hit clarity.

### Suggested Shared Pack Family Or Source Tier

The gameplay art workflow should first search style-matched fantasy martial sprite packs in shared_assets/game_art, then import a free and license-clear pack with humanoid action frames if coverage is insufficient. The target style is hand-painted or richly shaded pixel-fantasy, not plain stick figures or geometric placeholders.

### Default Facing And Facing Behavior

The hero and humanoid enemies default to right-facing in forward progression but must flip instantly based on movement and target orientation. Airborne attacks, dash slashes, and chi casts should align to facing direction, with select large chi arts using forward cone or full-screen directional sweeps.

### Minimum Visual States And Animation Expectations

Primary hero states must include idle, walk, run, light combo 1 to 4, heavy strike, launcher, jump rise, jump fall, aerial combo, dodge, hurt, knockdown, recovery, chi cast startup, chi cast release, and victory. Standard enemies need at least idle, move, attack, hurt, knockdown, and death. Bosses need windup, active attack, recovery, stun, and phase transition states. Movement should visibly alternate steps. Attacks should visibly wind up and recover, not only slide a bitmap.

### Animation Quality Tier For Primary Entities

Primary hero and chapter bosses target a medium-plus 2D action animation tier with clear anticipation and strong impact silhouettes. Standard enemies can use a lighter tier but must still show directional facing and state changes.

### Anchor Hitbox And Z-Order Assumptions For Primary Entities

Humanoid feet anchors should stay stable for lane grounding. Hitboxes should favor readable torsos and weapon reach rather than full sprite bounds. Hero projectiles and bright slash effects render above characters, while ground shockwaves render below feet but above floor decals.

### Visual Readability Constraints

Chi effects must be beautiful but cannot fully cover enemy telegraphs or the hero body. Enemy silhouettes need different shoulder, weapon, and color signatures by family. Boss warning effects should contrast with player cyan energy by leaning ember red, jade green, or violet depending on chapter.

### Fallback Rule If No Suitable Pack Exists

If no suitable shared or importable pack exists, the gameplay art workflow may use project-local prototype art only as a temporary blockout and must continue until a non-placeholder runtime art assignment is delivered for menu item 10.

## Icon Direction

### Subject

The icon subject is the main hero in three-quarter close-up, driving a glowing dragon-shaped chi punch toward camera.

### Silhouette

Use a strong diagonal forearm and flame-dragon energy arc so the silhouette reads as a fighter with unleashed power rather than a static portrait.

### Color Direction

Cyan chi, ember gold accents, and deep navy or black framing should dominate. Avoid generic red-only fireball branding.

### Tone

Confident, heroic, and explosive, with a polished arcade-action feel.

## Audio Direction

### Menu BGM

Menu music should be heroic and atmospheric with plucked strings, low percussion, and a restrained choir pad that sells mythic scale without becoming slow.

### Gameplay BGM

Gameplay music should be fast martial percussion with lead strings or synth-hybrid motifs, keeping forward drive for combo-heavy stages.

### Boss Or Climax BGM

Boss music should escalate with taiko-like hits, brass or synth stabs, and a sharper rhythmic pulse to support phase transitions.

### SFX Families

SFX roles include light hit taps, heavy impact thuds, launcher bursts, chi charge hums, chi beam releases, dodge swishes, enemy guard breaks, boss telegraph alarms, reward pickups, menu confirm clicks, and chapter clear stingers. Chi attacks should layer core burst, air tear, and sparkle tail components so they look and sound premium.

## Technical Implementation Notes

### Rendering Style

Use a custom GameView or SurfaceView for gameplay with XML or View overlays for HUD and screens. Combat should support multiple entities in lane depth, projectile effects, and short camera shakes.

### State Model

Minimum states are MENU, CHAPTER_SELECT, LOADOUT, PLAYING, PAUSED, RESULT, GAME_OVER, and TRAINING. Gameplay state handling should support combat gates, checkpoint resume, boss phase transitions, and scripted intro moments.

### Important Entities

Important entities are hero actor, melee enemy actor, ranged enemy actor, armored breaker, aerial diver, elite commander, chapter boss, chi projectile, slash trail effect, pickup item, breakable prop, stage hazard trigger, and gate controller.

### Systems To Plan Early

Plan early for combo buffering, cancel windows, juggle gravity tuning, lane depth sorting, chi meter gain and spend logic, enemy group spawning, boss phase scripting, training room move preview, reward payout logic, and hit-pause plus screen-shake tuning. Continuous attack feel is a first-class system requirement, not optional polish.

## Gameplay Diversity And Content Budget

### Genre And Sub Archetype

Genre family is action beat-em-up. The concrete sub-archetype is chapter-based side-scrolling lane-clearing fantasy combo brawler with aerial juggle emphasis and chi-finisher routing.

### Why It Is Not A Reskin Of An Existing Game

This game differs from existing side-view action projects by focusing on multi-enemy crowd control, sustained combo routing, juggle loops, and spectacle chi arts across longer chapter stages rather than short boss-trial loops, office-themed streets, or loot-first stage replays.

### Map Or Playfield Budget

The playfield model is a linked sequence of side-scrolling combat arenas with gated progression strips. The base content target is six chapters, each with at least three distinct combat scenes plus one boss arena. Required arena families include open road, narrow bridge, layered terrace, courtyard, indoor hall, and elevated gate approach. Functional map elements include breakable jars, hazard braziers, drop ledges, seal pylons, and temporary gate barriers. Landmarks should include chapter-specific set pieces such as dragon statues, moon gates, cliff shrines, furnace chains, and banner towers.

### Entity Roster Budget

The first delivery target should include one fully realized hero, three planned hero archetype directions for later expansion, at least six standard enemy families, two ranged enemy families, two armored heavy families, two agile aerial families, three elite minibosses, and six chapter bosses. Projectile and effect families must include punches, slash arcs, launcher bursts, narrow chi bolts, wide chi waves, pillar eruptions, impact sparks, knockdown dust, and reward pop effects.

### Mechanic Variety Budget

Primary player actions are combo chain, launcher, aerial continuation, dodge cancel, chi finisher, and pickup collection. Secondary actions are checkpoint continue, training practice, chapter boon choice after boss clears, and situational interactables such as breaking braziers for hazard use. Upgrade paths cover combo branch extension, chi meter growth, defensive recovery, and style-score reward multipliers. Level variants include escort defense stage, elite ambush stage, hazard-rich gauntlet, duel-like miniboss corridor, and full boss arena. Risk and reward comes from extending combos for more chi and score while exposing the player to surround pressure.

### Asset Variety Budget

Primary gameplay art should draw from one cohesive fantasy martial humanoid pack plus one effects-focused pack. Secondary support may include chapter-specific environment and boss packs. The minimum distinct sprite family count should cover hero, light bandit, spear soldier, shield brute, archer acolyte, masked assassin, winged demon, elite captain, and boss family set. Required animation states are sufficient to express movement, hurt, knockdown, active attacks, and chi casting rather than static slides.

### Forbidden Template Reuse

Do not reuse a one-room arena trial structure. Do not collapse the game into a boss-rush only loop. Do not reuse office street theming, district capture framing, or loot-first equipment progression from prior brawler projects. Do not reduce enemy roster to one ground unit plus one boss. Do not replace chi arts with a single simple projectile.

## Visual Identity Contract

### UI Layout Archetype

The gameplay layout uses a crest-cluster upper-left vitals stack, a free-floating upper-right combo bloom, lower-corner control halos, and a right-side vertical pause and skill column outside the central lane. Menus use a character-stage presentation with layered cards rather than a centered plain list.

### HUD Composition

Life and chi are grouped in one ornate crest module, combo and style rank appear as transient glowing numerals, objectives use a thin top-center banner, and boss status appears as a dramatic top-frame strip only during boss phases. The HUD should feel lightweight in the center and denser at the corners.

### Navigation Model

The navigation model is chapter card selection with horizontal swiping between chapter banners, then sub-stage node selection inside a parchment-dark route panel. Upgrade and codex screens slide over the menu stage rather than hard-cutting to disconnected plain screens.

### Playfield Safe Area

Reserve top eight percent for bars and prompts, bottom fifteen percent split into control dead zones, and leave the center lane and hero forward travel space free of thick chrome. Protected gameplay zones are hero feet lane, enemy approach lane, jump apex band, and projected chi travel corridor.

### Frame Overlay Policy

Decorative dragon-metal frames may border menus and results panels, but gameplay chrome must terminate before entering the live combat lane. Temporary boss intros may animate over the scene, then retract completely before input-critical action resumes.

### Palette And Material Language

The palette signature is midnight navy, black lacquer, cyan energy, gold ember highlights, and restrained crimson danger markers. Material language blends polished lacquer panels, etched metal trims, translucent energy glass, and brush-slash accent strokes.

### Typography Direction

Typography should feel sharp and heroic, using tall angular display styling for headers and clean condensed readable body styling for UI labels. It should not resemble soft casual cartoon lettering.

### UI Pack Strategy

The primary pack remains skin_dark_arcade tokens. A style-matched fantasy martial overlay pack with crest frames, slash dividers, and medallion badges should be assigned through the UI workflow. Secondary assets may include boss emblem backplates and chapter sigils, but the layout should remain structurally clean.

### Icon Subject And Silhouette

The icon uses the hero bust and forward chi punch with a dragon-energy arc wrapping the frame edge. The silhouette must read asymmetrical, aggressive, and unmistakably hand-strike focused.

### Forbidden Visual Reuse

Do not reuse the generic top pill HUD plus bottom command strip seen in many mobile prototypes. Do not use a plain centered menu card over a static background. Do not use a generic sword emblem icon, shield badge icon, or reused dark fantasy castle motif. Do not place thick decorative borders over the gameplay lane.

## Differentiation Note

Compared with existing registry entries, Blaze Dragon Reborn is the dedicated combo-spectacle chapter brawler: longer stage push flow than Ashen Strike Trial, less loot-centric than Relic Blade Trials, less district-strategy framing than Crownblock Hunt, and more heroic fantasy martial presentation than Overtime Street Fighter. Its identity depends on uninterrupted chain combat, chapter spectacle, and premium chi effects.

## Confirmation

Status: Draft
Initialization Gate: Blocked Until Explicit User Confirmation
Reviewer Action: Confirm The Requirements Or Request Revisions
