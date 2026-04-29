---
name: 需求策划
description: 在项目初始化前为本仓库的 Android Java 小游戏做需求策划。适用于根据大方向生成 10 个候选方案，或把已选方案扩展成完整需求文档，覆盖玩法、操作、成长、UI 结构、玩法美术、icon 方向、BGM 和 SFX 方向以及实现提示。不要用于生成代码、优化项目或打包。
---

Use this skill only for pre-initialization game planning inside this monorepo.

This skill is for requirements planning only.
It must not initialize projects, optimize projects, package builds, or modify existing game code.

Read these repository rules before producing planning output:
- AGENTS.md
- docs/GAME_GENERATION_STANDARD.md
- docs/UI_KIT_FACTORY_SPEC_v1_0.md
- docs/REQUIREMENTS_WORKFLOW.md
- docs/GAMEPLAY_DIVERSITY_WORKFLOW.md
- docs/VISUAL_IDENTITY_WORKFLOW.md
- registry/produced_games.json

Planning sequence:
1. Take a broad game direction from the user.
2. Generate 10 candidate mini-game concepts.
3. Let the user select exactly 1 concept.
4. Expand the selected concept into a complete game requirements document.
5. Wait for explicit requirements confirmation.
6. Only after confirmation may another workflow enter project initialization.

Menu item 10 orchestration rules:
- In menu item `10`, candidate generation and full requirements drafting must happen in separate replies.
- After candidate generation, stop and wait for exactly one concept selection.
- After full requirements drafting, show the full draft to the user and ask only for explicit confirmation or revisions.
- Do not initialize any project in the same reply as the full requirements draft.
- When a target `game_id` is known, persist the full requirements trace as status `draft` before asking for confirmation.
- If the user requests revisions, revise the requirements draft and keep status `draft`.
- Only after explicit confirmation may another workflow call `tools/requirements/confirm_trace.ps1`.
- In menu item `10`, confirmed requirements should unlock the downstream complete flow: initialization, optimization, icon, UI, gameplay art, audio, inspection, and final APK export.
- Menu item `10` must not skip candidate selection or requirements confirmation just because its final stage is APK export.

Requirements trace storage:
- Once the selected concept has a target `game_id`, store the planning trace under `artifacts/requirements/<game_id>/`
- The authoritative files are:
  - `metadata.json`
  - `requirements.md`
  - `gameplay_diversity.json`
  - `visual_identity.json`
- `candidates.md` is optional
- Use `tools/requirements/create_candidates_trace.ps1` for the candidate stage when a target `game_id` is already known
- Use `tools/requirements/create_requirements_trace.ps1` after one concept is selected
- When creating the full requirements trace, also provide or write a complete gameplay diversity contract with status `passed`
- When creating the full requirements trace, also provide or write a complete visual identity contract with status `passed`
- Use `tools/requirements/confirm_trace.ps1 -ExplicitUserConfirmation` only after the user explicitly confirms the requirements
- Use status `draft` before confirmation and `confirmed` after the user confirms the requirements
- The inspect workflow should be able to read this trace later

Hard planning rules:
- Do not generate project files.
- Do not update registry/produced_games.json during planning.
- Do not call initialization, optimization, inspection, or packaging workflows from this skill.
- Do not skip the 10-candidate step unless the user explicitly provides a locked concept and asks for full requirements only.
- Do not allow project initialization if full requirements confirmation is still missing.
- Do not allow project initialization if the gameplay diversity contract is missing, still draft, or too generic.
- Do not allow project initialization if the visual identity contract is missing, still draft, or too generic.
- Do not produce ten candidates that are minor variants of the same layout, roster, or loop.
- Keep all proposed games consistent with repository constraints:
  - Android Java mini-game
  - Root package policy remains com.android.boot at implementation time
  - Launcher policy remains com.android.boot.MainActivity at implementation time
  - Exactly one allowed ui_skin must be chosen during implementation planning
  - Manifest icon target must remain @mipmap/app_icon at implementation time
- Check registry/produced_games.json before proposing concepts and avoid obvious duplicate core loops.

Candidate concept output format:
- Produce exactly 10 concepts.
- Present them as a numbered list.
- Each concept must include:
  - Working title
  - One-sentence pitch
  - Core loop
  - Control scheme
  - Genre family and sub-archetype
  - Content-scale note
  - Distinctive hook
  - Monetization or retention potential
  - Duplicate-risk note against existing registry entries
