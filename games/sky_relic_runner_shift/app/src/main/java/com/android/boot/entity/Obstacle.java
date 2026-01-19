package com.android.boot.entity;

public class Obstacle {
    public enum Type {
        SPIKE,
        BLOCK,
        PLATFORM
    }

    public float x;
    public float y;
    public float width;
    public float height;
    public float baseY;
    public float moveRange;
    public float moveSpeed;
    public float collapseTimer;
    public boolean active;
    public boolean collapsing;
    public Type type;

    public void reset(float x, float y, float width, float height, Type type) {
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.active = true;
        this.collapsing = false;
        this.collapseTimer = 0f;
        this.moveRange = 0f;
        this.moveSpeed = 0f;
    }

    public boolean isHazard() {
        return type == Type.SPIKE || type == Type.BLOCK;
    }
}
