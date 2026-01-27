package com.android.boot.entity;

public class Enemy {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float speed;
    public float radius;
    public float hp;
    public float maxHp;
    public boolean elite;
    public boolean fast;
    public boolean tanky;
    public boolean regen;
    public boolean explode;
    public float hitCooldown;

    public void init(float startX, float startY, float baseSpeed, float baseHp, boolean isElite, int modifier) {
        x = startX;
        y = startY;
        vx = 0f;
        vy = 0f;
        radius = isElite ? 28f : 22f;
        elite = isElite;
        fast = false;
        tanky = false;
        regen = false;
        explode = false;
        if (isElite) {
            if (modifier == 0) {
                fast = true;
            } else if (modifier == 1) {
                tanky = true;
            } else if (modifier == 2) {
                regen = true;
            } else {
                explode = true;
            }
        }
        speed = baseSpeed * (fast ? 1.45f : 1f);
        maxHp = baseHp * (tanky ? 2.2f : 1f);
        hp = maxHp;
        hitCooldown = 0f;
    }
}
