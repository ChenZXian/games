package com.android.boot.entity;

public class Bullet {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float radius;
    public float damage;
    public float life;

    public Bullet(float x, float y, float vx, float vy, float damage) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        radius = 4f;
        life = 2.2f;
    }
}
