# Android Java Mini-Game Monorepo

This repository is a monorepo for Android Java mini-games.

It contains:

- multiple independent Android Studio game projects under `games/`
- shared repository rules and environment baselines under `docs/`
- active Cursor companion rules under `docs/cursor/`
- workflow tools under `tools/`
- shared assets under `shared_assets/`
- project registry data under `registry/`
- archived legacy process documents under `archive/`

## Repository Purpose

This repository is designed to support a consistent mini-game pipeline instead of one-off projects.

The current direction is:

1. plan game requirements before project generation
2. generate or update one game project inside `games/<game_id>/`
3. optimize game quality and polish
4. handle dedicated resource workflows such as icon, UI, gameplay art, and audio
5. inspect and package only when explicitly requested, or export APK as the final stage of menu item `10`

## Authoritative Files

Always read and follow these files before making project changes:

- `AGENTS.md`
- `docs/GAME_GENERATION_STANDARD.md`
- `docs/ENVIRONMENT_BASELINE.md`
- `docs/UI_KIT_FACTORY_SPEC_v1_0.md`
- `docs/PROJECT_ACCEPTANCE_BASELINE.md`
- `docs/GAMEPLAY_DIVERSITY_WORKFLOW.md`
- `registry/produced_games.json`

These files define:

- repository rules
- Android and Gradle baseline
- UI contract
- generic project acceptance baseline
- gameplay diversity and content-scale baseline
- launcher and package constraints
- game uniqueness requirements

## Current Workflow

### Repository Menu

Current menu structure:

```text
--------------------------------
菜单

需求阶段
1｜先出 10 个候选方案
2｜为已选方案补完整需求

开发阶段
3｜新建游戏项目
4｜优化现有项目
5｜生成或更新游戏图标
6｜优化游戏界面与美术素材
7｜生成或接入游戏音频

交付阶段
8｜先检查项目状态
9｜打包或发布项目

其他
10｜一键执行完整流程并导出 APK
11｜只讨论规则、方案或架构
--------------------------------
```

Menu display convention:

- new conversations should start with the menu only
- later discussion replies should append the menu at the very bottom
- `$菜单` or `$menu` can be used as a repository-level shortcut to request the current menu only
- menu numbers should be rendered as plain text labels to avoid Markdown list indentation in the app UI
- the full menu should be rendered inside one fenced `text` code block
- the menu block should contain one horizontal separator above and one below
- stable plain-text numeric menu codes such as `1｜` through `11｜` should be used inside the rendered menu block
- menu guidance should stay minimal and ask only for missing required input

### 1. Requirements Planning

Before first-time project initialization, use the planning flow:

1. provide a broad game direction
2. generate 10 candidate mini-game concepts
3. select exactly 1 concept
4. generate a full game requirements document
5. confirm the requirements
6. enter project initialization only after confirmation

Current planning skill:

- `.agents/skills/game-requirements-planner/`
- `docs/REQUIREMENTS_WORKFLOW.md`
- `tools/requirements/create_candidates_trace.ps1`
- `tools/requirements/create_requirements_trace.ps1`
- `tools/requirements/confirm_trace.ps1`
- `tools/requirements/init_trace.ps1`

Planning trace target after concept selection:

- `artifacts/requirements/<game_id>/`

Planning state flow:

- `candidates` -> candidate concepts stored
- `draft` -> full requirements written, shown to the user for review, and not confirmed yet
- `confirmed` -> full requirements explicitly confirmed and ready to unblock initialization

Machine-readable planning contract:

- `artifacts/requirements/<game_id>/gameplay_diversity.json`

This contract defines:

- genre family and sub-archetype
- control model and core loop signature
- map or playfield content budget
- entity and mechanic budgets
- asset variety budget
- forbidden template reuse

### 2. Project Initialization

Use initialization only after requirements are confirmed.

Current initialization skill:

- `.agents/skills/mini_game_project_init/`

### 3. Optimization

Use optimization for gameplay feel, UI polish, responsiveness, performance, and bug fixing inside an existing game project.

Current optimization skill:

- `.agents/skills/mini_game_optimize/`

### 4. Icon Workflow

Icon generation is now a dedicated workflow.

Default icon direction:

- cartoon style
- clear visual link to the target game's theme or core loop

Current icon workflow:

- `docs/ICON_WORKFLOW.md`
- `.agents/skills/mini_game_icon/`
- `tools/assets/generate_app_icon.ps1`

