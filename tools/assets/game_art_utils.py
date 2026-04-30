import json
import re
import shutil
from datetime import datetime, timezone
from pathlib import Path


MATCH_IGNORE = {
    "art",
    "asset",
    "assets",
    "game",
    "pack",
    "default",
    "generic",
    "png",
    "svg",
    "2d",
}

MAP_MATCH_HINTS = {
    "map",
    "tileset",
    "tile",
    "terrain",
    "building",
    "prop",
    "background",
    "layout",
    "tmx",
    "tsx",
    "tiled",
    "top",
    "down",
    "side",
    "isometric",
    "dungeon",
    "town",
    "urban",
    "city",
    "forest",
    "cave",
    "castle",
    "snow",
    "road",
    "lane",
}


def usage_count(pack_entry):
    used_by = pack_entry.get("used_by", []) if isinstance(pack_entry, dict) else []
    if not isinstance(used_by, list):
        return 0
    return len([item for item in used_by if str(item).strip()])


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def save_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def load_index(index_path: Path):
    if not index_path.exists():
        return {"version": 1, "packs": []}
    return load_json(index_path)


def load_source_catalog(catalog_path: Path):
    if not catalog_path.exists():
        return {"version": 1, "sources": []}
    return load_json(catalog_path)


def normalize_token(value: str) -> str:
    value = value.lower()
    value = re.sub(r"[^a-z0-9_]+", "_", value)
    value = re.sub(r"_+", "_", value).strip("_")
    return value or "pack"


def unique_tokens(values):
    seen = set()
    result = []
    for value in values:
        if isinstance(value, (list, tuple, set)):
            for nested in unique_tokens(value):
                if nested not in seen:
                    seen.add(nested)
                    result.append(nested)
            continue
        token = normalize_token(str(value)).replace("_", " ").strip()
        for part in token.split():
            if part and part not in seen:
                seen.add(part)
                result.append(part)
    return result


def tokenize_text(*values):
    tokens = []
    for value in values:
        if value is None:
            continue
        if isinstance(value, (list, tuple, set)):
            for item in value:
                tokens.extend(tokenize_text(item))
            continue
        text = str(value).strip().lower()
        if not text:
            continue
        text = text.replace(",", " ").replace("/", " ").replace("-", " ").replace("_", " ")
        for part in re.split(r"[^a-z0-9]+", text):
            if part.strip():
                tokens.append(part.strip())
    return unique_tokens(tokens)


def pack_tokens(pack_entry):
    tokens = []
    if isinstance(pack_entry, dict):
        for key in ("pack_id", "title", "license", "source_url"):
            tokens.extend(tokenize_text(pack_entry.get(key, "")))
        for key in ("style_tags", "art_roles", "recommended_game_types", "quality_tags", "animation_capabilities", "animation_states", "map_roles", "map_capabilities", "camera_perspectives"):
            value = pack_entry.get(key, [])
            if isinstance(value, list):
                tokens.extend(tokenize_text(value))
        tile_metrics = pack_entry.get("tile_asset_metrics", {})
        if isinstance(tile_metrics, dict):
            for metric_key, metric_value in tile_metrics.items():
                if isinstance(metric_value, (int, float)) and metric_value > 0:
                    tokens.extend(tokenize_text(metric_key))
    return {token for token in unique_tokens(tokens) if token not in MATCH_IGNORE}


def build_match_context(style_tags=None, art_roles=None, game_type: str = "", role_focus=None):
    style_tokens = {token for token in tokenize_text(style_tags) if token not in MATCH_IGNORE}
    role_tokens = {token for token in tokenize_text(art_roles) if token not in MATCH_IGNORE}
    game_type_tokens = {token for token in tokenize_text(game_type) if token not in MATCH_IGNORE}
    focus_tokens = role_focus if role_focus is not None else role_tokens
    focus_tokens = {token for token in tokenize_text(focus_tokens) if token not in MATCH_IGNORE}
    desired_tokens = style_tokens.union(role_tokens).union(game_type_tokens)
    map_focus_tokens = desired_tokens.intersection(MAP_MATCH_HINTS)
    desired_cameras = set()
    if {"top", "down"}.intersection(game_type_tokens) or {"roguelike", "rpg", "dungeon", "strategy", "tower"}.intersection(game_type_tokens):
        desired_cameras.add("top_down")
    if {"side", "platformer", "runner", "scroller"}.intersection(game_type_tokens):
        desired_cameras.add("side_view")
    if "isometric" in game_type_tokens:
        desired_cameras.add("isometric")
    return {
        "style_tokens": style_tokens,
        "role_tokens": role_tokens,
        "game_type_tokens": game_type_tokens,
        "focus_tokens": focus_tokens,
        "desired_tokens": desired_tokens,
        "map_focus_tokens": map_focus_tokens,
        "desired_cameras": desired_cameras,
    }


