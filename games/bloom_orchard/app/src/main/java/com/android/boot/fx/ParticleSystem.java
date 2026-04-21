package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ParticleSystem {
    private final float[] x = new float[32];
    private final float[] y = new float[32];
    private final float[] v = new float[32];
    private int head;

    public void burst(float px, float py) {
        x[head] = px;
        y[head] = py;
        v[head] = 1f;
        head = (head + 1) % x.length;
    }

    public void update(float dt) {
        for (int i = 0; i < x.length; i++) {
            if (v[i] > 0f) {
                v[i] -= dt;
                y[i] -= 20f * dt;
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        for (int i = 0; i < x.length; i++) {
            if (v[i] > 0f) {
                paint.setAlpha((int) (255 * v[i]));
                canvas.drawCircle(x[i], y[i], 6f * v[i], paint);
            }
        }
        paint.setAlpha(255);
    }
}
