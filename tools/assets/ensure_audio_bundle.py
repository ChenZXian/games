import argparse
import subprocess
import sys
from pathlib import Path

from audio_utils import load_index, pick_entry, tokenize_style_text, unique_tokens


DEFAULT_BGM_ROLES = ["menu", "play", "win", "fail"]
DEFAULT_SFX_ROLES = ["ui_click", "ui_confirm", "ui_back", "collect", "hit", "warning", "win", "fail"]
SHARED_MATCH_MIN_SCORE = 5
REQUIRED_BGM_ROLES = {"menu", "play"}


def parse_csv(text: str, fallback):
    if not text.strip():
        return list(fallback)
    parts = []
    for raw in text.split(","):
        value = raw.strip().lower()
        if value:
            parts.append(value)
    return parts


def key_lines(output: str):
    result = {}
    for line in output.splitlines():
        line = line.strip()
        if "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def primary_tag(theme: str, style_tokens, role: str):
    theme_tokens = tokenize_style_text(theme)
    if theme_tokens:
        return theme_tokens[0]
    if style_tokens:
        return style_tokens[0]
    return role or "default"


def build_query_text(audio_type: str, role: str, theme: str, mood: str, pacing: str, style_tokens):
    parts = [audio_type, role, theme, mood, pacing]
    if audio_type == "bgm":
        parts.extend(["music", "loop"])
    else:
        parts.extend(["sound", "effect"])
    parts.extend(style_tokens)
    return " ".join(unique_tokens(parts))


def run_tool(script_name: str, args):
    target = Path(__file__).with_name(script_name)
    cmd = [sys.executable, str(target)]
    cmd.extend(args)
    return subprocess.run(cmd, capture_output=True, text=True)


def choose_shared_entry(library_root: Path, audio_type: str, role: str, tag: str, style_tokens):
    data = load_index(library_root / "index.json")
    return pick_entry(
        data,
        audio_type,
        role,
        audio_id="",
        tag=tag,
        style_tokens=style_tokens,
        min_score=SHARED_MATCH_MIN_SCORE,
    )


def ensure_one(args, audio_type: str, role: str, style_tokens):
    tag = primary_tag(args.theme, style_tokens, role)
    query_text = build_query_text(audio_type, role, args.theme, args.mood, args.pacing, style_tokens)
    shared = choose_shared_entry(args.library_root, audio_type, role, tag, style_tokens)
    if shared:
        return {
            "method": "shared",
            "entry_id": shared.get("id", ""),
            "role": role,
            "type": audio_type,
            "tag": tag,
            "external_search": "not_needed",
        }

    if args.dry_run:
        planned = "fetch"
        if args.no_fetch and not args.no_synth:
            planned = "synth"
        elif args.no_fetch and args.no_synth:
            planned = "missing"
        return {
            "method": planned,
            "entry_id": "",
            "role": role,
            "type": audio_type,
            "tag": tag,
            "external_search": "required_pending",
        }

    fetch_attempted = False
    fetch_status = "not_attempted"
    if not args.no_fetch:
        fetch_attempted = True
        fetch_status = "required_failed"
        fetch_args = [
            "--game-id", args.game_id,
            "--type", audio_type,
            "--role", role,
            "--tag", tag,
            "--style-tags", args.style_tags,
            "--mood", args.mood,
            "--pacing", args.pacing,
            "--query-text", query_text,
            "--library-root", str(args.library_root),
        ]
        fetch_result = run_tool("fetch_audio.py", fetch_args)
        if fetch_result.returncode == 0:
            lines = key_lines(fetch_result.stdout)
            entry_id = lines.get("AUDIO_ADDED_ID", "")
            if entry_id:
                return {
                    "method": "fetch",
                    "entry_id": entry_id,
                    "role": role,
                    "type": audio_type,
                    "tag": tag,
                    "external_search": "required_done",
                }

    if args.no_synth:
        raise RuntimeError(f"No matching audio available for {audio_type}:{role} and synthesis is disabled")

    synth_args = [
        "--game-id", args.game_id,
        "--type", audio_type,
        "--role", role,
        "--tag", tag,
        "--style-tags", args.style_tags,
        "--mood", args.mood,
        "--pacing", args.pacing,
        "--library-root", str(args.library_root),
    ]
    if audio_type == "bgm":
        synth_args.extend(["--seconds", str(args.bgm_seconds)])
    synth_result = run_tool("synth_audio.py", synth_args)
    if synth_result.returncode != 0:
        raise RuntimeError(synth_result.stderr.strip() or f"Synthesis failed for {audio_type}:{role}")
    synth_lines = key_lines(synth_result.stdout)
    entry_id = synth_lines.get("AUDIO_SYNTH_ID", "")
    if not entry_id:
        raise RuntimeError(f"Missing synthesized entry id for {audio_type}:{role}")
    return {
        "method": "synth",
        "entry_id": entry_id,
        "role": role,
        "type": audio_type,
        "tag": tag,
        "external_search": "required_failed" if fetch_attempted else fetch_status,
    }


