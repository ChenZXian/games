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


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def save_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def load_index(index_path: Path):
    if not index_path.exists():
        return {"version": 1, "packs": []}
    return load_json(index_path)


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
        for key in ("style_tags", "art_roles", "recommended_game_types", "quality_tags", "animation_capabilities", "animation_states"):
            value = pack_entry.get(key, [])
            if isinstance(value, list):
                tokens.extend(tokenize_text(value))
    return {token for token in unique_tokens(tokens) if token not in MATCH_IGNORE}


def find_pack(index_data, pack_id: str):
    for item in index_data.get("packs", []):
        if isinstance(item, dict) and item.get("pack_id", "") == pack_id:
            return item
    return None


def find_best_pack(index_data, style_tags=None, art_roles=None, game_type: str = "", min_score: int = 0):
    style_tokens = {token for token in tokenize_text(style_tags) if token not in MATCH_IGNORE}
    role_tokens = {token for token in tokenize_text(art_roles) if token not in MATCH_IGNORE}
    game_type_tokens = {token for token in tokenize_text(game_type) if token not in MATCH_IGNORE}
    desired_tokens = style_tokens.union(role_tokens).union(game_type_tokens)
    packs = [item for item in index_data.get("packs", []) if isinstance(item, dict)]
    if not packs:
        return None
    if not desired_tokens:
        return packs[0]

    best_pack = None
    best_score = None
    for pack in packs:
        tokens = pack_tokens(pack)
        match_count = len(desired_tokens.intersection(tokens))
        if match_count <= 0:
            continue
        pack_role_tokens = set(tokenize_text(pack.get("art_roles", [])))
        pack_game_type_tokens = set(tokenize_text(pack.get("recommended_game_types", [])))
        pack_style_tokens = set(tokenize_text(pack.get("style_tags", [])))
        pack_animation_tokens = set(tokenize_text(pack.get("animation_capabilities", []), pack.get("animation_states", [])))
        role_match_count = len(role_tokens.intersection(pack_role_tokens))
        game_type_match_count = len(game_type_tokens.intersection(pack_game_type_tokens))
        style_match_count = len(style_tokens.intersection(pack_style_tokens))
        animation_match_count = len(desired_tokens.intersection(pack_animation_tokens))
        score = match_count + role_match_count * 4 + game_type_match_count * 6 + style_match_count * 5 + animation_match_count * 8
        if "production" in pack.get("quality_tags", []):
            score += 1
        if "animated" in pack.get("quality_tags", []):
            score += 4
        if best_pack is None or score > best_score:
            best_pack = pack
            best_score = score
    if best_pack is None:
        return None
    if best_score is not None and best_score < min_score:
        return None
    return best_pack


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
