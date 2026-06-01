import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def attr(node, name, default=""):
    return node.attrib.get(ANDROID_NS + name, node.attrib.get("android:" + name, default))


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
    node_width = width if w in ("match_parent", "fill_parent") else dimen_value(project, w)
    node_height = height if h in ("match_parent", "fill_parent") else dimen_value(project, h)
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
    points = parse_nodes(project)
    if not points:
        report["status"] = "warning"
        report["risk"] = "medium"
        report["warnings"].append("No normalized runtime node coordinates were detected")
        return finish(report, args.json)
    overlays = direct_visible_overlays(root, game_node)
    viewports = [(800, 480), (1280, 720), (1600, 900), (1200, 800)]
    for viewport in viewports:
        game_rect = estimate_node_rect(project, game_node, viewport)
        overlay_rects = []
        for overlay in overlays:
            overlay_rects.append({
                "id": attr(overlay, "id") or overlay.tag,
                "rect": estimate_node_rect(project, overlay, viewport),
            })
        vp_report = {"width": viewport[0], "height": viewport[1], "game_rect": game_rect, "overlay_count": len(overlay_rects)}
        report["viewports"].append(vp_report)
        for point in points:
            x = game_rect[0] + point["x"] * max(1.0, game_rect[2] - game_rect[0])
            y = game_rect[1] + point["y"] * max(1.0, game_rect[3] - game_rect[1])
            node_rect = (x - 34.0, y - 34.0, x + 34.0, y + 34.0)
            for overlay in overlay_rects:
                if intersects(node_rect, overlay["rect"], 2.0):
                    report["collisions"].append({
                        "viewport": f"{viewport[0]}x{viewport[1]}",
                        "node": point["name"],
                        "overlay": overlay["id"],
                    })
    if report["collisions"]:
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
        print(f"RUNTIME_UI_OCCLUSION_RISK={report['risk']}")
        print(f"RUNTIME_UI_COLLISIONS={len(report['collisions'])}")
        if report["errors"]:
            print("RUNTIME_UI_ERRORS=" + " | ".join(report["errors"]))
        if report["warnings"]:
            print("RUNTIME_UI_WARNINGS=" + " | ".join(report["warnings"]))
    return 0 if report["status"] == "passed" else 2


if __name__ == "__main__":
    sys.exit(main())
