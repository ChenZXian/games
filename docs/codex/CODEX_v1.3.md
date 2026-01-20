- - You are operating inside a Git monorepo that contains multiple Android game projects.

    Authoritative files (must read first, do not skip):
    - docs/GAME_GENERATION_STANDARD.md
    - registry/produced_games.json
    - docs/ENVIRONMENT_BASELINE.md

    Scope for this run (IMPORTANT):
    - You ONLY generate the initial game project under games/<new_game_id>/.
    - Do NOT run any scripts (no doctor/validate/build_apk), do NOT build APK, do NOT git commit.
    - Do NOT create KB entries. If you detect a likely issue, fix it directly in the generated files.

    Repo root and path safety (hard requirements):
    1) Locate the true repo root as the directory that contains ALL of: docs/, registry/, games/, tools/.
    2) If not already at repo root, cd to repo root before any file writes.
    3) Never create nested paths like games/games/<id>. Never write projects under docs/ or registry/.
    4) The new project must be created exactly at: games/<new_game_id>/ and must not overwrite existing directories.

    Mandatory generation rules:
    1) Treat all rules in docs/GAME_GENERATION_STANDARD.md as hard constraints.
    2) Enforce toolchain baseline from docs/ENVIRONMENT_BASELINE.md in generated project files:
       - Gradle wrapper version
       - Android Gradle Plugin (AGP) version
       - compileSdk / targetSdk / minSdk
       - any other baseline constraints listed there
      3) Read registry/produced_games.json and ensure the new game's core gameplay loop is NOT a duplicate of any existing entry.
       - If similar to an existing game, automatically adjust the loop to be clearly distinct.

    LFS and Binary Asset Constraints (Hard Rules):
    1) Never output Git LFS pointer files anywhere in the repo. A file is an LFS pointer if its first line equals:
       version https://git-lfs.github.com/spec/v1
    2) Do NOT add or modify .gitattributes, .lfsconfig, or any Git configuration files.
    3) Do NOT include binary assets (png/jpg/jpeg/webp/gif/bmp/ico/ogg/mp3/wav/m4a/aac/ttf/otf/so/bin/apk/aab/zip) in the generated project.
    4) Exception (allowed binary only if required by baseline):
       - gradle/wrapper/gradle-wrapper.jar is allowed ONLY if docs/ENVIRONMENT_BASELINE.md or standard Gradle wrapper structure requires it.
       - Do not include any other .jar files anywhere.
      5) Do not create app/src/main/res/raw at all.
      6) Ensure all generated files are text-based (Java/XML/Gradle/properties/json/md) except the single allowed wrapper jar exception above.

    App icon requirement adaptation (must still satisfy manifest rules and compile without bitmap files):
    - AndroidManifest.xml must use android:icon="@mipmap/app_icon" and android:label="@string/app_name"
    - Implement @mipmap/app_icon using XML resources only (no bitmap files):
      - Provide mipmap-anydpi-v26/app_icon.xml (adaptive icon)
      - Provide drawable/app_icon_fg.xml as a vector or shape XML (XML only)
      - Provide a background color resource and reference it from the adaptive icon XML
    - Ensure the project compiles with these XML icon placeholders.
    - Real bitmap icons will be provided later by another step outside Codex.

    No-Chinese / No-Comments rule:
    - No Chinese characters anywhere in the entire generated project (Java/XML/Gradle/strings/resources/asset files). All strings must be English.
    - No comments anywhere (no //, /* */, or XML comments).

    Color resource naming rule:
    - All custom color resource names must NOT conflict with android.jar names and must use prefix "cst_".
    - Do not use names like white/black/red/colorPrimary/material_*.

    Project output requirements (must be a complete Android Studio Java project):
    - Must be Java only (no Kotlin).
    - Root package: com.android.boot
    - Entry Activity must be com.android.boot.MainActivity and it must be the ONLY launcher activity.
    - Must include its own gradlew/gradlew.bat and gradle/wrapper/gradle-wrapper.properties (and wrapper jar only if required as the sole allowed binary).
    - Must include settings.gradle (Groovy) and an app/ module with manifest + Java sources.
    - AndroidManifest.xml must use android:label="@string/app_name" and android:icon="@mipmap/app_icon".
    - Must include res/layout/activity_main.xml.
    - Must include res/values/strings.xml with at least:
      - app_name
      - btn_start
      - btn_restart
      - btn_resume
      - btn_menu
      - btn_mute
      - btn_how_to_play
        All in English only.
    - Must include res/values/colors.xml and themes.xml, using only cst_ prefixed custom colors.

    Suggested package structure under com.android.boot (follow standard if it requires different):
    - com.android.boot (MainActivity)
    - com.android.boot.ui (GameView, UI overlays/controller)
    - com.android.boot.core (GameEngine, GameState enum, Timer, MathUtil)
    - com.android.boot.entity (game entities as needed)
    - com.android.boot.fx (Particle, FloatText if needed)
    - com.android.boot.audio (SoundManager if needed)

    Implementation baseline requirements:
    - Rendering: SurfaceView with a dedicated game loop thread, delta-time with clamping.
    - Pause/resume: stop updates when paused; handle lifecycle onPause/onResume correctly.
    - Keep allocations low inside the loop.
    - Do not include any binary assets.
    - Any audio behavior must avoid binary audio files (use ToneGenerator if audio is required).

    Registry update (format requirement only):
    - Append exactly one new entry to registry/produced_games.json for the newly generated game.
    - Do not remove or modify existing entries except appending the new one.
    - Ensure JSON remains valid.

    Game selection and new_game_id (PLACEHOLDER RULES ONLY):
    - Decide a unique new_game_id in lowercase snake_case and create the project at games/<new_game_id>/.
    - Do not overwrite existing directories under games/.
    - The actual game design content must be provided separately and is not included in this template.

    Deliverable:
    - Output the complete generated files under games/<new_game_id>/ and the updated registry/produced_games.json.
    - Do not include explanations unless asked.