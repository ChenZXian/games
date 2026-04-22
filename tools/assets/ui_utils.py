import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def load_index(index_path: Path):
    if not index_path.exists():
        return {"version": 1, "packs": []}
    return load_json(index_path)


def save_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def find_pack(index_data, pack_id: str):
    for item in index_data.get("packs", []):
        if isinstance(item, dict) and item.get("pack_id", "") == pack_id:
            return item
    return None


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
        "source_url": manifest.get("source_url", ""),
        "license": manifest.get("license", pack_entry.get("license", "")),
        "license_url": manifest.get("license_url", pack_entry.get("license_url", "")),
        "assigned_at_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "copied_items": copied_items
    }
