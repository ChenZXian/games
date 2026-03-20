package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class Particle {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float life;
    public float size;
    public boolean active;
    public int color;
    public boolean ring;

    public void init(float x, float y, float vx, float vy, float life, float size, int color, boolean ring) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.size = size;
        this.color = color;
        this.ring = ring;
        active = true;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        x += vx * dt;
        y += vy * dt;
        vy += 24f * dt;
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
        int alpha = (int) (Math.max(0f, Math.min(1f, life)) * 255f);
        paint.setAlpha(alpha);
        RectF rect = new RectF(x - size, y - size, x + size, y + size);
        if (ring) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, size * 0.24f));
            canvas.drawOval(rect, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            canvas.drawOval(rect, paint);
        }
        paint.setAlpha(255);
    }
}
