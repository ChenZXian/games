import argparse
import hashlib
import math
import sys
from pathlib import Path

def fail(msg: str) -> int:
    sys.stderr.write(msg + "\n")
    return 2

def ensure_dir(p: Path) -> None:
    p.mkdir(parents=True, exist_ok=True)

def write_text(p: Path, s: str) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(s, encoding="utf-8")

def hex_to_rgb(h: str):
    h = h.strip().lstrip("#")
    if len(h) != 6:
        return (16, 24, 32)
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))

def seeded_colors(seed: str):
    d = hashlib.sha256(seed.encode("utf-8")).hexdigest()
    c1 = "#" + d[0:6]
    c2 = "#" + d[6:12]
    c3 = "#" + d[12:18]
    return c1, c2, c3

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", required=True)
    ap.add_argument("--seed", required=True)
    ap.add_argument("--background", default="#101820")
    args = ap.parse_args()

    project = Path(args.project)
    seed = args.seed
    bg = args.background.strip()

    if not project.exists():
        return fail(f"Project not found: {project}")

    try:
        from PIL import Image, ImageDraw
    except Exception:
        return fail("Pillow not available. Install it in your environment: pip install pillow")

    res_root = project / "app" / "src" / "main" / "res"
    if not res_root.exists():
        return fail(f"res directory not found: {res_root}")

    base_size = 1024
    c1, c2, c3 = seeded_colors(seed)
    r1 = hex_to_rgb(c1)
    r2 = hex_to_rgb(c2)
    r3 = hex_to_rgb(c3)

    img = Image.new("RGBA", (base_size, base_size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Background: simple radial-like layers using circles
    d.rectangle([0, 0, base_size, base_size], fill=hex_to_rgb(bg) + (255,))
    for i in range(7, 0, -1):
        t = i / 7.0
        col = (
            int(r1[0] * t + r2[0] * (1 - t)),
            int(r1[1] * t + r2[1] * (1 - t)),
            int(r1[2] * t + r2[2] * (1 - t)),
            140
        )
        pad = int((1 - t) * 220)
        d.ellipse([pad, pad, base_size - pad, base_size - pad], fill=col)

    # Foreground: geometric emblem (hex + bolt)
    cx = cy = base_size // 2
    radius = 310
    pts = []
    for k in range(6):
        ang = math.radians(60 * k - 30)
        pts.append((cx + int(radius * math.cos(ang)), cy + int(radius * math.sin(ang))))
    d.polygon(pts, fill=r3 + (210,), outline=r2 + (255,))

    bolt = [
        (cx - 40, cy - 260),
        (cx + 40, cy - 260),
        (cx - 10, cy - 40),
        (cx + 110, cy - 40),
        (cx - 60, cy + 260),
        (cx + 10, cy + 40),
        (cx - 110, cy + 40),
    ]
    d.polygon(bolt, fill=r1 + (240,))

    # Legacy launcher sizes
    legacy = [
        ("mipmap-mdpi", 48),
        ("mipmap-hdpi", 72),
        ("mipmap-xhdpi", 96),
        ("mipmap-xxhdpi", 144),
        ("mipmap-xxxhdpi", 192),
    ]

    for folder, size in legacy:
        out_dir = res_root / folder
        ensure_dir(out_dir)
        out_path = out_dir / "app_icon.png"
        resized = img.resize((size, size), Image.LANCZOS)
        resized.save(out_path, format="PNG", optimize=True)

    # Adaptive icon
    anydpi = res_root / "mipmap-anydpi-v26"
    ensure_dir(anydpi)

    fg_dir = res_root / "mipmap-xxxhdpi"
    ensure_dir(fg_dir)
    fg_path = fg_dir / "app_icon_foreground.png"
    fg = img.resize((432, 432), Image.LANCZOS)
    fg.save(fg_path, format="PNG", optimize=True)

    colors_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="app_icon_background">{bg}</color>
</resources>
"""
    write_text(res_root / "values" / "app_icon_colors.xml", colors_xml)

    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/app_icon_background"/>
    <foreground android:drawable="@mipmap/app_icon_foreground"/>
</adaptive-icon>
"""
    write_text(anydpi / "app_icon.xml", adaptive_xml)
    write_text(anydpi / "app_icon_round.xml", adaptive_xml)

    return 0

if __name__ == "__main__":
    raise SystemExit(main())
