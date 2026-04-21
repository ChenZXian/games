package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;

public class FloatingTextSystem {
    private static final int MAX = 16;
    private final String[] text = new String[MAX];
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] life = new float[MAX];
    private final Paint paint;

    public FloatingTextSystem(Paint paint) {
        this.paint = paint;
    }

    public void push(String value, float px, float py) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0f) {
                text[i] = value;
                x[i] = px;
                y[i] = py;
                life[i] = 0.9f;
                return;
            }
        }
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] > 0f) {
                life[i] -= dt;
                y[i] -= dt * 20f;
            }
        }
    }

    public void render(Canvas canvas) {
        int prev = paint.getAlpha();
        for (int i = 0; i < MAX; i++) {
            if (life[i] > 0f) {
                paint.setAlpha((int) (life[i] * 255f));
                canvas.drawText(text[i], x[i], y[i], paint);
            }
        }
        paint.setAlpha(prev);
    }
}
