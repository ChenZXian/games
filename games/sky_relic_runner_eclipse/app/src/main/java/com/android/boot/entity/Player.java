package com.android.boot.entity;

public class Player {
    public float x;
    public float y;
    public float width;
    public float height;
    public float velocityY;
    public boolean grounded;
    public float dashTime;
    public float dashCooldown;
    public float jumpHoldTime;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
