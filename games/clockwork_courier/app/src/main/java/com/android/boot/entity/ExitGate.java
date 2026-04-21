package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ExitGate extends GameEntity {
    private final Paint paint;
    public boolean unlocked = true;

    public ExitGate(float x, float y, Paint paint) {
        bounds.set(x - 5, y - 5, x + 5, y + 5);
        this.paint = paint;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        paint.setAlpha(unlocked ? 255 : 110);
        canvas.drawRoundRect(bounds, 3, 3, paint);
        paint.setAlpha(prev);
    }
}
