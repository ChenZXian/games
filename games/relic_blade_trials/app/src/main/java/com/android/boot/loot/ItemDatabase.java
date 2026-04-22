package com.android.boot.loot;

import com.android.boot.model.Enums.Rarity;
import com.android.boot.model.Enums.SlotType;
import com.android.boot.model.ItemDefinition;
import java.util.ArrayList;
import java.util.List;

public class ItemDatabase {
    private final List<ItemDefinition> items = new ArrayList<>();

    public ItemDatabase() {
        add("wooden_sword", "Wooden Sword", SlotType.WEAPON, Rarity.WHITE, 1, 8, 0, 3, 0, 0f, 0f, 0f, 6, "Starter blade for first trials");
        add("ironbranch_blade", "Ironbranch Blade", SlotType.WEAPON, Rarity.GREEN, 1, 16, 0, 7, 0, 0f, 0.01f, 0f, 18, "Forged branch steel for gate skirmish");
        add("marshstep_boots", "Marshstep Boots", SlotType.SHOES, Rarity.WHITE, 1, 9, 10, 0, 1, 0.03f, 0f, 0f, 8, "Soft grip for muddy routes");
        add("emberguard_armor", "Emberguard Armor", SlotType.ARMOR, Rarity.GREEN, 1, 14, 20, 0, 4, 0f, 0f, 0f, 15, "Layered plates for frontline pushes");
        add("fang_helm", "Fang Helm", SlotType.HELMET, Rarity.WHITE, 1, 8, 12, 0, 2, 0f, 0f, 0f, 7, "Simple helm with beastbone crest");
        add("rift_bracers", "Rift Bracers", SlotType.BRACERS, Rarity.GREEN, 1, 13, 0, 4, 1, 0f, 0.02f, 0.04f, 16, "Wrist guards for quick cuts");
        add("titan_belt", "Titan Belt", SlotType.BELT, Rarity.WHITE, 1, 10, 15, 0, 2, 0f, 0f, 0f, 9, "Weight belt for stable stance");
        add("marshfang_blade", "Marshfang Blade", SlotType.WEAPON, Rarity.GREEN, 2, 21, 0, 10, 0, 0f, 0.02f, 0f, 24, "Poison marsh edge with keen tip");
        add("bogtrail_boots", "Bogtrail Boots", SlotType.SHOES, Rarity.GREEN, 2, 17, 12, 0, 2, 0.05f, 0f, 0f, 20, "Swamp tuned boots with deep treads");
        add("mireplate", "Mireplate", SlotType.ARMOR, Rarity.WHITE, 2, 15, 28, 0, 5, 0f, 0f, 0f, 17, "Damp plate armor that absorbs blows");
        add("mire_crown", "Mire Crown", SlotType.HELMET, Rarity.GREEN, 2, 18, 18, 0, 3, 0f, 0.01f, 0f, 21, "Helm blessed by marsh rites");
        add("bogpulse_bracers", "Bogpulse Bracers", SlotType.BRACERS, Rarity.WHITE, 2, 14, 0, 5, 1, 0f, 0.02f, 0.05f, 15, "Rhythm bands for combo timing");
        add("marshbound_belt", "Marshbound Belt", SlotType.BELT, Rarity.GREEN, 2, 18, 22, 0, 4, 0.02f, 0f, 0f, 20, "Belt tuned for wetland pushes");
        add("cinder_edge", "Cinder Edge", SlotType.WEAPON, Rarity.GREEN, 3, 28, 0, 14, 0, 0f, 0.03f, 0.04f, 30, "Heated edge from collapsing forges");
        add("ashrunner_boots", "Ashrunner Boots", SlotType.SHOES, Rarity.WHITE, 3, 20, 14, 0, 3, 0.06f, 0f, 0f, 22, "Heatproof soles for vent lanes");
        add("forgeguard_armor", "Forgeguard Armor", SlotType.ARMOR, Rarity.GREEN, 3, 27, 40, 0, 8, 0f, 0f, 0f, 32, "Dense armor for hammer impacts");
        add("ember_fang_helm", "Ember Fang Helm", SlotType.HELMET, Rarity.WHITE, 3, 21, 22, 0, 4, 0f, 0.01f, 0.02f, 24, "Forged visor with ember spine");
        add("heatrift_bracers", "Heatrift Bracers", SlotType.BRACERS, Rarity.GREEN, 3, 26, 0, 8, 2, 0f, 0.04f, 0.08f, 31, "Bracers that reward tight chains");
        add("forge_titan_belt", "Forge Titan Belt", SlotType.BELT, Rarity.WHITE, 3, 20, 30, 0, 5, 0f, 0f, 0f, 23, "Heavy belt for foundry marches");
        add("duelwind_blade", "Duelwind Blade", SlotType.WEAPON, Rarity.PURPLE, 4, 46, 0, 24, 0, 0.02f, 0.06f, 0.15f, 58, "Swift blade for crimson duels");
        add("hallstep_boots", "Hallstep Boots", SlotType.SHOES, Rarity.GREEN, 4, 32, 18, 0, 5, 0.08f, 0.02f, 0f, 36, "Boots for cursed corridor pivots");
        add("barracks_plate", "Barracks Plate", SlotType.ARMOR, Rarity.PURPLE, 4, 44, 60, 0, 12, 0f, 0f, 0f, 56, "Veteran plate from dark camp walls");
        add("crimson_fang_helm", "Crimson Fang Helm", SlotType.HELMET, Rarity.GREEN, 4, 33, 30, 0, 7, 0f, 0.03f, 0.05f, 37, "Helm tuned for counter strikes");
        add("rift_duelist_bracers", "Rift Duelist Bracers", SlotType.BRACERS, Rarity.PURPLE, 4, 45, 0, 15, 3, 0.02f, 0.08f, 0.16f, 57, "Pulse bracers for rapid cadence");
        add("warcord_belt", "Warcord Belt", SlotType.BELT, Rarity.GREEN, 4, 34, 40, 0, 8, 0.03f, 0f, 0f, 38, "Cord belt for relentless pushes");
        add("stormbreaker", "Stormbreaker", SlotType.WEAPON, Rarity.PURPLE, 5, 58, 0, 32, 0, 0.03f, 0.1f, 0.22f, 76, "Charged steel from thunder spires");
        add("thunderstep_boots", "Thunderstep Boots", SlotType.SHOES, Rarity.PURPLE, 5, 52, 24, 0, 7, 0.11f, 0.04f, 0f, 70, "Boots with storm dash recovery");
        add("tempest_armor", "Tempest Armor", SlotType.ARMOR, Rarity.GREEN, 5, 42, 72, 0, 14, 0f, 0f, 0f, 48, "Coil armor for lightning pressure");
        add("stormcrown", "Stormcrown", SlotType.HELMET, Rarity.PURPLE, 5, 54, 42, 0, 10, 0.01f, 0.07f, 0.14f, 72, "Arc helm with charged crest");
        add("sky_rift_bracers", "Sky Rift Bracers", SlotType.BRACERS, Rarity.GREEN, 5, 43, 0, 13, 4, 0f, 0.06f, 0.12f, 49, "Bands that amplify crit windows");
        add("tempest_belt", "Tempest Belt", SlotType.BELT, Rarity.PURPLE, 5, 53, 58, 0, 11, 0.05f, 0.03f, 0.08f, 71, "Belt with surge guard weave");
        add("abyss_oath_blade", "Abyss Oath Blade", SlotType.WEAPON, Rarity.GOLD, 6, 82, 0, 46, 0, 0.05f, 0.16f, 0.35f, 120, "Final relic blade of void oath");
        add("voidstride_boots", "Voidstride Boots", SlotType.SHOES, Rarity.PURPLE, 6, 62, 32, 0, 10, 0.14f, 0.05f, 0f, 86, "Boots for abyss chase paths");
        add("sovereign_armor", "Sovereign Armor", SlotType.ARMOR, Rarity.GOLD, 6, 80, 110, 0, 22, 0f, 0f, 0f, 118, "Regal shell against void storms");
        add("voidhelm", "Voidhelm", SlotType.HELMET, Rarity.PURPLE, 6, 61, 52, 0, 14, 0f, 0.08f, 0.18f, 84, "Helm tuned for phase assault");
        add("sovereign_bracers", "Sovereign Bracers", SlotType.BRACERS, Rarity.GOLD, 6, 79, 0, 24, 7, 0.02f, 0.18f, 0.38f, 116, "Elite bracers for final chains");
        add("abyss_titan_belt", "Abyss Titan Belt", SlotType.BELT, Rarity.PURPLE, 6, 63, 80, 0, 16, 0.06f, 0.05f, 0.1f, 88, "Belt holding relic core pressure");
    }

    private void add(String id, String name, SlotType slot, Rarity rarity, int tier, int power, int hp, int attack, int defense, float move, float crit, float critDmg, int sell, String desc) {
        items.add(new ItemDefinition(id, name, slot, rarity, tier, power, hp, attack, defense, move, crit, critDmg, sell, desc));
    }

    public List<ItemDefinition> all() {
        return items;
    }
}
