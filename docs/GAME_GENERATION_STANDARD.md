# GAME GENERATION STANDARD

Version: 1.2  
Last updated: 2026-01-18

---

## 0. Purpose

This document defines the mandatory and highest-priority generation standard for all games in this repository.

Any game generated manually, by AI (including Codex), or by automated pipelines must strictly comply with this specification.  
No individual request, instruction, or convenience may override or weaken these rules.

---

## 1. Scope

This standard applies to:

- All newly generated game projects
- All existing games stored in this repository
- All future refactors, optimizations, and iterations
- All automated generation workflows (Codex, scripts, CI)

---

## 2. Base Technical Requirements (Mandatory)

### 2.1 Platform and Language

- Platform: Android
- Programming language: Java
- Kotlin: Not allowed
- Minimum Android version: API 24 (Android 7.0)
- Build system: Android Studio official Gradle build
- Project type: Single module (`app`)

---

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

---

## 4. Global Code and Resource Constraints (Highest Priority)

### 4.1 No Chinese Characters and No Comments

The following must contain no Chinese characters and no comments of any kind:

- All Java source files
- All XML files
- All resource file names
- All string resource values

This rule applies regardless of generation method.

---

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

---

### 4.3 Application Label and Icon Definition

AndroidManifest.xml must define application name and icon using:

```
android:label="@string/app_name"
android:icon="@mipmap/app_icon"
```

Rules:

- Hardcoded strings are forbidden
- Alternative icon definitions are forbidden

---

## 5. Package Name and Single-Game Project Structure

### 5.1 Package Name

- Root package name must be:

```
com.android.boot
```

---

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

---

## 6. UI and Gameplay Minimum Requirements

### 6.1 UI

Each game must include:

- activity_main.xml
- A game rendering view (SurfaceView or custom View)
- HUD elements (score, life, energy, pause)
- A game-over or result screen

---

### 6.2 Controls

At least one mobile-friendly control scheme must be implemented:

- Virtual buttons
- Touch / gesture control
- Multi-touch interaction

---

### 6.3 Game States

Each game must implement at least the following states:

```
MENU
PLAYING
PAUSED
GAME_OVER
```

---

## 7. String and Asset Requirements

### 7.1 Strings

strings.xml must contain at least:

```
app_name
btn_start
btn_restart
btn_resume
```

All values must be in English.

---

### 7.2 Application Icon

- @mipmap/app_icon must exist
- File name: app_icon.png
- Required densities: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi

---

## 8. Build and Runtime Requirements

- Project must open and run in Android Studio without modification
- APK must be buildable via standard Gradle tasks
- Screen orientation may be fixed (landscape recommended)

---

## 9. Zip Delivery Requirements

Each game must be deliverable as a zip archive:

- Zip must contain a complete Android Studio project
- build/, .gradle/, and cache files must be excluded

---

## 10. Repository Structure (Monorepo)

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

---

## 11. Non-Duplication Policy

Before generating a new game, the generator must read:

```
registry/produced_games.json
```

A new game is considered a duplicate if its core gameplay loop is substantially similar to an existing entry.

Similarity is determined by:

- Player perspective
- Control scheme
- Primary objective
- Core feedback loop

If similarity is detected, the generator must automatically change genre or mechanics to ensure uniqueness.

---

## 12. Registry Update Requirement

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

A game is incomplete until it is registered.

---

## 13. Codex Automation Rules

When using Codex:

- User input may be in Chinese or English
- Codex must translate user input into clear English internally
- Codex must autonomously choose a unique game_id
- Codex must execute the full workflow:
  - Generate project
  - Validate rules
  - Update registry
  - Commit and push

Codex must not ask the user to restate constraints already defined in this document.

---

## 14. Standard User Input (Minimal)

Users may request new games using minimal descriptions, for example:

```
Generate a new game: ninja parkour, fast-paced, non-shooting
```

All missing details must be inferred while remaining compliant.

---

End of document.
