import argparse
from pathlib import Path

from game_art_utils import find_pack, load_index, load_pack_manifest, load_source_catalog, save_json
from map_catalog_utils import infer_map_metadata_from_assets, infer_map_metadata_from_descriptor


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--library-root", default="shared_assets/game_art")
    ap.add_argument("--source-catalog", default="shared_assets/game_art/source_catalog.json")
    ap.add_argument("--write-manifests", action="store_true")
    args = ap.parse_args()

    library_root = Path(args.library_root).resolve()
    index_path = library_root / "index.json"
    map_catalog_path = library_root / "map_catalog.json"
    source_catalog_path = Path(args.source_catalog).resolve()

    index_data = load_index(index_path)
    source_catalog = load_source_catalog(source_catalog_path)
    catalog_packs = []

    for pack_dir in sorted((library_root / "packs").iterdir(), key=lambda item: item.name):
        if not pack_dir.is_dir():
            continue
        manifest = load_pack_manifest(pack_dir)
        inferred = infer_map_metadata_from_assets(pack_dir / manifest.get("assets_path", "assets"), manifest)
        manifest.update(inferred)
        if args.write_manifests:
            save_json(pack_dir / "manifest.json", manifest)

        pack_id = str(manifest.get("pack_id", "")).strip()
        pack_entry = find_pack(index_data, pack_id)
        if pack_entry is not None:
            pack_entry.update(
                {
                    "map_roles": inferred.get("map_roles", []),
                    "map_capabilities": inferred.get("map_capabilities", []),
                    "camera_perspectives": inferred.get("camera_perspectives", []),
                    "tile_asset_metrics": inferred.get("tile_asset_metrics", {}),
                    "map_builder_strength": inferred.get("map_builder_strength", "weak"),
                }
            )

        if inferred.get("map_roles") or inferred.get("map_capabilities"):
            catalog_packs.append(
                {
                    "pack_id": pack_id,
                    "title": manifest.get("title", ""),
                    "path": f"packs/{pack_id}",
                    "source_url": manifest.get("source_url", ""),
                    "map_roles": inferred.get("map_roles", []),
                    "map_capabilities": inferred.get("map_capabilities", []),
                    "camera_perspectives": inferred.get("camera_perspectives", []),
                    "tile_asset_metrics": inferred.get("tile_asset_metrics", {}),
                    "map_builder_strength": inferred.get("map_builder_strength", "weak"),
                    "style_tags": manifest.get("style_tags", []),
                    "recommended_game_types": manifest.get("recommended_game_types", []),
                }
            )

    for source in source_catalog.get("sources", []):
        if not isinstance(source, dict):
            continue
        source.update(infer_map_metadata_from_descriptor(source))

    save_json(index_path, index_data)
    save_json(source_catalog_path, source_catalog)
    save_json(
        map_catalog_path,
        {
            "version": 1,
            "packs": catalog_packs,
        },
    )
    print(f"GAME_ART_MAP_CATALOG={map_catalog_path.as_posix()}")
    print(f"GAME_ART_MAP_PACK_COUNT={len(catalog_packs)}")


if __name__ == "__main__":
    raise SystemExit(main())

