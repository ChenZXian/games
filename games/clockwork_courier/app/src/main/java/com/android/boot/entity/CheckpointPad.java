package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class CheckpointPad extends GameEntity {
    private final Paint paint;
    public boolean triggered;

    public CheckpointPad(float x, float y, Paint paint) {
        bounds.set(x - 4, y - 4, x + 4, y + 4);
        this.paint = paint;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        paint.setAlpha(triggered ? 255 : 130);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() * 0.55f, paint);
        paint.setAlpha(prev);
    }
}
