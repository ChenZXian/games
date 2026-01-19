package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class FloatingText {
    private final String text;
    private float x;
    private float y;
    private float life;
    private final int color;

    public FloatingText(float x, float y, String text, int color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.life = 0.8f;
    }

    public void update(float dt) {
        life -= dt;
        y -= dt * 40f;
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        paint.setAlpha((int) (Math.max(0f, life / 0.8f) * 255f));
        paint.setTextSize(28f);
        canvas.drawText(text, x, y, paint);
    }

    public boolean isDead() {
        return life <= 0f;
    }
}
