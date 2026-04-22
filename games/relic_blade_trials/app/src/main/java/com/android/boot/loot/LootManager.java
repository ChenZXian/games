package com.android.boot.loot;

import com.android.boot.model.Enums.Rarity;
import com.android.boot.model.ItemDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootManager {
    private final ItemDatabase db;
    private final Random random = new Random();

    public LootManager(ItemDatabase db) {
        this.db = db;
    }

    public DropTable tableForStage(int stage) {
        switch (stage) {
            case 1: return new DropTable(0.16f, 0.02f, 0.92f, 0.08f, 0f);
            case 2: return new DropTable(0.15f, 0.03f, 0.88f, 0.12f, 0f);
            case 3: return new DropTable(0.14f, 0.04f, 0.78f, 0.22f, 0f);
            case 4: return new DropTable(0.13f, 0.04f, 0.68f, 0.30f, 0.02f);
            case 5: return new DropTable(0.12f, 0.05f, 0.58f, 0.36f, 0.06f);
            default: return new DropTable(0.11f, 0.06f, 0.45f, 0.43f, 0.12f);
        }
    }

    public ItemDefinition rollNormalDrop(int stage) {
        DropTable t = tableForStage(stage);
        float r = random.nextFloat();
        if (r < t.normalGreen) {
            return pick(stage, Rarity.GREEN, false);
        }
        if (r < t.normalGreen + t.normalWhite) {
            return pick(stage, Rarity.WHITE, false);
        }
        return null;
    }

    public ItemDefinition rollBossGuaranteed(int stage) {
        DropTable t = tableForStage(stage);
        float r = random.nextFloat();
        if (r < t.bossGold) {
            return pick(stage, Rarity.GOLD, true);
        }
        if (r < t.bossGold + t.bossPurple) {
            return pick(stage, Rarity.PURPLE, true);
        }
        return pick(stage, Rarity.GREEN, true);
    }

    private ItemDefinition pick(int stage, Rarity rarity, boolean bossOnlyHigh) {
        List<ItemDefinition> pool = new ArrayList<>();
        for (ItemDefinition item : db.all()) {
            if (item.stageTier <= stage && item.rarity == rarity) {
                if (!bossOnlyHigh || rarity == Rarity.WHITE || rarity == Rarity.GREEN || item.stageTier >= Math.max(3, stage - 1)) {
                    pool.add(item);
                }
            }
        }
        if (pool.isEmpty()) {
            for (ItemDefinition item : db.all()) {
                if (item.rarity == Rarity.GREEN) {
                    pool.add(item);
                }
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
