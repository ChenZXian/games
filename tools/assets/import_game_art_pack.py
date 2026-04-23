import argparse
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path
from urllib.request import Request, urlopen

from game_art_utils import clear_dir, load_index, merge_pack_entry, save_json, tokenize_text


ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".svg", ".json", ".txt", ".xml", ".tsx", ".tmx"}


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def download_to_path(url: str, dst: Path):
    request = Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urlopen(request, timeout=180) as response:
        dst.write_bytes(response.read())


def extract_zip_to_dir(zip_path: Path, target_dir: Path):
    target_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(target_dir)


def split_csv(value: str):
    return tokenize_text(value)


def copy_allowed_tree(src: Path, dst: Path):
    count = 0
    for item in src.rglob("*"):
        if not item.is_file():
            continue
        if item.suffix.lower() not in ALLOWED_EXTENSIONS:
            continue
        rel = item.relative_to(src)
        target = dst / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(item, target)
        count += 1
    return count


def copy_named_files(src: Path, dst: Path, names):
    dst.mkdir(parents=True, exist_ok=True)
    copied = []
    lower_names = {name.lower() for name in names}
    for item in src.rglob("*"):
        if not item.is_file():
            continue
        if item.name.lower() not in lower_names:
            continue
        target = dst / item.name
        shutil.copy2(item, target)
        copied.append(target.name)
    return copied


def first_license_file(src: Path):
    for item in src.rglob("*"):
        if item.is_file() and item.name.lower() in ("license.txt", "licence.txt", "license.md", "copying.txt"):
            return item
    return None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--pack-id", required=True)
    ap.add_argument("--title", required=True)
    ap.add_argument("--source-path", default="")
    ap.add_argument("--download-url", default="")
    ap.add_argument("--source-url", required=True)
    ap.add_argument("--library-root", default="shared_assets/game_art")
    ap.add_argument("--license", default="CC0-1.0")
    ap.add_argument("--license-url", default="https://creativecommons.org/publicdomain/zero/1.0/")
    ap.add_argument("--art-roles", default="")
    ap.add_argument("--game-types", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--quality-tags", default="production,free,open,cc0")
    ap.add_argument("--replace", action="store_true")
    args = ap.parse_args()

    if not args.source_path and not args.download_url:
        return fail("One of --source-path or --download-url is required")
    if args.source_path and args.download_url:
        return fail("Choose either --source-path or --download-url, not both")

    library_root = Path(args.library_root).resolve()
    pack_id = args.pack_id.strip()
    pack_dir = library_root / "packs" / pack_id
    temp_dir = None

    try:
        if pack_dir.exists() and not args.replace:
            return fail(f"Game art pack already exists: {pack_id}")

        if args.source_path:
            source_path = Path(args.source_path).resolve()
            if not source_path.exists():
                return fail(f"Source path not found: {source_path}")
            if source_path.is_dir():
                source_root = source_path
            else:
                temp_dir = Path(tempfile.mkdtemp(prefix="game_art_import_"))
                extract_zip_to_dir(source_path, temp_dir / "unzipped")
                source_root = temp_dir / "unzipped"
        else:
            temp_dir = Path(tempfile.mkdtemp(prefix="game_art_import_"))
            archive_path = temp_dir / "downloaded_pack.zip"
            download_to_path(args.download_url, archive_path)
            extract_zip_to_dir(archive_path, temp_dir / "unzipped")
            source_root = temp_dir / "unzipped"

        clear_dir(pack_dir)
        assets_dir = pack_dir / "assets"
        preview_dir = pack_dir / "preview"
        assets_count = copy_allowed_tree(source_root, assets_dir)
        preview_files = copy_named_files(source_root, preview_dir, ["Preview.png", "preview.png", "Sample.png", "sample.png", "Sample1.png", "Sample2.png"])

        license_src = first_license_file(source_root)
        pack_dir.mkdir(parents=True, exist_ok=True)
        if license_src:
            shutil.copy2(license_src, pack_dir / "LICENSE")
        else:
            (pack_dir / "LICENSE").write_text(f"{args.license}\n{args.license_url}\n", encoding="utf-8")

        notice = [
            f"Title: {args.title}",
            f"Source: {args.source_url}",
            f"License: {args.license}",
            f"License URL: {args.license_url}",
            "Imported for shared gameplay art reuse.",
        ]
        (pack_dir / "NOTICE").write_text("\n".join(notice) + "\n", encoding="utf-8")

        manifest = {
            "version": 1,
            "pack_id": pack_id,
            "title": args.title,
            "license": args.license,
            "license_url": args.license_url,
            "source_url": args.source_url,
            "download_url": args.download_url,
            "asset_types": sorted({p.suffix.lower().lstrip(".") for p in assets_dir.rglob("*") if p.is_file()}),
            "art_roles": split_csv(args.art_roles),
            "recommended_game_types": split_csv(args.game_types),
            "style_tags": split_csv(args.style_tags),
            "quality_tags": split_csv(args.quality_tags),
            "assets_path": "assets",
            "preview_files": preview_files,
            "asset_file_count": assets_count,
        }
        save_json(pack_dir / "manifest.json", manifest)

        index_path = library_root / "index.json"
        index_data = load_index(index_path)
        entry = {
            "pack_id": pack_id,
            "title": args.title,
            "license": args.license,
            "license_url": args.license_url,
            "source_url": args.source_url,
            "asset_types": manifest["asset_types"],
            "art_roles": manifest["art_roles"],
            "recommended_game_types": manifest["recommended_game_types"],
            "style_tags": manifest["style_tags"],
            "quality_tags": manifest["quality_tags"],
            "path": f"packs/{pack_id}",
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

    sys.stdout.write(f"GAME_ART_IMPORTED_PACK={pack_id}\n")
    sys.stdout.write(f"GAME_ART_IMPORTED_PATH={(library_root / 'packs' / pack_id).as_posix()}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
