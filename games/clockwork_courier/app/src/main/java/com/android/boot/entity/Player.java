package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Player extends GameEntity {
    private final Paint paint;
    public float speed = 42f;
    public float health = 100f;
    public float maxHealth = 100f;
    public float energy = 100f;
    public float maxEnergy = 100f;
    public float invuln;
    public float dashTimer;
    private boolean accelerating;
    public boolean hasKey;

    public Player(Paint paint) {
        this.paint = paint;
    }

    public void setPosition(float x, float y) {
        bounds.set(x - 3, y - 3, x + 3, y + 3);
    }

    public void move(float dx, float dy, float dt, float stageW, float stageH) {
        float velocity = speed * (accelerating ? 1.85f : 1f);
        bounds.offset(dx * velocity * dt, dy * velocity * dt);
        if (bounds.left < 0) bounds.offsetTo(0, bounds.top);
        if (bounds.top < 0) bounds.offsetTo(bounds.left, 0);
        if (bounds.right > stageW) bounds.offsetTo(stageW - bounds.width(), bounds.top);
        if (bounds.bottom > stageH) bounds.offsetTo(bounds.left, stageH - bounds.height());
    }

    public void setAccelerating(boolean accelerating) {
        this.accelerating = accelerating && energy > 0f;
    }

    public void damage(float amount) {
        if (invuln > 0) {
            return;
        }
        health = Math.max(0, health - amount);
        invuln = 0.7f;
    }

    @Override
    public void update(float dt) {
        if (invuln > 0) invuln -= dt;
        if (dashTimer > 0) dashTimer -= dt;
        if (accelerating && energy > 0f) {
            energy = Math.max(0f, energy - dt * 36f);
            if (energy <= 0f) accelerating = false;
        } else {
            energy = Math.min(maxEnergy, energy + dt * 16f);
        }
    }

    @Override
    public void render(Canvas canvas) {
        canvas.drawRoundRect(bounds, 3, 3, paint);
    }
}
