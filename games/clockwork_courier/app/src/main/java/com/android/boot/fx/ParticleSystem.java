package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ParticleSystem {
    private static final int MAX = 48;
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final float[] life = new float[MAX];
    private final Paint paint;

    public ParticleSystem(Paint paint) {
        this.paint = paint;
    }

    public void burst(float px, float py, int count, float speed) {
        for (int i = 0; i < MAX && count > 0; i++) {
            if (life[i] <= 0f) {
                float a = (float) (Math.PI * 2 * (i % 12) / 12f);
                x[i] = px;
                y[i] = py;
                vx[i] = (float) Math.cos(a) * speed;
                vy[i] = (float) Math.sin(a) * speed;
                life[i] = 0.35f;
                count--;
            }
        }
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] > 0f) {
                life[i] -= dt;
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
            }
        }
    }

    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        for (int i = 0; i < MAX; i++) {
            if (life[i] > 0f) {
                paint.setAlpha((int) (Math.max(0f, life[i]) * 600f));
                canvas.drawCircle(x[i], y[i], 1.6f, paint);
            }
        }
        paint.setAlpha(prev);
    }
}
