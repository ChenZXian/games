import argparse
import subprocess
import sys
from pathlib import Path


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", required=True)
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--bgm-id", default="")
    ap.add_argument("--tag", default="")
    ap.add_argument("--library-root", default="shared_assets/audio")
    args = ap.parse_args()

    target = Path(__file__).with_name("assign_audio.py")
    cmd = [
        sys.executable,
        str(target),
        "--project",
        args.project,
        "--game-id",
        args.game_id,
        "--type",
        "bgm",
        "--role",
        "play",
        "--library-root",
        args.library_root,
    ]
    if args.bgm_id:
        cmd.extend(["--audio-id", args.bgm_id])
    if args.tag:
        cmd.extend(["--tag", args.tag])
    return subprocess.run(cmd).returncode


if __name__ == "__main__":
    raise SystemExit(main())
