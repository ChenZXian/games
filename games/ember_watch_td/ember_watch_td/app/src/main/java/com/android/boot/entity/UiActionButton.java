package com.android.boot.entity;

import android.graphics.RectF;

public class UiActionButton {
    public String id;
    public String text;
    public RectF rect = new RectF();
    public boolean enabled = true;

    public UiActionButton(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public boolean hit(float x, float y) {
        return enabled && rect.contains(x, y);
    }
}
