# Project Acceptance Baseline

Version: 1.0
Last updated: 2026-04-22

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

## 5. Presentation Baseline

A project is not considered visually complete if it relies only on bare placeholder presentation.

For a delivery-ready result, the project should have:

- a coherent UI direction using the repository UI workflow
- a non-placeholder launcher icon using the repository icon workflow
- a defined audio direction, covering BGM and SFX when the game type needs them
- visual and interaction quality above plain prototype level

## 6. Requirements And Inspection Baseline

Before first-time initialization, the project should go through:

- candidate concept planning
- full requirements drafting
- explicit requirements confirmation

Before packaging or release preparation, the project should be inspectable through the repository inspection workflow.

The expected readiness ladder is:

- requirements trace is present
- inspect can report `CAN_ENTER_PACK=true`
- resource tracks are no longer deferred for the intended release target
- packaging is run only when explicitly requested
