package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;

public class CollapseTile extends GameEntity {
    private final Paint paint;
    private final float cycle;
    private final float activeTime;
    private float t;

    public CollapseTile(float x, float y, float w, float h, float cycle, float activeTime, Paint paint) {
        bounds.set(x, y, x + w, y + h);
        this.paint = paint;
        this.cycle = cycle;
        this.activeTime = activeTime;
    }

    public boolean isSolid() {
        return (t % cycle) < activeTime;
    }

    @Override
    public void update(float dt) {
        t += dt;
    }

    @Override
    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        paint.setAlpha(isSolid() ? 190 : 50);
        canvas.drawRect(bounds, paint);
        paint.setAlpha(prev);
    }
}