- Keep each concept concise but specific enough to compare.
- Make the 10 concepts meaningfully different from one another.
- If the user asks for one genre such as tower defense, vary the sub-archetype, route model, player decision, map model, roster, and progression model across the 10 concepts.

Concept selection behavior:
- After presenting 10 concepts, stop and wait for the user's selection.
- If the user selects multiple concepts, ask them to narrow to one before writing full requirements.
- If the user asks for refinement, regenerate or revise the concept list before moving on.

Full requirements document format:
- Title and one-paragraph game positioning
- Target feel and player fantasy
- Core gameplay loop
- Controls and input model
- Failure conditions and win conditions
- Progression structure
- Level structure or run structure
- Economy, rewards, drops, or upgrade model
- Gameplay diversity and content budget:
  - genre family and concrete sub-archetype
  - why it is not a reskin of an existing game
  - map or playfield model
  - minimum map regions, routes, lanes, zones, rooms, or screens
  - terrain, obstacle, landmark, and functional map element variety
  - player, enemy, neutral, item, projectile, and effect roster targets
  - mechanic variety and progression targets
  - forbidden template reuse
- Visual identity contract:
  - UI layout archetype
  - HUD composition
  - navigation model
  - playfield safe area
  - frame overlay policy
  - palette signature
  - material language
  - typography style
  - UI pack strategy
  - icon subject
  - icon silhouette and composition
  - icon palette and background
  - forbidden UI and icon reuse
- Screen map:
  - menu
  - gameplay HUD
  - pause
  - game over
  - any extra required screens
- UI direction:
  - recommended ui_skin
  - layout tone
  - HUD priorities
  - playfield safe area
  - frame and border policy
  - key overlay panels
- Gameplay art direction:
  - required art roles
  - camera perspective
  - suggested shared pack family or source tier
  - default facing and facing behavior for moving or attacking entities
  - minimum visual states and animation expectations
  - animation quality tier for primary entities
  - anchor, hitbox, and z-order assumptions for primary entities
  - visual readability constraints
  - fallback rule if no suitable pack exists
- Icon direction:
  - subject
  - silhouette
  - color direction
  - tone
- BGM direction:
  - menu music mood
  - gameplay loop mood
  - boss or climax mood if applicable
- Technical implementation notes:
  - rendering style
  - state model
  - important entities
  - special systems to plan early
- Differentiation note against the current registry

Gameplay diversity contract requirements:
- Create or update `artifacts/requirements/<game_id>/gameplay_diversity.json` with status `passed` when the full requirements draft is complete enough for review.
- The JSON contract must include the fields defined by `docs/GAMEPLAY_DIVERSITY_WORKFLOW.md`.
- The contract must be specific enough for `tools/requirements/check_gameplay_diversity.ps1 -GameId <game_id> -Strict` to pass before confirmation.
- If it cannot pass, keep the requirements as draft and revise the design instead of initializing the project.

Visual identity contract requirements:
- Create or update `artifacts/requirements/<game_id>/visual_identity.json` with status `passed` when the full requirements draft is complete enough for review.
- The JSON contract must include the fields defined by `docs/VISUAL_IDENTITY_WORKFLOW.md`.
- The contract must be specific enough for `tools/requirements/check_visual_identity.ps1 -GameId <game_id> -Strict` to pass before confirmation.
- If it cannot pass, keep the requirements as draft and revise UI or icon direction instead of initializing the project.

Full requirements quality bar:
- Keep the design implementable as a single Android Studio Java mini-game project.
- Avoid mechanics that require network services, live ops backends, or large external content pipelines unless the user explicitly asks for them.
- Prefer gameplay that can fit the repository's existing engineering style and UI contract.
- Ensure UI, gameplay art, icon, and BGM directions are explicit enough to drive later dedicated workflows.
- Do not leave icon subject, gameplay art pack strategy, or `menu` and `play` BGM roles ambiguous enough that downstream low-intelligence or auto runs can skip them.

Completion rule:
- End by asking for confirmation of the requirements document.
- State clearly that project initialization should begin only after this confirmation.
- After confirmation, persist the trace as `confirmed` before any initialization workflow begins.
