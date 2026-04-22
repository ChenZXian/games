import json
import os
import re
import shutil
from pathlib import Path


def fail(msg: str) -> int:
    raise RuntimeError(msg)


def normalize_id(value: str) -> str:
    value = value.lower()
    value = re.sub(r"[^a-z0-9_]+", "_", value)
    value = re.sub(r"_+", "_", value).strip("_")
    return value or "audio"


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


def pick_entry(data, audio_type: str, role: str, audio_id: str, tag: str):
    items = data.get("audio", [])
    candidates = [it for it in items if isinstance(it, dict) and it.get("type", "") == audio_type]
    if role:
      candidates = [it for it in candidates if it.get("role", "") == role]
    if audio_id:
        for item in candidates:
            if item.get("id", "") == audio_id:
                return item
        return None
    if tag:
        tagged = []
        for item in candidates:
            tags = item.get("tags", [])
            if isinstance(tags, list) and tag in tags:
                tagged.append(item)
        if tagged:
            return tagged[0]
    return candidates[0] if candidates else None


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
