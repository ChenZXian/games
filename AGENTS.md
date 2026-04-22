- # Mini Game Repository Rules

  This repository is for Android Java mini-games.

  Always read and obey these authoritative files before making changes:
  - docs/GAME_GENERATION_STANDARD.md
  - docs/ENVIRONMENT_BASELINE.md
  - docs/UI_KIT_FACTORY_SPEC_v1_0.md
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
  - Treat initialization, optimization, and packaging as separate workflows
  - Use repository-safe behavior by default
  - Do not modify files immediately at the start of a new conversation

  Conversation start policy:
  - At the beginning of each new conversation in this repository, the first response must be a menu only
  - The menu must be written in Chinese
  - Do not provide implementation details, file edits, action plans, or code before the user selects a menu item
  - Wait for the user's menu selection before taking action
  - After the user selects a menu item, follow the matching workflow skill if available
  - If the user request is already extremely explicit and clearly selects a workflow, you may honor it directly, but still prefer the repository menu format when possible

  Required start menu format:
  菜单
  1. 新建一个游戏项目
  2. 优化现有游戏项目
  3. 打包或发布现有游戏项目
  4. 只讨论规则、方案或架构
  5. 先检查项目当前状态再决定下一步

  请按以下格式回复：
  - 菜单编号
  - 目标游戏 id 或新游戏想法
  - 具体需求

  Behavior rules after menu selection:
  - If the user selects 1, use the initialization workflow
  - If the user selects 2, use the optimization workflow
  - If the user selects 3, use the packaging workflow
  - If the user selects 4, discuss only and do not modify files
  - If the user selects 5, inspect first and do not modify files until a follow-up instruction is given

  Workflow mapping:
  - Initialization workflow maps to mini_game_project_init
  - Optimization workflow maps to mini_game_optimize
  - Packaging workflow maps to mini_game_pack

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