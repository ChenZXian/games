import argparse
import subprocess
import sys
from pathlib import Path


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("-GameId", "--game-id", dest="game_id", required=True)
    ap.add_argument("-Tag", "--tag", dest="tag", default="default")
    ap.add_argument("-LibraryRoot", "--library-root", dest="library_root", default="shared_assets/audio")
    ap.add_argument("-Seconds", "--seconds", dest="seconds", type=int, default=16)
    ap.add_argument("-SampleRate", "--sample-rate", dest="sample_rate", type=int, default=44100)
    ap.add_argument("-AssignProject", "--assign-project", dest="assign_project", default="")
    args = ap.parse_args()

    target = Path(__file__).with_name("synth_audio.py")
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
        "--seconds",
        str(args.seconds),
        "--sample-rate",
        str(args.sample_rate),
    ]
    if args.assign_project:
        cmd.extend(["--assign-project", args.assign_project])
    return subprocess.run(cmd).returncode


if __name__ == "__main__":
    raise SystemExit(main())
