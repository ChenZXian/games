---
name: 项目检查
description: 检查本仓库 Android Java 小游戏项目的当前状态，并报告是否可以进入打包。适用于 preflight、当前状态检查、pack readiness 判断和下一步决策，不在检查流程里修改项目文件。
---

Use this skill only for the repository inspection workflow.

Read first:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/ENVIRONMENT_BASELINE.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md
- docs/INSPECT_WORKFLOW.md
- registry/produced_games.json

Hard rules:
- Java only
- No Chinese
- No comments
- Do not modify project files during inspection
- Do not package or build artifacts during inspection
- Prefer the repository inspect entry point over ad hoc checks

Workflow:
1. Identify the target project under `games/<game_id>/`.
2. Run `tools/inspect.ps1`.
3. Report:
   - pass or warning or fail counts
   - `CAN_ENTER_PACK`
   - `DELIVERY_READY`
   - requirements, gameplay diversity, implementation fidelity, icon, UI, gameplay art, gameplay art runtime, and audio status
   - the next recommended action

Use `-SkipDoctor` or `-SkipValidate` only when the user wants a faster local-only status pass.
