package com.android.boot.entity;

public class Orb {
    public enum Type {
        SHARD,
        CORE,
        RUNE
    }

    public float x;
    public float y;
    public float radius;
    public boolean active;
    public Type type;

    public void reset(float x, float y, float radius, Type type) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.type = type;
        this.active = true;
    }
}
