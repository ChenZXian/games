---
name: 游戏音频
description: 为本仓库的 Android Java 小游戏生成、获取、分配或整理音频资源。适用于 BGM 和 SFX 的方向定义、共享库沉淀与项目接入，不用于整项目生成、icon 工作或仅打包请求。
---

Use this skill only for the repository audio workflow.

Read first:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/AUDIO_WORKFLOW.md

Hard rules:
- Cover both BGM and SFX when the request calls for game audio
- Keep audio style aligned with the game's theme and pacing
- Store generated or fetched audio in `shared_assets/audio/` first when possible
- Assign project audio into `games/<game_id>/app/src/main/assets/audio/`
- Use consistent naming for project-local audio outputs
- Do not change unrelated gameplay, registry entries, packaging outputs, or git state unless explicitly requested

Workflow:
1. Identify the target game id and the requested audio scope.
2. Define the audio direction:
   - mood
   - pacing
   - bgm roles
   - sfx roles
3. Reuse existing shared library assets when possible.
4. Generate or fetch new audio only when reuse is not enough.
5. Store new assets in the shared library.
6. Assign the needed assets into the target project.
7. Report which shared assets and project-local files were produced.
