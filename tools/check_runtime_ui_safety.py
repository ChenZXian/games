import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
APP_NS = "{http://schemas.android.com/apk/res-auto}"
VIEWPORTS = [
    ("small_phone_portrait", 480, 800),
    ("small_phone_landscape", 800, 480),
    ("narrow_landscape", 600, 360),
    ("phone_landscape", 1280, 720),
    ("tablet_landscape", 1600, 900),
    ("tablet_square", 1200, 800),
]
LANDSCAPE_VIEWPORTS = [
    item for item in VIEWPORTS
    if item[1] >= item[2]
]
PORTRAIT_VIEWPORTS = [
    item for item in VIEWPORTS
    if item[1] <= item[2]
]
MIN_TOUCH_WIDTH = 48.0
MIN_TOUCH_HEIGHT = 44.0


def attr(node, name, default=""):
    return node.attrib.get(ANDROID_NS + name, node.attrib.get("android:" + name, default))


def app_attr(node, name, default=""):
    return node.attrib.get(APP_NS + name, node.attrib.get("app:" + name, default))


def dimen_value(project, value):
    if not value:
        return 0.0
    text = value.strip()
    if text.startswith("@dimen/"):
        name = text.split("/", 1)[1]
        dimen_path = project / "app/src/main/res/values/dimens.xml"
        if dimen_path.exists():
            root = ET.parse(dimen_path).getroot()
            for item in root:
                if item.tag == "dimen" and item.attrib.get("name") == name:
                    return dimen_value(project, item.text or "0")
        return 0.0
    m = re.match(r"(-?\d+(?:\.\d+)?)(dp|dip|px)?$", text)
    if m:
        return float(m.group(1))
    return 0.0


def parse_float(value, default=0.0):
    if value is None:
        return default
    try:
        return float(str(value).strip())
    except ValueError:
        return default


def load_res_value_xml(project, file_name):
    path = project / "app/src/main/res/values" / file_name
    if not path.exists():
        return None
    try:
        return ET.parse(path).getroot()
    except Exception:
        return None


def parse_strings(project):
    values = {}
    values_dir = project / "app/src/main/res/values"
    if not values_dir.exists():
        return values
    for path in values_dir.glob("*.xml"):
        try:
            root = ET.parse(path).getroot()
        except Exception:
            continue
        for item in root:
            if item.tag == "string" and item.attrib.get("name"):
                values[item.attrib["name"]] = "".join(item.itertext()).strip()
    return values


def resolve_text(node, strings):
    value = attr(node, "text")
    if value.startswith("@string/"):
        return strings.get(value.split("/", 1)[1], "")
    if value.startswith("@"):
        return ""
    return value


def manifest_orientation(project):
    manifest = project / "app/src/main/AndroidManifest.xml"
    if not manifest.exists():
        return ""
    try:
        root = ET.parse(manifest).getroot()
    except Exception:
        return ""
    for node in root.iter():
        orientation = attr(node, "screenOrientation")
        if orientation:
            return orientation.lower()
    return ""


def viewport_matrix(project):
    orientation = manifest_orientation(project)
    if "landscape" in orientation:
        return LANDSCAPE_VIEWPORTS
    if "portrait" in orientation:
        return PORTRAIT_VIEWPORTS
    return VIEWPORTS


def parse_gravity(value):
    return set(part.strip().lower() for part in (value or "").replace("|", " ").split() if part.strip())


