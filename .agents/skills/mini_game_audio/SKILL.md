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
- Define a concrete audio brief before assignment:
  - theme
  - mood
  - pacing
  - bgm roles
  - sfx roles
- Resolve audio in this order by default:
  - style-matched shared library
  - licensed fetch
  - repository synthesis fallback
- Do not silently reuse a style-mismatched legacy track just because the role matches
- Do not change unrelated gameplay, registry entries, packaging outputs, or git state unless explicitly requested

Workflow:
1. Identify the target game id and the requested audio scope.
2. Define the audio brief:
   - theme
   - mood
   - pacing
   - bgm roles
   - sfx roles
3. Use `tools/assets/ensure_audio_bundle.ps1` as the default orchestration entry point when the task needs actual asset resolution.
4. Reuse style-matched shared library assets when possible.
5. Fetch licensed audio when shared assets are not enough.
6. Synthesize fallback audio when fetch is unavailable or insufficient.
7. Store every new asset in the shared library first.
8. Assign the needed assets into the target project.
9. Report the audio brief, which roles were resolved, and which shared assets and project-local files were produced.
