from pathlib import Path

from game_art_utils import tokenize_text


MAP_ROLE_TOKENS = {
    "map",
    "tileset",
    "terrain",
    "building",
    "prop",
    "background",
}


def _normalized_files(assets_dir: Path):
    files = []
    if not assets_dir.exists():
        return files
    for item in assets_dir.rglob("*"):
        if item.is_file():
            files.append(item.relative_to(assets_dir).as_posix().lower())
    return files


def infer_camera_perspectives(tokens):
    perspectives = []
    token_set = set(tokens)
    if "isometric" in token_set:
        perspectives.append("isometric")
    if "side" in token_set or "platformer" in token_set or "runner" in token_set or "scroller" in token_set:
        perspectives.append("side_view")
    if "top" in token_set or "down" in token_set or "roguelike" in token_set or "rpg" in token_set or "dungeon" in token_set:
        perspectives.append("top_down")
    if "space" in token_set and "top_down" not in perspectives:
        perspectives.append("top_down")
    return perspectives


def infer_map_metadata_from_assets(assets_dir: Path, descriptor):
    descriptor = descriptor or {}
    rel_files = _normalized_files(assets_dir)
    token_set = set(
        tokenize_text(
            descriptor.get("title", ""),
            descriptor.get("pack_id", ""),
            descriptor.get("art_roles", []),
            descriptor.get("style_tags", []),
            descriptor.get("recommended_game_types", []),
        )
    )
    map_roles = sorted(token for token in tokenize_text(descriptor.get("art_roles", [])) if token in MAP_ROLE_TOKENS)
    tile_png_count = sum(1 for rel in rel_files if rel.endswith(".png") and "/tiles/" in f"/{rel}")
    tilemap_sheet_count = sum(1 for rel in rel_files if rel.endswith(".png") and "tilemap" in rel)
    tiled_tmx_count = sum(1 for rel in rel_files if rel.endswith(".tmx"))
    tiled_tsx_count = sum(1 for rel in rel_files if rel.endswith(".tsx"))
    sample_map_count = sum(1 for rel in rel_files if rel.endswith(".tmx") and "sample" in rel)
    packed_sheet_count = sum(1 for rel in rel_files if rel.endswith(".png") and "packed" in rel)
    preview_map_count = sum(1 for rel in rel_files if rel.endswith(".png") and ("preview" in rel or "sample" in rel))

    capabilities = []
    if tile_png_count > 0:
        capabilities.append("individual_tiles")
    if tilemap_sheet_count > 0:
        capabilities.append("tilemap_sheet")
    if packed_sheet_count > 0:
        capabilities.append("packed_tilemap_sheet")
    if tiled_tmx_count > 0:
        capabilities.append("tiled_tmx")
    if tiled_tsx_count > 0:
        capabilities.append("tiled_tsx")
    if sample_map_count > 0:
        capabilities.append("sample_map_layout")
    if tile_png_count >= 24:
        capabilities.append("modular_tileset")
    if tile_png_count >= 64:
        capabilities.append("large_tile_variation")
    if "terrain" in map_roles and tile_png_count >= 16:
        capabilities.append("terrain_variants")
    if "building" in map_roles and tile_png_count >= 16:
        capabilities.append("building_tiles")
    if "prop" in map_roles and tile_png_count >= 16:
        capabilities.append("prop_tiles")
    if "background" in map_roles:
        capabilities.append("background_layers")
    if tiled_tmx_count > 0 or tile_png_count >= 24:
        capabilities.append("grid_ready")

    camera_perspectives = infer_camera_perspectives(token_set)
    if "top_down" in camera_perspectives:
        capabilities.append("top_down_layout")
    if "side_view" in camera_perspectives:
        capabilities.append("side_view_layout")
    if "isometric" in camera_perspectives:
        capabilities.append("isometric_layout")

    score = 0
    score += min(tile_png_count // 32, 4)
    score += min(tilemap_sheet_count, 2)
    score += min(tiled_tmx_count, 2) * 2
    score += min(tiled_tsx_count, 2)
    score += 2 if "modular_tileset" in capabilities else 0
    score += 1 if "sample_map_layout" in capabilities else 0
    score += 1 if "terrain_variants" in capabilities else 0
    score += 1 if "building_tiles" in capabilities else 0
    score += 1 if "prop_tiles" in capabilities else 0

    if score >= 10:
        builder_strength = "strong"
    elif score >= 5:
        builder_strength = "moderate"
    elif score >= 2:
        builder_strength = "basic"
    else:
        builder_strength = "weak"

    return {
        "map_roles": map_roles,
        "map_capabilities": sorted(set(capabilities)),
        "camera_perspectives": camera_perspectives,
        "tile_asset_metrics": {
            "tile_png_count": tile_png_count,
            "tilemap_sheet_count": tilemap_sheet_count,
            "packed_sheet_count": packed_sheet_count,
            "tiled_tmx_count": tiled_tmx_count,
            "tiled_tsx_count": tiled_tsx_count,
            "sample_map_count": sample_map_count,
            "preview_map_count": preview_map_count,
        },
        "map_builder_strength": builder_strength,
    }


def infer_map_metadata_from_descriptor(descriptor):
    descriptor = descriptor or {}
    token_set = set(
        tokenize_text(
            descriptor.get("title", ""),
            descriptor.get("pack_id", ""),
            descriptor.get("art_roles", []),
            descriptor.get("style_tags", []),
            descriptor.get("recommended_game_types", []),
        )
    )
    map_roles = sorted(token for token in tokenize_text(descriptor.get("art_roles", [])) if token in MAP_ROLE_TOKENS)
    capabilities = []
    if "tileset" in map_roles:
        capabilities.append("tileset_ready")
    if "terrain" in map_roles:
        capabilities.append("terrain_ready")
    if "building" in map_roles:
        capabilities.append("building_ready")
    if "prop" in map_roles:
        capabilities.append("prop_ready")
    if "background" in map_roles:
        capabilities.append("background_layers")
    camera_perspectives = infer_camera_perspectives(token_set)
    if "top_down" in camera_perspectives:
        capabilities.append("top_down_layout")
    if "side_view" in camera_perspectives:
        capabilities.append("side_view_layout")
    if "isometric" in camera_perspectives:
        capabilities.append("isometric_layout")
    if len(map_roles) >= 3:
        builder_strength = "moderate"
    elif len(map_roles) >= 1:
        builder_strength = "basic"
    else:
        builder_strength = "weak"
    return {
        "map_roles": map_roles,
        "map_capabilities": sorted(set(capabilities)),
        "camera_perspectives": camera_perspectives,
        "tile_asset_metrics": {},
        "map_builder_strength": builder_strength,
    }

