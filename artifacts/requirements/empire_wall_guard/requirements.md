# Empire Wall Guard

Game ID: empire_wall_guard
Direction: ancient empire tower defense
Selected Concept: Empire Wall Guard - defend an ancient imperial wall with archer towers, barracks, stone throwers, oil skills, and wall repair decisions against barbarian waves.
Recommended UI Skin: skin_military_tech

## Game Positioning

Empire Wall Guard is a compact Android Java tower-defense mini-game about defending the frontier wall of an ancient empire. The player builds wall defenses, stations soldiers, repairs breached sections, and survives escalating barbarian waves before the invaders break through to the capital road.

## Target Feel And Player Fantasy

The game should feel like commanding a disciplined ancient border garrison. The player fantasy is being the wall commander who reads incoming enemy lanes, orders archers and shield troops into position, keeps the wall alive under pressure, and turns a fragile frontier gate into an imperial stronghold.

## Core Gameplay Loop

Each stage starts with a section of wall, several approach lanes, tower platforms, barracks slots, and a gate durability meter. The player spends supplies to build archer towers, shield barracks, stone throwers, and oil cauldrons, starts or survives enemy waves, collects supplies from defeated enemies and wave bonuses, repairs wall sections between attacks, upgrades defenses, and clears the stage when all waves are defeated before the gate falls.

## Controls And Input Model

Primary input is tap-based. Tap a build slot to choose a defense, tap an existing defense to upgrade or sell, tap a lane rally marker to send shield troops to block enemies, tap the repair button to restore wall durability, and tap the fire oil skill when enemies stack near the gate. Controls must be readable on landscape phones, with build actions in a bottom command panel and critical status at the top.

## Failure And Win Conditions

The player loses when the main gate durability reaches zero. Enemies damage the gate when they reach the wall breach point or when siege units fire long enough from range. The player wins a stage by surviving all scripted waves. Star rating should depend on remaining gate durability, number of wall breaches, and unused emergency skills.

## Progression Structure

Progression is campaign-based across frontier regions. Early stages teach archer towers and shield barracks, mid stages introduce armored raiders and siege carts, and late stages add multiple breach points, elite captains, and wave pressure from different sides. Persistent progression should be light: unlocked defense types, higher upgrade caps, and chapter access. The first implementation should avoid complex meta-economy and focus on clear stage-to-stage progression.

## Level Or Run Structure

A standard level has 7 to 10 waves. Before each wave, the player gets a short planning window to build, upgrade, sell, and repair. During waves, the player may still use skills and rally troops, but new construction should be limited or more expensive. Every third stage can include a siege wave, and final chapter stages should include a warlord boss with guard escorts.

## Economy Rewards And Upgrades

Supplies are the main currency. Supplies come from wave start income, enemy defeats, perfect defense bonuses, and optional supply crates dropped by support caravans. Defense classes should include Archer Tower for fast ranged damage, Shield Barracks for lane blocking, Stone Thrower for slow splash damage, Oil Cauldron for short-range burst, and Signal Beacon for temporary nearby attack speed buffs. Upgrades should use three tiers per defense: base, veteran, and imperial.

## Screen Map

Menu: title, Start, Continue, Stage Select, Help, Sound toggle.
Gameplay HUD: gate durability, supplies, wave number, breach count, next wave state, pause button, fire oil skill, repair button.
Pause: Resume, Restart, Stage Select, Sound toggle.
Game Over: result title, wave reached, breaches, gate durability, stars, Restart, Back to Menu.
Extra Screens: Stage Select map with frontier forts, Help screen explaining tower slots, barracks, repairs, siege units, and skills.

## UI Direction

Use exactly one UI skin: skin_military_tech. The layout tone should be tactical, disciplined, readable, and command-board-like, with olive metal panels, bronze accents, clear icons, and strong status hierarchy. HUD priorities are gate durability first, then supplies, wave progress, and emergency actions. Key overlay panels are the bottom build command panel, selected-defense upgrade card, repair confirmation, and result panel. UI assets should start from repository UI Kit contract and then be refined through the UI workflow; for menu item 10 or delivery-ready output, token-only project-local UI should not be treated as final if a stronger shared UI pack is available.

## Gameplay Art Direction

Required gameplay art roles are ancient wall segment, main gate, tower platforms, archer tower, shield barracks, stone thrower, oil cauldron, signal beacon, shield soldiers, barbarian raider, fast scout, armored brute, siege cart, warlord boss, arrows, stones, oil fire effect, gate hit effect, supply crate, dust trail, and frontier background. Camera perspective should be top-down or slight angled board view so lanes, wall durability, and blocking units are readable. Preferred source tier is a license-clear shared game art pack with ancient, medieval, or strategy war assets; if no suitable pack exists, import a free and provenance-clear pack before falling back to local generated art. Visual readability constraints: enemy classes must have distinct silhouettes, siege units must be larger and obvious, wall health feedback must be visible, and troop blockers must not obscure tower slots.

## Icon Direction

The icon subject should be an imperial wall gate with crossed arrows and a small shield troop in front. The silhouette should be a chunky fortress gate with two towers and an incoming fire arrow arc. Color direction should use bronze, olive green, stone gray, and warm torch orange. Tone should be cartoon-styled but strategic and battle-ready, clearly communicating ancient wall defense.

## Audio Direction

Menu BGM should feel like a calm imperial war room with low drums, horns, and restrained strings. Gameplay BGM should be tense and rhythmic with marching percussion and battle pulses. Boss or climax BGM should add heavy drums, horns, and siege impacts without overwhelming short mobile sessions. SFX roles include button tap, build tower, upgrade, sell, archer shot, stone throw, shield block, oil fire, enemy hit, enemy death, supply pickup, gate damage, repair, wave start, siege warning, victory, defeat, and pause.

## Technical Implementation Notes

Rendering should use a custom GameView or SurfaceView for lanes, enemies, towers, projectiles, blockers, wall effects, and combat feedback, while HUD and non-real-time screens use Android View or XML overlays. Required states are MENU, PLAYING, PAUSED, GAME_OVER, and STAGE_CLEAR. Important entities are StageConfig, Lane, BuildSlot, Defense, BarracksUnit, Enemy, SiegeEnemy, Projectile, Gate, WaveSpawner, SupplyDrop, Skill, Effect, and SaveState. Systems to plan early are lane pathing, tower targeting priority, barracks blocking, siege enemy ranged attacks, wall repair economy, wave scripting, deterministic update timing, and stage unlock persistence.

Implementation addendum after confirmation: production work must use tracked license-clear gameplay assets instead of bare placeholders. Primary moving and attacking characters must not be represented by a single static bitmap sliding across the screen. Soldiers, raiders, scouts, brutes, siege carts, projectiles, and the warlord should maintain explicit state, animation timing, and facing direction so movement, blocking, attacking, hit feedback, and death feedback read correctly.

## Differentiation Note

The registry already includes Castle Keep Defender, Royal Line Defense, Garden Siege, Minecart Bastion, Bowling Barrage, and other defense-adjacent games. Empire Wall Guard is differentiated by an ancient empire wall-defense fantasy, explicit wall durability and repair pressure, shield barracks that block lanes, siege enemies that attack from range, and command decisions between construction, troop blocking, and wall maintenance.

## Confirmation

Status: Confirmed
Initialization Gate: Open after explicit user confirmation.
Reviewer Action: Implement the confirmed scope with correct asset usage, animation, and facing behavior.
