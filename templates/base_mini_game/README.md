# Base Mini Game Skeleton

This skeleton is the only safe starting point for new project initialization when a finished game would otherwise be copied and edited.

Purpose:
- keep package and launcher wiring stable
- keep template identity neutral
- force every new game to rewrite gameplay, UI, icon, audio, and art identity on purpose

Before a generated project is treated as initialization-complete, rewrite and verify:
- `artifacts/requirements/<game_id>/metadata.json`
- `artifacts/requirements/<game_id>/gameplay_diversity.json`
- `artifacts/requirements/<game_id>/visual_identity.json`
- `artifacts/icons/<game_id>/metadata.json`
- `app/src/main/res/values/strings.xml`
- `app/src/main/assets/ui/ui_pack_assignment.json`
- `app/src/main/assets/game_art/game_art_assignment.json`
- `app/src/main/assets/game_art/runtime_art_map.json`
- `app/src/main/assets/audio/audio_assignment.json`

Required verification command:

```powershell
powershell -ExecutionPolicy Bypass -File tools/check_project_identity_residue.ps1 -Project games/<game_id> -GameId <game_id>
```

The check must return `IDENTITY_RESIDUE_OK=true`.
