# Requirements Workflow

Version: 1.6
Last updated: 2026-04-23

This document defines the repository requirements workflow for Android Java mini-games.

## 1. Purpose

The requirements workflow exists to:

- turn a chosen concept into an authoritative project requirement trace
- keep planning output reusable before initialization
- make requirements status inspectable before packaging
- separate candidate exploration from confirmed implementation scope

## 2. Scope

The requirements workflow covers:

- selected concept capture
- full game requirements writing
- gameplay diversity and content budget capture
- requirements confirmation state
- requirements trace storage

The requirements workflow does not:

- initialize Android projects
- update registry
- optimize an existing project
- package a build

## 3. Authoritative Trace Location

Once one concept is selected and a target `game_id` is known, the authoritative planning trace should live under:

- `artifacts/requirements/<game_id>/`

Authoritative files:

- `artifacts/requirements/<game_id>/metadata.json`
- `artifacts/requirements/<game_id>/requirements.md`
- `artifacts/requirements/<game_id>/gameplay_diversity.json`

Supporting file:

- `artifacts/requirements/<game_id>/candidates.md`

The requirements trace becomes authoritative at the selected-concept stage.
The initial 10-candidate discussion may stay conversational if no target `game_id` exists yet.

## 4. Status Model

Requirements trace status should use one of:

- `candidates`
- `draft`
- `confirmed`

Meaning:

- `candidates`: candidate list stored, but no final requirements document yet
- `draft`: requirements document exists, but the user has not confirmed it yet
- `draft` also means the full requirements must be shown to the user and must remain blocked on explicit confirmation
- `confirmed`: requirements document exists and is confirmed for initialization

If no trace exists, inspection should report `untracked`.

## 5. Workflow Stages

The repository requirements workflow should be executed as three explicit stages.

### 5.1 Candidate Concepts

Use this stage after the user gives a broad game direction and before one concept is selected.

Recommended command:

- `powershell -ExecutionPolicy Bypass -File tools/requirements/create_candidates_trace.ps1 -GameId <game_id> -Title "<title>" -Direction "<direction>"`

Expected result:

- `candidates.md` exists
- `metadata.json` exists
- `status=candidates`

### 5.2 Full Requirements Draft

Use this stage after the user selects exactly one concept.

Recommended command:

- `powershell -ExecutionPolicy Bypass -File tools/requirements/create_requirements_trace.ps1 -GameId <game_id> -SelectedConcept "<selected concept>" -UiSkin "<ui_skin>"`

Expected result:

- `requirements.md` exists
- `metadata.json` exists
- `status=draft`
- `gameplay_diversity.json` exists and is specific enough to guide implementation
- the full requirements draft is shown to the user for review before initialization

### 5.3 Confirmation

Use this stage only after the user explicitly confirms the requirements.

Recommended command:

- `powershell -ExecutionPolicy Bypass -File tools/requirements/confirm_trace.ps1 -GameId <game_id> -ExplicitUserConfirmation`

Expected result:

- `requirements.md` already exists
- existing `metadata.json` already exists with `status=draft`
- `selected_concept` is present
- `ui_skin` is present
- `status=confirmed`

### 5.4 Menu Item 10 Orchestration

When the repository flow uses menu item `10`, the sequence must remain:

1. broad direction input
2. 10 candidate concepts
3. one selected concept
4. full requirements draft
5. explicit requirements confirmation
6. initialization only after confirmation
7. optimization and resource completion
8. inspection
9. APK export through the packaging workflow

Additional menu item `10` rules:

- candidate generation and requirements drafting must happen in separate replies
- after candidate generation, stop and wait for exactly one concept selection
- after requirements drafting, show the full draft to the user and stop for explicit confirmation
- do not initialize a project in the same reply as the full requirements draft
- if the user requests revisions, update the draft and keep status `draft`
- only after explicit confirmation may the trace move to `confirmed`
- menu item `10` is the complete flow from confirmed requirements to APK output; APK export is the final stage, not a shortcut around earlier stages
- if inspection or packaging prerequisites fail, stop before APK export and report the blocker
- menu item `10` must not proceed with a generic or repeated gameplay template when the gameplay diversity contract is missing or not passed

