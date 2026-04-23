import json
import re
import shutil
from pathlib import Path

STYLE_MATCH_IGNORE = {
    "audio",
    "bgm",
    "sfx",
    "music",
    "sound",
    "effect",
    "game",
    "default",
    "generic",
    "medium",
    "slow",
    "fast",
    "neutral",
}


def fail(msg: str) -> int:
    raise RuntimeError(msg)


def normalize_id(value: str) -> str:
    value = value.lower()
    value = re.sub(r"[^a-z0-9_]+", "_", value)
    value = re.sub(r"_+", "_", value).strip("_")
    return value or "audio"


def tokenize_style_text(*values):
    tokens = []
    for value in values:
        if value is None:
            continue
        if isinstance(value, (list, tuple, set)):
            for item in value:
                tokens.extend(tokenize_style_text(item))
            continue
        text = str(value).strip().lower()
        if not text:
            continue
        text = text.replace(",", " ").replace("/", " ").replace("-", " ").replace("_", " ")
        for part in re.split(r"[^a-z0-9]+", text):
            part = part.strip()
            if part:
                tokens.append(part)
    return unique_tokens(tokens)


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
        token = normalize_id(str(value)).replace("_", " ").strip()
        for part in token.split():
            if part and part not in seen:
                seen.add(part)
                result.append(part)
    return result


def load_index(index_path: Path):
    if not index_path.exists():
        return {"version": 1, "audio": []}
    return json.loads(index_path.read_text(encoding="utf-8"))


def save_index(index_path: Path, obj):
    index_path.parent.mkdir(parents=True, exist_ok=True)
    index_path.write_text(json.dumps(obj, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def ensure_audio_root(library_root: Path):
    (library_root / "bgm").mkdir(parents=True, exist_ok=True)
    (library_root / "sfx").mkdir(parents=True, exist_ok=True)


def entry_tokens(entry):
    tokens = []
    tokens.extend(tokenize_style_text(entry.get("id", "")))
    tokens.extend(tokenize_style_text(entry.get("role", "")))
    tokens.extend(tokenize_style_text(entry.get("style", "")))
    tokens.extend(tokenize_style_text(entry.get("mood", "")))
    tokens.extend(tokenize_style_text(entry.get("pacing", "")))
    tokens.extend(tokenize_style_text(entry.get("query_text", "")))
    tags = entry.get("tags", [])
    if isinstance(tags, list):
        tokens.extend(tokenize_style_text(tags))
    style_tags = entry.get("style_tags", [])
    if isinstance(style_tags, list):
        tokens.extend(tokenize_style_text(style_tags))
    return set(unique_tokens(tokens))


def pick_entry(data, audio_type: str, role: str, audio_id: str, tag: str, style_tokens=None, min_score: int = 0):
    items = data.get("audio", [])
    candidates = [it for it in items if isinstance(it, dict) and it.get("type", "") == audio_type]

    if audio_id:
        for item in candidates:
            if item.get("id", "") == audio_id:
                return item
        return None

    if role:
        candidates = [it for it in candidates if it.get("role", "") == role]
        if not candidates:
            return None

    if not candidates:
        return None

    desired_tag = normalize_id(tag) if tag else ""
    desired_tokens = {token for token in tokenize_style_text(style_tokens, desired_tag) if token not in STYLE_MATCH_IGNORE}

    if not desired_tag and not desired_tokens:
        return candidates[0]

    best_item = None
    best_score = None
    for item in candidates:
        tokens = entry_tokens(item)
        item_tags = item.get("tags", [])
        if not isinstance(item_tags, list):
            item_tags = []
        item_style_tags = item.get("style_tags", [])
        if not isinstance(item_style_tags, list):
            item_style_tags = []
        normalized_item_tags = set(tokenize_style_text(item_tags, item_style_tags, item.get("style", "")))
        tag_match = desired_tag in normalized_item_tags if desired_tag else False
        style_matches = len(desired_tokens.intersection(tokens))
        if desired_tokens and style_matches <= 0 and not tag_match:
            continue

        score = 0
        if role and item.get("role", "") == role:
            score += 3
        if tag_match:
            score += 8
        score += style_matches * 3
        if item.get("style", "") == "default" and desired_tokens and not tag_match and style_matches <= 1:
            score -= 4

        if best_item is None or score > best_score:
            best_item = item
            best_score = score

    if best_item is None:
        return None
    if best_score is not None and best_score < min_score:
        return None
    return best_item


def project_target_name(entry):
    ext = Path(entry.get("file", "")).suffix or ".ogg"
    audio_type = entry.get("type", "bgm")
    role = normalize_id(entry.get("role", "") or "play")
    if audio_type == "bgm":
        if role in ("play", "loop", "main", "default"):
            return f"bgm{ext}"
        return f"bgm_{role}{ext}"
    return f"sfx_{role}{ext}"


def copy_entry_to_project(entry, library_root: Path, project: Path):
    src = library_root / entry.get("file", "")
    if not src.exists():
        raise RuntimeError(f"Audio file not found: {src}")
    dst_dir = project / "app" / "src" / "main" / "assets" / "audio"
    dst_dir.mkdir(parents=True, exist_ok=True)
    dst = dst_dir / project_target_name(entry)
    shutil.copyfile(src, dst)
    return dst


def append_used_by(entry, game_id: str):
    used_by = entry.get("used_by")
    if not isinstance(used_by, list):
        used_by = []
        entry["used_by"] = used_by
    if game_id not in used_by:
        used_by.append(game_id)


def build_entry_id(audio_type: str, role: str, tag: str, unique_token: str):
    parts = [audio_type, role or "play", tag or "default", unique_token]
    return normalize_id("_".join(parts))


def build_library_path(audio_type: str, file_name: str) -> str:
    folder = "bgm" if audio_type == "bgm" else "sfx"
    return f"{folder}/{file_name}"


def detect_format(file_name: str) -> str:
    suffix = Path(file_name).suffix.lower().lstrip(".")
    return suffix or "bin"


def duration_for_seconds(seconds: float) -> float:
    try:
        value = round(max(0.0, float(seconds)), 3)
        return value
    except Exception:
        return 0
