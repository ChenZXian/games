package com.android.boot.fx;

public class SlashEffect {
    public float x;
    public float y;
    public float width;
    public float life;

    public void trigger(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
        life = 0.15f;
    }

    public void update(float dt) {
        life -= dt;
    }
}
