import argparse
import shutil
import sys
import tempfile
from pathlib import Path

from ui_utils import clear_dir, extract_zip_to_dir, load_index, merge_pack_entry, save_json


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def copy_tree_contents(src: Path, dst: Path):
    for item in src.iterdir():
        target = dst / item.name
        if item.is_dir():
            shutil.copytree(item, target, dirs_exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(item, target)


def download_to_path(url: str, dst: Path):
    import requests

    response = requests.get(url, timeout=120, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()
    dst.write_bytes(response.content)


def ensure_required_pack_files(pack_dir: Path):
    required = [
        pack_dir / "manifest.json",
        pack_dir / "assignments",
        pack_dir / "LICENSE",
    ]
    for path in required:
        if not path.exists():
            raise RuntimeError(f"Imported UI pack is missing required path: {path}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--pack-id", default="")
    ap.add_argument("--source-path", default="")
    ap.add_argument("--download-url", default="")
    ap.add_argument("--library-root", default="shared_assets/ui")
    args = ap.parse_args()

    if not args.source_path and not args.download_url:
        return fail("One of --source-path or --download-url is required")
    if args.source_path and args.download_url:
        return fail("Choose either --source-path or --download-url, not both")

    library_root = Path(args.library_root).resolve()
    requested_pack_id = args.pack_id.strip()
    pack_dir = None
    temp_dir = None

    try:
        source_root = None
        if args.source_path:
            source_path = Path(args.source_path).resolve()
            if not source_path.exists():
                return fail(f"Source path not found: {source_path}")
            if source_path.is_dir():
                source_root = source_path
            else:
                temp_dir = Path(tempfile.mkdtemp(prefix="ui_pack_import_"))
                extract_zip_to_dir(source_path, temp_dir)
                source_root = temp_dir
        else:
            temp_dir = Path(tempfile.mkdtemp(prefix="ui_pack_import_"))
            archive_path = temp_dir / "downloaded_pack.zip"
            download_to_path(args.download_url, archive_path)
            extract_zip_to_dir(archive_path, temp_dir / "unzipped")
            source_root = temp_dir / "unzipped"

        manifest_root = source_root
        manifest_path = manifest_root / "manifest.json"
        if not manifest_path.exists():
            nested = list(source_root.glob("*/manifest.json"))
            if len(nested) == 1:
                manifest_root = nested[0].parent
            else:
                return fail("Imported source must contain exactly one manifest.json at the root or one nested pack root")

        import json

        manifest = json.loads((manifest_root / "manifest.json").read_text(encoding="utf-8"))
        manifest_pack_id = str(manifest.get("pack_id", "")).strip()
        if not manifest_pack_id:
            return fail("Imported manifest is missing pack_id")
        final_pack_id = requested_pack_id or manifest_pack_id
        if requested_pack_id and manifest_pack_id != requested_pack_id:
            return fail("Imported manifest pack_id does not match the requested --pack-id")

        pack_dir = library_root / "packs" / final_pack_id
        clear_dir(pack_dir)
        pack_dir.mkdir(parents=True, exist_ok=True)
        copy_tree_contents(manifest_root, pack_dir)
        ensure_required_pack_files(pack_dir)

        index_path = library_root / "index.json"
        index_data = load_index(index_path)
        entry = {
            "pack_id": final_pack_id,
            "title": manifest.get("title", ""),
            "license": manifest.get("license", ""),
            "license_url": manifest.get("license_url", ""),
            "source_url": manifest.get("source_url", ""),
            "asset_types": manifest.get("asset_types", []),
            "recommended_ui_skin": manifest.get("recommended_ui_skin", ""),
            "default_assignment_preset": manifest.get("default_assignment_preset", ""),
            "assignment_presets": manifest.get("assignment_presets", []),
            "style_tags": manifest.get("style_tags", []),
            "quality_tags": manifest.get("quality_tags", []),
            "path": f"packs/{final_pack_id}",
            "used_by": [],
            "validated_with": [],
        }
        merge_pack_entry(index_data, entry)
        save_json(index_path, index_data)
    except Exception as exc:
        return fail(str(exc))
    finally:
        if temp_dir and temp_dir.exists():
            shutil.rmtree(temp_dir, ignore_errors=True)

    imported_pack_id = pack_dir.name if pack_dir else requested_pack_id
    sys.stdout.write(f"UI_IMPORTED_PACK={imported_pack_id}\n")
    sys.stdout.write(f"UI_IMPORTED_PATH={(library_root / 'packs' / imported_pack_id).as_posix()}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
