package com.android.boot.entity;

public class FloatingText {
    public float x;
    public float y;
    public String text;
    public float life;

    public void set(float x, float y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.life = 1f;
    }
}
