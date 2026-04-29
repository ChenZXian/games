# Audio Workflow

Version: 1.1
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

- theme
- overall mood
- pacing
- menu mood
- gameplay mood
- climax mood if needed
- required BGM roles
- required SFX roles

Audio should match the target game's theme, pacing, and interaction feel.

The audio brief is mandatory before asset assignment.

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

- style-matched shared local library reuse
- online fetch workflows when allowed by policy and licensing rules
- repository synthesis tools as a fallback when shared and fetched assets are not enough

Default resolution order:

1. search the shared library for a style-matched asset
2. fetch a licensed online asset when no shared match exists
3. synthesize a fallback asset when fetch is unavailable or insufficient

Do not silently reuse a legacy asset if it only matches by role and not by style.

Delivery-ready inspection must verify BGM separately from general audio. A project is not audio-complete unless it has at least one project-local `bgm*` file and a matching shared-library BGM entry linked to the same game id.

## 7. Metadata

Every shared library entry should include:

- `id`
- `file`
- `type`
- `role`
- `tags`
- `style`
- `style_tags`
- `mood`
- `pacing`
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

- `tools/assets/ensure_audio_bundle.ps1`
- `tools/assets/assign_audio.ps1`
- `tools/assets/fetch_audio.ps1`
- `tools/assets/synth_audio.ps1`

Legacy BGM-only notes remain under:

- `archive/bgm_legacy/`
