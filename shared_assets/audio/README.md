# Shared Audio Library

This directory stores reusable repository-managed game audio.

Structure:

- `index.json` - shared metadata for reusable audio assets
- `bgm/` - background music assets
- `sfx/` - sound effect assets

Naming:

- BGM library files: `bgm_<role>_<tag>_<token>.<ext>`
- SFX library files: `sfx_<role>_<tag>_<token>.<ext>`

Project assignment names are normalized separately:

- primary gameplay BGM becomes `bgm.<ext>`
- additional BGM becomes `bgm_<role>.<ext>`
- sound effects become `sfx_<role>.<ext>`
