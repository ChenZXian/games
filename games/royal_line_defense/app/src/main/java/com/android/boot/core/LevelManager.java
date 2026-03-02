package com.android.boot.core;

import com.android.boot.entity.Enemy;
import java.util.ArrayList;
import java.util.List;

public class LevelManager {
    public static class LevelData {
        public String name;
        public List<float[]> path = new ArrayList<>();
        public List<WaveScript> waves = new ArrayList<>();
    }

    private final List<LevelData> levels = new ArrayList<>();

    public LevelManager() {
        levels.add(createLevel1());
        levels.add(createLevel2());
        levels.add(createLevel3());
        levels.add(createLevel4());
        levels.add(createLevel5());
    }

    public LevelData get(int index) {
        return levels.get(index);
    }

    private WaveScript wave(Enemy.Type t1, int c1, float i1, Enemy.Type t2, int c2, float i2) {
        WaveScript w = new WaveScript();
        w.entries.add(new WaveScript.Entry(t1, c1, i1));
        w.entries.add(new WaveScript.Entry(t2, c2, i2));
        return w;
    }

    private LevelData createLevel1() {
        LevelData l = new LevelData();
        l.name = "Meadow Gate";
        l.path.add(p(40, 120)); l.path.add(p(220, 120)); l.path.add(p(320, 180)); l.path.add(p(420, 260)); l.path.add(p(760, 260));
        l.waves.add(wave(Enemy.Type.GRUNT, 8, 0.8f, Enemy.Type.RUNNER, 4, 1.1f));
        l.waves.add(wave(Enemy.Type.ARMOR, 6, 1.1f, Enemy.Type.GRUNT, 12, 0.6f));
        l.waves.add(wave(Enemy.Type.BOSS, 1, 2.0f, Enemy.Type.RUNNER, 10, 0.5f));
        return l;
    }

    private LevelData createLevel2() {
        LevelData l = new LevelData();
        l.name = "Canyon Fork";
        l.path.add(p(40, 300)); l.path.add(p(200, 300)); l.path.add(p(300, 220)); l.path.add(p(420, 320)); l.path.add(p(540, 240)); l.path.add(p(760, 240));
        l.waves.add(wave(Enemy.Type.GRUNT, 10, 0.65f, Enemy.Type.RUNNER, 8, 0.8f));
        l.waves.add(wave(Enemy.Type.ARMOR, 8, 1.0f, Enemy.Type.FLYER, 6, 0.9f));
        l.waves.add(wave(Enemy.Type.BOSS, 1, 2.0f, Enemy.Type.FLYER, 12, 0.45f));
        return l;
    }

    private LevelData createLevel3() {
        LevelData l = new LevelData();
        l.name = "Swamp Spiral";
        l.path.add(p(60, 90)); l.path.add(p(720, 90)); l.path.add(p(720, 420)); l.path.add(p(160, 420)); l.path.add(p(160, 180)); l.path.add(p(620, 180)); l.path.add(p(620, 320)); l.path.add(p(760, 320));
        l.waves.add(wave(Enemy.Type.HEALER, 5, 1.2f, Enemy.Type.GRUNT, 12, 0.7f));
        l.waves.add(wave(Enemy.Type.HEALER, 8, 1.0f, Enemy.Type.ARMOR, 10, 0.9f));
        l.waves.add(wave(Enemy.Type.BOSS, 1, 2.0f, Enemy.Type.HEALER, 12, 0.7f));
        return l;
    }

    private LevelData createLevel4() {
        LevelData l = new LevelData();
        l.name = "Frost Bridge";
        l.path.add(p(20, 220)); l.path.add(p(200, 220)); l.path.add(p(300, 140)); l.path.add(p(520, 140)); l.path.add(p(640, 300)); l.path.add(p(760, 300));
        l.waves.add(wave(Enemy.Type.FLYER, 12, 0.5f, Enemy.Type.RUNNER, 8, 0.7f));
        l.waves.add(wave(Enemy.Type.FLYER, 16, 0.45f, Enemy.Type.ARMOR, 8, 1.0f));
        l.waves.add(wave(Enemy.Type.BOSS, 1, 2.0f, Enemy.Type.FLYER, 16, 0.4f));
        return l;
    }

    private LevelData createLevel5() {
        LevelData l = new LevelData();
        l.name = "Volcano Throne";
        l.path.add(p(40, 80)); l.path.add(p(260, 80)); l.path.add(p(260, 340)); l.path.add(p(520, 340)); l.path.add(p(520, 120)); l.path.add(p(760, 120));
        l.waves.add(wave(Enemy.Type.ARMOR, 12, 0.9f, Enemy.Type.RUNNER, 14, 0.45f));
        l.waves.add(wave(Enemy.Type.HEALER, 12, 0.75f, Enemy.Type.FLYER, 14, 0.55f));
        l.waves.add(wave(Enemy.Type.BOSS, 2, 4.0f, Enemy.Type.ARMOR, 18, 0.7f));
        return l;
    }

    private float[] p(float x, float y) {
        return new float[]{x, y};
    }
}
