package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class LaserGate extends GameEntity {
    private final Paint paint;
    private final float cycle;
    private float t;

    public LaserGate(float x, float y, float w, float h, float cycle, Paint paint) {
        bounds.set(x, y, x + w, y + h);
        this.cycle = cycle;
        this.paint = paint;
    }

    public boolean isDangerOn() {
        return (t % cycle) < cycle * 0.55f;
    }

    @Override
    public void update(float dt) {
        t += dt;
    }

    @Override
    public void render(Canvas canvas) {
        int alpha = isDangerOn() ? 255 : 100;
        int prev = paint.getAlpha();
        paint.setAlpha(alpha);
        canvas.drawRect(bounds, paint);
        paint.setAlpha(prev);
    }
}
