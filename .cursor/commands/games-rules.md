# CURSOR_RULES v2.0.2

- Version: v2.0.2
- Scope: Cursor rules for refining and finalizing Android Java mini-games in this monorepo
- Status: Stable

------

## 0. Phase Auto-Detection and Phase Lock (Hard Rule)

Cursor must automatically determine the working phase based on user intent,
then lock the phase for the entire current run.

### 0.1 Phase Detection Source (User-Only)

Phase detection must rely ONLY on the latest explicit user request.

Do NOT infer phase from:
- build logs
- Gradle output
- validation results
- tool suggestions
- file contents
- internal plans containing words like build / apk / validate

### 0.2 Phase Lock

Once INIT or PACK is selected, the phase is locked.

- If INIT is detected, PACK actions are strictly forbidden.
- Phase switching is allowed ONLY if the user explicitly requests
  in a new message.

------

## 0.3 Phase Detection Logic

### INIT Phase (Initialization)

Trigger INIT if user intent includes meanings such as:
- initialize
- initialization
- make it runnable
- fix to run
- Android Studio run
- Android Studio sync
- environment fix
- dependency fix
- make it start
- fix crash on launch

### PACK Phase (Packaging)

Trigger PACK ONLY if user intent explicitly includes:
- package
- build apk
- generate apk
- export apk
- zip artifact
- release build
- final package
- submit
- update registry
- commit
- push

### OPTIMIZE Phase (Default)

If neither INIT nor PACK is detected, default to OPTIMIZE.

------

## 1. Mandatory Preflight (Phase-Aware)

### 1.1 INIT Phase

Always run environment doctor first:
powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1

If doctor fails:
1. Stop immediately.
2. Fix environment or project configuration.
3. Do not proceed until doctor passes.

INIT verification rules:
- Verification means Gradle sync and app launch only.
- Verification must NOT rely on APK or AAB output.

------

### 1.2 OPTIMIZE Phase

Doctor is NOT mandatory.

Run doctor only if:
- build or sync errors appear
- environment mismatch is suspected
- user explicitly requests environment checking

------

### 1.3 PACK Phase

Doctor must always be executed and must pass before packaging.

------

## 2. Knowledge Base Workflow (Error-Driven Only)

