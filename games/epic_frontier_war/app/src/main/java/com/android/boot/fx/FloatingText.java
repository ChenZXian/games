package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class FloatingText {
    public String text = "";
    public float x;
    public float y;
    public float life;
    public boolean active;
    public int color;

    public void show(String text, float x, float y, int color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.life = 1.1f;
        this.color = color;
        this.active = true;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        y -= 28f * dt;
        life -= dt;
        if (life <= 0f) {
            active = false;
        }
    }

    public void render(Canvas canvas, Paint paint) {
        if (!active) {
            return;
        }
        paint.setColor(color);
        paint.setAlpha((int) (Math.max(0f, Math.min(1f, life)) * 255f));
        canvas.drawText(text, x, y, paint);
        paint.setAlpha(255);
    }
}
