# Shared UI Library

This directory stores reusable UI resource packs for the repository UI workflow.

Recommended structure:

- `index.json`
- `packs/<pack_id>/manifest.json`
- `packs/<pack_id>/assignments/<preset_id>.json`
- `packs/<pack_id>/LICENSE`
- `packs/<pack_id>/NOTICE`
- `packs/<pack_id>/drawable/`
- `packs/<pack_id>/font/`
- `packs/<pack_id>/preview/`

Use this library for:

- licensed open-source panel packs
- button and frame assets
- HUD icon packs
- repository-approved UI fonts

Project-local copies should be derived from these shared packs when possible.

Recommended assignment flow:

- define or choose an assignment preset under `packs/<pack_id>/assignments/`
- list available packs with `tools/assets/assign_ui.ps1 -ListPacks`
- list available presets with `tools/assets/assign_ui.ps1 -ListPresets -PackId <pack_id>`
- use `tools/assets/assign_ui.ps1`
- record the applied pack in `app/src/main/assets/ui/ui_pack_assignment.json`
