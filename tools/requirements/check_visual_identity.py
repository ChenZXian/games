import argparse
import json
import sys
from pathlib import Path


REQUIRED_TOP_LEVEL = [
    "version",
    "game_id",
    "status",
    "visual_differentiation_axes",
    "forbidden_visual_reuse",
    "ui_identity",
    "icon_identity",
]

UI_REQUIRED = [
    "layout_archetype",
    "hud_composition",
    "navigation_model",
    "palette_signature",
    "material_language",
    "typography_style",
    "primary_ui_pack",
    "unique_screen_motifs",
    "forbidden_ui_elements",
]

ICON_REQUIRED = [
    "subject",
    "silhouette",
    "composition",
    "palette",
    "background",
    "game_specific_motif",
    "forbidden_icon_reuse",
]


def is_non_empty(value):
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, list):
        return any(is_non_empty(item) for item in value)
    if isinstance(value, dict):
        return any(is_non_empty(item) for item in value.values())
    return value is not None


def validate_named_fields(data, keys, label):
    errors = []
    for key in keys:
        if key not in data:
            errors.append(f"{label} missing field: {key}")
        elif not is_non_empty(data.get(key)):
            errors.append(f"{label} empty field: {key}")
    return errors


def validate_contract(data):
    errors = []
    for key in REQUIRED_TOP_LEVEL:
        if key not in data:
            errors.append(f"missing top-level field: {key}")

    for key in ["game_id", "status"]:
        if not is_non_empty(data.get(key)):
            errors.append(f"empty required field: {key}")

    if data.get("status") not in ["draft", "passed", "needs_revision"]:
        errors.append("status must be draft, passed, or needs_revision")

    for key in ["visual_differentiation_axes", "forbidden_visual_reuse"]:
        if not isinstance(data.get(key), list) or not is_non_empty(data.get(key)):
            errors.append(f"{key} must be a non-empty list")

    ui_identity = data.get("ui_identity")
    if not isinstance(ui_identity, dict):
        errors.append("ui_identity must be an object")
    else:
        errors.extend(validate_named_fields(ui_identity, UI_REQUIRED, "ui_identity"))

    icon_identity = data.get("icon_identity")
    if not isinstance(icon_identity, dict):
        errors.append("icon_identity must be an object")
    else:
        errors.extend(validate_named_fields(icon_identity, ICON_REQUIRED, "icon_identity"))

    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--path", required=True)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    path = Path(args.path)
    if not path.exists():
        print("VISUAL_IDENTITY_STATUS=missing")
        print("VISUAL_IDENTITY_VALID=false")
        print("VISUAL_IDENTITY_ERRORS=1")
        print(f"ERROR=missing contract: {path}")
        return 2 if args.strict else 0

    try:
        data = json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception as exc:
        print("VISUAL_IDENTITY_STATUS=invalid")
        print("VISUAL_IDENTITY_VALID=false")
        print("VISUAL_IDENTITY_ERRORS=1")
        print(f"ERROR=invalid json: {exc}")
        return 2 if args.strict else 0

    status = str(data.get("status") or "invalid")
    errors = validate_contract(data)
    valid = not errors and status == "passed"

    print(f"VISUAL_IDENTITY_STATUS={status}")
    print(f"VISUAL_IDENTITY_VALID={str(valid).lower()}")
    print(f"VISUAL_IDENTITY_ERRORS={len(errors)}")
    for error in errors:
        print(f"ERROR={error}")

    if args.strict and not valid:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
