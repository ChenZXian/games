package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Particle {
    private float x;
    private float y;
    private final float vx;
    private final float vy;
    private float life;
    private final float size;
    private final int color;

    public Particle(float x, float y, float vx, float vy, int color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = 0.6f;
        this.size = 6f + (float) (Math.random() * 6f);
    }

    public void update(float dt) {
        life -= dt;
        x += vx * dt;
        y += vy * dt;
    }

    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAlpha((int) (Math.max(0f, life / 0.6f) * 255f));
        canvas.drawCircle(x, y, size, paint);
    }

    public boolean isDead() {
        return life <= 0f;
    }
}