## 6. Metadata Contract

`metadata.json` should include at least:

- `version`
- `game_id`
- `title`
- `direction`
- `selected_concept`
- `status`
- `ui_skin`
- `current_stage`
- `created_at_utc`
- `updated_at_utc`
- `selected_at_utc`
- `confirmed_at_utc`
- `files`

Recommended `files` fields:

- `metadata_json`
- `requirements_md`
- `candidates_md`
- `gameplay_diversity_json`

## 7. Markdown Contract

`requirements.md` should cover at least:

- game positioning
- target feel and player fantasy
- core gameplay loop
- controls and input model
- failure and win conditions
- progression structure
- level or run structure
- economy or rewards
- screen map
- UI direction
- gameplay art direction
- icon direction
- audio direction
- technical implementation notes
- differentiation note
- gameplay diversity and content budget

The gameplay art direction should be implementation-ready, not only a style note.
For moving, attacking, damageable, collectible, or animated objects, requirements should specify:

- role
- camera perspective
- default facing
- expected facing behavior
- minimum visual states
- movement animation expectation
- animation quality tier
- hit or damage feedback expectation
- anchor and hitbox assumptions when relevant

The gameplay diversity and content budget should define:

- genre family and concrete sub-archetype
- camera or playfield perspective
- control model
- core loop signature
- differentiation axes against existing registry entries
- forbidden template reuse
- map or playfield content budget
- entity roster budget
- mechanic variety budget
- gameplay asset variety budget

The machine-readable form of this section should be stored in `gameplay_diversity.json`.
See `docs/GAMEPLAY_DIVERSITY_WORKFLOW.md` for the required contract.

`candidates.md` should capture:

- all 10 concepts
- working title for each concept
- pitch
- core loop
- controls
- genre family and sub-archetype
- content-scale note
- hook
- retention or monetization note
- duplicate-risk note
- selected concept field
- selection rationale

## 8. Entry Points

Primary entry points:

- `tools/requirements/create_candidates_trace.ps1`
- `tools/requirements/create_requirements_trace.ps1`
- `tools/requirements/confirm_trace.ps1`
- `tools/requirements/assert_confirmed_trace.ps1`

Low-level compatibility helper:

- `tools/requirements/init_trace.ps1`

`init_trace.ps1` may still be used by scripts or older flows, but new planning work should prefer the three explicit stage commands above.

## 9. Confirmed-State Rule

Do not mark a requirements trace as `confirmed` unless all of the following are true:

- `requirements.md` exists
- `metadata.json` already exists with `status=draft`
- exactly one selected concept has been chosen
- `selected_concept` is stored in `metadata.json`
- one allowed `ui_skin` has been chosen
- the user explicitly confirmed the requirements
- `tools/requirements/confirm_trace.ps1` is called with `-ExplicitUserConfirmation`

Initialization should not begin before this state exists.

## 10. Inspect Integration

The inspect workflow should read:

- `artifacts/requirements/<game_id>/metadata.json`
- `artifacts/requirements/<game_id>/requirements.md`
- `artifacts/requirements/<game_id>/gameplay_diversity.json`

Expected inspect output:

- `REQUIREMENTS_STATUS=confirmed` when confirmed metadata and requirements markdown are present
- `REQUIREMENTS_STATUS=draft` when requirements markdown exists without confirmed metadata
- `REQUIREMENTS_STATUS=candidates` when only candidate trace exists
- `REQUIREMENTS_STATUS=untracked` when no trace exists
- `GAMEPLAY_DIVERSITY_STATUS=passed` only when the gameplay diversity contract exists, is valid, and is ready for downstream implementation checks
