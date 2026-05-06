package com.android.boot.fx;

public class CoinPopup {
    public float x;
    public float y;
    public float vy;
    public float time;

    public void spawn(float x, float y) {
        this.x = x;
        this.y = y;
        this.vy = -42f;
        this.time = 0.8f;
    }

    public void update(float dt) {
        if (time <= 0f) return;
        time -= dt;
        y += vy * dt;
    }
}

