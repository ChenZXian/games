- - - - - You are operating inside a Git monorepo that contains multiple Android game projects.

          Authoritative files (must read first, do not skip):
          - docs/GAME_GENERATION_STANDARD.md
          - docs/ENVIRONMENT_BASELINE.md
          - docs/UI_KIT_FACTORY_SPEC_v1_0.md
          - registry/produced_games.json

          SCOPE (HARD):
          - You ONLY generate the initial game project under games/<new_game_id>/.
          - Do NOT run any scripts (no doctor/validate/build_apk).
          - Do NOT run Gradle tasks (no assemble/build/bundle).
          - Do NOT build APK/AAB.
          - Do NOT create any zip artifacts.
          - Do NOT git commit or git push.
          - Do NOT create KB entries.

          Repo root and path safety (HARD):
          1) Locate the true repo root as the directory that contains ALL of:
             - docs/
             - registry/
             - games/
             - tools/
            2) If not already at repo root, cd to repo root before any file writes.
            3) Create exactly one new project directory at: games/<new_game_id>/.
            4) <new_game_id> must be globally unique, lowercase snake_case.
            5) Must NOT overwrite any existing directory under games/.
            6) Never create nested paths like games/games/<id>.
            7) Never write projects under docs/ or registry/.
            8) The ONLY allowed write outside games/<new_game_id>/ is appending ONE entry to registry/produced_games.json.

          Mandatory generation rules (HARD):
          1) Treat all rules in docs/GAME_GENERATION_STANDARD.md as HARD constraints.
          2) Enforce toolchain baseline from docs/ENVIRONMENT_BASELINE.md in generated files:
             - Gradle wrapper version (gradle-wrapper.properties)
             - Android Gradle Plugin (AGP) version
             - compileSdk / targetSdk / minSdk
             - any other baseline constraints listed there
            3) Read registry/produced_games.json and ensure the new game's core gameplay loop is NOT a duplicate.
             - If similarity is detected, automatically change mechanics to be clearly distinct.
            4) Do not ask the user to restate constraints that are already defined in authoritative docs.

          No-Chinese / No-Comments rule (HARD):
          - No non-ASCII characters anywhere in the project.
          - No comments anywhere:
            - No // or /* */ in Java or Gradle
            - No XML comments
          - All strings must be English ASCII.

          Project identity rules (HARD):
          - Root package MUST be: com.android.boot
          - Entry Activity MUST be: com.android.boot.MainActivity
          - It MUST be the ONLY launcher activity.
          - AndroidManifest.xml MUST use:
            - android:label="@string/app_name"
            - android:icon="@mipmap/app_icon"

          Binary + LFS rules (HARD):
          1) Never output Git LFS pointer files anywhere.
             A file is an LFS pointer if its first line equals:
             version https://git-lfs.github.com/spec/v1
          2) Do NOT add or modify:
             - .gitattributes
             - .lfsconfig
             - any Git configuration files
            3) Do NOT include any binary assets in the generated project:
               png / jpg / jpeg / webp / gif / bmp / ico
               ogg / mp3 / wav / m4a / aac
               ttf / otf
               so / bin
               apk / aab / zip
            4) gradle/wrapper/gradle-wrapper.jar:
             - Include it ONLY if the baseline requires it.
             - It MUST be a real jar file (not an LFS pointer).
             - If you cannot provide a real jar while respecting the binary ban, then omit the jar and ensure the wrapper properties match the baseline; do NOT create an LFS pointer as a placeholder.
            5) Do NOT create app/src/main/res/raw.

          App icon policy (HARD, generation-safe):
          - Implement @mipmap/app_icon using XML-only resources:
            - res/mipmap-anydpi-v26/app_icon.xml
            - res/drawable/app_icon_fg.xml
            - background color must be a cst_ token in res/values/colors.xml
          - Do NOT add bitmap mipmap icons.

          UI KIT SYSTEM (HARD, industrial):
          1) The generated project MUST use exactly ONE predefined UI skin id:
             - skin_dark_arcade
             - skin_cartoon_light
             - skin_neon_future
             - skin_post_apocalypse
             - skin_military_tech
            2) Select one skin id and apply it consistently across all UI elements.
            3) UI MUST comply with docs/UI_KIT_FACTORY_SPEC_v1_0.md (authoritative contract):
             - required cst_ color tokens exist
             - required dimens tokens exist
             - required styles exist
             - required drawable file set exists
             - required vector icon set exists
            4) UI implementation MUST be entirely text-based:
             - XML drawables (shape / gradient / layer-list / vector)
             - XML colors, dimens, styles, themes
             - Java code referencing tokens/styles only (no hardcoded styling)
            5) External assets are forbidden:
             - No bitmap images
             - No fonts
             - No downloaded UI kits
             - No online fetching

          UI file requirements (must generate at minimum):
          Values:
          - res/values/colors.xml
          - res/values/dimens.xml
          - res/values/styles.xml
          - res/values/themes.xml
            Drawables (XML only):
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
            Vector icons (XML only):
          - res/drawable/ic_play.xml
          - res/drawable/ic_pause.xml
          - res/drawable/ic_restart.xml
          - res/drawable/ic_sound_on.xml
          - res/drawable/ic_sound_off.xml
          - res/drawable/ic_help.xml

          Layout + screens (HARD):
          - Must include res/layout/activity_main.xml.
          - Must implement the required game states:
            MENU, PLAYING, PAUSED, GAME_OVER
          - All overlays and panels must use UI Kit drawables and styles.
          - Avoid inline literal colors in layouts; use tokens/styles.
          - UI must look like a commercial mobile UI baseline without external assets.

          Implementation baseline (HARD):
          - Rendering: SurfaceView with a dedicated game loop thread.
          - Use delta-time with clamping.
          - Keep allocations low inside the loop.
          - Pause/resume must stop updates correctly and handle lifecycle onPause/onResume.
          - Audio: no assets; use ToneGenerator only if necessary.

          Required strings (HARD):
          - res/values/strings.xml must include at least:
            app_name
            btn_start
            btn_restart
            btn_resume
            btn_menu
            btn_mute
            btn_how_to_play
            All values must be English ASCII.

          Gradle/project output requirements (HARD):
          - Java only. Kotlin is forbidden.
          - Must include Gradle wrapper scripts:
            - gradlew
            - gradlew.bat
            - gradle/wrapper/gradle-wrapper.properties
            - gradle/wrapper/gradle-wrapper.jar only if required and must NOT be LFS pointer
          - Must include:
            - settings.gradle (Groovy)
            - root build.gradle (per baseline)
            - app module with manifest + Java sources

          Suggested package structure (recommended):
          - com.android.boot (MainActivity)
          - com.android.boot.ui (GameView, overlays)
          - com.android.boot.core (engine/state/timer/util)
          - com.android.boot.entity
          - com.android.boot.fx
          - com.android.boot.audio

          Registry update (HARD):
          - Append exactly ONE new entry to registry/produced_games.json.
          - Do NOT modify or remove existing entries.
          - Ensure JSON remains valid.
          - Entry must include:
            id
            name
            tags
            core_loop
            created_at (YYYY-MM-DD)
            ui_skin (selected skin id)

          Deliverable (HARD):
          - Output the complete generated project under games/<new_game_id>/.
          - Output the updated registry/produced_games.json with exactly one appended entry.
          - Do NOT include explanations unless explicitly asked.