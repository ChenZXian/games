package com.android.boot.entity;

public class Projectile {
    public float x;
    public float y;
    public float vx;
    public float damage;
    public Team team;
    public boolean magical;
    public boolean active;

    public void launch(float x, float y, float vx, float damage, Team team, boolean magical) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.damage = damage;
        this.team = team;
        this.magical = magical;
        active = true;
    }

    public void update(float dt) {
        x += vx * dt;
    }
}
