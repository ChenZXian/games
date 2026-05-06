package com.android.boot.entity;

public class Player {
    public float x = 120f;
    public float y = 420f;
    public float vx;
    public float vy;
    public boolean onGround = true;
    public int maxHp = 160;
    public int hp = 160;
    public float invul;
    public float skillCd;
    public int comboStep;
    public float comboTimer;
    public float attackTimer;
    public int atkPowerBonus;
    public int hpBonus;
    public float cdBonus;

    public void resetRun() {
        x = 120f;
        y = 420f;
        vx = 0f;
        vy = 0f;
        hp = maxHp + hpBonus;
        invul = 0f;
        skillCd = 0f;
        comboStep = 0;
        comboTimer = 0f;
        attackTimer = 0f;
    }

    public void update(float dt, boolean left, boolean right, boolean jump) {
        invul -= dt;
        skillCd -= dt;
        comboTimer -= dt;
        attackTimer -= dt;
        float target = 0f;
        if (left) {
            target = -250f;
        }
        if (right) {
            target = 250f;
        }
        vx += (target - vx) * Math.min(1f, dt * 12f);
        if (attackTimer > 0f) {
            vx *= 0.9f;
        }
        if (jump && onGround) {
            vy = -520f;
            onGround = false;
        }
        vy += 1120f * dt;
        x += vx * dt;
        y += vy * dt;
        if (y >= 420f) {
            y = 420f;
            vy = 0f;
            onGround = true;
        }
    }

    public boolean tryDamage(int dmg, float knock) {
        if (invul > 0f) {
            return false;
        }
        hp -= dmg;
        vx += knock;
        invul = 0.45f;
        return true;
    }

    public void applyReward(int type) {
        if (type == 0) {
            hpBonus += 24;
        } else if (type == 1) {
            atkPowerBonus += 4;
        } else {
            cdBonus += 0.35f;
        }
    }
}
