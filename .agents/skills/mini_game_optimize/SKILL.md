---
name: 优化
description: Optimize an existing Android Java mini-game in this repository. Use this skill only for optimization work such as UI polish, gameplay feel tuning, performance tuning, responsiveness improvements, HUD readability, menu or pause or game over refinement, and bug fixing inside the current project. Do not use this skill for creating a brand new project or for packaging, APK, AAB, zip, registry update, or release workflows unless explicitly requested.
---

Use this skill when optimizing an existing Android Java mini-game in this repository.

Read first:
- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md

Hard rules:
- Do not change package identity unless explicitly requested
- Do not change launcher activity identity
- No Chinese
- No comments
- No binary assets
- No external UI kits
- Use only the existing UI Kit system
- Do not create APK, AAB, zip, or run packaging steps unless explicitly requested
- Do not update registry unless explicitly requested in a packaging workflow

Allowed focus:
- UI polish using token and style resources only
- Gameplay feel tuning
- Performance and allocation reduction
- Input responsiveness improvements
- HUD readability improvements
- Menu, pause, and game over refinements
- Bug fixing inside project scope

Completion target:
- The project remains spec-compliant
- The project remains repository-safe
- UI and gameplay quality improve without breaking baseline rules