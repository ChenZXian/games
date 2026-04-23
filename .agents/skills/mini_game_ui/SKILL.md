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
- Do not use UI packs as character, map, prop, item, projectile, effect, or background packs; route those through the gameplay art workflow
- Define a concrete UI brief before implementation:
  - screen list
  - state map
  - HUD priorities
  - chosen `ui_skin`
  - style tags
  - asset strategy
- Resolve UI assets in this order by default:
  - style-matched shared UI pack
  - imported licensed open-source UI pack
  - project-local custom refinement
- Do not silently fall back to generic shape-only placeholder UI for production-grade or delivery-ready requests
- Do not package, update registry, or change git state unless explicitly requested

Workflow:
1. Identify the target game and requested UI scope.
2. Define:
   - screen list
   - state map
   - HUD metrics
   - chosen `ui_skin`
   - style tags
   - asset strategy
3. Use `tools/assets/ensure_ui_pack.ps1` as the default orchestration entry point when the task needs actual pack selection or assignment.
4. Reuse or import UI assets from `shared_assets/ui/` when needed.
5. Use `tools/assets/assign_ui.ps1 -ListPacks` and `-ListPresets` to inspect reusable shared packs when needed.
6. Use `tools/assets/import_ui_pack.ps1` when a licensed structured UI pack should be added to the shared library.
7. Assign project-local resources.
8. Implement or refine layouts, overlays, and style wiring.
9. Verify the required UI foundation files and logical resource names still exist.

Report:
- chosen `ui_skin`
- screens covered
- style tags
- shared UI assets used or imported
- project-local resource paths changed
