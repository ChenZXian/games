# Icon Workflow

Version: 1.0
Last updated: 2026-04-22

This document defines the current repository icon workflow for Android Java mini-games in this monorepo.

## 1. Purpose

The icon workflow exists to:

- generate or update the in-project launcher icon resources used by each game
- export upload-ready icon files for store or platform submission
- keep icon generation aligned with the current repository structure

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

This direction may come from the requirements document or from a direct icon workflow request.

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

### 3.4 Packaging Stage

Packaging should reuse the existing icon workflow outputs whenever possible.

If project icon resources are missing or outdated, the packaging workflow may rerun the icon workflow before building artifacts.

## 4. Repository Rules

- The manifest icon target remains `@mipmap/app_icon`
- Icon assets may be binary
- Exported upload-ready icon files belong under `artifacts/icons/<game_id>/`
- Icon work should not require unrelated project refactors

## 5. Legacy Workflow

Older bitmap-generation flow files have been archived under:

`archive/icon_legacy/`

The archived flow is kept for reference only and should not be treated as the current repository standard.
