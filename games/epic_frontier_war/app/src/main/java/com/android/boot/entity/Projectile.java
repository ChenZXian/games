package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class Projectile {
    public Team team;
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float damage;
    public float life;
    public boolean heal;
    public boolean active;
    public int color;

    public void launch(Team team, float x, float y, float vx, float vy, float damage, float life, boolean heal, int color) {
        this.team = team;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.life = life;
        this.heal = heal;
        this.active = true;
        this.color = color;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        x += vx * dt;
        y += vy * dt;
        vy += 160f * dt;
        life -= dt;
        if (life <= 0f) {
            active = false;
        }
    }

    public void render(Canvas canvas, Paint paint) {
        if (!active) {
            return;
        }
        paint.setColor(color);
        Path path = new Path();
        path.moveTo(x - 14f, y - 4f);
        path.lineTo(x + 10f, y);
        path.lineTo(x - 14f, y + 4f);
        path.close();
        canvas.drawPath(path, paint);
    }
}
