import argparse
import datetime
import hashlib
import re
import sys
from pathlib import Path

from audio_utils import (
    append_used_by,
    build_entry_id,
    build_library_path,
    copy_entry_to_project,
    detect_format,
    ensure_audio_root,
    load_index,
    normalize_id,
    save_index,
)


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def http_get(url: str, timeout: int = 20):
    try:
        import requests
    except Exception:
        raise RuntimeError("requests not available. Install it in your environment: pip install requests")
    response = requests.get(url, timeout=timeout, headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()
    return response


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def ogg_link_candidates(html: str):
    return re.findall(r'https?://[^"\s]+\.ogg', html, flags=re.IGNORECASE)


def detect_license_text(html: str):
    text = html.lower()
    if "cc0" in text or "creative commons 0" in text:
        return ("CC0", "https://creativecommons.org/publicdomain/zero/1.0/")
    if "public domain" in text:
        return ("Public Domain", "")
    return ("", "")


def search_opengameart(query_text: str):
    query = re.sub(r"\s+", "+", query_text.strip())
    url = f"https://opengameart.org/art-search-advanced?keys={query}&sort_by=count&sort_order=DESC"
    response = http_get(url)
    match = re.search(r'href="(/content/[^"]+)"', response.text)
    if not match:
        return None
    return "https://opengameart.org" + match.group(1)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--type", required=True)
    ap.add_argument("--role", default="")
    ap.add_argument("--tag", required=True)
    ap.add_argument("--assign-project", default="")
    ap.add_argument("--library-root", default="shared_assets/audio")
    ap.add_argument("--max-kb", default="1000")
    args = ap.parse_args()

    game_id = args.game_id.strip()
    audio_type = args.type.strip().lower()
    role = (args.role.strip().lower() or ("play" if audio_type == "bgm" else "generic"))
    tag = args.tag.strip().lower()
    assign_project = args.assign_project.strip()
    library_root = Path(args.library_root)
    try:
        max_kb = int(args.max_kb)
    except Exception:
        max_kb = 1000

    if audio_type not in ("bgm", "sfx"):
        return fail(f"Unsupported audio type: {audio_type}")

    ensure_audio_root(library_root)
    index_path = library_root / "index.json"
    data = load_index(index_path)

    query_text = f"{audio_type} {role} {tag}"
    try:
      page_url = search_opengameart(query_text)
    except Exception as exc:
      return fail(f"SEARCH_FAILED: {exc}")
    if not page_url:
      return fail("SEARCH_FAILED: no result page found")

    try:
        page = http_get(page_url)
    except Exception as exc:
        return fail(f"PAGE_FETCH_FAILED: {exc}")

    license_name, license_url = detect_license_text(page.text)
    if license_name not in ("CC0", "Public Domain"):
        return fail("LICENSE_NOT_ALLOWED: only CC0 or Public Domain is allowed")

    links = ogg_link_candidates(page.text)
    if not links:
        return fail("NO_OGG_FOUND: no .ogg download link detected on page")

    ogg_url = links[0]
    try:
        download = http_get(ogg_url, timeout=60)
    except Exception as exc:
        return fail(f"DOWNLOAD_FAILED: {exc}")

    data_bytes = download.content
    size_kb = (len(data_bytes) + 1023) // 1024
    if size_kb > max_kb:
        return fail(f"FILE_TOO_LARGE_KB={size_kb} MAX_KB={max_kb}")

    unique_token = sha256_bytes(data_bytes)[:8]
    entry_id = build_entry_id(audio_type, role, tag, unique_token)
    file_name = f"{normalize_id(entry_id)}.ogg"
    relative_path = build_library_path(audio_type, file_name)
    output_path = library_root / relative_path
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(data_bytes)

    entry = {
        "id": entry_id,
        "file": relative_path.replace("\\", "/"),
        "type": audio_type,
        "role": role,
        "tags": [tag],
        "style": tag,
        "loop": audio_type == "bgm",
        "duration_sec": 0,
        "used_by": [],
        "source": page_url,
        "retrieved_at": datetime.date.today().isoformat(),
        "license": license_name,
        "license_url": license_url,
        "generated": False,
        "format": detect_format(file_name),
        "sha256": sha256_bytes(data_bytes),
    }

    audio_items = data.setdefault("audio", [])
    existing = next((item for item in audio_items if item.get("id", "") == entry_id), None)
    if existing is None:
        audio_items.append(entry)
    else:
        entry = existing

    if assign_project:
        try:
            dst = copy_entry_to_project(entry, library_root, Path(assign_project))
            append_used_by(entry, game_id)
            sys.stdout.write(f"AUDIO_ASSIGNED_PROJECT={assign_project}\n")
            sys.stdout.write(f"AUDIO_TARGET={dst.as_posix()}\n")
        except Exception as exc:
            return fail(f"ASSIGN_FAILED: {exc}")

    save_index(index_path, data)
    sys.stdout.write(f"AUDIO_ADDED_ID={entry_id}\n")
    sys.stdout.write(f"AUDIO_ADDED_FILE={relative_path}\n")
    sys.stdout.write(f"AUDIO_SOURCE_URL={page_url}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
