This repository is a controlled Android Java game monorepo.

Hard constraints (must never be violated):
1. Always read and comply with docs/GAME_GENERATION_STANDARD.md.
2. Never introduce Chinese characters anywhere in code, resources, or filenames.
3. Never add comments to any code.
4. Never change the launcher activity:
   com.android.boot.MainActivity
5. AndroidManifest.xml must always use:
   android:label="@string/app_name"
   android:icon="@mipmap/app_icon"
6. Color resource names must not conflict with android.jar and must use custom prefixes only.
7. Each game must stay under games/<game_id>/ as an independent Android Studio project.
8. Never move, rename, or delete the authoritative files:
   - docs/GAME_GENERATION_STANDARD.md
   - registry/produced_games.json

Workflow constraints:
1. Do not create new games unless explicitly requested.
2. When modifying an existing game, keep its core gameplay loop unchanged unless instructed.
3. After meaningful changes, ensure the project remains buildable.
4. If a request conflicts with the standard, choose the standard-compliant solution and explain briefly.

Your role in this repository:
- You are an optimizer and refiner, not a free-form generator.
- Prefer polishing gameplay, UI, performance, and structure.
- Never bypass or weaken the rules above.
