package com.android.boot.fx;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.Iterator;

public class EffectManager {
    private final ArrayList<float[]> pops = new ArrayList<>();

    public void pop(float x, float y) {
        pops.add(new float[]{x, y, 1f});
    }

    public void update(float delta) {
        Iterator<float[]> iterator = pops.iterator();
        while (iterator.hasNext()) {
            float[] item = iterator.next();
            item[1] -= delta * 50f;
            item[2] -= delta;
            if (item[2] <= 0f) iterator.remove();
        }
    }

    public void draw(Canvas canvas, Paint paint, int color) {
        paint.setColor(color);
        paint.setTextSize(28f);
        for (float[] item : pops) {
            paint.setAlpha((int)(255f * item[2]));
            canvas.drawText("Great", item[0], item[1], paint);
            paint.setAlpha(255);
        }
    }
}
