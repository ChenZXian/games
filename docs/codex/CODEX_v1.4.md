- - - You are operating inside a Git monorepo that contains multiple Android game projects.

      Authoritative files (must read first, do not skip):
      - docs/GAME_GENERATION_STANDARD.md
      - registry/produced_games.json
      - docs/ENVIRONMENT_BASELINE.md

      Scope for this run (IMPORTANT):
      - You ONLY generate the initial game project under games/<new_game_id>/.
      - Do NOT run any scripts (no doctor/validate/build_apk), do NOT build APK.
      - Do NOT git commit or git push.
      - Do NOT create KB entries.
      - If you detect a likely issue, fix it directly in the generated files.

      Repo root and path safety (hard requirements):
      1) Locate the true repo root as the directory that contains ALL of:
         - docs/
         - registry/
         - games/
         - tools/
        2) If not already at repo root, cd to repo root before any file writes.
        3) Never create nested paths like games/games/<id>.
        4) Never write projects under docs/ or registry/.
        5) The new project must be created exactly at: games/<new_game_id>/.
        6) Must not overwrite any existing directory under games/.

      Mandatory generation rules:
      1) Treat all rules in docs/GAME_GENERATION_STANDARD.md as HARD constraints.
      2) Enforce toolchain baseline from docs/ENVIRONMENT_BASELINE.md in generated files:
         - Gradle wrapper version
         - Android Gradle Plugin (AGP) version
         - compileSdk / targetSdk / minSdk
         - any other baseline constraints listed there
        3) Read registry/produced_games.json and ensure the new game's core gameplay loop is NOT a duplicate.
         - If similarity is detected, automatically change mechanics to be clearly distinct.

      --------------------------------------------------
      UI SKIN SYSTEM (HARD REQUIREMENT)
      --------------------------------------------------

      1) The generated project MUST use exactly ONE predefined UI skin.
      2) Allowed skin ids are defined in docs/GAME_GENERATION_STANDARD.md.
      3) Codex MUST:
         - Select one skin id.
         - Apply it consistently across all UI elements.
        4) UI appearance MUST resemble a commercial mobile game UI kit.
        5) UI implementation MUST be entirely text-based:
         - XML drawables (shape / gradient)
         - XML colors and dimens
         - Java code referencing tokens only
        6) External assets are FORBIDDEN:
         - No bitmap images
         - No fonts
         - No downloaded UI kits
         - No online fetching

      Required UI infrastructure to generate:
      - res/values/colors.xml (cst_ prefixed tokens only)
      - res/values/dimens.xml (spacing, radius, stroke tokens)
      - res/drawable/ui_panel.xml
      - res/drawable/ui_button_primary.xml
      - res/drawable/ui_button_secondary.xml

      Java code MUST NOT hardcode UI colors or dimensions.

      --------------------------------------------------
      LFS AND BINARY ASSET CONSTRAINTS (HARD)
      --------------------------------------------------

      1) Never output Git LFS pointer files anywhere in the repo.
         A file is an LFS pointer if its first line equals:
         version https://git-lfs.github.com/spec/v1
      2) Do NOT add or modify:
         - .gitattributes
         - .lfsconfig
         - any Git configuration files
        3) Do NOT include any binary assets:
           png / jpg / jpeg / webp / gif / bmp / ico
           ogg / mp3 / wav / m4a / aac
           ttf / otf
           so / bin
           apk / aab / zip
        4) Exception:
         - gradle/wrapper/gradle-wrapper.jar is allowed ONLY if required by baseline.
        5) Do NOT create app/src/main/res/raw.
        6) Ensure all generated files are text-based:
           Java / XML / Gradle / properties / json / md

      --------------------------------------------------
      APP ICON (TEXT-ONLY ADAPTATION)
      --------------------------------------------------

      AndroidManifest.xml MUST use:
      - android:icon="@mipmap/app_icon"
      - android:label="@string/app_name"

      Icon implementation MUST be XML-only:
      - mipmap-anydpi-v26/app_icon.xml (adaptive icon)
      - drawable/app_icon_fg.xml (vector or shape XML)
      - a background color resource referenced by the adaptive icon

      No bitmap icons are allowed in this step.

      --------------------------------------------------
      NO-CHINESE / NO-COMMENTS RULE
      --------------------------------------------------

      - No Chinese characters anywhere in the project.
      - All strings must be English ASCII only.
      - No comments anywhere:
        - No // or /* */ in Java or Gradle
        - No XML comments

      --------------------------------------------------
      COLOR RESOURCE RULE
      --------------------------------------------------

      - All custom colors MUST use prefix "cst_".
      - Names MUST NOT conflict with android.jar.
      - Forbidden examples:
        white, black, red, colorPrimary, material_*

      --------------------------------------------------
      PROJECT OUTPUT REQUIREMENTS
      --------------------------------------------------

      - Java only. Kotlin is forbidden.
      - Root package MUST be: com.android.boot
      - Entry Activity MUST be:
        com.android.boot.MainActivity
      - It MUST be the ONLY launcher activity.
      - Must include:
        - gradlew / gradlew.bat
        - gradle/wrapper/gradle-wrapper.properties
        - wrapper jar ONLY if required
      - Must include:
        - settings.gradle (Groovy)
        - app module with manifest + Java sources
      - AndroidManifest.xml MUST use:
        android:label="@string/app_name"
        android:icon="@mipmap/app_icon"

      Required resources:
      - res/layout/activity_main.xml
      - res/values/strings.xml containing at least:
        app_name
        btn_start
        btn_restart
        btn_resume
        btn_menu
        btn_mute
        btn_how_to_play
      - res/values/colors.xml
      - res/values/themes.xml

      --------------------------------------------------
      SUGGESTED PACKAGE STRUCTURE
      --------------------------------------------------

      com.android.boot
      com.android.boot.ui
      com.android.boot.core
      com.android.boot.entity
      com.android.boot.fx
      com.android.boot.audio

      --------------------------------------------------
      IMPLEMENTATION BASELINE
      --------------------------------------------------

      - Rendering: SurfaceView with a dedicated game loop thread
      - Use delta-time with clamping
      - Pause/resume must stop updates correctly
      - Handle lifecycle onPause/onResume
      - Keep allocations low inside the loop
      - No binary assets
      - Audio must use ToneGenerator only if required

      --------------------------------------------------
      REGISTRY UPDATE
      --------------------------------------------------

      - Append exactly ONE new entry to registry/produced_games.json
      - Do NOT modify or remove existing entries
      - Ensure JSON remains valid
      - Entry MUST include:
        id
        name
        tags
        core_loop
        created_at (YYYY-MM-DD)
        ui_skin (selected skin id)

      --------------------------------------------------
      GAME ID SELECTION
      --------------------------------------------------

      - Decide a unique new_game_id in lowercase snake_case
      - Create project at games/<new_game_id>/
      - Do NOT overwrite any existing directory

      --------------------------------------------------
      DELIVERABLE
      --------------------------------------------------

      - Output the complete generated project under games/<new_game_id>/
      - Output the updated registry/produced_games.json
      - Do NOT include explanations unless explicitly asked