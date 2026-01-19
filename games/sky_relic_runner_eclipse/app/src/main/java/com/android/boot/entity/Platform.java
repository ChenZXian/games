package com.android.boot.entity;

public class Platform {
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean collapsing;
    public float collapseTimer;
    public boolean active = true;

    public Platform(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
