package com.android.boot.entity;

public class FloatingText {
    public boolean active;
    public String text = "";
    public float x;
    public float y;
    public float life;
    public float velocityY;

    public void show(String value, float startX, float startY) {
        active = true;
        text = value;
        x = startX;
        y = startY;
        life = 0.7f;
        velocityY = -60f;
    }
}
