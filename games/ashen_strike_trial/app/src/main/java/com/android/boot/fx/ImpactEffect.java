package com.android.boot.fx;

public class ImpactEffect {
    public float x;
    public float y;
    public float life;
    public float size;

    public void trigger(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
        life = 0.12f;
    }

    public void update(float dt) {
        life -= dt;
    }
}
