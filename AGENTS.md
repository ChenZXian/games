# Mini Game Repository Rules

This repository is for Android Java mini-games.

Always read and obey these authoritative files before making changes:
- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md
- docs/PROJECT_ACCEPTANCE_BASELINE.md
- docs/GAMEPLAY_DIVERSITY_WORKFLOW.md
- registry/produced_games.json

Hard rules:
- Java only
- No Chinese in generated code, resources, file names, Gradle files, or generated project strings
- No comments in generated code, XML, or Gradle files
- Root package must be com.android.boot
- Launcher activity must be com.android.boot.MainActivity
- AndroidManifest.xml must use @string/app_name and @mipmap/app_icon
- Use exactly one allowed ui_skin
- Follow repository path safety rules
- Do not invent rules that conflict with the authoritative docs
- If a request conflicts with the spec, choose the spec-compliant solution

Default workflow policy:
- Default to discussion, design, and implementation only
- Do not package, build APK, create zip, or run packaging steps unless explicitly requested, except when menu item `10` reaches its final APK export stage after requirements confirmation and downstream project completion
- Treat requirements planning, initialization, optimization, resource completion, inspection, and packaging as separate workflows
- Use repository-safe behavior by default
- Do not modify files immediately at the start of a new conversation
- Do not enter the initialization workflow before requirements confirmation is completed
- Treat icon, UI, gameplay art, and audio as formal workflow tracks, not incidental polish items
- Allow the icon workflow to generate project icon assets and upload-ready exported icons under artifacts/icons/<game_id>/
- Allow the UI workflow to use licensed binary UI assets, fonts, and open-source UI resource packs through shared_assets/ui/ when requested
- Allow the gameplay art workflow to use free, license-clear character, map, tileset, prop, effect, and background assets through shared_assets/game_art/ when requested
- Keep active game projects as direct children under games/<game_id>/ for tool compatibility
- Use registry/projects_by_date.json for chronological organization instead of nesting active projects by date under games/

Conversation start policy:
- At the beginning of each new conversation in this repository, the first response must be a menu only
- The menu must be written in Chinese
- Do not provide implementation details, file edits, action plans, or code before the user selects a menu item
- Wait for the user's menu selection before taking action
- After the user selects a menu item, follow the matching workflow skill if available
- If the user request is already extremely explicit and clearly selects a workflow, you may honor it directly, but still prefer the repository menu format when possible
- For later discussion or planning replies in this repository, always append the current menu at the very bottom of the reply
- Prefer grouped menu sections and one clear action per line
- Never place the appended menu above the main body of the reply
- Treat `菜单` and `menu` as an explicit request to return the current repository menu only
- Render menu item numbers as plain text labels rather than Markdown ordered lists to avoid UI indentation
- Render the full menu inside one fenced `text` code block to avoid renderer-specific font-size or weight changes
- Keep one horizontal separator line at the top and bottom inside the menu block
- Use stable plain-text numeric menu codes such as `1｜` through `11｜` inside the rendered menu block
- After a menu selection, ask only for the smallest missing input required to continue
- Do not add explanatory filler, meta framing, or optional questionnaires unless the workflow is actually blocked

Required start menu format:
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

直接回复：
- 菜单代号
- 目标游戏 id、方向或方案名
- 具体需求

Menu selection parsing:
- Accept Arabic number input such as `1` to `11`
- Accept the exact menu line text when unambiguous

