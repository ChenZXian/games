# BGM_LIBRARY v1.1 (Local First, Then Online CC0/PD)

This repository uses a shared background music (BGM) library.

Key behavior:
- Prefer local library.
- If local has no suitable match, fetch online.
- Online fetch is limited to CC0 or Public Domain only.
- Every downloaded track must store license metadata in index.json.

Date: 2026-01-19

---

## Library Layout

shared_assets/
  bgm/
    files/
      <bgm_id>.ogg
    index.json

---

## Assign to a Game

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/assign_bgm.ps1 -Project games/<game_id> -GameId <game_id> -Tag runner
```

---

## Fetch Online and Assign (CC0/PD Only)

```powershell
powershell -ExecutionPolicy Bypass -File tools/assets/fetch_bgm.ps1 -GameId <game_id> -Tag runner -AssignProject games/<game_id>
```
