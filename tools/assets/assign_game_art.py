import argparse
import shutil
import sys
from pathlib import Path

from game_art_utils import append_used_by, find_best_pack, find_pack, load_index, load_pack_manifest, resolve_pack_dir, save_json, utc_now


def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2


def copy_tree(src: Path, dst: Path):
    count = 0
    for item in src.rglob("*"):
        if not item.is_file():
            continue
        rel = item.relative_to(src)
        target = dst / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(item, target)
        count += 1
    return count


def create_entity_template(entity_key: str, role: str, camera_perspective: str):
    side_view = "side" in camera_perspective or "platformer" in camera_perspective or "runner" in camera_perspective
    if side_view:
        facing_rule = "flip_horizontal_by_velocity_x_or_target_x"
        default_facing = "right"
        movement_rule = "advance animation frame by distance traveled and switch to idle when speed is near zero"
    else:
        facing_rule = "rotate_or_select_direction_frame_by_velocity_or_attack_vector"
        default_facing = "right"
        movement_rule = "advance move animation by speed and align front side to velocity or target"
    states = ["idle", "move"]
    if role in ("character", "enemy", "tower"):
        states.extend(["attack", "hit", "death"])
    if role in ("projectile",):
        states = ["fly", "hit"]
    if role in ("pickup", "item"):
        states = ["idle", "collect"]
    return {
        "entity_key": entity_key,
        "role": role,
        "asset_keys": [],
        "default_facing": default_facing,
        "facing_rule": facing_rule,
        "anchor": {
            "x": 0.5,
            "y": 0.9
        },
        "hitbox": {
            "x": 0.25,
            "y": 0.2,
            "w": 0.5,
            "h": 0.7
        },
        "render_size": {
            "w": 1.0,
            "h": 1.0,
            "unit": "entity"
        },
        "z_order": "by_y_then_role",
        "states": states,
        "animation_rule": "use frame animation for locomotion and action states; never rely on one static bitmap for primary moving or attacking entities",
        "movement_rule": movement_rule
    }