Icon outputs:

- in-project launcher icon resources under `games/<game_id>/app/src/main/res/`
- upload-ready exports under `artifacts/icons/<game_id>/`

### 5. UI Workflow

UI is now a dedicated workflow instead of incidental polish.

Current UI workflow goals:

- define screen structure before implementation
- keep exactly one `ui_skin`
- support a hybrid Java game UI model with `GameView` or `SurfaceView` for gameplay and Android View or XML overlays for HUD and screens
- allow licensed binary UI assets, fonts, and open-source UI resource packs
- keep reusable external UI resources in the shared library first when possible
- require a concrete UI brief before pack selection or implementation
- resolve in this order: style-matched shared UI pack -> imported licensed open-source UI pack -> project-local custom refinement
- treat UI Kit as the required foundation, not final visual quality for menu item `10` or delivery-ready output
- treat `project_local_xml_ui` as placeholder-only for production-grade requests
- avoid silently shipping shape-only placeholder UI for production-grade requests

Current UI workflow:

- `docs/UI_WORKFLOW.md`
- `.agents/skills/mini_game_ui/`
- `tools/assets/ensure_ui_pack.ps1`
- `tools/assets/import_ui_pack.ps1`
- `tools/assets/assign_ui.ps1`

Shared library target:

- `shared_assets/ui/`

### 6. Game Art Workflow

Gameplay art is now a dedicated workflow instead of being treated as UI or ad hoc Canvas placeholders.

Current gameplay art workflow goals:

- separate characters, enemies, animals, maps, tilesets, props, items, projectiles, effects, and backgrounds from UI packs
- use only free and license-clear sources by default
- prefer CC0 or public-domain equivalent packs
- keep reusable external gameplay art resources in the shared library first when possible
- require a concrete art brief before pack selection or implementation
- resolve in this order: style-matched shared game art pack -> imported official free and license-clear pack -> project-local prototype drawing only when placeholders are acceptable
- require assigned gameplay art to be used by the running game for delivery-ready output
- require `runtime_art_map.json` for delivery-ready output
- require entity facing and animation rules instead of one static bitmap moving across the screen
- require primary characters to use walk or run frame animation and attack pose changes when the game calls for it
- allow the sprite pipeline to generate normalized whole-strip animations from approved license-clear seed frames when no suitable animated pack exists
- avoid silently shipping circle-only or rectangle-only gameplay placeholders for production-grade requests
- follow the gameplay diversity contract so different games do not collapse into the same tiny map, same roster, and same asset layout

Current gameplay art workflow:

- `docs/GAME_ART_WORKFLOW.md`
- `.agents/skills/mini_game_art/`
- `tools/assets/ensure_game_art_pack.ps1`
- `tools/assets/import_game_art_pack.ps1`
- `tools/assets/assign_game_art.ps1`

Shared library target:

- `shared_assets/game_art/`
- `shared_assets/game_art/animation_catalog.json`

### 7. Audio Workflow

Audio generation and assignment is a dedicated workflow.

Current audio workflow goals:

- support both BGM and SFX
- keep generated or fetched audio in the shared library first
- assign project audio from the shared library as needed
- keep audio direction aligned with the game's theme and feel
- require an explicit audio brief before assignment
- resolve in this order: style-matched shared library -> licensed fetch -> synthesis fallback
- avoid silently reusing style-mismatched legacy tracks

Current audio workflow:

- `docs/AUDIO_WORKFLOW.md`
- `.agents/skills/mini_game_audio/`
- `tools/assets/ensure_audio_bundle.ps1`
- `tools/assets/assign_audio.ps1`
- `tools/assets/fetch_audio.ps1`
- `tools/assets/synth_audio.ps1`

Shared library target:

- `shared_assets/audio/`

### 8. Inspect Workflow

Inspect is a dedicated pre-packaging status workflow.

Current inspect workflow goals:

- report whether a project can enter packaging
- summarize requirements, gameplay diversity, icon, UI, gameplay art, and audio completion state
- surface the next most useful action without modifying project files

Current inspect workflow:

- `docs/INSPECT_WORKFLOW.md`
- `.agents/skills/mini_game_inspect/`
- `tools/inspect.ps1`

### 9. Packaging

Packaging is a separate workflow. It should run only when directly requested through packaging, or when menu item `10` reaches the final APK export stage after requirements confirmation, initialization, resource completion, and inspection.

Current packaging skill:

