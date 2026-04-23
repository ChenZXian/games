# Inspect Workflow

Version: 1.1
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
- icon track status
- UI track status
- gameplay art track status
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

### 4.2 Icon, UI, Gameplay Art, Audio

- `complete`
- `placeholder_only`
- `deferred`

Meaning:

- `complete`: the workflow track has clear project evidence and expected shared/export evidence
- `placeholder_only`: the project has some local implementation, but tracking or export evidence is incomplete
- `deferred`: the workflow track has not been completed for the current project

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
- icon status is `complete`
- UI status is not `deferred`
- gameplay art status is not `deferred`
- audio status is not `deferred`

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
- `ICON_STATUS`
- `UI_STATUS`
- `GAME_ART_STATUS`
- `AUDIO_STATUS`
- `NEXT_STEP`

## 7. Current Skill

Current inspect skill:

- `.agents/skills/mini_game_inspect/`
