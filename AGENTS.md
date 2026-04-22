# Mini Game Repository Rules

This repository is for Android Java mini-games.

Always read and obey these authoritative files before making changes:
- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md
- docs/PROJECT_ACCEPTANCE_BASELINE.md
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
- Do not package, build APK, create zip, or run packaging steps unless explicitly requested
- Treat requirements planning, initialization, optimization, resource completion, inspection, and packaging as separate workflows
- Use repository-safe behavior by default
- Do not modify files immediately at the start of a new conversation
- Do not enter the initialization workflow before requirements confirmation is completed
- Treat icon, UI, and audio as formal workflow tracks, not incidental polish items
- Allow the icon workflow to generate project icon assets and upload-ready exported icons under artifacts/icons/<game_id>/
- Allow the UI workflow to use licensed binary UI assets, fonts, and open-source UI resource packs through shared_assets/ui/ when requested
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

Required start menu format:
菜单

需求阶段
`1.` 先出 10 个候选方案
`2.` 为已选方案补完整需求

开发阶段
`3.` 新建游戏项目
`4.` 优化现有项目
`5.` 生成或更新游戏图标
`6.` 优化游戏界面
`7.` 生成或接入游戏音频

交付阶段
`8.` 先检查项目状态
`9.` 打包或发布项目

其他
`10.` 一键执行完整流程
`11.` 只讨论规则、方案或架构

请按以下格式回复：
- 菜单编号
- 目标游戏 id、方向或方案名
- 具体需求

Behavior rules after menu selection:
- If the user selects 1, use the game-requirements-planner skill to generate 10 candidate mini-game concepts first and do not modify project files
- If the user selects 2, use the game-requirements-planner skill to generate the full game requirements for the chosen concept and do not enter project initialization yet
- If the user selects 3, use the initialization workflow only after the game requirements are explicitly confirmed
- If the user selects 4, use the optimization workflow for an existing project
- If the user selects 5, use the mini_game_icon skill when available; define the icon direction first, prefer a cartoon style that clearly matches the game, then update project assets and export upload-ready icon files when requested
- If the user selects 6, use the mini_game_ui skill when available; define UI structure, ui_skin, and asset strategy first, then perform implementation or polish as requested
- If the user selects 7, use the mini_game_audio skill when available; define the audio direction first, cover both BGM and SFX, then generate, store, and assign resources by stage
- If the user selects 8, use the mini_game_inspect skill when available and do not modify project files during the inspection workflow
- If the user selects 9, use the packaging workflow
- If the user selects 10, orchestrate the full repository flow in order: requirements planning -> initialization -> optimization/resource completion -> inspection -> packaging
- In menu item 10, the default behavior is to pause at concept selection and requirements confirmation unless the user explicitly authorizes automatic decisions
- If the user selects 11, discuss only and do not modify files

Requirements planning policy:
- Requirements planning must happen before first-time project initialization
- Menu items 1 and 2 must route through the game-requirements-planner skill when it is available
- The standard planning sequence is: direction input -> 10 candidate concepts -> user selects 1 concept -> full game requirements -> user confirmation -> initialization
- The full game requirements must include gameplay loop, controls, progression or level structure, UI structure, UI asset strategy, icon direction, audio direction, and implementation notes
- Once the selected concept has a target game id, store the requirements trace under artifacts/requirements/<game_id>/
- The authoritative requirements trace should include metadata.json and requirements.md
- Requirements trace status should remain draft until the user confirms it, then move to confirmed
- If requirements confirmation is missing, do not call the initialization workflow

Project structure and acceptance policy:
- Any mini-game type is allowed if it satisfies the authoritative repository rules and docs/PROJECT_ACCEPTANCE_BASELINE.md
- Do not encode one specific game genre as a repo-wide requirement
- A project should be runnable in Android Studio and packageable into an APK through the repository packaging workflow when packaging is explicitly requested
- Delivery-ready projects should not rely on bare placeholder UI, icon, or audio tracks

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
- Audio workflow is a formal track and should define the audio direction before generation, selection, or project wiring
- Audio workflow should cover both BGM and SFX and keep generated assets reusable through the shared audio library
- Before inspection or packaging, explicitly determine whether icon, UI, and audio are deferred, placeholder-only, or complete

Workflow mapping:
- Candidate concept workflow maps to game-requirements-planner
- Full requirements workflow maps to game-requirements-planner
- Initialization workflow maps to mini_game_project_init
- Optimization workflow maps to mini_game_optimize
- Packaging workflow maps to mini_game_pack
- Icon workflow maps to mini_game_icon
- UI workflow maps to mini_game_ui
- Audio workflow maps to mini_game_audio
- Inspect workflow maps to mini_game_inspect

Responsibility model:
- The user provides direction, selects one concept, confirms the full requirements, and decides when to package or deliver
- Codex leads requirements planning, workflow routing, initialization, optimization, inspection, packaging, and the main icon, UI, and audio process
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
