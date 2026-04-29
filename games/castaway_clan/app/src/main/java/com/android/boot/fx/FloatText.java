package com.android.boot.fx;

public class FloatText {
    public String text = "";
    public float x;
    public float y;
    public int color;
    public float life;

    public void set(String text, float x, float y, int color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.life = 1.5f;
    }
}