def estimate_node_rect(project, node, viewport):
    width, height = viewport
    w = attr(node, "layout_width")
    h = attr(node, "layout_height")
    gravity = parse_gravity(attr(node, "layout_gravity"))
    margin_start = dimen_value(project, attr(node, "layout_marginStart") or attr(node, "layout_marginLeft") or attr(node, "layout_margin"))
    margin_end = dimen_value(project, attr(node, "layout_marginEnd") or attr(node, "layout_marginRight") or attr(node, "layout_margin"))
    margin_top = dimen_value(project, attr(node, "layout_marginTop") or attr(node, "layout_margin"))
    margin_bottom = dimen_value(project, attr(node, "layout_marginBottom") or attr(node, "layout_margin"))
    horizontal_constraint = bool(app_attr(node, "layout_constraintStart_toStartOf") or app_attr(node, "layout_constraintStart_toEndOf") or app_attr(node, "layout_constraintLeft_toLeftOf") or app_attr(node, "layout_constraintLeft_toRightOf")) and bool(app_attr(node, "layout_constraintEnd_toEndOf") or app_attr(node, "layout_constraintEnd_toStartOf") or app_attr(node, "layout_constraintRight_toRightOf") or app_attr(node, "layout_constraintRight_toLeftOf"))
    vertical_constraint = bool(app_attr(node, "layout_constraintTop_toTopOf") or app_attr(node, "layout_constraintTop_toBottomOf")) and bool(app_attr(node, "layout_constraintBottom_toBottomOf") or app_attr(node, "layout_constraintBottom_toTopOf"))
    node_width = max(0.0, width - margin_start - margin_end) if w in ("match_parent", "fill_parent") or (w in ("0dp", "0dip") and horizontal_constraint) else dimen_value(project, w)
    node_height = max(0.0, height - margin_top - margin_bottom) if h in ("match_parent", "fill_parent") or (h in ("0dp", "0dip") and vertical_constraint) else dimen_value(project, h)
    if h == "wrap_content":
        node_height = estimate_wrap_height(project, node)
    if w == "wrap_content":
        node_width = estimate_wrap_width(project, node)
    if "end" in gravity or "right" in gravity:
        x = width - margin_end - node_width
    elif "center" in gravity and not ("start" in gravity or "left" in gravity):
        x = (width - node_width) * 0.5
    else:
        x = margin_start
    if "bottom" in gravity:
        y = height - margin_bottom - node_height
    elif "center_vertical" in gravity or ("center" in gravity and not ("top" in gravity or "bottom" in gravity)):
        y = (height - node_height) * 0.5
    else:
        y = margin_top
    return (x, y, x + node_width, y + node_height)


def estimate_wrap_height(project, node):
    total = dimen_value(project, attr(node, "paddingTop") or attr(node, "padding") or "0") + dimen_value(project, attr(node, "paddingBottom") or attr(node, "padding") or "0")
    children = list(node)
    if not children:
        return max(44.0, total)
    child_total = 0.0
    for child in children:
        h = attr(child, "layout_height")
        margin_top = dimen_value(project, attr(child, "layout_marginTop"))
        margin_bottom = dimen_value(project, attr(child, "layout_marginBottom"))
        if h == "wrap_content":
            child_h = 36.0
        elif h == "match_parent":
            child_h = 64.0
        else:
            child_h = dimen_value(project, h) or 36.0
        child_total += child_h + margin_top + margin_bottom
    orientation = attr(node, "orientation")
    if orientation == "horizontal":
        child_total = max(44.0, max((dimen_value(project, attr(child, "layout_height")) or 36.0) for child in children))
    return max(44.0, total + child_total)


def estimate_wrap_width(project, node):
    width = dimen_value(project, attr(node, "minWidth"))
    text = attr(node, "text")
    if text:
        width = max(width, min(260.0, 24.0 + len(text) * 8.0))
    children = list(node)
    if children:
        if attr(node, "orientation") == "horizontal":
            width = max(width, sum((dimen_value(project, attr(child, "layout_width")) or 70.0) for child in children))
        else:
            width = max(width, max((dimen_value(project, attr(child, "layout_width")) or 120.0) for child in children))
    return max(44.0, width)


def text_width(text):
    if not text:
        return 0.0
    wide = sum(1 for ch in text if ch in "MW@#%&")
    return 24.0 + len(text) * 7.2 + wide * 2.0


