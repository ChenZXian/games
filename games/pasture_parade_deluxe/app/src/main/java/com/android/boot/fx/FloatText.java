package com.android.boot.fx;

public class FloatText {
    public String text;
    public float x;
    public float y;
    public float time;
    public int color;

    public void set(String text, float x, float y, int color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.time = 1f;
        this.color = color;
    }
}
