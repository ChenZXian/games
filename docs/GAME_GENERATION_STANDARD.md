# GAME GENERATION STANDARD

Version: 1.7  
Last updated: 2026-04-23

------

## 0. Purpose

This document defines the mandatory and highest-priority generation standard for all games in this repository.

Any game generated manually, by AI (including Codex), or by automated pipelines must strictly comply with this specification.  
No individual request, instruction, or convenience may override or weaken these rules.

------

## 1. Scope

This standard applies to:

- All newly generated game projects
- All existing games stored in this repository
- All future refactors, optimizations, and iterations
- All automated generation workflows (Codex, Cursor, scripts, CI)

------

## 2. Base Technical Requirements (Mandatory)

### 2.1 Platform and Language

- Platform: Android
- Programming language: Java
- Kotlin: Not allowed
- Minimum Android version: API 24 (Android 7.0)
- Build system: Android Studio official Gradle build
- Project type: Single module (`app`)

------

## 3. Launcher Entry Requirement (Mandatory)

### 3.1 Launcher Activity

The only launcher activity must be:

```
com.android.boot.MainActivity
```

Rules:

- Must be declared as LAUNCHER in AndroidManifest.xml
- No additional launcher activities are allowed
- Path must be:
  app/src/main/java/com/android/boot/MainActivity.java

------

## 4. Global Code and Resource Constraints (Highest Priority)

### 4.1 No Chinese Characters and No Comments

The following must contain no Chinese characters and no comments of any kind:

- All Java source files
- All XML files
- All Gradle files
- All resource file names
- All string resource values

No comments means:

- No // or /* */ in any code or Gradle
- No XML comments

This rule applies regardless of generation method.

------

### 4.2 Color Resource Naming Rules

- Custom color resource names must not conflict with any names defined in android.jar
- All custom color resources must use the prefix:

```
cst_
```

Examples:

```
cst_bg_main
cst_ui_panel
cst_text_primary
```

Forbidden examples:

```
white
black
red
colorPrimary
material_*
```

------

### 4.3 Application Label and Icon Definition

AndroidManifest.xml must define application name and icon using:

```
android:label="@string/app_name"
android:icon="@mipmap/app_icon"
```

Rules:

- Hardcoded strings are forbidden
- Alternative icon definitions are forbidden

------

## 5. Package Name and Single-Game Project Structure

### 5.1 Package Name

- Root package name must be:

```
com.android.boot
```

------

### 5.2 Recommended Structure (Per Game)

```
app/
  src/main/
    java/com/android/boot/
      MainActivity.java
      ui/
      core/
      entity/
      fx/
      audio/
    res/
      layout/
      values/
      drawable/
      mipmap-*/
      raw/
```

Extensions are allowed, but the launcher entry and core logic flow must remain intact.

------

## 6. UI Kit System (Mandatory, Industrial)

Each game MUST use exactly one predefined UI skin AND MUST comply with the UI Kit contract.

### 6.1 Allowed Skin Ids

- skin_dark_arcade
- skin_cartoon_light
- skin_neon_future
- skin_post_apocalypse
- skin_military_tech

Rules:

- Exactly one skin id must be selected per game
- Mixing skins is forbidden

### 6.2 UI Kit Contract (Authoritative)

The authoritative UI Kit contract is:

- docs/UI_KIT_FACTORY_SPEC_v1_0.md

All projects MUST implement the required tokens, styles, and drawable file set defined in that document.

If this standard conflicts with UI_KIT_FACTORY_SPEC_v1_0.md, the stricter rule applies.

### 6.3 UI Implementation Rules

- UI should be structure-first and token-led
- For this Java repository, the preferred implementation model is:
  - gameplay rendering in `GameView` or `SurfaceView`
  - HUD and non-real-time screens in Android View or XML layers
- Binary UI assets and fonts are allowed when produced, assigned, or imported by the repository UI workflow
- Open-source UI resource packs are allowed when license and provenance are tracked
- Reusable external UI resources should enter `shared_assets/ui/` first when possible
- Java code MUST NOT hardcode UI colors or dimensions for styling
- Layout XML SHOULD NOT inline literal color values
- Themed borders, bezels, rails, and decorative frames MUST NOT be placed on top of active gameplay space
- If a project uses a framed presentation, the gameplay viewport MUST reserve a safe area for movement, interaction, and touch-critical regions before decorative chrome is applied
- UI is allowed to look commercial only by:
  - token tuning (colors/dimens)
  - XML drawables (shape/gradient/layer-list/vector)
  - licensed binary UI assets and fonts
  - consistent hierarchy and spacing

