import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2

def http_get(url: str, timeout: int = 20):
    try:
        import requests
    except Exception:
        raise RuntimeError("requests not available. Install it in your environment: pip install requests")
    r = requests.get(url, timeout=timeout, headers={"User-Agent":"Mozilla/5.0"})
    r.raise_for_status()
    return r

def load_json(p: Path):
    if not p.exists():
        return {"bgm": []}
    return json.loads(p.read_text(encoding="utf-8"))

def save_json(p: Path, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")

def sha256_bytes(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()

def normalize_id(s: str) -> str:
    s = s.lower()
    s = re.sub(r"[^a-z0-9_]+", "_", s)
    s = re.sub(r"_+", "_", s).strip("_")
    return s or "bgm"

def ogg_link_candidates(html: str):
    return re.findall(r'https?://[^"\s]+\.ogg', html, flags=re.IGNORECASE)

def detect_license_text(html: str):
    t = html.lower()
    if "cc0" in t or "creative commons 0" in t:
        return ("CC0", "https://creativecommons.org/publicdomain/zero/1.0/")
    if "public domain" in t:
        return ("Public Domain", "")
    return ("", "")

def search_opengameart(tag: str):
    q = re.sub(r"\s+", "+", tag.strip())
    url = f"https://opengameart.org/art-search-advanced?keys={q}&field_art_type_tid%5B%5D=12&sort_by=count&sort_order=DESC"
    r = http_get(url)
    m = re.search(r'href="(/content/[^"]+)"', r.text)
    if not m:
        return None
    return "https://opengameart.org" + m.group(1)

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--tag", required=True)
    ap.add_argument("--assign-project", default="")
    ap.add_argument("--max-kb", default="1000")
    args = ap.parse_args()

    game_id = args.game_id.strip()
    tag = args.tag.strip()
    assign_project = args.assign_project.strip()
    try:
        max_kb = int(args.max_kb)
    except Exception:
        max_kb = 1000

    repo_root = Path.cwd()
    index_path = repo_root / "shared_assets" / "bgm" / "index.json"
    files_dir = repo_root / "shared_assets" / "bgm" / "files"
    files_dir.mkdir(parents=True, exist_ok=True)

    try:
        page_url = search_opengameart(tag)
    except Exception as e:
        return fail(f"SEARCH_FAILED: {e}")
    if not page_url:
        return fail("SEARCH_FAILED: no result page found")

    try:
        page = http_get(page_url)
    except Exception as e:
        return fail(f"PAGE_FETCH_FAILED: {e}")

    license_name, license_url = detect_license_text(page.text)
    if license_name not in ("CC0", "Public Domain"):
        return fail("LICENSE_NOT_ALLOWED: only CC0 or Public Domain is allowed")

    links = ogg_link_candidates(page.text)
    if not links:
        return fail("NO_OGG_FOUND: no .ogg download link detected on page")

    ogg_url = links[0]
    try:
        dl = http_get(ogg_url, timeout=60)
    except Exception as e:
        return fail(f"DOWNLOAD_FAILED: {e}")

    data = dl.content
    size_kb = (len(data) + 1023) // 1024
    if size_kb > max_kb:
        return fail(f"FILE_TOO_LARGE_KB={size_kb} MAX_KB={max_kb}")

    lib = load_json(index_path)
    existing = set([it.get("id","") for it in lib.get("bgm", []) if isinstance(it, dict)])

    base_id = normalize_id(f"{tag}_bgm")
    final_id = base_id
    n = 1
    while final_id in existing:
        n += 1
        final_id = f"{base_id}_{n}"

    file_name = f"{final_id}.ogg"
    out_path = files_dir / file_name
    out_path.write_bytes(data)

    entry = {
        "id": final_id,
        "file": file_name,
        "tags": [tag],
        "loop": True,
        "duration_sec": 0,
        "used_by": [],
        "source_title": page_url.split("/")[-1],
        "source_url": page_url,
        "license": license_name,
        "license_url": license_url,
        "retrieved_at": "2026-01-19",
        "sha256": sha256_bytes(data)
    }

    lib.setdefault("bgm", []).append(entry)
    save_json(index_path, lib)

    sys.stdout.write(f"BGM_ADDED_ID={final_id}\n")
    sys.stdout.write(f"BGM_ADDED_FILE={file_name}\n")
    sys.stdout.write(f"BGM_SOURCE_URL={page_url}\n")

    if assign_project:
        try:
            import shutil
            project = Path(assign_project)
            dst_dir = project / "app" / "src" / "main" / "assets" / "audio"
            dst_dir.mkdir(parents=True, exist_ok=True)
            dst = dst_dir / "bgm.ogg"
            shutil.copyfile(out_path, dst)

            entry["used_by"].append(game_id)
            save_json(index_path, lib)
            sys.stdout.write(f"BGM_ASSIGNED_PROJECT={assign_project}\n")
        except Exception as e:
            return fail(f"ASSIGN_FAILED: {e}")

    return 0

if __name__ == "__main__":
    raise SystemExit(main())
