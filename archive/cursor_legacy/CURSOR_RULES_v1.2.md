# CURSOR_RULES v1.6

- Version: v1.6
- Scope: Cursor rules for refining and finalizing Android Java mini-games in this monorepo
- Status: Stable

------

## 0. Phase Auto-Detection (Hard Rule)

Cursor must automatically determine the working phase based on user intent.

### 0.1 Phase Detection Logic

Cursor must classify the phase before taking any action:

#### INIT Phase (Initialization)

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

#### PACK Phase (Packaging)

Trigger PACK if user intent includes meanings such as:

- package
- build
- build_apk
- validate
- apk
- zip
- artifact
- final version
- release
- submit
- update registry

#### OPTIMIZE Phase (Default)

If neither INIT nor PACK is detected, default to OPTIMIZE.

------

## 1. Mandatory Preflight (Phase-Aware)

### 1.1 INIT Phase

Always run environment doctor first:

```powershell
powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1
```

If doctor fails:

1. Stop immediately.
2. Fix environment or project configuration.
3. Do not proceed until doctor passes.

Goal of INIT:

- Project can be opened, synced, and run directly from Android Studio.
- Only minimal fixes required to achieve runnability.

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

```bash
rg -n "<key error snippet>" kb/problems
```

### 2.2 If Found

1. Apply the fix exactly.
2. Improve Prevention if applicable.

### 2.3 If Not Found

Create a new KB entry:

```powershell
powershell -ExecutionPolicy Bypass -File tools/kb/new_kb_entry.ps1 -Slug "<short_slug>"
```

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

Never change launcher activity:

- com.android.boot.MainActivity

AndroidManifest.xml must always use:

- android:label="@string/app_name"
- android:icon="@mipmap/app_icon"

Color resource names:

1. Must not conflict with android.jar.
2. Must use custom prefixes only.

Each game must stay under:

- games/<game_id>/

Immutable files:

- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- registry/produced_games.json

------

## 4. LFS Pointer Handling (Hard Gate)

### 4.1 Definition

A file is an LFS pointer if its first line equals:

- version https://git-lfs.github.com/spec/v1

### 4.2 INIT Must Auto-Remediate

During INIT, Cursor must ensure there are no LFS pointer files inside the target project directory.

Required behavior:

1. Detect LFS pointer files under games/<game_id>/.
2. For each pointer file, Cursor must automatically remediate using one of:
   - Replace with a small non-LFS placeholder file that keeps the project runnable (preferred).
   - Remove the file only if it is not required for build/run, and update references accordingly.
3. After remediation, Cursor must re-scan until no pointers remain.

If tools/validate_lfs_pointers.ps1 exists, use it:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate_lfs_pointers.ps1 -Path games/<game_id>
```

If the script does not exist, Cursor must perform an equivalent scan during INIT.

### 4.3 OPTIMIZE Must Not Introduce New Pointers

During OPTIMIZE, Cursor must not add any LFS pointer files.

If Cursor needs to introduce a real binary asset (e.g., icon or audio), it must defer that change to PACK unless the user explicitly requests otherwise.

### 4.4 PACK Must Fail If Any Pointer Exists

During PACK, Cursor must run the LFS scan before validate/build and must fail immediately if any LFS pointer is found.

Required order in PACK:

1. doctor
2. LFS scan (must pass)
3. validate (if exists)
4. build_apk (if exists)

------

## 5. Icon Generation Policy (INIT Required, No Default Bitmap)

### 5.1 Purpose

Codex uses XML-only icon placeholders to avoid binary assets.
During INIT, Cursor must generate real launcher icons so Android Studio runs with a proper app icon.

### 5.2 Required Tool

If present, use the repo script:

- tools/assets/generate_app_icon.ps1

### 5.3 INIT Requirement

During INIT, Cursor must ensure that real icon files exist for @mipmap/app_icon.

Required behavior:

1. If the project already contains bitmap icons for app_icon in mipmap densities (or adaptive icon resources that resolve cleanly), do nothing.
2. Otherwise, generate icons by running procedural generation (no input bitmap required):

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -GameId <game_id>
```

1. Do not create or depend on any default bitmap file (no default_icon_1024.png).
2. After generation, ensure AndroidManifest.xml still references android:icon="@mipmap/app_icon".

INIT completion criteria includes:

- app_icon exists as bitmap files in mipmap-* folders, or adaptive icon resources exist and resolve cleanly.

### 5.4 OPTIMIZE and PACK

- OPTIMIZE:
  - Regenerate icons only if the user requests icon/visual improvements.
- PACK:
  - If icon files are missing or still placeholders, regenerate using the same tool before packaging.

