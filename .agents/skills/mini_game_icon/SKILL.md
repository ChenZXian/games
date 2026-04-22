---
name: 游戏图标
description: 为本仓库的 Android Java 小游戏生成或更新图标。先定义图标方向，优先卡通风格且与游戏主题或核心循环强相关，更新项目内 app_icon 资源并导出上传用文件到 artifacts/icons/<game_id>/。不要用于整项目生成、玩法优化或仅打包请求。
---

Use this skill only for the repository icon workflow.

Read first:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/ICON_WORKFLOW.md

Hard rules:
- Keep the manifest icon target as `@mipmap/app_icon`
- Prefer a cartoon style with clear association to the specific game
- Update project icon resources only inside the requested game project
- Export upload-ready icon files to `artifacts/icons/<game_id>/`
- Do not change unrelated gameplay, UI flow, registry entries, packaging outputs, or git state unless explicitly requested

Workflow:
1. Identify the target game id and project path.
2. Determine the icon direction:
   - subject
   - silhouette
   - palette
   - tone
   - game-specific hook
3. Generate or update project icon assets.
4. Export upload-ready icon files.
5. Report the exported location for user pickup.

Default output expectation:
- In-project icon resources under `games/<game_id>/app/src/main/res/`
- Upload-ready exports under `artifacts/icons/<game_id>/`
- Clear note of which icon file is the primary upload target
