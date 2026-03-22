package com.android.boot.fx;

public class Particle {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float size;
    public int color;
    public float life;
    public boolean active;

    public void set(float x, float y, float vx, float vy, float size, int color, float life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.color = color;
        this.life = life;
        this.active = true;
    }

    public void update(float dt) {
        x += vx * dt;
        y += vy * dt;
        life -= dt;
        vy += 120f * dt;
        if (life <= 0f) {
            active = false;
        }
    }
}