def find_gameview(root):
    for node in root.iter():
        if node.tag.lower().endswith("gameview") or node.tag.lower().endswith("surfaceview") or node.tag.lower().endswith("textureview"):
            return node
    return None


def direct_visible_overlays(root, game_node):
    result = []
    for child in list(root):
        if child is game_node:
            continue
        node_id = attr(child, "id").lower()
        visibility = attr(child, "visibility").lower()
        if visibility == "gone":
            continue
        if "overlay" in node_id or "menu_" in node_id or "pause_" in node_id or "result_" in node_id:
            continue
        gravity = attr(child, "layout_gravity")
        if gravity or attr(child, "layout_width") or attr(child, "layout_height"):
            result.append(child)
    return result


def parse_nodes(project):
    java_root = project / "app/src/main/java"
    points = []
    if not java_root.exists():
        return points
    pattern = re.compile(r"nodes\.add\(new\s+Node\(\"([^\"]+)\"\s*,\s*([0-9.]+)f?\s*,\s*([0-9.]+)f?")
    for path in java_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for m in pattern.finditer(text):
            points.append({"name": m.group(1), "x": float(m.group(2)), "y": float(m.group(3)), "file": str(path)})
    return points


def intersects(a, b, pad=0.0):
    return not (a[2] < b[0] - pad or a[0] > b[2] + pad or a[3] < b[1] - pad or a[1] > b[3] + pad)


def is_visible(node):
    return attr(node, "visibility").lower() != "gone"


def visible_iter(node, inherited=True):
    current = inherited and is_visible(node)
    if current:
        yield node
    for child in list(node):
        yield from visible_iter(child, current)


def is_button_like(node):
    name = node.tag.lower()
    node_id = attr(node, "id").lower()
    return name.endswith("button") or "button" in name or "btn_" in node_id or "_button" in node_id


def check_dense_control_rows(root, report):
    for node in visible_iter(root):
        if attr(node, "orientation").lower() != "horizontal" or not is_visible(node):
            continue
        buttons = [child for child in list(node) if is_visible(child) and is_button_like(child)]
        if len(buttons) > 6:
            node_id = attr(node, "id") or node.tag
            report["errors"].append(f"Horizontal control row has {len(buttons)} buttons: {node_id}")


def check_fixed_width_panels(project, root, report, viewports):
    min_width = min(width for _, width, _ in viewports)
    for node in visible_iter(root):
        if not is_visible(node):
            continue
        width_value = attr(node, "layout_width")
        width = dimen_value(project, width_value)
        node_id = attr(node, "id").lower()
        if width > 0 and width > min_width - 48 and ("menu" in node_id or "panel" in node_id or "overlay" in node_id):
            label = attr(node, "id") or node.tag
            report["errors"].append(f"Fixed panel width can exceed small viewport: {label}={width_value}")


def row_padding(project, node):
    start = dimen_value(project, attr(node, "paddingStart") or attr(node, "paddingLeft") or attr(node, "padding"))
    end = dimen_value(project, attr(node, "paddingEnd") or attr(node, "paddingRight") or attr(node, "padding"))
    top = dimen_value(project, attr(node, "paddingTop") or attr(node, "padding"))
    bottom = dimen_value(project, attr(node, "paddingBottom") or attr(node, "padding"))
    return start, end, top, bottom


def child_margins(project, node):
    start = dimen_value(project, attr(node, "layout_marginStart") or attr(node, "layout_marginLeft") or attr(node, "layout_margin"))
    end = dimen_value(project, attr(node, "layout_marginEnd") or attr(node, "layout_marginRight") or attr(node, "layout_margin"))
    top = dimen_value(project, attr(node, "layout_marginTop") or attr(node, "layout_margin"))
    bottom = dimen_value(project, attr(node, "layout_marginBottom") or attr(node, "layout_margin"))
    return start, end, top, bottom


def estimated_button_intrinsic_width(project, node, strings):
    min_width = dimen_value(project, attr(node, "minWidth"))
    text = resolve_text(node, strings)
    value = text_width(text)
    if not text:
        value = 48.0
    return max(min_width, value, 44.0)


