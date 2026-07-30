package com.android.boot.fx;

public class ChiBurst {
    public float x;
    public float y;
    public float radius;
    public float life;
    public boolean active;

    public void trigger(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.life = 0.42f;
        this.active = true;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        life -= dt;
        radius += dt * 240f;
        if (life <= 0f) {
            active = false;
        }
    }
}
