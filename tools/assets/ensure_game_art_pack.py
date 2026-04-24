import argparse
import subprocess
import sys
from pathlib import Path

from game_art_utils import load_index, load_source_catalog, rank_pack_candidates, rank_source_candidates, tokenize_text, usage_count


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
    role_tokens = set(tokenize_text(art_roles))
    selected = []
    selected_ids = set()
    remaining_roles = set(role_tokens)

    for _ in range(max_packs):
        ranked = rank_pack_candidates(
            index_data,
            style_tags=style_tags,
            art_roles=art_roles,
            game_type=game_type,
            role_focus=list(remaining_roles or role_tokens),
            limit=12,
            exclude_pack_ids=selected_ids,
        )
        if not ranked:
            break
        best_pack = ranked[0]["pack"]
        selected.append(best_pack)
        selected_ids.add(str(best_pack.get("pack_id", "")).strip())
        remaining_roles -= set(tokenize_text(best_pack.get("art_roles", [])))
        if role_tokens and not remaining_roles:
            break
    return selected


def import_source_candidate(candidate, library_root):
    source = candidate["source"]
    args = [
        "--pack-id", str(source.get("pack_id", "")).strip(),
        "--title", str(source.get("title", "")).strip(),
        "--source-url", str(source.get("source_url", "")).strip(),
        "--library-root", str(library_root),
        "--art-roles", ",".join(source.get("art_roles", [])),
        "--game-types", ",".join(source.get("recommended_game_types", [])),
        "--style-tags", ",".join(source.get("style_tags", [])),
        "--quality-tags", ",".join(source.get("quality_tags", [])),
    ]
    page_url = str(source.get("download_page_url", "")).strip()
    direct_url = str(source.get("download_url", "")).strip()
    if page_url:
        args.extend(["--download-page-url", page_url])
    elif direct_url:
        args.extend(["--download-url", direct_url])
    else:
        return False
    result = run_tool("import_game_art_pack.py", args)
    if result.returncode != 0:
        return False
    return True


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
    ap.add_argument("--source-catalog", default="shared_assets/game_art/source_catalog.json")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    library_root = Path(args.library_root).resolve()
    index_data = load_index(library_root / "index.json")
    source_catalog = load_source_catalog(Path(args.source_catalog).resolve())
    local_index_pack_ids = [str(pack.get("pack_id", "")).strip() for pack in index_data.get("packs", []) if isinstance(pack, dict)]
    pack_ids = [item.strip() for item in args.pack_id.split(",") if item.strip()]
    role_tokens = tokenize_text(args.art_roles)
    style_tokens = tokenize_text(args.theme, args.style_tags)
    local_candidates = rank_pack_candidates(
        index_data,
        style_tags=style_tokens,
        art_roles=role_tokens,
        game_type=args.game_type,
        limit=6,
    )
    source_candidates = rank_source_candidates(
        source_catalog,
        style_tags=style_tokens,
        art_roles=role_tokens,
        game_type=args.game_type,
        limit=6,
        exclude_pack_ids=local_index_pack_ids,
    )

    if not pack_ids:
        selected_packs = choose_pack_bundle(
            index_data,
            style_tags=style_tokens,
            art_roles=role_tokens,
            game_type=args.game_type,
            max_packs=max(1, args.max_packs),
        )
        pack_ids = [str(pack.get("pack_id", "")).strip() for pack in selected_packs if str(pack.get("pack_id", "")).strip()]

    if (not pack_ids or all(usage_count(item["pack"]) >= 2 for item in local_candidates[:max(1, len(pack_ids))])) and source_candidates and not args.dry_run:
        imported = import_source_candidate(source_candidates[0], library_root)
        if imported:
            index_data = load_index(library_root / "index.json")
            local_candidates = rank_pack_candidates(
                index_data,
                style_tags=style_tokens,
                art_roles=role_tokens,
                game_type=args.game_type,
                limit=6,
            )
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
        if local_candidates:
            print("GAME_ART_LOCAL_CANDIDATES=" + ",".join(str(item["pack"].get("pack_id", "")).strip() for item in local_candidates if str(item["pack"].get("pack_id", "")).strip()))
        if source_candidates:
            print("GAME_ART_SOURCE_CANDIDATES=" + ",".join(str(item["source"].get("pack_id", "")).strip() for item in source_candidates if str(item["source"].get("pack_id", "")).strip()))
        print(f"GAME_ART_ASSIGNED_FILES={lines.get('GAME_ART_ASSIGNED_FILES', '')}")
        return

    print(f"GAME_ART_SELECTED_PACK={','.join(pack_ids)}")
    if local_candidates:
        print("GAME_ART_LOCAL_CANDIDATES=" + ",".join(str(item["pack"].get("pack_id", "")).strip() for item in local_candidates if str(item["pack"].get("pack_id", "")).strip()))
    if source_candidates:
        print("GAME_ART_SOURCE_CANDIDATES=" + ",".join(str(item["source"].get("pack_id", "")).strip() for item in source_candidates if str(item["source"].get("pack_id", "")).strip()))


if __name__ == "__main__":
    raise SystemExit(main())
