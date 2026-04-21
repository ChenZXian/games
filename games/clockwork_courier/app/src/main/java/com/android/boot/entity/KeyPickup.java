package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class KeyPickup extends GameEntity {
    private final Paint paint;

    public KeyPickup(float x, float y, Paint paint) {
        bounds.set(x - 3, y - 3, x + 3, y + 3);
        this.paint = paint;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(Canvas canvas) {
        if (active) canvas.drawOval(bounds, paint);
    }
}
