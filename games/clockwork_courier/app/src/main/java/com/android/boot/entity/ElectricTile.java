package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ElectricTile extends GameEntity {
    private final Paint paint;
    private final float cycle;
    private final float onTime;
    private float t;

    public ElectricTile(float x, float y, float w, float h, float cycle, float onTime, Paint paint) {
        bounds.set(x, y, x + w, y + h);
        this.cycle = cycle;
        this.onTime = onTime;
        this.paint = paint;
    }

    public boolean isOn() {
        return (t % cycle) <= onTime;
    }

    @Override
    public void update(float dt) {
        t += dt;
    }

    @Override
    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        paint.setAlpha(isOn() ? 210 : 70);
        canvas.drawRoundRect(bounds, 2, 2, paint);
        paint.setAlpha(prev);
    }
}
