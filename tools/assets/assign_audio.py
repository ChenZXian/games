import argparse
import sys
from pathlib import Path

from audio_utils import append_used_by, copy_entry_to_project, load_index, pick_entry, save_index, tokenize_style_text


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", required=True)
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--type", required=True)
    ap.add_argument("--role", default="")
    ap.add_argument("--audio-id", default="")
    ap.add_argument("--tag", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--min-score", type=int, default=0)
    ap.add_argument("--library-root", default="shared_assets/audio")
    args = ap.parse_args()

    project = Path(args.project)
    game_id = args.game_id.strip()
    audio_type = args.type.strip().lower()
    role = args.role.strip().lower()
    audio_id = args.audio_id.strip()
    tag = args.tag.strip().lower()
    style_tokens = tokenize_style_text(args.style_tags)
    library_root = Path(args.library_root)

    if audio_type not in ("bgm", "sfx"):
        return fail(f"Unsupported audio type: {audio_type}")

    index_path = library_root / "index.json"
    if not index_path.exists():
        return fail(f"Missing audio index: {index_path}")

    data = load_index(index_path)
    entry = pick_entry(data, audio_type, role, audio_id, tag, style_tokens=style_tokens, min_score=args.min_score)
    if not entry:
        return fail("No matching audio entry found")

    try:
        dst = copy_entry_to_project(entry, library_root, project)
    except Exception as exc:
        return fail(str(exc))

    append_used_by(entry, game_id)
    save_index(index_path, data)

    sys.stdout.write(f"AUDIO_ASSIGNED_ID={entry.get('id','')}\n")
    sys.stdout.write(f"AUDIO_ASSIGNED_FILE={entry.get('file','')}\n")
    sys.stdout.write(f"AUDIO_TARGET={dst.as_posix()}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
