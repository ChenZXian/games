package com.android.boot.entity;

public class Projectile {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float speed;
    public float radius;
    public float damage;
    public DamageType damageType;
    public Enemy target;
    public boolean active = true;
    public boolean splash;
    public float splashRadius;
    public float slowFactor = 1f;
    public float slowDuration;
    public float chainRadius;
    public int chainCount;

    public Projectile(float x, float y, float speed, float radius, float damage, DamageType damageType, Enemy target) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.radius = radius;
        this.damage = damage;
        this.damageType = damageType;
        this.target = target;
    }
}
