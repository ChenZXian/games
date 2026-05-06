package com.android.boot.core;

public class StageController {
    public int level = 1;
    public int wave = 0;
    public boolean gateLocked;
    public float gateStart = 550f;
    public float gateEnd = 880f;
    public float bossStart = 1450f;
    public boolean bossArena;
    public boolean bossSpawned;
    public float stageWidth = 2100f;
    public boolean exitActive;
    public float exitX;

    public void reset() {
        level = 1;
        resetForLevel();
    }

    public void resetForLevel() {
        wave = 0;
        gateLocked = false;
        bossArena = false;
        bossSpawned = false;
        exitActive = false;

        // Level tuning: keep simple but distinct
        if (level == 1) {
            gateStart = 550f;
            gateEnd = 880f;
            bossStart = 1450f;
            stageWidth = 2100f;
        } else if (level == 2) {
            gateStart = 620f;
            gateEnd = 1030f;
            bossStart = 1650f;
            stageWidth = 2400f;
        } else if (level == 3) {
            gateStart = 700f;
            gateEnd = 1180f;
            bossStart = 1880f;
            stageWidth = 2700f;
        } else {
            // Endless-ish scaling
            gateStart = 720f + (level - 3) * 40f;
            gateEnd = 1200f + (level - 3) * 60f;
            bossStart = 1900f + (level - 3) * 80f;
            stageWidth = bossStart + 650f;
        }

        exitX = stageWidth - 180f;
    }

    public void update(float playerX, int remainingEnemies) {
        if (wave == 0 && playerX > gateStart) {
            wave = 1;
            gateLocked = true;
        }
        if (wave == 1 && remainingEnemies == 0) {
            wave = 2;
            gateLocked = false;
        }
        if (wave == 2 && playerX > gateEnd) {
            wave = 3;
            gateLocked = true;
        }
        if (wave == 3 && remainingEnemies == 0) {
            wave = 4;
            gateLocked = false;
        }
        if (wave == 4 && playerX > bossStart) {
            wave = 5;
            gateLocked = true;
            bossArena = true;
        }
        if (wave == 5 && remainingEnemies == 0) {
            wave = 6;
            gateLocked = false;
            exitActive = true;
        }
    }

    public boolean isClear() {
        return wave >= 6;
    }

    public void nextLevel() {
        level += 1;
        resetForLevel();
    }
}
