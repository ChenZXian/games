import argparse
import subprocess
import sys
from pathlib import Path


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--tag", required=True)
    ap.add_argument("--assign-project", default="")
    ap.add_argument("--max-kb", default="1000")
    ap.add_argument("--library-root", default="shared_assets/audio")
    args = ap.parse_args()

    target = Path(__file__).with_name("fetch_audio.py")
    cmd = [
        sys.executable,
        str(target),
        "--game-id",
        args.game_id,
        "--type",
        "bgm",
        "--role",
        "play",
        "--tag",
        args.tag,
        "--library-root",
        args.library_root,
        "--max-kb",
        args.max_kb,
    ]
    if args.assign_project:
        cmd.extend(["--assign-project", args.assign_project])
    return subprocess.run(cmd).returncode


if __name__ == "__main__":
    raise SystemExit(main())
