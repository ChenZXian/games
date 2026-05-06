package com.android.boot.data;

import com.android.boot.entity.EnemyTemplate;
import com.android.boot.entity.LevelConfig;
import com.android.boot.entity.PointF2;
import com.android.boot.entity.TowerSlot;
import com.android.boot.entity.Wave;

public class GameDatabase {
    public static LevelConfig createLevel(int index) {
        int safe = Math.max(1, Math.min(3, index));
        if (safe == 1) return createLevelOne();
        if (safe == 2) return createLevelTwo();
        return createLevelThree();
    }

    public static LevelConfig createLevelOne() {
        LevelConfig level = new LevelConfig();
        level.id = "sunbreak_pass";
        level.name = "Sunbreak Pass";
        level.startGold = 240;
        level.startLife = 20;

        level.path.add(new PointF2(100f, 420f));
        level.path.add(new PointF2(250f, 420f));
        level.path.add(new PointF2(250f, 200f));
        level.path.add(new PointF2(520f, 200f));
        level.path.add(new PointF2(520f, 520f));
        level.path.add(new PointF2(820f, 520f));
        level.path.add(new PointF2(820f, 260f));
        level.path.add(new PointF2(1120f, 260f));
        level.path.add(new PointF2(1220f, 360f));

        level.slots.add(new TowerSlot(0, 190f, 300f));
        level.slots.add(new TowerSlot(1, 370f, 118f));
        level.slots.add(new TowerSlot(2, 430f, 360f));
        level.slots.add(new TowerSlot(3, 610f, 120f));
        level.slots.add(new TowerSlot(4, 720f, 420f));
        level.slots.add(new TowerSlot(5, 940f, 150f));
        level.slots.add(new TowerSlot(6, 940f, 560f));
        level.slots.add(new TowerSlot(7, 1130f, 420f));

        level.enemies.put("runner", new EnemyTemplate("runner", "Ash Runner", 60f, 84f, 5f, 5f, 8, false, 1, false, null, 0, false));
        level.enemies.put("raider", new EnemyTemplate("raider", "Cinder Raider", 110f, 54f, 10f, 5f, 12, false, 2, false, null, 0, false));
        level.enemies.put("shell", new EnemyTemplate("shell", "Basalt Shell", 270f, 34f, 60f, 10f, 18, false, 4, false, null, 0, false));
        level.enemies.put("wisp", new EnemyTemplate("wisp", "Smoke Wisp", 90f, 82f, 0f, 55f, 14, true, 3, false, null, 0, false));
        level.enemies.put("nest", new EnemyTemplate("nest", "Spore Nest", 160f, 36f, 10f, 15f, 16, false, 3, true, "runner", 2, false));
        level.enemies.put("warden", new EnemyTemplate("warden", "Ash Warden", 1200f, 28f, 40f, 35f, 120, false, 10, false, null, 0, true));

        level.waves.add(new Wave("Scouts", 3f, false).add("runner", 8, 0.8f).add("raider", 3, 1.2f));
        level.waves.add(new Wave("Heavy Line", 7f, false).add("raider", 6, 0.9f).add("shell", 4, 1.1f));
        level.waves.add(new Wave("Mist Flight", 7f, false).add("wisp", 7, 0.8f).add("runner", 5, 0.6f));
        level.waves.add(new Wave("Spores", 8f, false).add("nest", 5, 1.5f).add("shell", 3, 1.2f).add("wisp", 4, 0.8f));
        level.waves.add(new Wave("Ash Warden", 10f, true).add("warden", 1, 0.5f).add("raider", 6, 0.8f).add("wisp", 4, 0.9f));

        return level;
    }

    public static LevelConfig createLevelTwo() {
        LevelConfig level = createLevelOne();
        level.id = "ember_valley";
        level.name = "Ember Valley";
        level.startGold = 260;
        level.startLife = 18;
        scaleEnemies(level, 1.28f, 1.08f, 1.20f);
        for (Wave wave : level.waves) {
            wave.prepareTime = Math.max(2.2f, wave.prepareTime - 0.8f);
        }
        level.waves.add(new Wave("Crimson Vanguard", 8f, true)
                .add("shell", 4, 1.0f)
                .add("raider", 8, 0.7f)
                .add("warden", 1, 0.6f));
        return level;
    }

    public static LevelConfig createLevelThree() {
        LevelConfig level = createLevelOne();
        level.id = "obsidian_front";
        level.name = "Obsidian Front";
        level.startGold = 280;
        level.startLife = 16;
        scaleEnemies(level, 1.55f, 1.15f, 1.38f);
        for (Wave wave : level.waves) {
            wave.prepareTime = Math.max(1.6f, wave.prepareTime - 1.2f);
        }
        level.waves.add(new Wave("Abyss Surge", 7f, false)
                .add("wisp", 10, 0.6f)
                .add("nest", 6, 1.0f)
                .add("shell", 5, 0.9f));
        level.waves.add(new Wave("Abyss Sovereign", 10f, true)
                .add("warden", 1, 0.5f)
                .add("raider", 10, 0.6f)
                .add("wisp", 8, 0.6f));
        return level;
    }

    private static void scaleEnemies(LevelConfig level, float hpMul, float speedMul, float rewardMul) {
        for (EnemyTemplate template : level.enemies.values()) {
            template.maxHp *= hpMul;
            template.speed *= speedMul;
            template.reward = Math.max(1, Math.round(template.reward * rewardMul));
        }
    }
}