def create_runtime_map(game_id: str, theme: str, game_type: str, art_roles: str):
    roles = [item.strip() for item in art_roles.replace(",", " ").split() if item.strip()]
    camera_perspective = "side_view" if any(item in game_type.lower() for item in ("platformer", "runner", "side")) else ""
    entities = []
    if "character" in roles or "player" in roles:
        entities.append(create_entity_template("player", "character", camera_perspective))
    if "enemy" in roles or "zombie" in roles:
        entities.append(create_entity_template("enemy", "enemy", camera_perspective))
    if "tower" in roles:
        entities.append(create_entity_template("tower", "tower", camera_perspective))
    if "projectile" in roles:
        entities.append(create_entity_template("projectile", "projectile", camera_perspective))
    if "pickup" in roles or "item" in roles:
        entities.append(create_entity_template("pickup", "pickup", camera_perspective))
    return {
        "version": 1,
        "game_id": game_id,
        "status": "draft",
        "camera_perspective": camera_perspective,
        "default_coordinate_space": "normalized_screen",
        "theme": theme,
        "game_type": game_type,
        "animation_quality_target": "advanced_sprite_animation",
        "minimum_primary_entity_states": [
            "idle",
            "move",
            "attack",
            "hit",
            "death"
        ],
        "entities": entities,
        "required_runtime_checks": [
            "primary moving entities have idle and move states",
            "attacking entities have attack or fire states",
            "damageable entities have hit or damage feedback states",
            "removed entities have death destroy or equivalent effects",
            "directional entities define default_facing and facing_rule",
            "movement direction matches face weapon or front side",
            "projectiles and weapons rotate or choose frames toward travel or target direction",
            "anchors and hitboxes match gameplay contact points",
            "humanoid run or walk uses alternating leg frames when source art provides them",
            "attack uses windup contact and recovery timing or equivalent pose changes",
        ],
        "suggested_roles": roles,
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", default="")
    ap.add_argument("--game-id", default="")
    ap.add_argument("--pack-id", default="")
    ap.add_argument("--theme", default="")
    ap.add_argument("--game-type", default="")
    ap.add_argument("--art-roles", default="")
    ap.add_argument("--style-tags", default="")
    ap.add_argument("--library-root", default="shared_assets/game_art")
    ap.add_argument("--list-packs", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    library_root = Path(args.library_root).resolve()
    index_path = library_root / "index.json"
    index_data = load_index(index_path)

    if args.list_packs:
        for pack in index_data.get("packs", []):
            if isinstance(pack, dict):
                print(f"{pack.get('pack_id','')}\t{pack.get('title','')}\t{','.join(pack.get('art_roles', []))}\t{','.join(pack.get('style_tags', []))}")
        return 0

    requested_pack_ids = [item.strip() for item in args.pack_id.split(",") if item.strip()]
    if not requested_pack_ids:
        pack = find_best_pack(
            index_data,
            style_tags=[args.theme, args.style_tags],
            art_roles=args.art_roles,
            game_type=args.game_type,
            min_score=1,
        )
        if pack:
            requested_pack_ids = [str(pack.get("pack_id", "")).strip()]
    if not requested_pack_ids:
        return fail("No matching game art pack found")

    resolved = []
    for pack_id in requested_pack_ids:
        pack_entry = find_pack(index_data, pack_id)
        if not pack_entry:
            return fail(f"Unknown game art pack: {pack_id}")
        pack_dir = resolve_pack_dir(library_root, pack_entry, pack_id)
        manifest = load_pack_manifest(pack_dir)
        resolved.append((pack_id, pack_entry, pack_dir, manifest))

    if not args.project:
        print(f"GAME_ART_SELECTED_PACK={','.join(requested_pack_ids)}")
        return 0

    if not args.game_id:
        return fail("--game-id is required when assigning to a project")

    project_root = Path(args.project).resolve()
    if not project_root.exists():
        return fail(f"Project path not found: {project_root}")

    target_root = project_root / "app" / "src" / "main" / "assets" / "game_art"
    copied_count = 0
    assigned_packs = []
    if not args.dry_run:
        for pack_id, pack_entry, pack_dir, manifest in resolved:
            source_assets = pack_dir / manifest.get("assets_path", "assets")
            if not source_assets.exists():
                return fail(f"Missing pack assets directory: {source_assets}")
            target_pack = target_root / pack_id
            if target_pack.exists():
                shutil.rmtree(target_pack)
            pack_count = copy_tree(source_assets, target_pack / "assets")
            copied_count += pack_count
            assigned_packs.append({
                "pack_id": pack_id,
                "pack_title": manifest.get("title", ""),
                "license": manifest.get("license", ""),
                "license_url": manifest.get("license_url", ""),
                "source_url": manifest.get("source_url", ""),
                "art_roles": manifest.get("art_roles", []),
                "recommended_game_types": manifest.get("recommended_game_types", []),
                "style_tags": manifest.get("style_tags", []),
                "quality_tags": manifest.get("quality_tags", pack_entry.get("quality_tags", [])),
                "animation_capabilities": manifest.get("animation_capabilities", pack_entry.get("animation_capabilities", [])),
                "animation_states": manifest.get("animation_states", pack_entry.get("animation_states", [])),
                "project_pack_path": f"app/src/main/assets/game_art/{pack_id}",
                "copied_file_count": pack_count,
            })
            append_used_by(pack_entry, args.game_id)
        record = {
            "version": 1,
            "game_id": args.game_id,
            "assigned_at_utc": utc_now(),
            "packs": assigned_packs,
            "copied_file_count": copied_count,
        }
        save_json(target_root / "game_art_assignment.json", record)
        runtime_map_path = target_root / "runtime_art_map.json"
        if not runtime_map_path.exists():
            save_json(runtime_map_path, create_runtime_map(args.game_id, args.theme, args.game_type, args.art_roles))
        save_json(index_path, index_data)
    else:
        for pack_id, pack_entry, pack_dir, manifest in resolved:
            source_assets = pack_dir / manifest.get("assets_path", "assets")
            if not source_assets.exists():
                return fail(f"Missing pack assets directory: {source_assets}")
            copied_count += sum(1 for p in source_assets.rglob("*") if p.is_file())

    print(f"GAME_ART_SELECTED_PACK={','.join(requested_pack_ids)}")
    print(f"GAME_ART_ASSIGNED_FILES={copied_count}")
    print(f"GAME_ART_DRY_RUN={str(args.dry_run).lower()}")
    if not args.dry_run and args.project:
        print("GAME_ART_RUNTIME_MAP=app/src/main/assets/game_art/runtime_art_map.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
