package com.android.boot.entity;

public class HeavyEnemy extends Enemy {
    public HeavyEnemy(float x, float y) {
        super(x, y);
        hp = 94;
        radius = 34f;
    }

    @Override
    public void update(float dt, Player player) {
        if (dead) {
            return;
        }
        if (hitStun > 0f) {
            hitStun -= dt;
            x += vx * dt;
            vx *= 0.88f;
            return;
        }
        atkCd -= dt;
        float dx = player.x - x;
        float ad = Math.abs(dx);
        if (ad > 88f) {
            vx = dx > 0 ? 80f : -80f;
            x += vx * dt;
        } else {
            vx = 0f;
            if (atkCd <= 0f) {
                atkCd = 1.8f;
                player.tryDamage(18, dx > 0 ? 210f : -210f);
            }
        }
    }

    @Override
    public void takeHit(int dmg, float knock) {
        super.takeHit((int) (dmg * 0.9f), knock * 0.55f);
        hitStun *= 0.65f;
    }
}
