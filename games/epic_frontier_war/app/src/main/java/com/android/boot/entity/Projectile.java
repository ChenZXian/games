package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Projectile {
    public final Team team;
    public float x;
    public float y;
    public float vx;
    public float damage;
    public float radius;
    public boolean lightning;
    public boolean active = true;

    public Projectile(Team team, float x, float y, float vx, float damage, float radius, boolean lightning) {
        this.team = team;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.damage = damage;
        this.radius = radius;
        this.lightning = lightning;
    }

    public void update(float dt) {
        x += vx * dt;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) {
            return;
        }
        if (lightning) {
            canvas.drawRect(x - radius, y - radius * 1.6f, x + radius, y + radius * 1.6f, paint);
        } else {
            canvas.drawOval(x - radius, y - radius * 0.55f, x + radius, y + radius * 0.55f, paint);
        }
    }
}
