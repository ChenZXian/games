# CURSOR_RULES v1.8

- Version: v1.8
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
- Phase switching is allowed ONLY if the user explicitly requests it
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

### 3.1 Package Identity Lock (NEW)

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

------

## 4. LFS Pointer Handling (Hard Gate)

(unchanged from v1.6)

------

## 5. Icon Generation Policy (INIT Required, No Default Bitmap)

(unchanged from v1.6)

------

## 6. BGM Library Policy (Local First, Then Online)

(unchanged from v1.6)

------

## 7. Phase-Specific Workflow Constraints

### 7.1 INIT Phase (HARD GATE)

Before executing any action, Cursor MUST output a one-line execution plan:

PHASE=INIT; WILL_RUN=[doctor, sync_fixes, lfs_cleanup, icon_generation]

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
- Icon generation

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

INIT completion criteria:

- Android Studio sync succeeds
- App can be launched without immediate crash
- No LFS pointer files remain
- app_icon resolves correctly

------

### 7.2 OPTIMIZE Phase Allowed Actions

(unchanged from v1.6)

------

### 7.3 PACK Phase Allowed Actions

(unchanged from v1.6)

------

## 8. Registry and Git Rules

(unchanged from v1.6)

------

## 9. Role Definition

(unchanged from v1.6)

------

## 10. Versioning Policy

Current version: v1.8