------

## 6. BGM Library Policy (Local First, Then Online)

### 6.1 Purpose

Each game may use background music (BGM) without requiring Codex to add binary assets.

Priority:

1. Prefer local shared library.
2. If no suitable local match exists, optionally fetch online and add to the local library.
3. Assign BGM to the game.

### 6.2 Library Layout

Shared library:

- shared_assets/bgm/index.json
- shared_assets/bgm/files/*.ogg

Game target path:

- games/<game_id>/app/src/main/assets/audio/bgm.ogg

### 6.3 License Gate (Hard Rule)

If Cursor downloads BGM from the internet, it must be redistributable.

Allowed:

- CC0
- Public Domain

Cursor must store license metadata in shared_assets/bgm/index.json for any downloaded BGM:

- source_title
- source_url
- license
- license_url
- retrieved_at (YYYY-MM-DD)

If license is not clearly CC0 or Public Domain, do not download or store the file.

### 6.4 Phase Behavior

- INIT:
  - Do not add BGM binaries.
  - No online download.
- OPTIMIZE:
  - Assign BGM only if the user requests BGM/audio.
  - Try local library first. If no suitable local match exists, then fetch online (CC0/PD only) and add to library, then assign.
  - If online fetch fails, proceed without BGM.
- PACK:
  - Must ensure the game has BGM if the local library has at least one entry.
  - If local library has no suitable entry, attempt online fetch (CC0/PD only) and add to library, then assign.
  - If online fetch fails and library is empty, proceed without BGM and report that BGM could not be assigned.

### 6.5 Required Commands

Assign from local library:

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/assign_bgm.ps1 -Project games/<game_id> -GameId <game_id> -Tag <tag>
```

Fetch online into local library (CC0/PD only) and optionally assign:

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/fetch_bgm.ps1 -GameId <game_id> -Tag <tag> -AssignProject games/<game_id>
```

------

## 7. Phase-Specific Workflow Constraints

### 7.1 INIT Phase Allowed Actions

Allowed:

- Fix Gradle sync issues
- Fix missing or incorrect wrapper files
- Fix manifest or resource errors blocking run
- Minimal code fixes required for app startup
- LFS pointer auto-remediation (required)
- Icon generation (required)

Forbidden:

- Feature expansion
- UI redesign beyond minimal correction
- Running validate.ps1
- Running build_apk.ps1
- Producing apk or zip artifacts
- Updating registry
- Git commit
- Online BGM download

INIT completion criteria:

- Android Studio sync succeeds
- App can be launched from Android Studio without immediate crash
- No LFS pointer files remain under games/<game_id>/
- app_icon resources resolve and show a real launcher icon

------

### 7.2 OPTIMIZE Phase Allowed Actions

Allowed:

- Gameplay feel improvements
- UI and UX polishing
- Visual enhancements
- Performance optimizations
- Stability fixes
- Rule compliance checks during edits

Forbidden:

- Running build_apk.ps1 by default
- Producing final apk or zip
- Updating registry or committing unless explicitly requested
- Introducing LFS pointer files

------

### 7.3 PACK Phase Allowed Actions

Required sequence:

1. Run environment doctor

2. Run LFS scan (must pass):

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/validate_lfs_pointers.ps1 -Path games/<game_id>
   ```

3. Ensure icon exists (generate if missing):

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/assets/generate_app_icon.ps1 -Project games/<game_id> -GameId <game_id>
   ```

4. Ensure BGM assigned (local first; if missing, fetch online CC0/PD only):

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/assets/assign_bgm.ps1 -Project games/<game_id> -GameId <game_id> -Tag <tag>
   ```

   If no suitable local entry exists:

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/assets/fetch_bgm.ps1 -GameId <game_id> -Tag <tag> -AssignProject games/<game_id>
   ```

5. Run validation if tool exists:

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/<game_id>
   ```

6. Run build if tool exists:

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/<game_id> -Variant debug
   ```

Allowed:

- Produce apk and zip artifacts
- Update registry/produced_games.json if user intends finalization
- Git commit after successful validation and build

Forbidden:

- Skipping LFS scan, validate, or build steps when tools exist

------

## 8. Registry and Git Rules

Registry and Git operations are allowed only during PACK phase and only after successful validation and build.

------

## 9. Role Definition

Cursor is:

1. An optimizer and refiner
2. A phase-aware pipeline executor
3. A stabilizer

Cursor is not:

1. A free-form game generator
2. A rule negotiator
3. A structure rewriter

------

## 10. Versioning Policy

Current version: v1.6

All changes must update the version number and remain consistent with pipeline tools and standards.