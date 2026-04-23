import argparse
import subprocess
import sys
from pathlib import Path

from game_art_utils import load_index, pack_tokens, tokenize_text


def key_lines(output: str):
    result = {}
    for line in output.splitlines():
        line = line.strip()
        if "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def run_tool(script_name: str, args):
    target = Path(__file__).with_name(script_name)
    cmd = [sys.executable, str(target)]
    cmd.extend(args)
    return subprocess.run(cmd, capture_output=True, text=True)


def choose_pack_bundle(index_data, style_tags, art_roles, game_type, max_packs):
    style_tokens = set(tokenize_text(style_tags))
    role_tokens = set(tokenize_text(art_roles))
    game_type_tokens = set(tokenize_text(game_type))
    packs = [pack for pack in index_data.get("packs", []) if isinstance(pack, dict)]
    selected = []
    selected_ids = set()
    remaining_roles = set(role_tokens)

    for _ in range(max_packs):
        best_pack = None
        best_score = 0
        for pack in packs:
            pack_id = str(pack.get("pack_id", "")).strip()
            if not pack_id or pack_id in selected_ids:
                continue
            tokens = pack_tokens(pack)
            pack_role_tokens = set(tokenize_text(pack.get("art_roles", [])))
            pack_style_tokens = set(tokenize_text(pack.get("style_tags", [])))
            pack_game_tokens = set(tokenize_text(pack.get("recommended_game_types", [])))
            role_matches = len((remaining_roles or role_tokens).intersection(pack_role_tokens))
            style_matches = len(style_tokens.intersection(pack_style_tokens))
            game_matches = len(game_type_tokens.intersection(pack_game_tokens))
            broad_matches = len((style_tokens.union(role_tokens).union(game_type_tokens)).intersection(tokens))
            if broad_matches <= 0:
                continue
            score = role_matches * 7 + style_matches * 5 + game_matches * 6 + broad_matches
            if selected and role_tokens and role_matches <= 0 and score < 8:
                continue
            if score > best_score:
                best_score = score
                best_pack = pack
        if not best_pack:
            break
        selected.append(best_pack)
        selected_ids.add(str(best_pack.get("pack_id", "")).strip())
        remaining_roles -= set(tokenize_text(best_pack.get("art_roles", [])))
        if role_tokens and not remaining_roles:
            break
    return selected


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--project", default="")
    ap.add_argument("--theme", required=True)
    ap.add_argument("--game-type", default="")
    ap.add_argument("--art-roles", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--quality-target", default="production")
    ap.add_argument("--pack-id", default="")
    ap.add_argument("--max-packs", type=int, default=3)
    ap.add_argument("--library-root", default="shared_assets/game_art")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    library_root = Path(args.library_root).resolve()
    index_data = load_index(library_root / "index.json")
    pack_ids = [item.strip() for item in args.pack_id.split(",") if item.strip()]
    role_tokens = tokenize_text(args.art_roles)
    style_tokens = tokenize_text(args.theme, args.style_tags)

    if not pack_ids:
        selected_packs = choose_pack_bundle(
            index_data,
            style_tags=style_tokens,
            art_roles=role_tokens,
            game_type=args.game_type,
            max_packs=max(1, args.max_packs),
        )
        pack_ids = [str(pack.get("pack_id", "")).strip() for pack in selected_packs if str(pack.get("pack_id", "")).strip()]

    if not pack_ids:
        if args.quality_target.strip().lower() in ("production", "complete", "delivery"):
            raise RuntimeError("No suitable shared game art pack found. Import a free, license-clear pack before production-grade art work continues.")
        print("GAME_ART_PLACEHOLDER_ONLY=true")
        return

    if args.project.strip():
        assign_args = [
            "--project", args.project.strip(),
            "--game-id", args.game_id,
            "--pack-id", ",".join(pack_ids),
            "--library-root", str(library_root),
        ]
        if args.theme.strip():
            assign_args.extend(["--theme", args.theme.strip()])
        if args.game_type.strip():
            assign_args.extend(["--game-type", args.game_type.strip()])
        if args.art_roles.strip():
            assign_args.extend(["--art-roles", args.art_roles.strip()])
        if args.style_tags.strip():
            assign_args.extend(["--style-tags", args.style_tags.strip()])
        if args.dry_run:
            assign_args.append("--dry-run")
        assign_result = run_tool("assign_game_art.py", assign_args)
        if assign_result.returncode != 0:
            raise RuntimeError(assign_result.stderr.strip() or "Game art assignment failed")
        lines = key_lines(assign_result.stdout)
        print(f"GAME_ART_SELECTED_PACK={','.join(pack_ids)}")
        print(f"GAME_ART_ASSIGNED_FILES={lines.get('GAME_ART_ASSIGNED_FILES', '')}")
        return

    print(f"GAME_ART_SELECTED_PACK={','.join(pack_ids)}")


if __name__ == "__main__":
    raise SystemExit(main())
