package com.android.boot.fx;

public class FloatingText {
    public float x;
    public float y;
    public String text;
    public int color;
    public float life;
    public boolean active;

    public void set(float x, float y, String text, int color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.life = 1.2f;
        this.active = true;
    }

    public void update(float dt) {
        y -= 40f * dt;
        life -= dt;
        if (life <= 0f) {
            active = false;
        }
    }
}
