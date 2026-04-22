package com.android.boot.model;

public class Player {
    public float x = 120f;
    public float y = 420f;
    public float velocityX;
    public float velocityY;
    public float maxHp = 120f;
    public float hp = 120f;
    public float attack = 12f;
    public float defense = 4f;
    public float moveSpeed = 260f;
    public float critChance = 0.05f;
    public float critDamage = 1.5f;
    public float invulnTimer;
    public int comboIndex;
    public float comboTimer;

    public void update(float dt) {
        if (invulnTimer > 0f) {
            invulnTimer -= dt;
        }
        comboTimer -= dt;
        if (comboTimer <= 0f) {
            comboIndex = 0;
        }
        velocityY += 760f * dt;
        x += velocityX * dt;
        y += velocityY * dt;
        if (y > 420f) {
            y = 420f;
            velocityY = 0f;
        }
    }
}
