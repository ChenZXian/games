package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ShieldZone extends GameEntity {
    private final Paint paint;

    public ShieldZone(float x, float y, float w, float h, Paint paint) {
        bounds.set(x, y, x + w, y + h);
        this.paint = paint;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        paint.setAlpha(90);
        canvas.drawRoundRect(bounds, 8, 8, paint);
        paint.setAlpha(prev);
    }
}
