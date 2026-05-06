package com.android.boot.entity;

public class VisualEffect {
    public final float x;
    public final float y;
    public final float radius;
    public final int color;
    public float life;
    public final String label;

    public VisualEffect(float x, float y, float radius, int color, float life, String label) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.life = life;
        this.label = label;
    }
}
