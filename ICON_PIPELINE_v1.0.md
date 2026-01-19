# ICON_PIPELINE v1.0

This repository uses a two-step icon workflow:

1) Codex generates an XML-only placeholder icon (no bitmap files).
2) Cursor (or you) replaces the placeholder with real bitmap launcher icons when you say "make icon" or during PACK.

This avoids Git LFS pointer issues and keeps Codex free of binary assets.

---

## Inputs

A single square PNG:

- Recommended size: 1024x1024
- No text in the image
- Transparent background is allowed

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

The adaptive icon foreground uses a 432x432 PNG so it scales cleanly.

---

## How to Run

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -Source <path_to_png>
```

Optional background color (adaptive icon):

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -Source <path_to_png> -Background "#101820"
```

---

## Phase Rules

- INIT:
  - Allowed to keep placeholder icon.
  - If the user explicitly requests a real icon, run the script.

- OPTIMIZE:
  - Run the script only when the user asks to improve visuals/icons.

- PACK:
  - Run the script if the project still uses the XML placeholder or if icon files are missing.

---

## Notes

- The launcher icon is referenced by AndroidManifest.xml as:
  - android:icon="@mipmap/app_icon"
- This script does not modify the manifest; it only ensures the referenced resources exist.
