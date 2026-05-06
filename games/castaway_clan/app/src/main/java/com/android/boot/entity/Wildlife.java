package com.android.boot.entity;

public class Wildlife {
    public enum Type {
        BOAR,
        MONKEY,
        SNAKE,
        CRAB
    }

    public final Type type;
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float animTime;
    public boolean active;

    public Wildlife(Type type, float x, float y, float vx, float vy) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.active = true;
    }
}
