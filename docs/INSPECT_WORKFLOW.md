# Inspect Workflow

Version: 1.4
Last updated: 2026-04-23

This document defines the repository inspection workflow for Android Java mini-games.

## 1. Purpose

The inspect workflow exists to:

- check project status without modifying project files
- determine whether a project can enter packaging
- summarize resource-track completion before release delivery
- report the next most useful action instead of silently failing

## 2. Scope

The inspect workflow may check:

- environment readiness
- project validation status
- registry presence
- requirements trace presence
- gameplay diversity contract status
- visual identity contract status
- icon track status
- UI track status
- gameplay art track status
- gameplay art runtime-map status
- audio track status
- existing APK export history

The inspect workflow does not:

- generate code
- modify project files
- package or build artifacts
- update registry

## 3. Entry Point

Current inspect tool entry point:

- `tools/inspect.ps1`

Recommended usage:

- `powershell -ExecutionPolicy Bypass -File tools/inspect.ps1 -Project games/<game_id>`

Optional fast-path flags:

- `-SkipDoctor`
- `-SkipValidate`

## 4. Status Model

The inspect workflow should report resource-track status using the labels below.

### 4.1 Requirements

- `candidates`
- `draft`
- `confirmed`
- `untracked`

### 4.2 Gameplay Diversity

- `passed`
- `draft`
- `needs_revision`
- `missing`
- `invalid`

Meaning:

- `passed`: gameplay diversity contract exists and is ready to gate implementation and delivery
- `draft`: contract exists but should not gate delivery yet
- `needs_revision`: contract is known to be too generic, too small, or too similar to an existing project
- `missing`: no gameplay diversity contract was found
- `invalid`: contract JSON could not be parsed or has no status

### 4.3 Visual Identity

- `passed`
- `draft`
- `needs_revision`
- `missing`
- `invalid`

Meaning:

- `passed`: UI and icon visual identity contract exists and is ready to gate delivery
- `draft`: contract exists but should not gate delivery yet
- `needs_revision`: contract is known to be too generic or too close to another project
- `missing`: no visual identity contract was found
- `invalid`: contract JSON could not be parsed or has no status

### 4.4 Icon, UI, Gameplay Art, Audio

- `complete`
- `placeholder_only`
- `deferred`

Meaning:

- `complete`: the workflow track has clear project evidence and expected shared/export evidence
- `placeholder_only`: the project has some local implementation, but tracking or export evidence is incomplete
- `deferred`: the workflow track has not been completed for the current project

### 4.5 Gameplay Art Runtime

- `integrated`
- `draft`
- `needs_revision`
- `missing`
- `invalid`

Meaning:

- `integrated`: runtime mapping exists and is ready for delivery checks
- `draft`: assets are assigned but runtime facing, animation, anchor, hitbox, and state mapping is not finalized
- `needs_revision`: runtime mapping exists but has a known mismatch
- `missing`: no runtime mapping was found
- `invalid`: runtime mapping JSON could not be parsed or has no status

## 5. Gating Rules

The inspect workflow should report two top-level conclusions.

### 5.1 `CAN_ENTER_PACK`

`true` only when all of the following are true:

- environment doctor passes, unless intentionally skipped
- project validator passes, unless intentionally skipped
- registry entry exists for the target game

### 5.2 `DELIVERY_READY`

`true` only when all of the following are true:

- `CAN_ENTER_PACK=true`
- requirements status is `confirmed`
- gameplay diversity status is `passed`
- visual identity status is `passed`
- implementation fidelity status is `passed`
- icon status is `complete`
- UI status is `complete`
- gameplay art status is `complete`
- gameplay art runtime status is `integrated`, `passed`, or `complete`
- audio status is `complete`

`DELIVERY_READY` is stricter than `CAN_ENTER_PACK`.
It is meant to describe release completeness, not just technical pack readiness.
It should also require a confirmed requirements trace for new-project workflow compliance.

## 6. Report Contract

Every inspect run should report:

- pass count
- warning count
- fail count
- `CAN_ENTER_PACK`
- `DELIVERY_READY`
- `REQUIREMENTS_STATUS`
- `GAMEPLAY_DIVERSITY_STATUS`
- `VISUAL_IDENTITY_STATUS`
- `IMPLEMENTATION_FIDELITY_STATUS`
- `ICON_STATUS`
- `UI_STATUS`
- `GAME_ART_STATUS`
- `GAME_ART_RUNTIME_STATUS`
- `AUDIO_STATUS`
- `NEXT_STEP`

## 7. Current Skill

Current inspect skill:

- `.agents/skills/mini_game_inspect/`