def estimated_child_height(project, node, row_height):
    value = attr(node, "layout_height")
    if value in ("match_parent", "fill_parent", "0dp", "0dip"):
        return row_height
    if value == "wrap_content" or not value:
        return max(44.0, dimen_value(project, attr(node, "minHeight")))
    return dimen_value(project, value) or row_height


def estimate_horizontal_button_widths(project, row, buttons, viewport, strings):
    row_rect = estimate_node_rect(project, row, (viewport[1], viewport[2]))
    row_width = max(0.0, row_rect[2] - row_rect[0])
    row_height = max(0.0, row_rect[3] - row_rect[1])
    if row_width <= 0.0 and parse_float(attr(row, "layout_weight"), 0.0) > 0.0:
        row_width = estimate_wrap_width(project, row)
    if row_height <= 0.0 and parse_float(attr(row, "layout_weight"), 0.0) > 0.0:
        row_height = estimate_wrap_height(project, row)
    pad_start, pad_end, pad_top, pad_bottom = row_padding(project, row)
    available = max(0.0, row_width - pad_start - pad_end)
    fixed_total = 0.0
    weighted = []
    results = []
    for child in buttons:
        margin_start, margin_end, margin_top, margin_bottom = child_margins(project, child)
        available = max(0.0, available - margin_start - margin_end)
        width_value = attr(child, "layout_width")
        weight = parse_float(attr(child, "layout_weight"), 0.0)
        if weight > 0.0 and width_value in ("0dp", "0dip", ""):
            weighted.append((child, weight, margin_top, margin_bottom))
            continue
        if width_value in ("match_parent", "fill_parent"):
            child_width = available
        elif width_value == "wrap_content" or not width_value:
            child_width = estimated_button_intrinsic_width(project, child, strings)
        else:
            child_width = dimen_value(project, width_value)
        fixed_total += max(0.0, child_width)
        child_height = estimated_child_height(project, child, max(0.0, row_height - pad_top - pad_bottom - margin_top - margin_bottom))
        results.append((child, max(0.0, child_width), child_height))
    weighted_total = sum(item[1] for item in weighted)
    remaining = max(0.0, available - fixed_total)
    for child, weight, margin_top, margin_bottom in weighted:
        child_width = remaining * (weight / weighted_total) if weighted_total > 0 else 0.0
        child_height = estimated_child_height(project, child, max(0.0, row_height - pad_top - pad_bottom - margin_top - margin_bottom))
        results.append((child, max(0.0, child_width), child_height))
    return results


def check_button_touch_and_text(project, root, report, viewports, strings):
    for row in visible_iter(root):
        if attr(row, "orientation").lower() != "horizontal":
            continue
        buttons = [child for child in list(row) if is_visible(child) and is_button_like(child)]
        if not buttons:
            continue
        row_id = attr(row, "id") or row.tag
        for viewport in viewports:
            estimates = estimate_horizontal_button_widths(project, row, buttons, viewport, strings)
            for child, width, height in estimates:
                child_id = attr(child, "id") or child.tag
                label = f"{viewport[0]} {row_id} {child_id}"
                if width < MIN_TOUCH_WIDTH or height < MIN_TOUCH_HEIGHT:
                    report["errors"].append(f"Small touch target: {label}={width:.1f}x{height:.1f}")
                text = resolve_text(child, strings)
                if text and text_width(text) > width:
                    report["errors"].append(f"Button text may overflow: {label} text={text}")


