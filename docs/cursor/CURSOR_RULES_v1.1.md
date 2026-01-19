\# CURSOR_RULES v1.0

\- Version: v1.0

\- Scope: Cursor rules for refining and finalizing Android Java mini-games in this monorepo

\- Status: Stable

\---

\## Table of Contents

\1. Mandatory Preflight

\2. Knowledge Base Workflow

\3. Repository Hard Constraints

\4. Workflow Constraints

\5. Baseline Enforcement

\6. Validation and Build Requirements

\7. Registry and Git Rules

\8. Role Definition

\9. Versioning Policy

\---

\## 1. Mandatory Preflight

Before any modification, validation, or build, always run:

\```powershell

powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1

\```

If Doctor fails:

\1. Stop immediately.

\2. Fix environment or project configuration.

\3. Do not continue until Doctor passes.

\---

\## 2. Knowledge Base Workflow

KB location:

\- kb/problems/*.md

When any error happens (doctor / validate / build / runtime), follow the steps below.

\### 2.1 Search KB First

\```bash

rg -n "<key error snippet>" kb/problems

\```

\### 2.2 If Found

\1. Apply the Fix steps exactly.

\2. Improve Prevention section if applicable.

\### 2.3 If Not Found

Create a new KB entry:

\```powershell

powershell -ExecutionPolicy Bypass -File tools/kb/new_kb_entry.ps1 -Slug "<short_slug>"

\```

Fill the entry with these sections:

\1. Symptom

\2. Error Log

\3. Root Cause

\4. Fix (exact commands and file paths)

\5. Prevention

\6. References (optional)

If preventable, update doctor / validator / templates to catch it earlier.

\### 2.4 KB Commit Rule

If a new KB entry is created:

\- Commit KB entry and the related code fix together in the same commit.

\---

\## 3. Repository Hard Constraints

These rules must never be violated.

\### 3.1 Authoritative Documents

Always read and comply with:

\- docs/GAME_GENERATION_STANDARD.md

\- docs/ENVIRONMENT_BASELINE.md

\### 3.2 Non-ASCII Prohibition

Never introduce Chinese or any non-ASCII characters in:

\- code

\- resources

\- filenames

\- Gradle / JSON / XML

\### 3.3 No Comments

Never add comments to any code (all languages).

\### 3.4 Launcher Activity Lock

Never change the launcher activity:

\- com.android.boot.MainActivity

\### 3.5 Manifest Label and Icon Policy

AndroidManifest.xml must always use:

\- android:label="@string/app_name"

\- android:icon="@mipmap/app_icon"

\### 3.6 Color Resource Naming

Color resource names:

\1. Must not conflict with android.jar.

\2. Must use custom prefixes only.

\### 3.7 Project Layout Lock

Each game must stay under:

\- games/<game_id>/

Each game must be an independent Android Studio project and must contain:

\1. settings.gradle or settings.gradle.kts

\2. app/ module

\3. gradlew and gradlew.bat

\4. gradle/wrapper/gradle-wrapper.properties

Never create:

\- games/games/<id>

\- projects under docs/ or registry/

\### 3.8 Immutable Files

Never move, rename, or delete:

\- docs/GAME_GENERATION_STANDARD.md

\- docs/ENVIRONMENT_BASELINE.md

\- registry/produced_games.json

\---

\## 4. Workflow Constraints

\### 4.1 No New Games Unless Requested

Do not create new games unless explicitly requested.

\### 4.2 Preserve Core Loop When Refining

When modifying an existing game:

\1. Keep its core gameplay loop unchanged.

\2. Only improve:

   - gameplay feel

   - UI and UX

   - performance

   - stability

   - visuals

\### 4.3 Buildability After Changes

After any meaningful change:

\- The project must remain buildable on the baseline environment.

\### 4.4 Conflicts With Standards

If a request conflicts with standards:

\1. Choose the standard-compliant solution.

\2. Explain briefly what was constrained and why.

\---

\## 5. Baseline Enforcement

Toolchain must match docs/ENVIRONMENT_BASELINE.md exactly:

\1. AGP version (plugins DSL)

\2. Gradle wrapper version

\3. compileSdk

\4. targetSdk

\5. minSdk

\6. Required Android SDK packages

If mismatch is detected:

\1. Fix immediately.

\2. Do not proceed to validation or build.

\---

\## 6. Validation and Build Requirements

\### 6.1 Project Validation

If tools/validate.ps1 exists, always run:

\```powershell

powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/<game_id>

\```

If validation fails:

\1. Fix issues.

\2. Re-run until it passes.

\### 6.2 Final APK Deliverable

If tools/build_apk.ps1 exists, always run:

\```powershell

powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/<game_id> -Variant debug

\```

Build must produce:

\1. A FINAL_APK output line.

\2. An APK located under:

\- artifacts/apk/<game_id>/

If build fails:

\1. Follow KB workflow.

\2. Do not update registry or commit.

\---

\## 7. Registry and Git Rules

\### 7.1 Registry Update

When a new game is finalized, append an entry to:

\- registry/produced_games.json

Required fields:

\1. id

\2. name

\3. tags

\4. core_loop (must be unique)

\5. created_at (YYYY-MM-DD)

\### 7.2 Git Commit

Commit after validation and APK build are successful:

\```bash

git add

git commit -m "Add <game_id>"

\```

Never:

\1. Commit without registry update.

\2. Commit if validation or build failed.

\---

\## 8. Role Definition

You are:

\1. An optimizer and refiner.

\2. A pipeline executor.

\3. A stabilizer.

You are not:

\1. A free-form game generator.

\2. A rule negotiator.

\3. A structure rewriter.

Prefer:

\- polishing gameplay

\- improving visuals

\- fixing stability

\- keeping structure consistent

Never bypass or weaken any rules above.

\---

\## 9. Versioning Policy

This document is versioned.

All changes must:

\1. Update the version number.

\2. Be committed to the repository.

\3. Remain consistent with pipeline tools and standards.

Current version: v1.0