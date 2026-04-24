---
name: 新建项目
description: 在本单仓库中初始化一个全新的 Android Java 小游戏项目。只用于初始生成，不用于优化或打包。先读仓库标准，在 games/<new_game_id> 下创建一个新项目，追加一条 registry 记录，并刷新日期索引。
---

Use this skill only for initializing a brand new Android Java mini-game project inside the monorepo.

This skill is for initial project generation only.
It must not be used for optimization or packaging workflows.

You are operating inside a Git monorepo that contains multiple Android game projects.

Authoritative files must be read first and must not be skipped:
- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md
- docs/REQUIREMENTS_WORKFLOW.md
- docs/GAMEPLAY_DIVERSITY_WORKFLOW.md
- docs/VISUAL_IDENTITY_WORKFLOW.md
- registry/produced_games.json

Scope rules:
- Generate only the initial game project under games/<new_game_id>.
- Require a confirmed requirements trace before first-time project generation.
- If `artifacts/requirements/<new_game_id>/metadata.json` is missing, still `draft`, still `candidates`, or otherwise unconfirmed, stop and route back to requirements confirmation instead of generating project files.
- Prefer validating the requirements gate with `tools/requirements/assert_confirmed_trace.ps1 -GameId <new_game_id>` before any writes.
- Treat `artifacts/requirements/<new_game_id>/gameplay_diversity.json` as a required generation contract.
- If the gameplay diversity contract is missing, invalid, still draft, or not specific enough to pass `tools/requirements/check_gameplay_diversity.ps1 -GameId <new_game_id> -Strict`, stop and route back to requirements planning.
- Treat `artifacts/requirements/<new_game_id>/visual_identity.json` as a required UI and icon generation contract.
- If the visual identity contract is missing, invalid, still draft, or not specific enough to pass `tools/requirements/check_visual_identity.ps1 -GameId <new_game_id> -Strict`, stop and route back to requirements planning.
- Do not simplify the project into a generic map, one-unit roster, or repeated template when the contract asks for broader content.
- Do not initialize a project with a repeated top HUD pill layout, bottom command strip, or copied icon concept when the visual identity contract requires a different direction.
- Do not run doctor, validate, or build packaging workflows unless explicitly requested outside this skill.
- Do not run Gradle build tasks unless explicitly requested outside this skill.
- Do not build APK or AAB unless explicitly requested outside this skill.
- Do not create zip artifacts unless explicitly requested outside this skill.
- Do not git commit or git push.

Repo root and path safety rules:
1. Locate the true repo root as the directory that contains all of:
   - docs/
   - registry/
   - games/
   - tools/
2. If not already at repo root, switch to repo root before any file writes.
3. Create exactly one new project directory at games/<new_game_id>/.
4. <new_game_id> must be globally unique and use lowercase snake_case.
5. Do not overwrite any existing directory under games/.
6. Never create nested paths like games/games/<id>.
7. Never write projects under docs/ or registry/.
8. The only allowed write outside games/<new_game_id>/ is appending one entry to registry/produced_games.json and refreshing registry/projects_by_date.json.

Mandatory generation rules:
1. Treat all rules in docs/GAME_GENERATION_STANDARD.md as hard constraints.
2. Enforce the toolchain baseline from docs/ENVIRONMENT_BASELINE.md in generated files, including:
   - Gradle wrapper version in gradle-wrapper.properties
   - Android Gradle Plugin version
   - compileSdk
   - targetSdk
   - minSdk
   - any other listed baseline constraints
3. Read registry/produced_games.json and ensure the new game's core gameplay loop is not a duplicate.
4. If similarity is detected, automatically change mechanics until the design is clearly distinct.
5. Do not ask the user to restate constraints already defined in authoritative docs.

No-Chinese and no-comments rules:
- No non-ASCII characters anywhere in generated project text files unless the repository spec explicitly allows them.
- No comments anywhere:
  - No // or /* */ in Java or Gradle
  - No XML comments
- All generated strings must be English ASCII.

Project identity rules:
- Root package must be com.android.boot
- Entry Activity must be com.android.boot.MainActivity
- It must be the only launcher activity
- AndroidManifest.xml must use:
  - android:label="@string/app_name"
  - android:icon="@mipmap/app_icon"

Repository safety and binary handling rules:
1. Never output Git LFS pointer files anywhere.
2. Do not add or modify:
   - .gitattributes
   - .lfsconfig
   - Git configuration files
3. User-provided local binary files may be read, referenced, or used as inputs when the desktop environment supports them.
4. Do not introduce binary files into the generated project unless the repository rules and the current task explicitly allow them.
5. If the repository standard or current workflow requires text-only generation, prefer XML, Gradle, Java, and other text resources over binary assets.
6. gradle/wrapper/gradle-wrapper.jar:
   - Include it only if the baseline requires it
   - It must be a real jar file, not an LFS pointer
   - If a real jar is not available, omit it rather than creating a placeholder pointer
7. Do not create disallowed resource directories if prohibited by the active repository rules.

