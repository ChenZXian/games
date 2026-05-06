# GAME GENERATION STANDARD

Version: 1.6  
Last updated: 2026-01-23

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

- UI must be XML-only and token-driven
- External assets are forbidden:
  - No bitmap images (png/jpg/jpeg/webp/gif/bmp/ico)
  - No fonts (ttf/otf)
  - No downloaded UI kits
- Java code MUST NOT hardcode UI colors or dimensions for styling
- Layout XML SHOULD NOT inline literal color values
- UI is allowed to look commercial only by:
  - token tuning (colors/dimens)
  - XML drawables (shape/gradient/layer-list/vector)
  - consistent hierarchy and spacing

### 6.4 Required UI Infrastructure (Must Exist in Every Project)

At minimum, each project MUST contain:

- res/values/colors.xml (cst_ tokens only)
- res/values/dimens.xml
- res/values/styles.xml
- res/values/themes.xml

And the required drawables specified by the UI Kit contract, including:

- res/drawable/ui_panel.xml
- res/drawable/ui_button_primary.xml
- res/drawable/ui_button_secondary.xml

------

## 7. UI and Gameplay Minimum Requirements

### 7.1 UI

Each game must include:

- activity_main.xml
- A game rendering view (SurfaceView or custom View)
- HUD elements (score, life, energy, pause)
- A game-over or result screen

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

### 8.2 Application Icon (Two-Stage Policy)

Stage A (generation-safe):

- @mipmap/app_icon must exist using XML-only resources
- Adaptive icon via mipmap-anydpi-v26/app_icon.xml
- Foreground via drawable/app_icon_fg.xml
- Background color via cst_ token

Stage B (packaging):

- Bitmap icons may be added only during packaging
- Required densities: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi

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

- Per-game custom bitmap art sourcing as a required step
- Adding any bitmap assets during generation or optimization
- Adding any font files
- Downloading or copying third-party UI kits
- Creating new UI systems per game instead of using the UI Kit contract
- Hardcoding UI styling values in Java (colors, radii, strokes, paddings)
- Mixing multiple skins in one project
- Using Git LFS pointer files anywhere in the repository
- Introducing binary audio assets unless explicitly requested in a packaging-only workflow

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