Behavior rules after menu selection:
- If the user selects 1, use the game-requirements-planner skill to generate 10 candidate mini-game concepts first and do not modify project files
- If the user selects 2, use the game-requirements-planner skill to generate the full game requirements for the chosen concept and do not enter project initialization yet
- If the user selects 3, use the initialization workflow only after the game requirements are explicitly confirmed
- If the user selects 4, use the optimization workflow for an existing project
- If the user selects 5, use the mini_game_icon skill when available; define the icon direction first, prefer a cartoon style that clearly matches the game, then update project assets and export upload-ready icon files when requested
- If the user selects 6 and the request is about menus, HUD, panels, buttons, overlays, or screen layout, use the mini_game_ui skill when available; define UI structure, ui_skin, and asset strategy first, then perform implementation or polish as requested
- If the user selects 6 and the request is about characters, enemies, animals, maps, tilesets, props, items, projectiles, effects, or gameplay backgrounds, use the mini_game_art skill when available; use only free, license-clear assets and prefer CC0 sources
- If the user selects 7, use the mini_game_audio skill when available; define the audio direction first, cover both BGM and SFX, then generate, store, and assign resources by stage
- If the user selects 8, use the mini_game_inspect skill when available and do not modify project files during the inspection workflow
- If the user selects 9, use the packaging workflow
- If the user selects 10 and provides a direction, theme, or subject, immediately generate 10 candidate concepts in the same reply and stop for concept selection
- After the user selects one candidate under menu item 10, immediately generate the full requirements draft in the next reply, show that draft to the user, and stop for explicit requirements confirmation
- When menu item 10 reaches the full requirements draft stage and a target game_id is known, persist the requirements trace as status `draft`
- Do not enter initialization, optimization, icon, UI, gameplay art, audio, inspection, or packaging in the same reply as the full requirements draft under menu item 10
- After the user explicitly confirms the requirements under menu item 10, mark the trace as `confirmed` and automatically continue initialization, optimization, icon completion, UI completion, gameplay art completion, audio completion, inspection, and APK export without requiring another menu selection
- Menu item 10 is the complete flow from requirements confirmation to APK output, and must end with the packaging workflow exporting an APK when `CAN_ENTER_PACK=true`
- If inspection or packaging prerequisites fail during menu item 10, stop before APK export, report the blocker, and do not claim the full flow is complete
- If the user requests revisions instead of confirming, revise the requirements draft and keep the trace status at `draft`
- In menu item 10, ask only for missing game direction input when the user has not provided enough theme or subject information to generate candidates
- Do not ask meta permission questions for the standard menu item 10 flow
- Use optional auto-decision behavior in menu item 10 only when the user explicitly states that Codex may decide on concept selection or requirements confirmation
- If the user selects 11, discuss only and do not modify files

Requirements planning policy:
- Requirements planning must happen before first-time project initialization
- Menu items 1 and 2 must route through the game-requirements-planner skill when it is available
- The standard planning sequence is: direction input -> 10 candidate concepts -> user selects 1 concept -> full game requirements -> user confirmation -> initialization
- The full game requirements must include gameplay loop, controls, progression or level structure, UI structure, UI asset strategy, gameplay art asset strategy, gameplay art facing and animation expectations, icon direction, audio direction, and implementation notes
- The full game requirements must also include a gameplay diversity and content budget contract, persisted as artifacts/requirements/<game_id>/gameplay_diversity.json
- Once the selected concept has a target game id, store the requirements trace under artifacts/requirements/<game_id>/
- The authoritative requirements trace should include metadata.json and requirements.md
- The authoritative requirements trace should also include gameplay_diversity.json
- Requirements trace status should remain draft until the user confirms it, then move to confirmed
- Menu item 10 must always surface the full requirements draft for review and explicit confirmation before initialization
- A draft requirements trace must never trigger initialization until the user explicitly confirms it
- Do not initialize a new project unless the gameplay diversity contract exists and passes strict validation
- If requirements confirmation is missing, do not call the initialization workflow

Project structure and acceptance policy:
- Any mini-game type is allowed if it satisfies the authoritative repository rules and docs/PROJECT_ACCEPTANCE_BASELINE.md
- Do not encode one specific game genre as a repo-wide requirement
- A project should be runnable in Android Studio and packageable into an APK through the repository packaging workflow when packaging is explicitly requested
- Delivery-ready projects should not rely on bare placeholder UI, gameplay art, icon, or audio tracks

Date organization policy:
- Active projects must remain under games/<game_id>/
- Use registry/produced_games.json as the source of created_at truth
- Refresh registry/projects_by_date.json with tools/rebuild_projects_by_date.ps1 when new projects are added or created_at values change
- Do not move active projects into nested year or month directories unless the repository tools are refactored first

