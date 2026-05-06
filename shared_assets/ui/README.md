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

Current curated packs:

- `kenney_ui_pack` - bright generic cartoon mobile UI
- `kenney_pixel_ui_pack` - retro pixel arcade and adventure UI
- `kenney_ui_pack_sci_fi` - futuristic sci-fi and space-action UI
- `kenney_ui_pack_adventure` - fantasy adventure and quest UI
- `kenney_ui_pack_rpg_expansion` - progression-heavy RPG and upgrade UI

Resolution order:

1. style-matched shared UI pack
2. imported licensed open-source UI pack
3. project-local custom refinement

Do not treat generic shape-only placeholder controls as production-grade UI.

Project-local copies should be derived from these shared packs when possible.

Recommended assignment flow:

- define or choose an assignment preset under `packs/<pack_id>/assignments/`
- list available packs with `tools/assets/assign_ui.ps1 -ListPacks`
- list available presets with `tools/assets/assign_ui.ps1 -ListPresets -PackId <pack_id>`
- resolve a matching pack with `tools/assets/ensure_ui_pack.ps1`
- import a licensed structured pack with `tools/assets/import_ui_pack.ps1`
- use `tools/assets/assign_ui.ps1`
- record the applied pack in `app/src/main/assets/ui/ui_pack_assignment.json`
