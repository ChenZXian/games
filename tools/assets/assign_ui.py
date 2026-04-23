import argparse
import sys
from pathlib import Path

from ui_utils import append_used_by, build_project_record, copy_assignment, find_pack, find_best_pack, load_assignment_preset, load_index, load_pack_manifest, resolve_pack_dir, resolve_preset_id, save_json, tokenize_style_text


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def print_packs(index_data) -> int:
    for item in index_data.get("packs", []):
        if not isinstance(item, dict):
            continue
        sys.stdout.write(
            "UI_PACK={}|{}|{}|{}|{}\n".format(
                item.get("pack_id", ""),
                item.get("title", ""),
                item.get("recommended_ui_skin", ""),
                item.get("default_assignment_preset", ""),
                item.get("license", "")
            )
        )
    return 0


def print_presets(manifest, pack_dir: Path) -> int:
    preset_ids = manifest.get("assignment_presets", [])
    if not isinstance(preset_ids, list) or not preset_ids:
        return fail("No assignment presets declared for the requested UI pack")
    for preset_id in preset_ids:
        preset = load_assignment_preset(pack_dir, str(preset_id))
        sys.stdout.write(
            "UI_PRESET={}|{}|{}|{}\n".format(
                preset.get("preset_id", str(preset_id)),
                preset.get("title", ""),
                preset.get("ui_skin", ""),
                len(preset.get("assignments", []))
            )
        )
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", default="")
    ap.add_argument("--game-id", default="")
    ap.add_argument("--pack-id", default="")
    ap.add_argument("--preset", default="")
    ap.add_argument("--ui-skin", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--min-score", type=int, default=0)
    ap.add_argument("--library-root", default="shared_assets/ui")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--list-packs", action="store_true")
    ap.add_argument("--list-presets", action="store_true")
    args = ap.parse_args()

    pack_id = args.pack_id.strip()
    style_tags = tokenize_style_text(args.style_tags)
    library_root = Path(args.library_root).resolve()
    index_path = library_root / "index.json"
    if not index_path.exists():
        return fail(f"Missing UI index: {index_path}")

    try:
        index_data = load_index(index_path)

        if args.list_packs:
            return print_packs(index_data)

        if args.list_presets:
            if not pack_id:
                return fail("--pack-id is required with --list-presets")
            pack_entry = find_pack(index_data, pack_id)
            if not pack_entry:
                return fail(f"UI pack not found in index: {pack_id}")
            pack_dir = resolve_pack_dir(library_root, pack_entry, pack_id)
            manifest = load_pack_manifest(pack_dir)
            return print_presets(manifest, pack_dir)

        if not args.project:
            return fail("--project is required when applying a UI pack")
        project = Path(args.project).resolve()
        if not project.exists():
            return fail(f"Project path not found: {project}")

        game_id = args.game_id.strip()
        if not game_id:
            return fail("--game-id is required when applying a UI pack")
        if not pack_id:
            pack_entry = find_best_pack(index_data, style_tags=style_tags, ui_skin=args.ui_skin.strip(), min_score=args.min_score)
            if not pack_entry:
                return fail("No matching UI pack found")
            pack_id = str(pack_entry.get("pack_id", "")).strip()
        else:
            pack_entry = find_pack(index_data, pack_id)
        if not pack_entry:
            return fail(f"UI pack not found in index: {pack_id}")
        pack_dir = resolve_pack_dir(library_root, pack_entry, pack_id)
        manifest = load_pack_manifest(pack_dir)
        preset_id = resolve_preset_id(manifest, args.preset.strip())
        preset = load_assignment_preset(pack_dir, preset_id)
        ui_skin = args.ui_skin.strip() or str(preset.get("ui_skin", "")).strip() or str(manifest.get("recommended_ui_skin", "")).strip()
        copied_items = []
        for assignment in preset.get("assignments", []):
            copied_items.append(copy_assignment(project, pack_dir, assignment, True, args.dry_run))
        record = build_project_record(pack_entry, manifest, preset, game_id, ui_skin, copied_items)
        record_path = project / "app" / "src" / "main" / "assets" / "ui" / "ui_pack_assignment.json"
        if not args.dry_run:
            append_used_by(pack_entry, game_id)
            save_json(index_path, index_data)
            save_json(record_path, record)
    except Exception as exc:
        return fail(str(exc))

    sys.stdout.write(f"UI_ASSIGNED_PACK={pack_id}\n")
    sys.stdout.write(f"UI_ASSIGNED_PRESET={preset.get('preset_id', preset_id)}\n")
    sys.stdout.write(f"UI_ASSIGNED_UI_SKIN={ui_skin}\n")
    sys.stdout.write(f"UI_ASSIGNMENT_RECORD={Path('app/src/main/assets/ui/ui_pack_assignment.json').as_posix()}\n")
    for item in copied_items:
        sys.stdout.write(f"UI_COPIED={item.get('target', '')}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