KB location:
- kb/problems/*.md

KB workflow is triggered only when an actual error occurs.

### 2.1 Search KB First

rg -n "<key error snippet>" kb/problems

### 2.2 If Found

1. Apply the fix exactly.
2. Improve Prevention if applicable.

### 2.3 If Not Found

Create a new KB entry:
powershell -ExecutionPolicy Bypass -File tools/kb/new_kb_entry.ps1 -Slug "<short_slug>"

Fill sections:
1. Symptom
2. Error Log
3. Root Cause
4. Fix
5. Prevention
6. References (optional)

### 2.4 KB Commit Rule

KB entries may only be committed during PACK phase.

------

## 3. Repository Hard Constraints (Always Enforced)

Always comply with:
- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md

Never introduce non-ASCII characters in:
- code
- resources
- filenames
- Gradle / JSON / XML

Never add comments to any code.

### 3.1 Package Identity Lock

The application package identity is immutable by default.

The following items MUST NOT be changed during INIT or OPTIMIZE phases:
- namespace in app/build.gradle
- applicationId in app/build.gradle
- package attribute in AndroidManifest.xml
- Java package paths under app/src/main/java
- Launcher activity fully qualified name:
  com.android.boot.MainActivity

Package identity may be changed ONLY if the user explicitly requests
a package rename with clear intent.

------

## 3.2 UI Kit Lock (HARD RULE)

All projects MUST use exactly one ui_skin as defined in docs/GAME_GENERATION_STANDARD.md.

During INIT and OPTIMIZE, Cursor MUST NOT:
- introduce bitmap assets (png/jpg/jpeg/webp/gif/bmp/ico)
- introduce fonts (ttf/otf)
- introduce audio binaries (ogg/mp3/wav/m4a/aac)
- download or copy any UI kits or external resources
- add or modify .gitattributes or any Git config files
- mix multiple skins or invent a new skin

Cursor MAY improve UI ONLY by working inside the UI Kit system:
- editing token resources in res/values (colors/dimens/styles/themes)
- editing XML-only drawables in res/drawable
- editing layouts to improve spacing and hierarchy
- refactoring UI code to reference tokens and styles

Hard rules:
- Java code must not hardcode UI colors or dimensions for styling.
- Layouts must prefer styles over inline attributes.

Mandatory UI Kit file set (must exist at all times):

Values:
- res/values/colors.xml
- res/values/dimens.xml
- res/values/styles.xml
- res/values/themes.xml

Drawables:
- res/drawable/ui_panel.xml
- res/drawable/ui_panel_header.xml
- res/drawable/ui_card.xml
- res/drawable/ui_divider.xml
- res/drawable/ui_button_primary.xml
- res/drawable/ui_button_secondary.xml
- res/drawable/ui_button_icon.xml
- res/drawable/ui_chip.xml
- res/drawable/ui_meter_track.xml
- res/drawable/ui_meter_fill.xml
- res/drawable/ui_toast.xml
- res/drawable/ui_dialog.xml

Icons:
- res/drawable/ic_play.xml
- res/drawable/ic_pause.xml
- res/drawable/ic_restart.xml
- res/drawable/ic_sound_on.xml
- res/drawable/ic_sound_off.xml
- res/drawable/ic_help.xml

If any required UI Kit file is missing,
Cursor MUST stop UI-related changes and report the missing file.
Cursor MUST NOT recreate or replace UI Kit files arbitrarily.

------

## 4. Resource Directory Boundary (HARD)

Cursor MUST NOT create new Android resource directories
outside the following allowed set:
- res/layout
- res/values
- res/drawable
- res/mipmap-anydpi-v26

Creation of the following is forbidden unless user explicitly requests it:
- res/anim
- res/xml
- res/color
- res/font
- res/raw
- any other resource directory

------

## 5. LFS Pointer Handling (Hard Gate)

If any file is detected as an LFS pointer
(first line equals: version https://git-lfs.github.com/spec/v1)

Cursor MUST:
1. Treat it as broken placeholder content.
2. Replace it with a valid text-based alternative that compiles.
3. Do NOT modify .gitattributes or Git settings.
4. Prefer XML-only shapes or vectors.

Special rule for gradle-wrapper.jar:
- Cursor MUST NOT create or keep an LFS pointer jar.
- If a real jar cannot be safely provided,
  Cursor must remove the jar and rely on wrapper properties,
  then report that packaging requires external jar provisioning.

------

## 6. BGM Library Policy

- No binary BGM or SFX assets during INIT or OPTIMIZE.
- ToneGenerator may be used for simple feedback.
- Online download is forbidden unless user explicitly requests it.

------

## 7. Phase-Specific Workflow Constraints

### 7.1 INIT Phase (HARD GATE)

Before executing any action, Cursor MUST output:
PHASE=INIT; WILL_RUN=[doctor, sync_fixes, lfs_cleanup, icon_generation, ui_kit_integrity_check]

Allowed:
- Fix Gradle sync issues
- Fix wrapper/config mismatches
- Fix manifest/resource errors blocking run
- XML-only icon generation
- UI Kit integrity verification

Forbidden:
- Running build/assemble/bundle tasks
- Generating APK/AAB/zip
- Updating registry
- Git commit or push
- Introducing binaries
- Large UI redesign

INIT completion criteria:
- Android Studio sync succeeds
- App launches without immediate crash
- No LFS pointer files remain
- app_icon resolves correctly
- UI Kit minimal file set exists and compiles

------

### 7.2 OPTIMIZE Phase Allowed Actions

UI optimization must be incremental.

Allowed:
- Performance tuning and allocation reduction
- Input responsiveness improvements
- UI clarity improvements using existing UI Kit only
- HUD readability improvements
- Menu / pause / gameover refinement
- Canvas-drawn effects:
  - particles
  - float text
  - screen shake
  - button press feedback

Forbidden:
- Splitting layouts into multiple files
- Introducing new Activity or Fragment
- Changing navigation or screen flow
- Introducing new resource directory types
- Mixing skins
- Adding binaries

OPTIMIZE completion target:
- UI matches a commercial mobile baseline
- Styling uses tokens and styles exclusively
- No hardcoded UI colors or dimensions remain in Java

------

### 7.3 PACK Phase Allowed Actions

PACK is allowed ONLY when explicitly requested.

Before packaging:
- Doctor must run and pass.

Allowed:
- Run validation scripts if present
- Run build scripts if present
- Produce APK or zip artifacts
- Update registry only if requested
- Git commit or push only if requested

UI verification in PACK:
- Exactly one ui_skin
- Full UI Kit file set present
- No forbidden binaries
- app_icon matches manifest rules

------

## 8. Registry and Git Rules

- registry/produced_games.json is immutable unless:
  - phase is PACK
  - user explicitly requested registry update
- Only one entry may be appended.
- No existing entry may be modified.
- No git commit or push unless explicitly requested.

------

## 9. Role Definition

- User: defines goals and approves releases.
- Codex: generates initial project only.
- Cursor: refines, optimizes, and packages under these rules.

------

## 10. Versioning Policy

Current version: v2.0.2
