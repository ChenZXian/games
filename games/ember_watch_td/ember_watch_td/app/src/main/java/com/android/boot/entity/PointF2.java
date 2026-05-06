package com.android.boot.entity;

public class PointF2 {
    public float x;
    public float y;

    public PointF2() {
    }

    public PointF2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public PointF2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public float dst(PointF2 other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
