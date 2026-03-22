package com.android.boot.fx;

public class FloatingText {
    public float x;
    public float y;
    public float vy;
    public float life;
    public float maxLife;
    public String text;
    public int color;
    public boolean active;

    public void init(float x, float y, String text, int color) {
        this.x = x;
        this.y = y;
        this.vy = -22f;
        this.life = 1.2f;
        this.maxLife = 1.2f;
        this.text = text;
        this.color = color;
        this.active = true;
    }
}
