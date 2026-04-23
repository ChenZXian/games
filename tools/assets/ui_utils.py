import json
import shutil
import zipfile
from datetime import datetime, timezone
from pathlib import Path
import re


STYLE_MATCH_IGNORE = {
    "ui",
    "kit",
    "pack",
    "skin",
    "default",
    "generic",
    "game",
    "mobile",
    "clean",
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
        token = normalize_token(str(value)).replace("_", " ").strip()
        for part in token.split():
            if part and part not in seen:
                seen.add(part)
                result.append(part)
    return result


def pack_tokens(pack_entry):
    tokens = []
    if isinstance(pack_entry, dict):
        tokens.extend(tokenize_style_text(pack_entry.get("pack_id", "")))
        tokens.extend(tokenize_style_text(pack_entry.get("title", "")))
        tokens.extend(tokenize_style_text(pack_entry.get("recommended_ui_skin", "")))
        style_tags = pack_entry.get("style_tags", [])
        if isinstance(style_tags, list):
            tokens.extend(tokenize_style_text(style_tags))
        quality_tags = pack_entry.get("quality_tags", [])
        if isinstance(quality_tags, list):
            tokens.extend(tokenize_style_text(quality_tags))
    return {token for token in unique_tokens(tokens) if token not in STYLE_MATCH_IGNORE}


def find_pack(index_data, pack_id: str):
    for item in index_data.get("packs", []):
        if isinstance(item, dict) and item.get("pack_id", "") == pack_id:
            return item
    return None


def find_best_pack(index_data, style_tags=None, ui_skin: str = "", min_score: int = 0):
    desired_tokens = set(tokenize_style_text(style_tags, ui_skin))
    desired_tokens = {token for token in desired_tokens if token not in STYLE_MATCH_IGNORE}
    packs = [item for item in index_data.get("packs", []) if isinstance(item, dict)]
    if not packs:
        return None
    if not desired_tokens and not ui_skin:
        return packs[0]

    best_pack = None
    best_score = None
    for pack in packs:
        score = 0
        tokens = pack_tokens(pack)
        style_matches = len(desired_tokens.intersection(tokens))
        if desired_tokens and style_matches <= 0:
            continue
        score += style_matches * 3
        if ui_skin and str(pack.get("recommended_ui_skin", "")) == ui_skin:
            score += 5
        if "premium" in pack.get("quality_tags", []):
            score += 1
        if best_pack is None or score > best_score:
            best_pack = pack
            best_score = score
    if best_pack is None:
        return None
    if best_score is not None and best_score < min_score:
        return None
    return best_pack


def resolve_pack_dir(library_root: Path, pack_entry, pack_id: str):
    rel = pack_entry.get("path", "") if isinstance(pack_entry, dict) else ""
    if rel:
        return ensure_within(library_root, library_root / rel, "UI pack path")
    return ensure_within(library_root, library_root / "packs" / pack_id, "UI pack path")


def load_pack_manifest(pack_dir: Path):
    manifest_path = pack_dir / "manifest.json"
    if not manifest_path.exists():
        raise RuntimeError(f"Missing UI pack manifest: {manifest_path}")
    return load_json(manifest_path)


def resolve_preset_id(manifest, requested_preset: str):
    if requested_preset:
        return requested_preset
    default_preset = manifest.get("default_assignment_preset", "")
    if default_preset:
        return default_preset
    presets = manifest.get("assignment_presets", [])
    if isinstance(presets, list) and len(presets) == 1:
        return str(presets[0])
    raise RuntimeError("UI preset is required because the pack does not expose a single default")


def load_assignment_preset(pack_dir: Path, preset_id: str):
    preset_path = pack_dir / "assignments" / f"{preset_id}.json"
    if not preset_path.exists():
        raise RuntimeError(f"Missing UI assignment preset: {preset_path}")
    data = load_json(preset_path)
    assignments = data.get("assignments", [])
    if not isinstance(assignments, list) or not assignments:
        raise RuntimeError(f"UI preset has no assignments: {preset_path}")
    return data


def ensure_within(root: Path, target: Path, label: str):
    root_resolved = root.resolve()
    target_resolved = target.resolve()
    try:
        target_resolved.relative_to(root_resolved)
    except ValueError as exc:
        raise RuntimeError(f"{label} escapes allowed root: {target_resolved}") from exc
    return target_resolved


def remove_conflicting_logical_resources(target: Path, logical_name: str):
    parent = target.parent
    keep_name = target.name
    for candidate in parent.glob(f"{logical_name}.*"):
        if candidate.name != keep_name and candidate.is_file():
            candidate.unlink()


def copy_assignment(project_root: Path, pack_dir: Path, assignment: dict, replace_logical: bool, dry_run: bool):
    src_rel = str(assignment.get("source", "")).strip()
    target_rel = str(assignment.get("target", "")).strip()
    logical_name = str(assignment.get("logical_name", "")).strip()
    kind = str(assignment.get("type", "")).strip()
    if not src_rel or not target_rel:
        raise RuntimeError("Each UI assignment requires source and target")
    src = ensure_within(pack_dir, pack_dir / src_rel, "UI source path")
    if not src.exists():
        raise RuntimeError(f"UI source file not found: {src}")
    project_main_root = ensure_within(project_root, project_root / "app" / "src" / "main", "UI project main root")
    target = ensure_within(project_main_root, project_root / target_rel, "UI target path")
    if not dry_run:
        target.parent.mkdir(parents=True, exist_ok=True)
        if replace_logical and logical_name:
            remove_conflicting_logical_resources(target, logical_name)
        shutil.copyfile(src, target)
    return {
        "type": kind,
        "logical_name": logical_name,
        "source": src_rel.replace("\\", "/"),
        "target": target_rel.replace("\\", "/")
    }


def append_used_by(pack_entry, game_id: str):
    used_by = pack_entry.get("used_by")
    if not isinstance(used_by, list):
        used_by = []
        pack_entry["used_by"] = used_by
    if game_id not in used_by:
        used_by.append(game_id)


def build_project_record(pack_entry, manifest, preset, game_id: str, ui_skin: str, copied_items):
    return {
        "version": 1,
        "game_id": game_id,
        "pack_id": manifest.get("pack_id", ""),
        "pack_title": manifest.get("title", ""),
        "preset_id": preset.get("preset_id", ""),
        "preset_title": preset.get("title", ""),
        "ui_skin": ui_skin,
        "style_tags": manifest.get("style_tags", []),
        "source_url": manifest.get("source_url", ""),
        "license": manifest.get("license", pack_entry.get("license", "")),
        "license_url": manifest.get("license_url", pack_entry.get("license_url", "")),
        "assigned_at_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "copied_items": copied_items
    }


def extract_zip_to_dir(zip_path: Path, target_dir: Path):
    target_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(target_dir)


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
