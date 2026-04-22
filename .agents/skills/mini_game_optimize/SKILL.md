---
name: 优化项目
description: 优化本仓库现有 Android Java 小游戏项目。适用于 UI 打磨、玩法手感调整、性能和响应性优化、HUD 可读性提升、菜单或暂停或 Game Over 精修以及 bug 修复。不要用于新建项目或常规打包流程。
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
- No ad hoc binary assets outside the dedicated icon, UI, or audio workflows
- No external UI kits without tracked provenance
- Use the existing UI foundation and route larger UI asset work through the dedicated UI workflow
- Do not create APK, AAB, zip, or run packaging steps unless explicitly requested
- Do not update registry unless explicitly requested in a packaging workflow

Allowed focus:
- UI polish within the existing project scope
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
