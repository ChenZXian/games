import argparse
import json
import shutil
import sys
from pathlib import Path

def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2

def load_json(p: Path):
    return json.loads(p.read_text(encoding="utf-8"))

def save_json(p: Path, obj):
    p.write_text(json.dumps(obj, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")

def pick_entry(data, bgm_id: str, tag: str):
    items = data.get("bgm", [])
    if bgm_id:
        for it in items:
            if it.get("id") == bgm_id:
                return it
        return None
    if tag:
        for it in items:
            tags = it.get("tags", [])
            if isinstance(tags, list) and tag in tags:
                return it
    return items[0] if items else None

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", required=True)
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--bgm-id", default="")
    ap.add_argument("--tag", default="")
    args = ap.parse_args()

    project = Path(args.project)
    game_id = args.game_id
    bgm_id = args.bgm_id.strip()
    tag = args.tag.strip()

    repo_root = Path.cwd()
    index_path = repo_root / "shared_assets" / "bgm" / "index.json"
    files_dir = repo_root / "shared_assets" / "bgm" / "files"

    if not index_path.exists():
        return fail(f"Missing library index: {index_path}")
    if not files_dir.exists():
        return fail(f"Missing library files dir: {files_dir}")

    data = load_json(index_path)
    entry = pick_entry(data, bgm_id, tag)
    if not entry:
        return fail("No BGM entries available in index.json")

    file_name = entry.get("file", "")
    if not file_name:
        return fail("Selected entry has no file field")

    src = files_dir / file_name
    if not src.exists():
        return fail(f"BGM file not found: {src}")

    dst_dir = project / "app" / "src" / "main" / "assets" / "audio"
    dst_dir.mkdir(parents=True, exist_ok=True)
    dst = dst_dir / "bgm.ogg"

    shutil.copyfile(src, dst)

    used_by = entry.get("used_by")
    if not isinstance(used_by, list):
        used_by = []
        entry["used_by"] = used_by
    if game_id not in used_by:
        used_by.append(game_id)

    save_json(index_path, data)
    sys.stdout.write(f"BGM_ASSIGNED_ID={entry.get('id','')}\n")
    sys.stdout.write(f"BGM_ASSIGNED_FILE={file_name}\n")
    sys.stdout.write(f"BGM_TARGET={dst.as_posix()}\n")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
