- You are operating inside a Git monorepo that contains multiple Android game projects.

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
  3) Do NOT include binary assets (png/jpg/webp/ogg/mp3/wav) in the generated project.
  4) App icon requirement adaptation (must still satisfy manifest rules):
     - AndroidManifest.xml must still use android:icon="@mipmap/app_icon"
     - Implement @mipmap/app_icon using XML resources only (no bitmap files):
       - Provide mipmap-anydpi-v26/app_icon.xml (adaptive icon)
       - Provide drawable/app_icon_fg.xml as a vector or shape XML
       - Provide a background color resource and reference it from the adaptive icon XML
     - Ensure the project compiles with these XML icon placeholders.
     - Real bitmap icons will be provided later by another step outside Codex.

  Game selection and ID:
  - Decide a unique new_game_id in lowercase snake_case (e.g., sky_relic_runner).
  - Create the project at games/<new_game_id>/.

  Game to generate (initial version, must be playable):
  Name: Sky Relic Runner
  Core loop:
  - Endless 2D side-scrolling auto-runner with platforming.
  - Player auto-runs right.
  - Inputs: Jump (short/long press height), Dash skill (brief invulnerability, cooldown, energy).
  - Collectibles: shards (score), cores (energy), rare rune (temporary buff).
  - Hazards: spikes, moving blocks, collapsing platforms, falling off-screen.
  - Difficulty scales with distance (speed + spawn frequency + pattern complexity).
    Game states:
  - Menu (Start button)
  - Playing (HUD: distance, score, energy)
  - Pause overlay (Resume + Restart)
  - Game over overlay (results + Restart)
    Persistence:
  - Store best score and best distance locally.

  Visual/UI requirements:
  - No external downloads required.
  - Use Canvas drawing with layered/parallax background and clean HUD panels.
  - On-screen buttons must be large, rounded, and positioned to avoid blocking gameplay (Jump left-bottom, Skill right-bottom, Pause top-right).
  - Provide press feedback and reliable multi-touch handling.

  Project output requirements:
  - Must be a complete Android Studio Java project (no Kotlin).
  - Must include its own gradlew/gradlew.bat and gradle/wrapper/gradle-wrapper.properties.
  - Must include settings.gradle(.kts) and app/ module with manifest + Java sources.
  - Must match launcher activity, labels, icons, and “no Chinese / no comments” rules as defined in GAME_GENERATION_STANDARD.md.
  - Must use android:label="@string/app_name" and android:icon="@mipmap/app_icon" in manifest.
  - Do NOT include bitmap icon assets in mipmap densities; icon must be XML-only as defined above.

  Registry update:
  - Append a new entry to registry/produced_games.json describing this new game and its unique core_loop (do not commit; just modify the file).

  Deliverable:
  - Output the complete generated files under games/<new_game_id>/ and the updated registry/produced_games.json.
  - Do not include explanations unless asked.