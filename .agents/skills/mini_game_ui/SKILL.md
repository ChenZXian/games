---
name: 游戏界面
description: 为本仓库 Android Java 小游戏定义、构建或打磨 UI。适用于 HUD、菜单、帮助、暂停、奖励、结算等界面，先确定 screen structure、ui_skin 和资源策略，再实施或精修。允许合规的二进制和开源 UI 资源。
---

Use this skill only for the repository UI workflow.

Read first:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md
- docs/UI_WORKFLOW.md

Hard rules:
- Java only
- No Chinese
- No comments
- Keep exactly one `ui_skin`
- Define screen structure and HUD first
- Prefer `GameView` or `SurfaceView` for gameplay rendering
- Prefer Android View or XML overlays for menu, help, pause, reward, and result screens
- Reusable external UI resources should be stored under `shared_assets/ui/` first when possible
- Imported open-source UI assets must keep license and provenance metadata
- Do not package, update registry, or change git state unless explicitly requested

Workflow:
1. Identify the target game and requested UI scope.
2. Define:
   - screen list
   - state map
   - HUD metrics
   - chosen `ui_skin`
   - asset strategy
3. Reuse or import UI assets from `shared_assets/ui/` when needed.
4. Use `tools/assets/assign_ui.ps1 -ListPacks` and `-ListPresets` to inspect reusable shared packs when needed.
5. When applying a shared UI pack to a project, prefer `tools/assets/assign_ui.ps1`.
6. Assign project-local resources.
7. Implement or refine layouts, overlays, and style wiring.
8. Verify the required UI foundation files and logical resource names still exist.

Report:
- chosen `ui_skin`
- screens covered
- shared UI assets used or imported
- project-local resource paths changed
