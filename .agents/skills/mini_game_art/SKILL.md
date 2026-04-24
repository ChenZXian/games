---
name: 游戏美术
description: 为本仓库 Android Java 小游戏生成、导入、匹配或接入玩法美术素材。适用于角色、敌人、动物、地图、tileset、道具、特效、背景等非 UI、非 icon、非音频资源。只使用免费且授权清楚的素材，优先 CC0。
---

Use this skill only for the repository gameplay art workflow.

Read first:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/GAME_ART_WORKFLOW.md
- docs/GAMEPLAY_DIVERSITY_WORKFLOW.md
- registry/produced_games.json

Hard rules:
- Java only for project code
- No Chinese in generated project code or resources
- No comments in generated project code, XML, or Gradle
- Do not use unclear, personal-use-only, or mixed-license assets
- Prefer CC0 or public-domain equivalent sources
- Keep license and source provenance in `shared_assets/game_art/`
- Search both the shared library and `shared_assets/game_art/source_catalog.json` before deciding the library is too small
- Do not mix gameplay art assets into `shared_assets/ui/`
- Do not use UI packs as character, map, or prop packs
- Treat `project_local_canvas_art` as prototype-only unless placeholder quality is explicitly acceptable
- Production-grade gameplay art must be assigned from shared or imported license-clear packs and used by the running game
- Production-grade gameplay art must define `app/src/main/assets/game_art/runtime_art_map.json`
- Do not mark gameplay art complete if entity facing is mismatched with movement, target, or attack direction
- Do not mark gameplay art complete when primary moving or attacking entities only use one static bitmap with smooth position movement
- Primary humanoid, animal, zombie, soldier, or creature entities should use alternating walk or run frames when moving
- Attack-capable primary entities should show windup, action, and recovery or equivalent pose changes
- If a license-clear pack does not include required frames, use a whole-strip sprite workflow from an approved seed frame instead of independent drifting frames
- Do not package, update registry, or change git state unless explicitly requested

Workflow:
1. Identify the target game, theme, camera perspective, and game type.
2. Read the gameplay diversity contract when it exists and preserve its asset variety budget, entity budget, map budget, and animation tier.
3. Define required art roles:
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
4. Resolve assets in this order:
   - style-matched shared game art pack
   - source-catalog search across official free and license-clear packs
   - imported official free and license-clear game art pack
   - project-local prototype art only when placeholder quality is acceptable
5. Use `tools/assets/ensure_game_art_pack.ps1` for matching.
6. Use `tools/assets/import_game_art_pack.ps1` for importing free license-clear packs when source-catalog search finds a better match.
7. Use `tools/assets/assign_game_art.ps1` for project assignment.
8. Record assignment under `app/src/main/assets/game_art/game_art_assignment.json`.
9. Create or update `app/src/main/assets/game_art/runtime_art_map.json`:
   - entity roles
   - asset keys
   - default facing
   - facing rule
   - anchor
   - hitbox
   - z-order
   - states
   - animation rule
   - movement rule
   - animation quality tier
10. Update runtime code so the mapped assets face, animate, and layer correctly.
11. Set runtime map status to `integrated` only after runtime behavior matches the map.

Report:
- selected or imported pack
- license and source URL
- art roles covered
- project-local asset path if assigned
- runtime art map status
- any remaining missing art roles
