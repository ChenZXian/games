from pathlib import Path

from game_art_utils import load_json, save_json, tokenize_text


def load_map_diversity_registry(path: Path):
    if not path.exists():
        return {"version": 1, "entries": []}
    return load_json(path)


def save_map_diversity_registry(path: Path, registry):
    save_json(path, registry)


def recent_entries(registry, limit=12):
    entries = registry.get("entries", []) if isinstance(registry, dict) else []
    if not isinstance(entries, list):
        return []
    return entries[-limit:]


def build_pack_map_signature(pack_entry):
    if not isinstance(pack_entry, dict):
        return {}
    return {
        "pack_id": str(pack_entry.get("pack_id", "")).strip(),
        "camera_perspectives": pack_entry.get("camera_perspectives", []),
        "map_roles": pack_entry.get("map_roles", []),
        "map_capabilities": pack_entry.get("map_capabilities", []),
        "style_tags": pack_entry.get("style_tags", []),
        "recommended_game_types": pack_entry.get("recommended_game_types", []),
        "map_builder_strength": pack_entry.get("map_builder_strength", "weak"),
    }


def register_map_selection(registry, game_id: str, pack_entries, theme="", game_type="", art_roles=""):
    entries = registry.setdefault("entries", [])
    pack_entries = [item for item in (pack_entries or []) if isinstance(item, dict)]
    combined_tokens = tokenize_text(
        theme,
        game_type,
        art_roles,
        [item.get("style_tags", []) for item in pack_entries],
        [item.get("recommended_game_types", []) for item in pack_entries],
        [item.get("map_roles", []) for item in pack_entries],
        [item.get("camera_perspectives", []) for item in pack_entries],
    )
    entries.append(
        {
            "game_id": game_id,
            "pack_ids": [str(item.get("pack_id", "")).strip() for item in pack_entries if str(item.get("pack_id", "")).strip()],
            "camera_perspectives": sorted({token for item in pack_entries for token in item.get("camera_perspectives", [])}),
            "style_tags": sorted({token for item in pack_entries for token in item.get("style_tags", [])}),
            "map_roles": sorted({token for item in pack_entries for token in item.get("map_roles", [])}),
            "map_capabilities": sorted({token for item in pack_entries for token in item.get("map_capabilities", [])}),
            "map_builder_strengths": sorted({str(item.get("map_builder_strength", "weak")) for item in pack_entries}),
            "theme": theme,
            "game_type": game_type,
            "art_roles": art_roles,
            "signature_tokens": combined_tokens,
        }
    )
    return registry


def compute_map_diversity_penalty(pack_entry, registry, context_tokens):
    if not isinstance(pack_entry, dict):
        return 0
    penalty = 0
    context_tokens = set(tokenize_text(context_tokens))
    pack_id = str(pack_entry.get("pack_id", "")).strip()
    pack_cameras = set(tokenize_text(pack_entry.get("camera_perspectives", [])))
    pack_roles = set(tokenize_text(pack_entry.get("map_roles", [])))
    pack_styles = set(tokenize_text(pack_entry.get("style_tags", []), pack_entry.get("recommended_game_types", [])))
    pack_tokens = set(tokenize_text(pack_entry.get("map_capabilities", []), pack_entry.get("style_tags", []), pack_entry.get("recommended_game_types", [])))
    recent = recent_entries(registry, limit=12)
    for index, entry in enumerate(reversed(recent)):
        recency_weight = max(1, 4 - index)
        previous_pack_ids = {str(item).strip() for item in entry.get("pack_ids", [])}
        previous_cameras = set(tokenize_text(entry.get("camera_perspectives", [])))
        previous_roles = set(tokenize_text(entry.get("map_roles", [])))
        previous_styles = set(tokenize_text(entry.get("style_tags", [])))
        previous_signature = set(tokenize_text(entry.get("signature_tokens", [])))
        if pack_id and pack_id in previous_pack_ids:
            penalty += 18 * recency_weight
        if pack_cameras and pack_cameras.intersection(previous_cameras):
            penalty += 4 * recency_weight
        if pack_roles and len(pack_roles.intersection(previous_roles)) >= 2:
            penalty += 4 * recency_weight
        if pack_styles and len(pack_styles.intersection(previous_styles)) >= 2:
            penalty += 5 * recency_weight
        if context_tokens and pack_tokens and len((context_tokens.union(pack_tokens)).intersection(previous_signature)) >= 5:
            penalty += 6 * recency_weight
    return penalty
