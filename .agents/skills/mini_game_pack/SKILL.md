---
name: 打包
description: Package or prepare release output for an Android Java mini-game in this repository. Use this skill only when packaging, build output, APK, AAB, zip, export, release preparation, validator, doctor, or build workflow is explicitly requested. Do not use this skill for initial project generation or routine optimization work.
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