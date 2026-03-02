package com.android.boot.entity;

public class Hero {
    public float x;
    public float y;
    private float targetX;
    private float targetY;
    public float speed = 220f;

    public void setPosition(float px, float py) {
        x = px;
        y = py;
        targetX = px;
        targetY = py;
    }

    public void moveTo(float tx, float ty) {
        targetX = tx;
        targetY = ty;
    }

    public void update(float dt) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 1f) {
            float step = speed * dt;
            if (step >= dist) {
                x = targetX;
                y = targetY;
            } else {
                x += dx / dist * step;
                y += dy / dist * step;
            }
        }
    }
}
