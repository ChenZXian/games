package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class BadgePickup extends GameEntity {
    private final Paint paint;

    public BadgePickup(float x, float y, Paint paint) {
        bounds.set(x - 2.5f, y - 2.5f, x + 2.5f, y + 2.5f);
        this.paint = paint;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(Canvas canvas) {
        if (active) canvas.drawRoundRect(bounds, 1.8f, 1.8f, paint);
    }
}
