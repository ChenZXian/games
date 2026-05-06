# Project Acceptance Baseline

Version: 1.4
Last updated: 2026-04-23

This document defines the generic structure and acceptance baseline for any Android Java mini-game in this repository.

It is intentionally genre-agnostic.
It does not require one specific game type such as a match game, runner, or tower defense project.

## 1. Core Delivery Baseline

Every new game project should satisfy all of the following:

- it is an Android Studio Java project
- it remains a single-module app project unless the repository standard changes
- it can be opened and worked on in Android Studio
- it can enter the repository packaging workflow and produce an APK when packaging is explicitly requested
- it remains self-contained and offline by default unless the user explicitly asks for online or backend-dependent features

## 2. Identity Baseline

Every project must satisfy:

- root package is `com.android.boot`
- launcher activity is `com.android.boot.MainActivity`
- `AndroidManifest.xml` uses:
  - `android:label="@string/app_name"`
  - `android:icon="@mipmap/app_icon"`
- the launcher activity is the only launcher activity

## 3. Code And Resource Baseline

Every project must satisfy:

- Java only
- no Chinese in generated code, XML, Gradle files, resource file names, or generated strings
- no comments in Java, XML, or Gradle files
- custom color resources use the `cst_` prefix and must not conflict with `android.jar`
- exactly one allowed `ui_skin` is selected

## 4. Gameplay And Screen Baseline

Every project should have a complete core loop and the minimum expected screen structure for its game type.

At minimum, the project should define:

- a main gameplay entry
- a playable core loop
- a menu or equivalent start surface
- a gameplay HUD when the game type needs one
- pause or interruption handling when the game type needs it
- a result, failure, or game-over state

The first playable implementation must preserve the confirmed requirements instead of replacing them with a simpler unrelated loop.

For a menu item `10` or delivery-ready flow, the implementation should include the required core mechanics from the confirmed requirements trace, or stop and report the missing mechanics before APK export.

## 5. Gameplay Diversity And Content Scale Baseline

Every new delivery-ready game should satisfy the gameplay diversity contract defined in `docs/GAMEPLAY_DIVERSITY_WORKFLOW.md`.

The implementation should not be a thin reskin of another generated project. It should differ by meaningful gameplay dimensions such as:

- genre sub-archetype
- camera or playfield perspective
- control model
- primary player decision
- map or playfield structure
- entity roster
- progression or upgrade structure
- failure pressure
- asset family and animation behavior

For delivery-ready output, the project should implement the confirmed content budgets instead of collapsing them into a tiny map, one enemy type, one unit type, or one repeated interaction.

Tower defense is a common failure case, but the rule is generic. A new runner, shooter, puzzle, farming, action, or survival game must also define and preserve its own map, entity, mechanic, and asset-variety budgets.

## 6. Visual Identity Baseline

Every new delivery-ready game should satisfy the visual identity contract defined in `docs/VISUAL_IDENTITY_WORKFLOW.md`.

The UI and icon should not be thin visual reuse from another generated project. They should differ by meaningful visual dimensions such as:

- UI layout archetype
- HUD composition
- navigation model
- palette signature
- material language
- typography direction
- screen-specific motifs
- icon subject
- icon silhouette
- icon foreground and background composition

Using the same `ui_skin`, the same top HUD pill layout, the same bottom command strip, or the same icon source object across multiple games is not enough for delivery-ready output unless the visual identity contract explicitly justifies it and changes other major axes.

## 7. Presentation Baseline

A project is not considered visually complete if it relies only on bare placeholder presentation.

For a delivery-ready result, the project should have:

- a coherent UI direction using the repository UI workflow
- a documented playfield safe area so decorative borders or HUD chrome do not block active gameplay space
- a gameplay layout that materially reserves that safe area in `activity_main.xml`, container padding, or an equivalent verified viewport wrapper instead of relying on overlays sitting on top of a full-screen playfield
- gameplay visuals that are not limited to bare placeholder circles or rectangles when a delivery-ready target is requested
- UI that is more than UI Kit-only token scaffolding when a delivery-ready target is requested
- a tracked gameplay art strategy using the repository gameplay art workflow when external or reusable character, map, prop, item, effect, or background assets are used
- a runtime gameplay art map for delivery-ready projects, including facing rules, anchors, hitboxes, z-order, and visual states for primary moving or attacking entities
- gameplay art animation that is more than one static bitmap moving smoothly across the screen when the entity has directional or action meaning
- primary humanoid, animal, zombie, soldier, or creature entities should visibly alternate run or walk poses when moving
- attack-capable primary entities should show windup, action, and recovery or an equivalent pose progression when source frames exist
- a non-placeholder launcher icon using the repository icon workflow
- a defined audio direction, covering BGM and SFX when the game type needs them
- visual and interaction quality above plain prototype level

## 8. Requirements And Inspection Baseline

Before first-time initialization, the project should go through:

- candidate concept planning
- full requirements drafting
- explicit requirements confirmation

Before packaging or release preparation, the project should be inspectable through the repository inspection workflow.

The expected readiness ladder is:

- requirements trace is present
- gameplay diversity contract is present and passed for delivery-ready output
- visual identity contract is present and passed for delivery-ready output
- inspect can report `CAN_ENTER_PACK=true`
- resource tracks are complete, not deferred or placeholder-only, for the intended release target
- inspect and validate can report the playfield safe area as passed rather than warning or failed
- packaging is run only when explicitly requested

## 9. Initialization Residue Baseline

A newly initialized project should not carry identity residue from an older finished game.

At minimum:

- initialization should start from the repository skeleton contract under `templates/base_mini_game/`
- the project should not contain another game's `game_id` inside requirements, icon metadata, UI assignment, gameplay art assignment, runtime art map, audio assignment, or visible strings
- unresolved template markers should not remain in identity-bearing files
- the project should pass `tools/check_project_identity_residue.ps1 -Project games/<game_id> -GameId <game_id>` before downstream polish or packaging
