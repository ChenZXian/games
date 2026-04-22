# Audio Workflow

Version: 1.0
Last updated: 2026-04-22

This document defines the current repository audio workflow for Android Java mini-games in this monorepo.

## 1. Purpose

The audio workflow exists to:

- define game-specific audio direction before asset assignment
- support both BGM and SFX
- keep generated or fetched audio reusable through a shared library
- assign project audio from the shared library when needed

## 2. Style Direction

Every audio workflow run should define:

- game tone
- gameplay rhythm
- menu mood
- battle or tension level
- reward or failure feedback style

Audio should match the target game's theme, pacing, and interaction feel.

## 3. Scope

The workflow covers:

- BGM:
  - menu
  - play
  - boss or climax
  - win
  - fail
- SFX:
  - ui click
  - ui confirm
  - ui back
  - attack
  - hit
  - collect
  - warning
  - upgrade
  - win
  - fail

## 4. Shared Library

The active shared library root is:

- `shared_assets/audio/`

Library layout:

- `shared_assets/audio/index.json`
- `shared_assets/audio/bgm/`
- `shared_assets/audio/sfx/`

All generated or fetched audio should be stored in the shared library first whenever possible.

Shared library naming should be consistent:

- BGM entries: `bgm_<role>_<tag>_<token>.<ext>`
- SFX entries: `sfx_<role>_<tag>_<token>.<ext>`
- `role` should describe usage, not genre
- `tag` should describe style, mood, or theme
- `token` should be a short uniqueness suffix

## 5. Project Assignment

Project-local audio should be assigned from the shared library.

Recommended project target path:

- `app/src/main/assets/audio/`

Recommended names:

- primary BGM: `bgm.<ext>`
- additional BGM: `bgm_<role>.<ext>`
- sound effects: `sfx_<role>.<ext>`

## 6. Asset Sources

Audio may come from:

- shared local library reuse
- repository synthesis tools
- online fetch workflows when allowed by policy and licensing rules

## 7. Metadata

Every shared library entry should include:

- `id`
- `file`
- `type`
- `role`
- `tags`
- `style`
- `loop`
- `duration_sec`
- `used_by`
- `source`
- `retrieved_at`
- `license`
- `license_url`
- `generated`
- `format`

## 8. Current Tools

Current active tool entry points:

- `tools/assets/assign_audio.ps1`
- `tools/assets/fetch_audio.ps1`
- `tools/assets/synth_audio.ps1`

Legacy BGM-only notes remain under:

- `archive/bgm_legacy/`
