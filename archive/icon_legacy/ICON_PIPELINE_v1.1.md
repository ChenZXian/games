# ICON_PIPELINE v1.1 (No Default Bitmap)

This repository uses a two-step icon workflow:

1) Codex generates an XML-only placeholder icon (no bitmap files).
2) Cursor (or you) generates real bitmap launcher icons during INIT (required) or during PACK.

This workflow avoids Git LFS pointer issues and keeps Codex free of binary assets.

---

## Inputs

No input image is required.

The tool generates a deterministic 1024x1024 PNG using a seed derived from:

- game_id (recommended), or
- an explicit seed string

No text is rendered.

---

## Output Targets (Android Launcher Icons)

The script will create:

### Legacy mipmap icons

- app/src/main/res/mipmap-mdpi/app_icon.png (48x48)
- app/src/main/res/mipmap-hdpi/app_icon.png (72x72)
- app/src/main/res/mipmap-xhdpi/app_icon.png (96x96)
- app/src/main/res/mipmap-xxhdpi/app_icon.png (144x144)
- app/src/main/res/mipmap-xxxhdpi/app_icon.png (192x192)

### Adaptive icon (API 26+)

- app/src/main/res/mipmap-anydpi-v26/app_icon.xml
- app/src/main/res/mipmap-anydpi-v26/app_icon_round.xml
- app/src/main/res/mipmap-xxxhdpi/app_icon_foreground.png (432x432)
- app/src/main/res/values/app_icon_colors.xml

---

## How to Run

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -GameId <game_id>
```

Optional background color (adaptive icon):

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -GameId <game_id> -Background "#101820"
```

Optional explicit seed (overrides GameId):

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -Seed "any-stable-seed"
```

---

## Phase Rules

- INIT:
  - Must generate real launcher icons using this tool.

- OPTIMIZE:
  - Regenerate icons only when the user requests icon/visual improvements.

- PACK:
  - Ensure icons exist and resolve correctly. Regenerate if missing or placeholder-only.

---

## Notes

- The launcher icon is referenced by AndroidManifest.xml as:
  - android:icon="@mipmap/app_icon"
- This tool does not modify the manifest; it only ensures the referenced resources exist.
