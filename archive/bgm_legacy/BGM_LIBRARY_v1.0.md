# BGM_LIBRARY v1.0

This repository uses a shared background music (BGM) library to avoid per-game ad-hoc assets.

Goals:
- Codex never adds audio binaries.
- Cursor selects and copies BGM from a shared library during OPTIMIZE (only when requested) or during PACK (required).
- BGM usage is tracked in a single index file.

---

## Library Layout (Repo Root)

shared_assets/
  bgm/
    files/
      <bgm_file>.ogg
    index.json

Notes:
- Use .ogg for small size and fast decode.
- No LFS pointers are allowed in this repo.

---

## index.json Schema

Example:

```json
{
  "bgm": [
    {
      "id": "calm_loop_01",
      "file": "calm_loop_01.ogg",
      "tags": ["calm", "menu"],
      "loop": true,
      "duration_sec": 42,
      "used_by": []
    }
  ]
}
```

Fields:
- id: unique stable id
- file: filename under shared_assets/bgm/files/
- tags: free-form strings for selection
- loop: whether it should loop
- duration_sec: optional metadata
- used_by: array of game_id values that used this BGM

---

## Game Target Path

Each game should store its selected BGM at:

app/src/main/assets/audio/bgm.ogg

The game code should load:

assets/audio/bgm.ogg

---

## How to Assign BGM

From repo root:

Assign by id:

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/assign_bgm.ps1 -Project games/<game_id> -GameId <game_id> -BgmId calm_loop_01
```

Assign by tag (first match):

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/assign_bgm.ps1 -Project games/<game_id> -GameId <game_id> -Tag runner
```

---

## Cursor Phase Rules

- INIT:
  - Do not add BGM binaries.
  - Placeholder behavior is allowed (no BGM).

- OPTIMIZE:
  - Assign BGM only if the user asks for audio/BGM.

- PACK:
  - If shared_assets/bgm/index.json exists and has at least one entry, assign a BGM (by tag if possible, otherwise first entry).
  - Update used_by in shared_assets/bgm/index.json.

---

## Operational Notes

- If the library is empty, PACK may proceed without BGM only if the user explicitly approves.
- Keep BGM files small to avoid repository bloat.
