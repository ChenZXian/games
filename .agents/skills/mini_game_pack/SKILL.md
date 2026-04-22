---
name: 打包发布
description: 为本仓库 Android Java 小游戏进行打包或发布准备。仅在明确需要 APK、AAB、zip、导出、release preparation、validator、doctor 或 build workflow 时使用。不要用于初始项目生成或常规优化。
---

Use this skill only when packaging or release preparation is explicitly requested.

  Read first:
  - docs/GAME_GENERATION_STANDARD.md
  - docs/ENVIRONMENT_BASELINE.md
  - docs/UI_KIT_FACTORY_SPEC_v1_0.md
  - registry/produced_games.json

  Packaging workflow:
  1. Run environment verification first
  2. Run repository or project validation next
  3. Run the packaging or build script only after verification passes
  4. Export only the requested artifact type
  5. Update registry only if requested and allowed by repository policy
  6. Do not commit or push unless explicitly requested

  Hard rules:
  - No Chinese
  - No comments
  - Preserve package identity unless explicitly requested
  - Preserve launcher identity unless explicitly requested
  - Respect repository path safety and UI Kit rules