def check_adaptive_layout_evidence(project, report):
    res_dir = project / "app/src/main/res"
    if not res_dir.exists():
        return
    adaptive_dirs = [
        child.name for child in res_dir.iterdir()
        if child.is_dir() and child.name.startswith("layout-")
    ]
    if not adaptive_dirs:
        report["warnings"].append("No alternate layout qualifier directory; single layout must pass viewport heuristics")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    project = Path(args.project).resolve()
    layout = project / "app/src/main/res/layout/activity_main.xml"
    report = {
        "status": "unknown",
        "risk": "unknown",
        "errors": [],
        "warnings": [],
        "viewports": [],
        "collisions": [],
    }
    if not layout.exists():
        report["status"] = "missing"
        report["risk"] = "high"
        report["errors"].append("activity_main.xml is missing")
        return finish(report, args.json)
    try:
        root = ET.parse(layout).getroot()
    except Exception as exc:
        report["status"] = "invalid"
        report["risk"] = "high"
        report["errors"].append(f"activity_main.xml is invalid: {exc}")
        return finish(report, args.json)
    game_node = find_gameview(root)
    if game_node is None:
        report["status"] = "unknown"
        report["risk"] = "medium"
        report["warnings"].append("No GameView or SurfaceView found")
        return finish(report, args.json)
    strings = parse_strings(project)
    viewports = viewport_matrix(project)
    check_dense_control_rows(root, report)
    check_fixed_width_panels(project, root, report, viewports)
    check_button_touch_and_text(project, root, report, viewports, strings)
    check_adaptive_layout_evidence(project, report)
    points = parse_nodes(project)
    if not points:
        report["warnings"].append("No normalized runtime node coordinates were detected")
    overlays = direct_visible_overlays(root, game_node)
    for viewport in viewports:
        game_rect = estimate_node_rect(project, game_node, (viewport[1], viewport[2]))
        overlay_rects = []
        for overlay in overlays:
            overlay_rects.append({
                "id": attr(overlay, "id") or overlay.tag,
                "rect": estimate_node_rect(project, overlay, (viewport[1], viewport[2])),
            })
        vp_report = {"name": viewport[0], "width": viewport[1], "height": viewport[2], "game_rect": game_rect, "overlay_count": len(overlay_rects)}
        report["viewports"].append(vp_report)
        for point in points:
            x = game_rect[0] + point["x"] * max(1.0, game_rect[2] - game_rect[0])
            y = game_rect[1] + point["y"] * max(1.0, game_rect[3] - game_rect[1])
            node_rect = (x - 34.0, y - 34.0, x + 34.0, y + 34.0)
            for overlay in overlay_rects:
                if intersects(node_rect, overlay["rect"], 2.0):
                    report["collisions"].append({
                        "viewport": f"{viewport[0]}:{viewport[1]}x{viewport[2]}",
                        "node": point["name"],
                        "overlay": overlay["id"],
                    })
    if report["errors"]:
        report["status"] = "failed"
        report["risk"] = "high"
    elif report["collisions"]:
        report["status"] = "failed"
        report["risk"] = "high"
        report["errors"].append(f"{len(report['collisions'])} runtime node overlay collision(s) detected")
    else:
        report["status"] = "passed"
        report["risk"] = "low"
    return finish(report, args.json)


def finish(report, as_json):
    if as_json:
        print(json.dumps(report, ensure_ascii=True, indent=2))
    else:
        print(f"RUNTIME_UI_SAFETY_STATUS={report['status']}")
        print(f"RUNTIME_UI_ADAPTIVE_STATUS={report['status']}")
        print(f"RUNTIME_UI_OCCLUSION_RISK={report['risk']}")
        print(f"RUNTIME_UI_TOUCH_TARGET_RISK={report['risk']}")
        print(f"RUNTIME_UI_COLLISIONS={len(report['collisions'])}")
        print(f"RUNTIME_UI_ADAPTIVE_ERRORS={len(report['errors'])}")
        if report["errors"]:
            print("RUNTIME_UI_ERRORS=" + " | ".join(report["errors"]))
        if report["warnings"]:
            print("RUNTIME_UI_WARNINGS=" + " | ".join(report["warnings"]))
    return 0 if report["status"] == "passed" else 2


if __name__ == "__main__":
    sys.exit(main())