- `.agents/skills/mini_game_pack/`

### 10. Full Pipeline Mode

The repository menu now includes a full-pipeline mode.

Default behavior:

- requirements planning
- initialization
- optimization and resource completion, including icon, UI, gameplay art, and audio
- inspection
- APK export through the packaging workflow

Expected interaction behavior:

- if the user provides a direction together with menu item `10`, generate 10 candidate concepts immediately and stop for concept selection
- after the user selects one candidate, generate the full requirements draft immediately, show it to the user, and stop for explicit confirmation
- when a target `game_id` is known, store that draft requirements trace under `artifacts/requirements/<game_id>/` before continuing
- do not enter initialization or downstream workflows in the same reply as the full requirements draft
- after the user explicitly confirms the requirements, continue the downstream generation flow automatically
- menu item `10` is the complete flow from requirements confirmation to APK output
- after initialization and resource completion, run inspection and export an APK only when `CAN_ENTER_PACK=true`
- if inspection or packaging prerequisites fail, stop and report the blocker instead of claiming completion
- if the user asks for revisions, revise the requirements draft and keep it in `draft`
- ask only for missing direction input when menu item `10` does not include enough idea information
- do not ask meta permission questions in the standard full-pipeline flow
- only use auto-decision behavior when the user explicitly authorizes Codex to decide on concept selection or requirements confirmation

## Project Acceptance Baseline

This repository does not require one fixed game genre.

Any mini-game type is acceptable if it satisfies:

- `docs/PROJECT_ACCEPTANCE_BASELINE.md`
- `docs/GAME_GENERATION_STANDARD.md`
- `docs/ENVIRONMENT_BASELINE.md`
- `docs/UI_KIT_FACTORY_SPEC_v1_0.md`

## Date Organization

For tool compatibility, active projects still remain under:

- `games/<game_id>/`

Chronological organization is maintained through:

- `registry/projects_by_date.json`
- `tools/rebuild_projects_by_date.ps1`

This keeps inspection, validation, initialization, and packaging scripts stable while still giving the repository a date-based index.

## Roles

Current role split:

- User: gives direction, selects a concept, confirms requirements, and decides when to package or deliver
- Codex: supports the full repository workflow end to end
- Cursor: optional auxiliary role for UI polish, local resource wiring, and focused engineering refinements

## Root Directory Layout

Main root directories:

- `docs/` - active standards and workflow documents
- `archive/` - legacy or superseded process documents kept for reference
- `games/` - actual Android Studio mini-game projects
- `tools/` - repository scripts such as validation, build, env, and asset tools
- `registry/` - game registry, uniqueness tracking, and chronological project index
- `shared_assets/` - shared reusable assets and asset libraries
- `artifacts/` - build outputs and exported deliverables

Main root files:

- `AGENTS.md` - repository interaction and workflow rules
- `README.md` - repository entry documentation
- `gradle.properties` - root Gradle baseline settings
- `.gitignore`
- `.gitattributes`

## Archive Policy

Legacy process notes and replaced workflows should not stay in the root directory.

Current archive areas:

- `archive/icon_legacy/` - older icon generation flow and source samples
- `archive/bgm_legacy/` - older BGM workflow notes
- `archive/cursor_legacy/` - older Cursor rules versions kept for reference
- `archive/misc/` - uncategorized retired root-level files

Archived documents are kept only for historical reference.
They should not be treated as the active repository standard.

## Current Repository State

The repository now has:

- a menu-driven workflow entry point in `AGENTS.md`
- a dedicated requirements planning stage before initialization
- a dedicated icon workflow with project outputs and export outputs
- a dedicated UI workflow with a shared UI asset library target
- a dedicated gameplay art workflow with a shared free and license-clear asset library target
- a dedicated audio workflow for reusable BGM and SFX assets
- a dedicated inspect workflow before packaging decisions
- a generic project acceptance baseline that is not tied to one game genre
- a chronological index under `registry/projects_by_date.json`
- a single active Cursor rules document under `docs/cursor/`
- root-level cleanup of outdated pipeline notes into `archive/`

## Recommended Next Steps

Recommended next improvements:

1. validate the new gameplay art workflow on a real game project
2. validate the new UI workflow on a real game project
3. validate the new audio workflow on a real game project
4. validate the new icon workflow on a real game project
5. validate the full pipeline mode on a brand new game from planning through packaging
6. validate the gameplay diversity contract on multiple genres, not only tower defense
