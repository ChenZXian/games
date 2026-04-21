package com.android.boot.core;

public class EconomySystem {
    public int coins = 200;
    public int xp;
    public int ranchLevel = 1;
    public int tokens;
    public float combo = 1f;
    public float comboTimer;
    public int highestCombo;

    public void addCoins(int value) {
        coins += value;
    }

    public void addXp(int amount) {
        xp += amount;
        while (xp >= levelCap()) {
            xp -= levelCap();
            ranchLevel++;
            tokens += 1;
        }
    }

    public int levelCap() {
        return 50 + ranchLevel * 20;
    }

    public void hitCombo() {
        combo += 0.5f;
        comboTimer = 4f;
        if (combo > highestCombo) {
            highestCombo = (int) combo;
        }
    }

    public void update(float dt, float retentionBonus) {
        comboTimer -= dt;
        if (comboTimer <= 0f) {
            combo -= dt * (1f - retentionBonus);
            if (combo < 1f) {
                combo = 1f;
            }
        }
    }
}