App icon policy:
- Follow the current repository icon policy defined by the authoritative docs.
- If generation-safe text-only icon resources are required, implement @mipmap/app_icon with XML resources.
- If binary icon assets are explicitly allowed by the repository rules and the task, they may be used.
- Prefer the repository-standard icon pipeline over ad hoc icon handling.

UI Kit system rules:
1. The generated project must use exactly one predefined UI skin id from the repository standard.
2. Select one skin id and apply it consistently across all UI elements.
3. UI must comply with docs/UI_KIT_FACTORY_SPEC_v1_0.md.
4. UI Kit is the required foundation and may be enough only for prototype-grade initialization.
5. Prefer text-based UI implementation for the structural scaffold:
   - XML drawables
   - XML colors
   - XML dimens
   - XML styles
   - XML themes
   - Java code referencing tokens and styles
6. Do not treat UI Kit-only XML scaffolding as delivery-ready UI for menu item `10`.
7. Do not use external UI kits or downloaded UI resources unless routed through the repository UI workflow and stored with license provenance.
8. Do not use external gameplay art assets unless they are routed through the repository gameplay art workflow and stored with license provenance.

Minimum required UI files:
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

Vector icons:
- res/drawable/ic_play.xml
- res/drawable/ic_pause.xml
- res/drawable/ic_restart.xml
- res/drawable/ic_sound_on.xml
- res/drawable/ic_sound_off.xml
- res/drawable/ic_help.xml

Layout and screen rules:
- Must include res/layout/activity_main.xml
- Must implement the required game states:
  - MENU
  - PLAYING
  - PAUSED
  - GAME_OVER
- All overlays and panels must use repository-approved UI styles and drawables
- Avoid inline literal colors in layouts when token resources are required
- UI should match a commercial mobile baseline within repository constraints

Implementation baseline:
- Rendering should use SurfaceView with a dedicated game loop thread if required by the repository standard
- Use delta time with clamping
- Keep allocations low inside the loop
- Pause and resume must stop updates correctly and handle lifecycle properly
- Use the repository-approved audio strategy for initialization-stage output

Required strings:
- res/values/strings.xml must include at least:
  - app_name
  - btn_start
  - btn_restart
  - btn_resume
  - btn_menu
  - btn_mute
  - btn_how_to_play
- All values must be English ASCII unless the repository standard explicitly allows otherwise

Gradle and project output requirements:
- Java only
- Kotlin is forbidden unless the repository standard explicitly changes
- Include the required Gradle wrapper scripts and Gradle files defined by the repository baseline
- Include:
  - settings.gradle
  - root build.gradle
  - app module with manifest and Java sources

Suggested package structure:
- com.android.boot
- com.android.boot.ui
- com.android.boot.core
- com.android.boot.entity
- com.android.boot.fx
- com.android.boot.audio

Registry update rules:
- Append exactly one new entry to registry/produced_games.json
- Do not modify or remove existing entries
- Ensure JSON remains valid
- Entry must include the repository-required fields

Development-stage hardening requirements:
A. Prepare release-ready configuration without executing release packaging:
1. Provide release buildTypes configuration with shrinking and obfuscation only where repository policy allows
2. Include a proguard rules file suitable for the generated project structure
3. Keep debug stable and debuggable

B. Controlled project structure variation:
1. Choose exactly one allowed structure variant if the repository workflow requires variants
2. Keep com.android.boot.MainActivity in com.android.boot
3. Do not invent unsupported variants
4. Reflect the chosen variant in registry tags if required

C. Static resource isolation:
1. Follow repository rules for binary and text resources
2. Achieve visual differentiation through UI tokens and repository-approved icon, UI, gameplay art, and audio assets
3. Keep all UI within the repository UI contract
4. For menu item `10`, continue into UI, gameplay art, icon, and audio completion after initialization instead of shipping the initialization scaffold

D. Packaging-stage preparedness:
1. Do not include keystore files
2. Keep signing ready for external configuration only
3. Keep Gradle structure compatible with future packaging workflows
4. Ensure versionCode and versionName are easy to override in later workflows

E. Non-duplication enforcement:
1. Read registry/produced_games.json and compare the new core_loop against existing entries
2. Read the confirmed gameplay diversity contract and preserve its genre sub-archetype, map budget, entity budget, mechanic budget, and asset variety budget
3. If similar, change the genre or mechanics until clearly distinct by:
   - perspective
   - control scheme
   - primary objective
   - feedback loop
   - map or playfield structure
   - entity roster
   - progression model
4. Update tags and core_loop to reflect the final distinct design

Execution requirements:
1. Verify that the requirements trace for <new_game_id> is already confirmed
2. Verify that the gameplay diversity contract for <new_game_id> has status `passed`
3. Verify that the visual identity contract for <new_game_id> has status `passed`
4. Choose a globally unique <new_game_id> using lowercase snake_case
5. Create games/<new_game_id>/ with a complete Android Studio project matching the baseline and standards
6. Append exactly one entry to registry/produced_games.json
7. Output only the generated project files and the updated registry file unless the user explicitly asks for explanation
