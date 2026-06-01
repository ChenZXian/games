import argparse
import json
import re
import sys
from pathlib import Path


STOP = {
    "a", "an", "the", "and", "or", "to", "of", "in", "on", "with", "by", "for", "from",
    "game", "player", "enemy", "unit", "units", "stage", "match", "use", "using", "into",
    "through", "before", "after", "while", "then", "short", "android", "java"
}


AXES = {
    "node_graph_capture": {"node", "nodes", "graph", "linked", "route", "routes", "connected", "capture", "conquest", "capital", "resource"},
    "drag_route_dispatch": {"drag", "route", "routes", "send", "dispatch", "stream", "streams", "troop", "troops"},
    "lane_push": {"lane", "lanes", "spawn", "summon", "push", "base", "fortress"},
    "tower_defense": {"tower", "build", "path", "waves", "defense", "turret"},
    "worker_economy": {"worker", "villager", "gather", "food", "wood", "stone", "building"},
    "timer_pressure": {"timer", "minute", "time", "countdown", "score"},
    "territory_map": {"territory", "prefecture", "region", "map", "capital", "conquer"},
    "survival_arena": {"survive", "arena", "horde", "zombie", "shooter"},
    "runner": {"runner", "auto", "jump", "slide", "dash", "lane"},
    "puzzle_grid": {"puzzle", "grid", "tile", "match", "line", "solve"},
}


def read_json(path):
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception:
        return None


def tokens(value):
    if value is None:
        return set()
    if isinstance(value, (list, tuple)):
        text = " ".join(str(x) for x in value)
    elif isinstance(value, dict):
        text = " ".join(str(x) for x in flatten(value))
    else:
        text = str(value)
    parts = re.findall(r"[a-z0-9]+", text.lower())
    return {p for p in parts if p not in STOP and len(p) > 2}


def flatten(value):
    if isinstance(value, dict):
        for v in value.values():
            yield from flatten(v)
    elif isinstance(value, list):
        for v in value:
            yield from flatten(v)
    else:
        yield value


def collect_contract(repo, game_id):
    path = repo / "artifacts" / "requirements" / game_id / "gameplay_diversity.json"
    contract = read_json(path) or {}
    reg = read_json(repo / "registry" / "produced_games.json") or {}
    entry = {}
    for item in reg.get("games", []):
        if item.get("id") == game_id:
            entry = item
            break
    return contract, entry


def make_fingerprint(repo, game_id):
    contract, entry = collect_contract(repo, game_id)
    all_tokens = set()
    fields = [
        contract.get("genre_family"),
        contract.get("genre_archetype"),
        contract.get("camera_perspective"),
        contract.get("control_model"),
        contract.get("core_loop_signature"),
        contract.get("map_content_budget"),
        contract.get("entity_content_budget"),
        contract.get("mechanic_content_budget"),
        entry.get("core_loop"),
        entry.get("tags", []),
    ]
    for field in fields:
        all_tokens |= tokens(field)
    axes = []
    for axis, axis_tokens in AXES.items():
        if len(all_tokens & axis_tokens) >= 2:
            axes.append(axis)
    return {
        "version": 1,
        "game_id": game_id,
        "tokens": sorted(all_tokens),
        "mechanic_axes": sorted(axes),
        "genre_family": contract.get("genre_family", ""),
        "genre_archetype": contract.get("genre_archetype", ""),
        "control_model": contract.get("control_model", ""),
        "core_loop_signature": contract.get("core_loop_signature", entry.get("core_loop", "")),
    }


def similarity(left, right):
    lt = set(left.get("tokens", []))
    rt = set(right.get("tokens", []))
    la = set(left.get("mechanic_axes", []))
    ra = set(right.get("mechanic_axes", []))
    token_score = 0.0
    if lt or rt:
        token_score = len(lt & rt) / max(1, len(lt | rt))
    axis_score = 0.0
    if la or ra:
        axis_score = len(la & ra) / max(1, len(la | ra))
    return round(token_score * 0.45 + axis_score * 0.55, 4)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=".")
    parser.add_argument("--game-id", required=True)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--warn-threshold", type=float, default=0.30)
    parser.add_argument("--fail-threshold", type=float, default=0.42)
    args = parser.parse_args()
    repo = Path(args.repo).resolve()
    current = make_fingerprint(repo, args.game_id)
    scored = []
    registry = read_json(repo / "registry" / "produced_games.json") or {}
    for item in registry.get("games", []):
        other_id = item.get("id")
        if not other_id or other_id == args.game_id:
            continue
        other = make_fingerprint(repo, other_id)
        score = similarity(current, other)
        scored.append({
            "game_id": other_id,
            "score": score,
            "shared_axes": sorted(set(current["mechanic_axes"]) & set(other["mechanic_axes"])),
        })
    scored.sort(key=lambda x: x["score"], reverse=True)
    comparisons = [item for item in scored if item["score"] >= args.warn_threshold]
    comparisons.sort(key=lambda x: x["score"], reverse=True)
    top_score = scored[0]["score"] if scored else 0.0
    if top_score >= args.fail_threshold:
        status = "failed"
        risk = "high"
    elif top_score >= args.warn_threshold:
        status = "warning"
        risk = "medium"
    else:
        status = "passed"
        risk = "low"
    report = {
        "version": 1,
        "game_id": args.game_id,
        "status": status,
        "duplicate_risk": risk,
        "top_similarity": top_score,
        "fingerprint": current,
        "similar_projects": comparisons[:8],
        "nearest_projects": scored[:8],
    }
    if args.write:
        out_dir = repo / "artifacts" / "requirements" / args.game_id
        out_dir.mkdir(parents=True, exist_ok=True)
        (out_dir / "mechanic_fingerprint.json").write_text(json.dumps(report, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")
    if args.json:
        print(json.dumps(report, ensure_ascii=True, indent=2))
    else:
        print(f"MECHANIC_FINGERPRINT_STATUS={status}")
        print(f"MECHANIC_DUPLICATE_RISK={risk}")
        print(f"MECHANIC_TOP_SIMILARITY={top_score}")
        if comparisons:
            print("MECHANIC_SIMILAR_PROJECTS=" + ",".join(f"{x['game_id']}:{x['score']}" for x in comparisons[:5]))
    return 0 if status == "passed" else 2


if __name__ == "__main__":
    sys.exit(main())
