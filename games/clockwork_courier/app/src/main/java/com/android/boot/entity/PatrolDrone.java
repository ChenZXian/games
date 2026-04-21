package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class PatrolDrone extends GameEntity {
    private final Paint paint;
    private final float range;
    private final float speed;
    private float startX;
    private float dir = 1f;

    public PatrolDrone(float x, float y, float w, float h, float range, float speed, Paint paint) {
        bounds.set(x, y, x + w, y + h);
        startX = x;
        this.range = range;
        this.speed = speed;
        this.paint = paint;
    }

    @Override
    public void update(float dt) {
        bounds.offset(dir * speed * dt, 0);
        if (bounds.left < startX - range || bounds.left > startX + range) {
            dir *= -1f;
        }
    }

    @Override
    public void render(Canvas canvas) {
        canvas.drawRoundRect(bounds, 3, 3, paint);
    }
}
