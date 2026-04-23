import argparse
import subprocess
import sys
from pathlib import Path

from ui_utils import find_best_pack, load_index, tokenize_style_text


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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--project", default="")
    ap.add_argument("--theme", required=True)
    ap.add_argument("--ui-skin", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--scope", default="")
    ap.add_argument("--quality-target", default="production")
    ap.add_argument("--pack-id", default="")
    ap.add_argument("--preset", default="")
    ap.add_argument("--import-source", default="")
    ap.add_argument("--download-url", default="")
    ap.add_argument("--library-root", default="shared_assets/ui")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    library_root = Path(args.library_root).resolve()
    index_data = load_index(library_root / "index.json")
    style_tags = tokenize_style_text([args.theme, args.style_tags, args.scope])
    pack_id = args.pack_id.strip()

    if not pack_id:
        pack = find_best_pack(index_data, style_tags=style_tags, ui_skin=args.ui_skin.strip(), min_score=1)
        if pack:
            pack_id = str(pack.get("pack_id", "")).strip()

    imported = False
    if not pack_id and (args.import_source.strip() or args.download_url.strip()):
        if not args.dry_run:
            import_args = []
            if args.pack_id.strip():
                import_args.extend(["--pack-id", args.pack_id.strip()])
            if args.import_source.strip():
                import_args.extend(["--source-path", args.import_source.strip()])
            else:
                import_args.extend(["--download-url", args.download_url.strip()])
            import_args.extend(["--library-root", str(library_root)])
            import_result = run_tool("import_ui_pack.py", import_args)
            if import_result.returncode != 0:
                raise RuntimeError(import_result.stderr.strip() or "UI pack import failed")
            lines = key_lines(import_result.stdout)
            pack_id = lines.get("UI_IMPORTED_PACK", "").strip()
            imported = True
        else:
            pack_id = args.pack_id.strip()

    if not pack_id:
        if args.quality_target.strip().lower() in ("production", "complete", "delivery"):
            raise RuntimeError("No suitable shared UI pack found. Import a licensed UI pack before production-grade UI work continues.")
        print("UI_PLACEHOLDER_ONLY=true")
        return

    if args.project.strip():
        assign_args = [
            "--project", args.project.strip(),
            "--game-id", args.game_id,
            "--pack-id", pack_id,
            "--library-root", str(library_root),
            "--style-tags", " ".join(style_tags),
        ]
        if args.preset.strip():
            assign_args.extend(["--preset", args.preset.strip()])
        if args.ui_skin.strip():
            assign_args.extend(["--ui-skin", args.ui_skin.strip()])
        if args.dry_run:
            assign_args.append("--dry-run")
        assign_result = run_tool("assign_ui.py", assign_args)
        if assign_result.returncode != 0:
            raise RuntimeError(assign_result.stderr.strip() or "UI pack assignment failed")
        lines = key_lines(assign_result.stdout)
        print(f"UI_SELECTED_PACK={pack_id}")
        print(f"UI_SELECTED_PRESET={lines.get('UI_ASSIGNED_PRESET', '')}")
        print(f"UI_SELECTED_UI_SKIN={lines.get('UI_ASSIGNED_UI_SKIN', '')}")
        if imported:
            print("UI_IMPORTED_DURING_RESOLUTION=true")
        return

    print(f"UI_SELECTED_PACK={pack_id}")
    if imported:
        print("UI_IMPORTED_DURING_RESOLUTION=true")


if __name__ == "__main__":
    raise SystemExit(main())
