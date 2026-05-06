# Icon Workflow

Version: 1.2
Last updated: 2026-04-30

This document defines the current repository icon workflow for Android Java mini-games in this monorepo.

Hard rule added on 2026-04-30:

- every icon refresh must be a fresh generation run
- reusing another project's motif, subject skeleton, silhouette template, exported PNG, or metadata-only pass is forbidden
- delivery-ready output requires `icon_duplicate_risk=low`

## 1. Purpose

The icon workflow exists to:

- generate or update the in-project launcher icon resources used by each game
- export upload-ready icon files for store or platform submission
- keep icon generation aligned with the current repository structure
- prevent multiple games from sharing the same icon subject, silhouette, or source asset

## 2. Style Direction

Every generated icon should follow these defaults unless the user requests a different direction:

- cartoon style
- strong visual link to the specific game's theme, fantasy, or core loop
- readable silhouette at small sizes
- bright contrast and clean foreground separation

## 3. Workflow Stages

### 3.1 Requirements Stage

Before generating assets, define:

- main subject
- silhouette
- palette direction
- tone
- any game-specific motif
- forbidden icon reuse from recent or related projects

This direction may come from the requirements document or from a direct icon workflow request.
When `artifacts/requirements/<game_id>/visual_identity.json` exists, it should be treated as the preferred icon direction source.

### 3.2 Project Asset Stage

When the icon workflow runs for a project, it should update the project-local icon resources required by `@mipmap/app_icon`.

Recommended outputs:

- `app/src/main/res/mipmap-anydpi-v26/app_icon.xml`
- `app/src/main/res/drawable/app_icon_fg.png` or `app_icon_fg.xml`
- `app/src/main/res/mipmap-mdpi/app_icon.png`
- `app/src/main/res/mipmap-hdpi/app_icon.png`
- `app/src/main/res/mipmap-xhdpi/app_icon.png`
- `app/src/main/res/mipmap-xxhdpi/app_icon.png`
- `app/src/main/res/mipmap-xxxhdpi/app_icon.png`

The workflow may also normalize the manifest to keep `android:icon="@mipmap/app_icon"`.

### 3.3 Export Stage

The icon workflow must also export upload-ready icon files to:

`artifacts/icons/<game_id>/`

Recommended exports:

- `<game_id>-upload-1024.png`
- `<game_id>-upload-512.png`
- `metadata.json`

`metadata.json` should identify:

- `game_id`
- `project_path`
- `generated_at`
- `primary_export`
- `icon_subject`
- `icon_silhouette`
- `visual_identity_source`
- `icon_duplicate_risk`
- `duplicate_review`

`metadata.json` is a report artifact, not the source of truth by itself.
Changing only `metadata.json` is never a valid way to resolve duplicate risk, motif mismatch, or visual identity mismatch.
If the subject, silhouette, motif, or duplicate review changes, the workflow must regenerate the project icon resources and exported upload files in the same run.

### 3.4 Uniqueness Review

Before marking icon work complete for delivery-ready output, verify that the icon does not reuse:

- another game's foreground subject without meaningful change
- another game's background badge or frame
- another game's color pairing and composition
- another game's exported upload icon
- a generic subject that does not clearly identify this game

Inspection must treat generic fallback motifs as incomplete for delivery-ready output. If icon metadata reports a broad motif such as `shieldstar`, `swordshield`, or `castle` without a game-specific subject, the icon workflow must regenerate a distinct icon before packaging.

The uniqueness review must also run before export, not only during inspection.

Minimum duplicate-risk checks:

- compare the candidate icon subject against existing `artifacts/icons/*/metadata.json`
- compare motif and silhouette against existing icon metadata
- treat identical subject, reused motif, or reused export hash as blocked reuse
- block export on any non-low duplicate risk instead of silently writing a repeated icon

Fresh-generation requirements:

- the workflow must regenerate the project icon bitmaps and upload exports in the same run
- metadata must declare `generation_mode=fresh_render`
- metadata must declare `reuse_policy=no_reuse`
- metadata must record content hashes for the generated foreground and primary export
- inspection should fail if hashes, timestamps, or policy markers indicate reuse or metadata-only edits

Low-intelligence, auto, or speed-priority runs may reduce candidate count, but they must not skip icon duplicate review.

### 3.4.1 Integrity Gate

Inspection and packaging must also verify icon generation integrity, not only duplicate wording in metadata.

Minimum integrity requirements:

- the metadata motif must be supported by the active generator
- the metadata subject must align with `visual_identity.json` when that contract exists
- the metadata silhouette should align with `visual_identity.json` when that contract exists
- `metadata.json`, the project icon resources, and the exported upload icon should be produced in the same generation window
- a metadata file that is significantly newer than the generated PNG resources must be treated as suspicious and should fail delivery-ready inspection
- packaging must not treat a metadata-only edit as a successful icon refresh

Recommended inspect outputs:

- `ICON_GENERATION_INTEGRITY=passed|warning|failed|missing`
- `ICON_METADATA_TRUST=high|low`

### 3.5 Packaging Stage

Packaging should reuse the existing icon workflow outputs whenever possible.

If project icon resources are missing or outdated, the packaging workflow may rerun the icon workflow before building artifacts.
If icon integrity fails, packaging must stop before APK export instead of trusting the existing metadata.

## 4. Repository Rules

- The manifest icon target remains `@mipmap/app_icon`
- Icon assets may be binary
- Exported upload-ready icon files belong under `artifacts/icons/<game_id>/`
- Icon work should not require unrelated project refactors

## 5. Legacy Workflow

Older bitmap-generation flow files have been archived under:

`archive/icon_legacy/`

The archived flow is kept for reference only and should not be treated as the current repository standard.