### 6.4 Required UI Infrastructure (Must Exist in Every Project)

At minimum, each project MUST contain:

- res/values/colors.xml (cst_ tokens only)
- res/values/dimens.xml
- res/values/styles.xml
- res/values/themes.xml

And the required drawables specified by the UI Kit contract, including:

- res/drawable/ui_panel.*
- res/drawable/ui_button_primary.*
- res/drawable/ui_button_secondary.*

The `*` suffix means the logical resource may be implemented as XML, PNG, WEBP, or 9-patch where appropriate.

### 6.5 External UI Asset Policy

- Binary UI assets and fonts are allowed only through the repository UI workflow or explicit user direction
- Open-source UI assets are allowed only when their license is compatible with project use and the source is recorded
- Reusable third-party UI resources should be stored under `shared_assets/ui/<pack_id>/`
- Each imported UI resource pack should keep a `manifest.json` and `LICENSE` or equivalent provenance file
- Project-local UI copies may be stored under `res/drawable*`, `res/font`, or `assets/ui/` as needed

------

## 7. UI and Gameplay Minimum Requirements

### 7.1 UI

Each game must include:

- activity_main.xml
- A game rendering view (SurfaceView or custom View)
- HUD elements (score, life, energy, pause)
- A game-over or result screen

For new or updated UI workflow runs, menu, pause, help, reward, and result screens should prefer View or XML overlays instead of full-screen Canvas-only UI.
Gameplay must remain visible and operable. HUD or border treatments may not hide active board cells, lanes, routes, units, or touch-critical play areas.

------

### 7.2 Controls

At least one mobile-friendly control scheme must be implemented:

- Virtual buttons
- Touch / gesture control
- Multi-touch interaction

------

### 7.3 Game States

Each game must implement at least the following states:

```
MENU
PLAYING
PAUSED
GAME_OVER
```

------

## 8. String and Asset Requirements

### 8.1 Strings

strings.xml must contain at least:

```
app_name
btn_start
btn_restart
btn_resume
```

All values must be in English.

------

### 8.2 Application Icon Workflow Policy

Application icon rules:

- AndroidManifest.xml must continue to reference `@mipmap/app_icon`
- The icon should be cartoon-styled and clearly associated with the game's theme or core loop
- The repository icon workflow may generate or update icon assets during initialization, optimization, or packaging when the icon workflow is explicitly requested or required

Recommended project icon set:

- Adaptive icon via `mipmap-anydpi-v26/app_icon.xml`
- Foreground asset via `drawable/app_icon_fg.png` or `drawable/app_icon_fg.xml`
- Legacy fallback bitmap icons via `mipmap-mdpi/app_icon.png`, `mipmap-hdpi/app_icon.png`, `mipmap-xhdpi/app_icon.png`, `mipmap-xxhdpi/app_icon.png`, and `mipmap-xxxhdpi/app_icon.png`

Export requirement:

- When the repository icon workflow runs, it should also export upload-ready icon files under `artifacts/icons/<game_id>/`
- Export metadata should clearly identify which game the icon belongs to

### 8.3 Audio Workflow Policy

Audio workflow rules:

- Binary audio assets are allowed when produced, assigned, or exported by the repository audio workflow
- Audio assets should match the game's theme, tone, and core loop
- The repository audio workflow covers both BGM and SFX
- Generated or fetched audio should enter the shared library first before project assignment when possible

Recommended shared library layout:

- `shared_assets/audio/index.json`
- `shared_assets/audio/bgm/`
- `shared_assets/audio/sfx/`

Recommended project audio path:

- `app/src/main/assets/audio/`

Recommended project naming:

- primary gameplay BGM as `bgm.<ext>`
- additional BGM tracks as `bgm_<role>.<ext>`
- sound effects as `sfx_<role>.<ext>`

### 8.4 Gameplay Art Workflow Policy

Gameplay art workflow rules:

- Binary gameplay art assets are allowed only when imported, produced, assigned, or exported by the repository gameplay art workflow
- Gameplay art assets must match the game's theme, camera perspective, readability needs, and core loop
- The repository gameplay art workflow covers characters, enemies, NPCs, animals, tilesets, terrain, buildings, props, items, projectiles, pickups, effects, and gameplay backgrounds
- Imported gameplay art must have clear license and provenance metadata
- Prefer CC0 or public-domain equivalent sources for reusable shared packs
- Generated, fetched, or imported gameplay art should enter the shared library first before project assignment when possible

