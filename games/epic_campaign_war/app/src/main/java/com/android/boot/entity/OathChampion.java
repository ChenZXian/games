package com.android.boot.entity;

public class OathChampion extends Unit {
    private float retreatTimer;

    public OathChampion(float x, float y) {
        super("Oath Champion", Team.ALLY, false, false, false, true, x, y, 44f, 240f, 30f, 58f, 58f, 1.0f);
    }

    @Override
    public void update(float dt, Unit target, float frontLine) {
        if (hp < maxHp * 0.2f) {
            retreatTimer = 2.8f;
        }
        if (retreatTimer > 0f) {
            retreatTimer -= dt;
            heal(16f * dt);
        }
        super.update(dt, target, frontLine - 10f);
        if (retreatTimer <= 0f && hp < maxHp * 0.75f) {
            heal(8f * dt);
        }
    }
}