def assign_one(args, result, style_tokens):
    if not args.project:
        return None
    if args.dry_run:
        return "DRY_RUN"
    assign_args = [
        "--project", args.project,
        "--game-id", args.game_id,
        "--type", result["type"],
        "--role", result["role"],
        "--audio-id", result["entry_id"],
        "--tag", result["tag"],
        "--style-tags", " ".join(style_tokens),
        "--library-root", str(args.library_root),
    ]
    assign_result = run_tool("assign_audio.py", assign_args)
    if assign_result.returncode != 0:
        raise RuntimeError(assign_result.stderr.strip() or f"Assignment failed for {result['type']}:{result['role']}")
    assign_lines = key_lines(assign_result.stdout)
    return assign_lines.get("AUDIO_TARGET", "")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--project", default="")
    ap.add_argument("--theme", required=True)
    ap.add_argument("--mood", default="")
    ap.add_argument("--pacing", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--bgm-roles", default="")
    ap.add_argument("--sfx-roles", default="")
    ap.add_argument("--bgm-seconds", type=int, default=24)
    ap.add_argument("--library-root", default="shared_assets/audio")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--no-fetch", action="store_true")
    ap.add_argument("--no-synth", action="store_true")
    args = ap.parse_args()

    args.library_root = Path(args.library_root)
    style_tokens = unique_tokens([args.theme, args.mood, args.pacing, tokenize_style_text(args.style_tags)])
    bgm_roles = parse_csv(args.bgm_roles, DEFAULT_BGM_ROLES)
    sfx_roles = parse_csv(args.sfx_roles, DEFAULT_SFX_ROLES)

    resolved = []
    for role in bgm_roles:
        result = ensure_one(args, "bgm", role, style_tokens)
        target = assign_one(args, result, style_tokens)
        result["target"] = target or ""
        resolved.append(result)

    for role in sfx_roles:
        result = ensure_one(args, "sfx", role, style_tokens)
        target = assign_one(args, result, style_tokens)
        result["target"] = target or ""
        resolved.append(result)

    for item in resolved:
        print(f"AUDIO_RESOLVED_TYPE={item['type']}")
        print(f"AUDIO_RESOLVED_ROLE={item['role']}")
        print(f"AUDIO_RESOLVED_METHOD={item['method']}")
        print(f"EXTERNAL_ASSET_SEARCH={item['external_search']}")
        if item["entry_id"]:
            print(f"AUDIO_RESOLVED_ID={item['entry_id']}")
        if item["target"]:
            print(f"AUDIO_RESOLVED_TARGET={item['target']}")

    bgm_resolved_roles = {item["role"] for item in resolved if item["type"] == "bgm"}
    required_bgm_roles = {role for role in bgm_roles if role in REQUIRED_BGM_ROLES}
    if not required_bgm_roles:
        bgm_coverage = "complete" if bgm_resolved_roles else "missing"
    else:
        covered_required_roles = required_bgm_roles.intersection(bgm_resolved_roles)
        if covered_required_roles == required_bgm_roles:
            bgm_coverage = "complete"
        elif covered_required_roles:
            bgm_coverage = "partial"
        else:
            bgm_coverage = "missing"
    print(f"BGM_COVERAGE={bgm_coverage}")


if __name__ == "__main__":
    main()
