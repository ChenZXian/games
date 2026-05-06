import argparse
import json
import sys
from pathlib import Path


REQUIRED_TOP_LEVEL = [
    "version",
    "game_id",
    "status",
    "genre_family",
    "genre_archetype",
    "camera_perspective",
    "control_model",
    "core_loop_signature",
    "differentiation_axes",
    "forbidden_template_reuse",
    "map_content_budget",
    "entity_content_budget",
    "mechanic_content_budget",
    "asset_variety_budget",
]


def is_non_empty(value):
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, list):
        return any(is_non_empty(item) for item in value)
    if isinstance(value, dict):
        return any(is_non_empty(item) for item in value.values())
    return value is not None


def count_non_empty_fields(data, keys):
    total = 0
    for key in keys:
        if is_non_empty(data.get(key)):
            total += 1
    return total


def validate_contract(data):
    errors = []
    for key in REQUIRED_TOP_LEVEL:
        if key not in data:
            errors.append(f"missing top-level field: {key}")

    for key in [
        "game_id",
        "status",
        "genre_family",
        "genre_archetype",
        "camera_perspective",
        "control_model",
        "core_loop_signature",
    ]:
        if not is_non_empty(data.get(key)):
            errors.append(f"empty required field: {key}")

    if data.get("status") not in ["draft", "passed", "needs_revision"]:
        errors.append("status must be draft, passed, or needs_revision")

    for key in ["differentiation_axes", "forbidden_template_reuse"]:
        if not isinstance(data.get(key), list) or not is_non_empty(data.get(key)):
            errors.append(f"{key} must be a non-empty list")

    map_budget = data.get("map_content_budget")
    if not isinstance(map_budget, dict):
        errors.append("map_content_budget must be an object")
    else:
        required_map_keys = [
            "play_area_model",
            "route_or_region_count",
            "interactive_regions",
            "terrain_types",
            "functional_map_elements",
        ]
        if count_non_empty_fields(map_budget, required_map_keys) < 4:
            errors.append("map_content_budget is too generic")

    entity_budget = data.get("entity_content_budget")
    if not isinstance(entity_budget, dict):
        errors.append("entity_content_budget must be an object")
    else:
        if count_non_empty_fields(entity_budget, list(entity_budget.keys())) < 3:
            errors.append("entity_content_budget needs at least three non-empty role fields")

    mechanic_budget = data.get("mechanic_content_budget")
    if not isinstance(mechanic_budget, dict):
        errors.append("mechanic_content_budget must be an object")
    else:
        if count_non_empty_fields(mechanic_budget, list(mechanic_budget.keys())) < 4:
            errors.append("mechanic_content_budget needs at least four non-empty fields")

    asset_budget = data.get("asset_variety_budget")
    if not isinstance(asset_budget, dict):
        errors.append("asset_variety_budget must be an object")
    else:
        required_asset_keys = [
            "primary_game_art_packs",
            "animation_tier",
            "required_animation_states",
            "minimum_distinct_sprite_families",
            "asset_reuse_note",
        ]
        if count_non_empty_fields(asset_budget, required_asset_keys) < 4:
            errors.append("asset_variety_budget is too generic")

    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--path", required=True)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    path = Path(args.path)
    if not path.exists():
        print("GAMEPLAY_DIVERSITY_STATUS=missing")
        print("GAMEPLAY_DIVERSITY_VALID=false")
        print("GAMEPLAY_DIVERSITY_ERRORS=1")
        print(f"ERROR=missing contract: {path}")
        return 2 if args.strict else 0

    try:
        data = json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception as exc:
        print("GAMEPLAY_DIVERSITY_STATUS=invalid")
        print("GAMEPLAY_DIVERSITY_VALID=false")
        print("GAMEPLAY_DIVERSITY_ERRORS=1")
        print(f"ERROR=invalid json: {exc}")
        return 2 if args.strict else 0

    status = str(data.get("status") or "invalid")
    errors = validate_contract(data)
    valid = not errors and status == "passed"

    print(f"GAMEPLAY_DIVERSITY_STATUS={status}")
    print(f"GAMEPLAY_DIVERSITY_VALID={str(valid).lower()}")
    print(f"GAMEPLAY_DIVERSITY_ERRORS={len(errors)}")
    for error in errors:
        print(f"ERROR={error}")

    if args.strict and not valid:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
