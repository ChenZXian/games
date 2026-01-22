# CURSOR_RULES v2.0

- Version: v2.0
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
  - com.android.boot.MainActivity

Package identity may be changed ONLY if the user explicitly requests
a package rename with clear intent.

### 3.2 UI Kit Lock (HARD RULE)

All projects MUST use exactly one ui_skin as defined in docs/GAME_GENERATION_STANDARD.md.

During INIT and OPTIMIZE, Cursor MUST NOT:

- introduce bitmap assets (png/jpg/jpeg/webp/gif/bmp/ico)
- introduce fonts (ttf/otf)
- introduce audio binaries (ogg/mp3/wav/m4a/aac) unless PACK phase explicitly allows and the user requested it
- download or copy any UI kits or external resources
- add or modify .gitattributes or any Git config files
- mix multiple skins or invent a new skin not listed in the standard

Cursor MAY improve UI ONLY by working inside the UI Kit system:

- editing token resources in res/values (colors/dimens/styles/themes)
- editing XML-only drawables in res/drawable (shape/gradient/layer-list/vector)
- editing layouts to improve spacing/hierarchy while keeping one-skin consistency
- refactoring UI code to reference tokens/styles instead of hardcoded values

Hard rules:
- Java code must not hardcode UI colors for styling. Use cst_ tokens.
- Layouts must prefer styles (TextAppearance, Widget styles) over inline attributes.

UI Kit minimal file set is mandatory per project:

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

Icons (vector xml):
- res/drawable/ic_play.xml
- res/drawable/ic_pause.xml
- res/drawable/ic_restart.xml
- res/drawable/ic_sound_on.xml
- res/drawable/ic_sound_off.xml
- res/drawable/ic_help.xml

------

## 4. LFS Pointer Handling (Hard Gate)

If any file is detected as an LFS pointer (first line equals):
version https://git-lfs.github.com/spec/v1

Then Cursor MUST:

1. Treat it as a broken binary fetch placeholder.
2. Replace it with a valid text-based alternative that compiles.
3. Do not modify .gitattributes or Git settings.
4. Prefer XML-only vectors/shapes instead of bitmaps.

------

## 5. Icon Generation Policy (INIT Required, No Default Bitmap)

INIT must ensure app icon compiles without bitmaps.

Rules:

- AndroidManifest.xml must use android:icon="@mipmap/app_icon"
- Provide XML-only adaptive icon resources:
  - mipmap-anydpi-v26/app_icon.xml
  - drawable/app_icon_fg.xml
  - background color token in res/values/colors.xml
- Do not add bitmap mipmap icons during INIT or OPTIMIZE.

Bitmap replacement is allowed ONLY if:
- user explicitly requests packaging with bitmap icons
- and the repo rules for binaries are satisfied in that packaging step

------

## 6. BGM Library Policy (Local First, Then Online)

Default policy:

- Do not add any binary BGM/SFX assets during INIT or OPTIMIZE.
- If audio is needed for gameplay feedback, prefer ToneGenerator.

Online download policy:

- Online download of any media is forbidden unless the user explicitly requests it.
- Even when explicitly requested, prefer code-generated or placeholder audio approaches.

------

## 7. Phase-Specific Workflow Constraints

### 7.1 INIT Phase (HARD GATE)

Before executing any action, Cursor MUST output a one-line execution plan:

PHASE=INIT; WILL_RUN=[doctor, sync_fixes, lfs_cleanup, icon_generation, ui_kit_minimum_check]

The plan MUST NOT contain:

- validate
- build
- assemble
- bundle
- apk
- zip

Allowed:

- Fix Gradle sync issues
- Fix missing or incorrect wrapper files
- Fix manifest or resource errors blocking run
- Minimal code fixes required for app startup
- LFS pointer auto-remediation
- Icon generation (XML-only)
- UI kit minimum check and minimal remediation:
  - ensure cst_ colors exist
  - ensure required UI Kit files exist
  - ensure layouts reference UI Kit drawables/styles and do not crash

Forbidden (ABSOLUTE):

- Running tools/validate.ps1
- Running tools/build_apk.ps1
- Running ./gradlew build
- Running ./gradlew assemble*
- Running ./gradlew bundle*
- Generating APK or AAB
- Producing any zip or artifact
- Updating registry
- Git commit
- Online BGM download
- Changing applicationId / namespace / Java package paths
- Large UI redesign

INIT completion criteria:

- Android Studio sync succeeds
- App can be launched without immediate crash
- No LFS pointer files remain
- app_icon resolves correctly (XML-only)
- UI Kit infrastructure exists and compiles (minimal file set present)

------

### 7.2 OPTIMIZE Phase Allowed Actions

Primary goal:
- Improve gameplay, performance, and visual/UI quality while remaining compliant.

Allowed actions:

- Refactor game loop to reduce allocations and stabilize delta-time
- Improve input handling and responsiveness
- Improve UI/UX strictly inside the UI Kit system:
  - adjust spacing, alignment, hierarchy
  - enforce TextAppearance usage for all text
  - enforce Widget styles for buttons/panels
  - improve HUD readability using ui_meter_track/ui_meter_fill
  - improve menu/pause/gameover structure using ui_dialog/ui_panel_header
  - improve discoverability using icon buttons (ic_play/ic_pause/etc)
- Replace hardcoded UI styling values with cst_ tokens and style references
- Create additional XML-only UI drawables if needed, but they must:
  - be shape/gradient/layer-list/vector only
  - follow the selected ui_skin
  - not replace or remove the required minimal set
- Add small code-driven effects that do not require assets:
  - screen shake
  - particles drawn via Canvas
  - float text
  - simple hit flashes
  - button press scale feedback

Forbidden actions:

- Introducing bitmap assets or fonts
- Mixing skins
- Adding raw/ media binaries
- Running packaging scripts unless user intent is PACK

OPTIMIZE completion criteria (target):

- UI looks like a commercial mobile game UI kit while remaining text-only
- All screens use the same ui_skin consistently
- Text uses TextAppearance styles, buttons use Widget styles
- No hardcoded UI colors remain in Java for styling

------

### 7.3 PACK Phase Allowed Actions

Before packaging:
- Doctor must run and pass.

Allowed actions in PACK (only when user requested PACK):

- Run tools/validate.ps1 if present and if it helps produce a correct package
- Run tools/build_apk.ps1 if present, or Gradle assemble tasks if policy allows
- Produce APK and/or zip artifact as requested
- Update registry/produced_games.json only if user explicitly requested registry update
- Commit KB entries only in PACK phase
- Git commit/push only if user explicitly requested it in the current run

UI rules in PACK:

- Verify exactly one ui_skin is used consistently
- Verify UI Kit minimal file set exists
- Verify no forbidden binaries exist unless explicitly allowed by the packaging request
- Verify app_icon resolves correctly and matches manifest constraints

------

## 8. Registry and Git Rules

- Do not update registry/produced_games.json unless user explicitly requests it or the phase is PACK and registry update was requested.
- Do not git commit or push unless user explicitly requests it in the current run.

------

## 9. Role Definition

- User: sets goals, approves packaging, decides releases.
- Codex: generates initial project only, no scripts, no packaging.
- Cursor: fixes, optimizes, and packages under these rules.

------

## 10. Versioning Policy

Current version: v2.0
