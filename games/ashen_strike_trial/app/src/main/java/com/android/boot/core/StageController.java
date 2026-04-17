package com.android.boot.core;

public class StageController {
    public int wave = 0;
    public boolean gateLocked;
    public float gateStart = 550f;
    public float gateEnd = 880f;
    public float bossStart = 1450f;
    public boolean bossArena;
    public boolean bossSpawned;

    public void reset() {
        wave = 0;
        gateLocked = false;
        bossArena = false;
        bossSpawned = false;
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
        }
    }
}
