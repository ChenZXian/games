package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class RotatingGear extends GameEntity {
    private final Paint paint;
    private float angle;
    private final float radius;

    public RotatingGear(float x, float y, float size, float speed, Paint paint) {
        bounds.set(x, y, x + size, y + size);
        this.angle = speed;
        this.paint = paint;
        this.radius = size * 0.5f;
    }

    @Override
    public void update(float dt) {
        angle += dt * 180f;
    }

    @Override
    public void render(Canvas canvas) {
        float cx = bounds.centerX();
        float cy = bounds.centerY();
        canvas.drawCircle(cx, cy, radius, paint);
    }
}
