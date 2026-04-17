package com.android.boot.entity;

public class BossEnemy extends Enemy {
    public float patternTimer;
    public int pattern;

    public BossEnemy(float x, float y) {
        super(x, y);
        hp = 380;
        radius = 52f;
    }

    @Override
    public void update(float dt, Player player) {
        if (dead) {
            return;
        }
        if (hitStun > 0f) {
            hitStun -= dt;
            x += vx * dt;
            vx *= 0.92f;
            return;
        }
        atkCd -= dt;
        patternTimer -= dt;
        if (patternTimer <= 0f) {
            pattern = pattern == 0 ? 1 : 0;
            patternTimer = 2.2f;
        }
        float dx = player.x - x;
        float ad = Math.abs(dx);
        if (pattern == 0) {
            if (ad > 130f) {
                x += (dx > 0 ? 95f : -95f) * dt;
            } else if (atkCd <= 0f) {
                atkCd = 2.1f;
                player.tryDamage(24, dx > 0 ? 260f : -260f);
            }
        } else {
            if (atkCd <= 0f) {
                atkCd = 2.8f;
                if (ad < 260f) {
                    player.tryDamage(16, dx > 0 ? 180f : -180f);
                }
            }
        }
    }

    @Override
    public void takeHit(int dmg, float knock) {
        hp -= dmg;
        vx = knock * 0.35f;
        hitStun = 0.06f;
        if (hp <= 0) {
            dead = true;
        }
    }
}