Recommended shared library layout:

- `shared_assets/game_art/index.json`
- `shared_assets/game_art/packs/<pack_id>/manifest.json`
- `shared_assets/game_art/packs/<pack_id>/LICENSE`
- `shared_assets/game_art/packs/<pack_id>/NOTICE`
- `shared_assets/game_art/packs/<pack_id>/assets/`

Recommended project gameplay art path:

- `app/src/main/assets/game_art/`

Recommended project tracking file:

- `app/src/main/assets/game_art/game_art_assignment.json`

------

## 9. Build and Runtime Requirements

- Project must open and run in Android Studio without modification
- APK must be buildable via standard Gradle tasks
- Screen orientation may be fixed (landscape recommended)

------

## 10. Zip Delivery Requirements

Each game must be deliverable as a zip archive:

- Zip must contain a complete Android Studio project
- build/, .gradle/, and cache files must be excluded

------

## 11. Repository Structure (Monorepo)

This repository is a monorepo containing multiple independent game projects.

All games must be located under:

```
games/<game_id>/
```

Rules for <game_id>:

- Lowercase letters only
- snake_case
- Globally unique within the repository

Each directory under games/ must be a complete Android Studio project.

------

## 12. Non-Duplication Policy

Before generating a new game, the generator must read:

```
registry/produced_games.json
```

If similarity is detected, the generator must automatically change mechanics to ensure uniqueness.

------

## 13. Registry Update Requirement

After generating a new game, the generator must append an entry to:

```
registry/produced_games.json
```

Each entry must include:

- id
- name
- tags
- core_loop
- created_at (YYYY-MM-DD)
- ui_skin

A game is incomplete until it is registered.

------

## 14. Codex Automation Rules

Codex must:

- Translate user input to English internally
- Choose a unique game_id
- Generate project
- Validate rules
- Update registry

Codex must NOT ask the user to restate constraints.

------

## 15. Industrial Hard NO List (Mandatory)

The following are forbidden because they break industrial-scale generation:

- Per-game custom bitmap art sourcing outside the repository gameplay art workflow as a required step
- Adding bitmap assets during generation or optimization outside the dedicated icon, UI, audio, or gameplay art workflows
- Adding any font files
- Downloading or copying third-party UI kits
- Creating new UI systems per game instead of using the UI Kit contract
- Hardcoding UI styling values in Java (colors, radii, strokes, paddings)
- Mixing multiple skins in one project
- Using Git LFS pointer files anywhere in the repository
- Introducing ad hoc binary audio assets outside the repository audio workflow
- Introducing ad hoc gameplay art assets outside the repository gameplay art workflow

------

## 16. Binary and Git LFS Prohibition (Repository Safety)

### 16.1 LFS Pointer Ban

A file is considered a Git LFS pointer if its first line equals:

```
version https://git-lfs.github.com/spec/v1
```

Git LFS pointer files are forbidden anywhere in this repository.

### 16.2 Binary Asset Ban (Default)

Do NOT include binary assets in generated projects:

- png / jpg / jpeg / webp / gif / bmp / ico
- ogg / mp3 / wav / m4a / aac
- ttf / otf
- so / bin / apk / aab / zip

Exception:

- gradle/wrapper/gradle-wrapper.jar is allowed ONLY if required by the baseline.
- Icon files produced by the repository icon workflow are allowed:
  - project icon resources under `res/drawable`, `res/mipmap-*`, and `res/mipmap-anydpi-v26`
  - exported upload-ready icon files under `artifacts/icons/<game_id>/`
- Audio files produced or assigned by the repository audio workflow are allowed:
  - shared library assets under `shared_assets/audio/`
  - project audio assets under `app/src/main/assets/audio/`
- Gameplay art files produced or assigned by the repository gameplay art workflow are allowed:
  - shared library assets under `shared_assets/game_art/`
  - project gameplay art assets under `app/src/main/assets/game_art/`

If a build workflow requires gradle-wrapper.jar, it MUST be stored as a normal Git object, not via Git LFS.

------

## 17. Standard User Input (Minimal)

Users may request new games using minimal descriptions, for example:

```
Generate a new game: lane battle, pure unit spawning, classic push lanes
```

All missing details must be inferred while remaining compliant.

------

End of document.