def score_pack_entry(pack_entry, context):
    tokens = pack_tokens(pack_entry)
    desired_tokens = context["desired_tokens"]
    if desired_tokens and not desired_tokens.intersection(tokens):
        return None
    pack_role_tokens = set(tokenize_text(pack_entry.get("art_roles", [])))
    pack_style_tokens = set(tokenize_text(pack_entry.get("style_tags", [])))
    pack_game_tokens = set(tokenize_text(pack_entry.get("recommended_game_types", [])))
    pack_animation_tokens = set(tokenize_text(pack_entry.get("animation_capabilities", []), pack_entry.get("animation_states", [])))
    pack_map_tokens = set(
        tokenize_text(
            pack_entry.get("map_roles", []),
            pack_entry.get("map_capabilities", []),
            pack_entry.get("camera_perspectives", []),
            [key for key, value in (pack_entry.get("tile_asset_metrics", {}) or {}).items() if isinstance(value, (int, float)) and value > 0],
        )
    )
    pack_camera_tokens = set(tokenize_text(pack_entry.get("camera_perspectives", [])))
    focus_tokens = context["focus_tokens"] or context["role_tokens"]
    role_matches = len(focus_tokens.intersection(pack_role_tokens))
    style_matches = len(context["style_tokens"].intersection(pack_style_tokens))
    game_matches = len(context["game_type_tokens"].intersection(pack_game_tokens))
    animation_matches = len(desired_tokens.intersection(pack_animation_tokens))
    map_matches = len(context["map_focus_tokens"].intersection(pack_map_tokens))
    camera_matches = len(context["desired_cameras"].intersection(set(pack_entry.get("camera_perspectives", []))))
    broad_matches = len(desired_tokens.intersection(tokens))
    score = role_matches * 7 + style_matches * 5 + game_matches * 6 + animation_matches * 9 + map_matches * 8 + camera_matches * 10 + broad_matches
    quality_tags = pack_entry.get("quality_tags", [])
    if "production" in quality_tags:
        score += 2
    if "animated" in quality_tags:
        score += 5
    pack_usage = usage_count(pack_entry)
    score -= min(pack_usage, 4) * 4
    if pack_usage == 0:
        score += 2
    if focus_tokens and role_matches <= 0:
        score -= 3
    if context["map_focus_tokens"] and map_matches <= 0:
        score -= 4
    if context["desired_cameras"] and not camera_matches and pack_entry.get("camera_perspectives"):
        score -= 12
    return {
        "score": score,
        "usage_count": pack_usage,
        "role_matches": role_matches,
        "style_matches": style_matches,
        "game_matches": game_matches,
        "animation_matches": animation_matches,
        "map_matches": map_matches,
        "camera_matches": camera_matches,
        "broad_matches": broad_matches,
        "pack": pack_entry,
    }


def rank_pack_candidates(index_data, style_tags=None, art_roles=None, game_type: str = "", role_focus=None, min_score: int = 0, limit: int = 10, exclude_pack_ids=None):
    exclude_pack_ids = {str(item).strip() for item in (exclude_pack_ids or []) if str(item).strip()}
    context = build_match_context(style_tags=style_tags, art_roles=art_roles, game_type=game_type, role_focus=role_focus)
    candidates = []
    for pack in index_data.get("packs", []):
        if not isinstance(pack, dict):
            continue
        pack_id = str(pack.get("pack_id", "")).strip()
        if not pack_id or pack_id in exclude_pack_ids:
            continue
        scored = score_pack_entry(pack, context)
        if not scored:
            continue
        if scored["score"] < min_score:
            continue
        candidates.append(scored)
    candidates.sort(key=lambda item: (-item["score"], item["usage_count"], item["pack"].get("pack_id", "")))
    if limit and limit > 0:
        return candidates[:limit]
    return candidates


def source_entry_tokens(source_entry):
    tokens = []
    if isinstance(source_entry, dict):
        for key in ("pack_id", "title", "source_url", "download_page_url"):
            tokens.extend(tokenize_text(source_entry.get(key, "")))
        for key in ("art_roles", "recommended_game_types", "style_tags", "quality_tags", "map_roles", "map_capabilities", "camera_perspectives"):
            value = source_entry.get(key, [])
            if isinstance(value, list):
                tokens.extend(tokenize_text(value))
    return {token for token in unique_tokens(tokens) if token not in MATCH_IGNORE}


