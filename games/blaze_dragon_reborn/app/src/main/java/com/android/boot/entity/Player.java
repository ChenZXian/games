package com.android.boot.entity;

public class Player {
    public float x = 180f;
    public float y = 0f;
    public float vx;
    public float vy;
    public boolean onGround = true;
    public boolean facingRight = true;
    public int maxHp = 220;
    public int hp = 220;
    public float chi = 30f;
    public float maxChi = 100f;
    public float attackTimer;
    public float comboWindow;
    public int comboStep;
    public int comboCount;
    public float comboLife;
    public float invul;
    public float chiCooldown;
    public float flashLife;
    public float dodgeTimer;
    public float dodgeCooldown;
    public boolean queuedAttack;
    public float attackGlow;

    public void reset(float floorY) {
        x = 180f;
        y = floorY;
        vx = 0f;
        vy = 0f;
        onGround = true;
        facingRight = true;
        hp = maxHp;
        chi = 30f;
        attackTimer = 0f;
        comboWindow = 0f;
        comboStep = 0;
        comboCount = 0;
        comboLife = 0f;
        invul = 0f;
        chiCooldown = 0f;
        flashLife = 0f;
        dodgeTimer = 0f;
        dodgeCooldown = 0f;
        queuedAttack = false;
        attackGlow = 0f;
    }

    public void update(float dt, boolean left, boolean right, boolean jump, float floorY) {
        invul = Math.max(0f, invul - dt);
        attackTimer = Math.max(0f, attackTimer - dt);
        comboWindow -= dt;
        comboLife -= dt;
        chiCooldown = Math.max(0f, chiCooldown - dt);
        flashLife = Math.max(0f, flashLife - dt);
        dodgeTimer = Math.max(0f, dodgeTimer - dt);
        dodgeCooldown = Math.max(0f, dodgeCooldown - dt);
        attackGlow = Math.max(0f, attackGlow - dt);
        if (comboWindow <= 0f) {
            comboStep = 0;
            queuedAttack = false;
        }
        if (comboLife <= 0f) {
            comboCount = 0;
        }
        float target = 0f;
        if (left && !right) {
            target = -290f;
            facingRight = false;
        } else if (right && !left) {
            target = 290f;
            facingRight = true;
        }
        float moveBlend = dodgeTimer > 0f ? 4f : 14f;
        vx += (target - vx) * Math.min(1f, dt * moveBlend);
        if (attackTimer > 0f) {
            vx *= dodgeTimer > 0f ? 0.96f : 0.88f;
        }
        if (jump && onGround) {
            vy = -560f;
            onGround = false;
        }
        vy += 1400f * dt;
        x += vx * dt;
        y += vy * dt;
        if (y >= floorY) {
            y = floorY;
            vy = 0f;
            onGround = true;
        }
        chi = Math.min(maxChi, chi + dt * 3.2f);
    }

    public boolean takeDamage(int damage, float knock) {
        if (invul > 0f) {
            return false;
        }
        hp -= damage;
        vx += knock;
        invul = 0.55f;
        flashLife = 0.18f;
        comboCount = 0;
        comboLife = 0f;
        queuedAttack = false;
        return true;
    }
}
