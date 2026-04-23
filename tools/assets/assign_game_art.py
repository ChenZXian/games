import argparse
import shutil
import sys
from pathlib import Path

from game_art_utils import append_used_by, find_best_pack, find_pack, load_index, load_pack_manifest, resolve_pack_dir, save_json, utc_now


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def copy_tree(src: Path, dst: Path):
    count = 0
    for item in src.rglob("*"):
        if not item.is_file():
            continue
        rel = item.relative_to(src)
        target = dst / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(item, target)
        count += 1
    return count


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", default="")
    ap.add_argument("--game-id", default="")
    ap.add_argument("--pack-id", default="")
    ap.add_argument("--theme", default="")
    ap.add_argument("--game-type", default="")
    ap.add_argument("--art-roles", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--library-root", default="shared_assets/game_art")
    ap.add_argument("--list-packs", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    library_root = Path(args.library_root).resolve()
    index_path = library_root / "index.json"
    index_data = load_index(index_path)

    if args.list_packs:
        for pack in index_data.get("packs", []):
            if isinstance(pack, dict):
                print(f"{pack.get('pack_id','')}\t{pack.get('title','')}\t{','.join(pack.get('art_roles', []))}\t{','.join(pack.get('style_tags', []))}")
        return 0

    requested_pack_ids = [item.strip() for item in args.pack_id.split(",") if item.strip()]
    if not requested_pack_ids:
        pack = find_best_pack(
            index_data,
            style_tags=[args.theme, args.style_tags],
            art_roles=args.art_roles,
            game_type=args.game_type,
            min_score=1,
        )
        if pack:
            requested_pack_ids = [str(pack.get("pack_id", "")).strip()]
    if not requested_pack_ids:
        return fail("No matching game art pack found")

    resolved = []
    for pack_id in requested_pack_ids:
        pack_entry = find_pack(index_data, pack_id)
        if not pack_entry:
            return fail(f"Unknown game art pack: {pack_id}")
        pack_dir = resolve_pack_dir(library_root, pack_entry, pack_id)
        manifest = load_pack_manifest(pack_dir)
        resolved.append((pack_id, pack_entry, pack_dir, manifest))

    if not args.project:
        print(f"GAME_ART_SELECTED_PACK={','.join(requested_pack_ids)}")
        return 0

    if not args.game_id:
        return fail("--game-id is required when assigning to a project")

    project_root = Path(args.project).resolve()
    if not project_root.exists():
        return fail(f"Project path not found: {project_root}")

    target_root = project_root / "app" / "src" / "main" / "assets" / "game_art"
    copied_count = 0
    assigned_packs = []
    if not args.dry_run:
        for pack_id, pack_entry, pack_dir, manifest in resolved:
            source_assets = pack_dir / manifest.get("assets_path", "assets")
            if not source_assets.exists():
                return fail(f"Missing pack assets directory: {source_assets}")
            target_pack = target_root / pack_id
            if target_pack.exists():
                shutil.rmtree(target_pack)
            pack_count = copy_tree(source_assets, target_pack / "assets")
            copied_count += pack_count
            assigned_packs.append({
                "pack_id": pack_id,
                "pack_title": manifest.get("title", ""),
                "license": manifest.get("license", ""),
                "license_url": manifest.get("license_url", ""),
                "source_url": manifest.get("source_url", ""),
                "art_roles": manifest.get("art_roles", []),
                "recommended_game_types": manifest.get("recommended_game_types", []),
                "style_tags": manifest.get("style_tags", []),
                "project_pack_path": f"app/src/main/assets/game_art/{pack_id}",
                "copied_file_count": pack_count,
            })
            append_used_by(pack_entry, args.game_id)
        record = {
            "version": 1,
            "game_id": args.game_id,
            "assigned_at_utc": utc_now(),
            "packs": assigned_packs,
            "copied_file_count": copied_count,
        }
        save_json(target_root / "game_art_assignment.json", record)
        save_json(index_path, index_data)
    else:
        for pack_id, pack_entry, pack_dir, manifest in resolved:
            source_assets = pack_dir / manifest.get("assets_path", "assets")
            if not source_assets.exists():
                return fail(f"Missing pack assets directory: {source_assets}")
            copied_count += sum(1 for p in source_assets.rglob("*") if p.is_file())

    print(f"GAME_ART_SELECTED_PACK={','.join(requested_pack_ids)}")
    print(f"GAME_ART_ASSIGNED_FILES={copied_count}")
    print(f"GAME_ART_DRY_RUN={str(args.dry_run).lower()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
