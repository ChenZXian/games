---
name: 游戏美术
description: 为本仓库 Android Java 小游戏生成、导入、匹配或接入玩法美术素材。适用于角色、敌人、动物、地图、tileset、道具、特效、背景等非 UI、非 icon、非音频资源。只使用免费且授权清楚的素材，优先 CC0。
---

Use this skill only for the repository gameplay art workflow.

Read first:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/GAME_ART_WORKFLOW.md
- registry/produced_games.json

Hard rules:
- Java only for project code
- No Chinese in generated project code or resources
- No comments in generated project code, XML, or Gradle
- Do not use unclear, personal-use-only, or mixed-license assets
- Prefer CC0 or public-domain equivalent sources
- Keep license and source provenance in `shared_assets/game_art/`
- Do not mix gameplay art assets into `shared_assets/ui/`
- Do not use UI packs as character, map, or prop packs
- Do not package, update registry, or change git state unless explicitly requested

Workflow:
1. Identify the target game, theme, camera perspective, and game type.
2. Define required art roles:
   - character
   - enemy
   - npc
   - animal
   - tileset
   - terrain
   - building
   - prop
   - item
   - projectile
   - pickup
   - effect
   - background
3. Resolve assets in this order:
   - style-matched shared game art pack
   - imported official free and license-clear game art pack
   - project-local prototype art only when placeholder quality is acceptable
4. Use `tools/assets/ensure_game_art_pack.ps1` for matching.
5. Use `tools/assets/import_game_art_pack.ps1` for importing free license-clear packs.
6. Use `tools/assets/assign_game_art.ps1` for project assignment.
7. Record assignment under `app/src/main/assets/game_art/game_art_assignment.json`.

Report:
- selected or imported pack
- license and source URL
- art roles covered
- project-local asset path if assigned
- any remaining missing art roles