Resource workflow policy:
- Icon workflow is a formal track and should define concept direction before asset generation or replacement
- Icon workflow should prefer cartoon-styled icons with strong visual association to the selected game
- Icon workflow should update the project icon resources and export upload-ready icons to artifacts/icons/<game_id>/
- UI workflow is a formal track and should define screen structure and HUD first, then implementation and polish
- UI workflow may use binary UI assets, fonts, and licensed open-source UI resources when provenance is tracked
- UI workflow should keep reusable external UI resources under shared_assets/ui/ first when possible
- UI Kit is the required structural foundation, not sufficient final visual quality for menu item `10` or delivery-ready output
- Treat `project_local_xml_ui` as placeholder-only for production-grade, menu item `10`, or delivery-ready output
- UI workflow must produce a concrete UI brief with screen list, state map, HUD priorities, chosen ui_skin, style tags, and asset strategy before implementation
- UI workflow should resolve assets in this order by default: style-matched shared UI pack -> import licensed open-source UI pack into shared_assets/ui/ -> project-local custom refinement
- UI workflow must not silently fall back to generic shape-only placeholder controls for production-grade or delivery-ready requests
- Gameplay art workflow is a formal track and should define gameplay visual roles before asset selection or project wiring
- Gameplay art workflow covers characters, enemies, animals, maps, tilesets, props, items, projectiles, effects, and gameplay backgrounds
- Gameplay art workflow may use binary gameplay art assets only when the license and provenance are tracked
- Gameplay art workflow should keep reusable external gameplay art resources under shared_assets/game_art/ first when possible
- Gameplay art workflow should resolve assets in this order by default: style-matched shared game art pack -> import official free and license-clear pack into shared_assets/game_art/ -> project-local prototype drawing only when placeholders are explicitly acceptable
- Gameplay art workflow must not silently fall back to generic circle or rectangle gameplay placeholders for production-grade or delivery-ready requests
- Treat `project_local_canvas_art` as placeholder-only for production-grade, menu item `10`, or delivery-ready output
- Assigned gameplay art must be used by the running game, not only copied into project assets
- Delivery-ready gameplay art must define and implement `app/src/main/assets/game_art/runtime_art_map.json`
- `runtime_art_map.json` should include entity roles, asset keys, default facing, facing rules, anchors, hitboxes, z-order, states, animation rules, and movement rules
- A static bitmap that only moves by position interpolation is placeholder-quality when the entity should turn, attack, take damage, die, or otherwise animate
- Entity facing must match movement, target, or attack direction by selecting directional frames, flipping, or rotating assets
- Primary humanoid, animal, zombie, soldier, or creature entities should use alternating walk or run poses when moving
- Attack-capable primary entities should use windup, action, and recovery timing or equivalent pose changes
- If imported free assets lack required frames, use the sprite pipeline from an approved seed frame instead of inventing unrelated frames
- Requirements planning and initialization must not silently reuse the same tiny map, same roster, or same loop template across multiple projects; use the gameplay diversity workflow to enforce subtype, content scale, and forbidden template reuse
- Audio workflow is a formal track and should define the audio direction before generation, selection, or project wiring
- Audio workflow should cover both BGM and SFX and keep generated assets reusable through the shared audio library
- Audio workflow must produce a concrete audio brief with theme, mood, pacing, BGM roles, and SFX roles before assignment
- Audio workflow should resolve assets in this order by default: style-matched shared library -> licensed fetch -> repository synthesis fallback
- Audio workflow must not silently reuse a style-mismatched legacy track just because the role matches
- Before inspection or packaging, explicitly determine whether icon, UI, gameplay art, and audio are deferred, placeholder-only, or complete
- In menu item 10, APK export is mandatory after successful inspection because it is the final stage of that complete flow; do not skip Gradle build or APK export as "not explicitly requested"
- In menu item 10, do not export APK when the first implementation clearly drops required mechanics from the confirmed requirements; repair the implementation or report the blocker first

Workflow mapping:
- Candidate concept workflow maps to game-requirements-planner
- Full requirements workflow maps to game-requirements-planner
- Initialization workflow maps to mini_game_project_init
- Optimization workflow maps to mini_game_optimize
- Packaging workflow maps to mini_game_pack
- Icon workflow maps to mini_game_icon
- UI workflow maps to mini_game_ui
- Gameplay art workflow maps to mini_game_art
- Audio workflow maps to mini_game_audio
- Inspect workflow maps to mini_game_inspect

Responsibility model:
- The user provides direction, selects one concept, confirms the full requirements, and decides when to package or deliver
- Codex leads requirements planning, workflow routing, initialization, optimization, inspection, packaging, and the main icon, UI, gameplay art, and audio process
- Codex is expected to support the full repository workflow end to end
- Cursor is an auxiliary execution role for UI polish, local resource wiring, and focused engineering refinements when needed
- Cursor is optional, not the primary workflow controller

Repository safety expectations:
- Respect the existing monorepo structure
- Do not overwrite unrelated projects
- Do not change package identity unless explicitly requested and allowed by repository rules
- Do not change launcher identity unless explicitly requested and allowed by repository rules
- Do not update registry unless the active workflow requires it
- Do not commit or push unless explicitly requested
- Prefer minimal safe changes before broad refactors

Project preference:
- Prefer well-structured, maintainable Android Studio Java projects
- Keep outputs practical and repository-safe
- Prefer clear architecture and stable project organization
- Prefer skill-driven workflow execution when skills are available