def rank_source_candidates(catalog_data, style_tags=None, art_roles=None, game_type: str = "", role_focus=None, limit: int = 10, exclude_pack_ids=None):
    exclude_pack_ids = {str(item).strip() for item in (exclude_pack_ids or []) if str(item).strip()}
    context = build_match_context(style_tags=style_tags, art_roles=art_roles, game_type=game_type, role_focus=role_focus)
    candidates = []
    for source in catalog_data.get("sources", []):
        if not isinstance(source, dict):
            continue
        pack_id = str(source.get("pack_id", "")).strip()
        if not pack_id or pack_id in exclude_pack_ids:
            continue
        tokens = source_entry_tokens(source)
        desired_tokens = context["desired_tokens"]
        if desired_tokens and not desired_tokens.intersection(tokens):
            continue
        role_matches = len((context["focus_tokens"] or context["role_tokens"]).intersection(set(tokenize_text(source.get("art_roles", [])))))
        style_matches = len(context["style_tokens"].intersection(set(tokenize_text(source.get("style_tags", [])))))
        game_matches = len(context["game_type_tokens"].intersection(set(tokenize_text(source.get("recommended_game_types", [])))))
        map_matches = len(context["map_focus_tokens"].intersection(set(tokenize_text(source.get("map_roles", []), source.get("map_capabilities", []), source.get("camera_perspectives", [])))))
        camera_matches = len(context["desired_cameras"].intersection(set(source.get("camera_perspectives", []))))
        broad_matches = len(desired_tokens.intersection(tokens))
        score = role_matches * 7 + style_matches * 5 + game_matches * 6 + map_matches * 8 + camera_matches * 10 + broad_matches + 2
        if context["desired_cameras"] and not camera_matches and source.get("camera_perspectives"):
            score -= 12
        candidates.append({
            "score": score,
            "source": source,
            "role_matches": role_matches,
            "style_matches": style_matches,
            "game_matches": game_matches,
            "map_matches": map_matches,
            "camera_matches": camera_matches,
            "broad_matches": broad_matches,
        })
    candidates.sort(key=lambda item: (-item["score"], item["source"].get("pack_id", "")))
    if limit and limit > 0:
        return candidates[:limit]
    return candidates


def find_pack(index_data, pack_id: str):
    for item in index_data.get("packs", []):
        if isinstance(item, dict) and item.get("pack_id", "") == pack_id:
            return item
    return None


def find_best_pack(index_data, style_tags=None, art_roles=None, game_type: str = "", min_score: int = 0):
    ranked = rank_pack_candidates(index_data, style_tags=style_tags, art_roles=art_roles, game_type=game_type, min_score=min_score, limit=1)
    if not ranked:
        return None
    return ranked[0]["pack"]


def ensure_within(root: Path, target: Path, label: str):
    root_resolved = root.resolve()
    target_resolved = target.resolve()
    try:
        target_resolved.relative_to(root_resolved)
    except ValueError as exc:
        raise RuntimeError(f"{label} escapes allowed root: {target_resolved}") from exc
    return target_resolved


def resolve_pack_dir(library_root: Path, pack_entry, pack_id: str):
    rel = pack_entry.get("path", "") if isinstance(pack_entry, dict) else ""
    if rel:
        return ensure_within(library_root, library_root / rel, "Game art pack path")
    return ensure_within(library_root, library_root / "packs" / pack_id, "Game art pack path")


def load_pack_manifest(pack_dir: Path):
    manifest_path = pack_dir / "manifest.json"
    if not manifest_path.exists():
        raise RuntimeError(f"Missing game art pack manifest: {manifest_path}")
    return load_json(manifest_path)


def clear_dir(path: Path):
    if path.exists():
        shutil.rmtree(path)


def merge_pack_entry(index_data, pack_entry):
    packs = index_data.setdefault("packs", [])
    existing = find_pack(index_data, pack_entry.get("pack_id", ""))
    if existing is None:
        packs.append(pack_entry)
        return pack_entry
    existing.clear()
    existing.update(pack_entry)
    return existing


def append_used_by(pack_entry, game_id: str):
    used_by = pack_entry.get("used_by")
    if not isinstance(used_by, list):
        used_by = []
        pack_entry["used_by"] = used_by
    if game_id not in used_by:
        used_by.append(game_id)


def utc_now():
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
