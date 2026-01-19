package com.android.boot.fx;

public class FloatText {
    public float x;
    public float y;
    public float alpha;
    public String text;
    public boolean active = true;

    public FloatText(float x, float y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.alpha = 1f;
    }
}
