package com.android.boot.entity;

public class Player {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float speed;
    public float radius;
    public float hp;
    public float maxHp;
    public float energy;
    public float maxEnergy;
    public float regen;
    public float damage;
    public float fireRate;
    public int pierce;
    public int multishot;
    public float knockback;
    public float magnetRange;
    public float shield;
    public float skillCooldown;
    public float skillDamage;

    public void reset(float startX, float startY) {
        x = startX;
        y = startY;
        vx = 0f;
        vy = 0f;
        speed = 260f;
        radius = 22f;
        maxHp = 100f;
        hp = maxHp;
        maxEnergy = 100f;
        energy = maxEnergy * 0.4f;
        regen = 0.6f;
        damage = 12f;
        fireRate = 6f;
        pierce = 0;
        multishot = 0;
        knockback = 40f;
        magnetRange = 120f;
        shield = 0f;
        skillCooldown = 9f;
        skillDamage = 40f;
    }
}
