package com.android.boot.core;

public final class MathUtil {
    private MathUtil() {
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float vx = x2 - x1;
        float vy = y2 - y1;
        float wx = px - x1;
        float wy = py - y1;
        float c1 = vx * wx + vy * wy;
        if (c1 <= 0f) {
            return dist(px, py, x1, y1);
        }
        float c2 = vx * vx + vy * vy;
        if (c2 <= c1) {
            return dist(px, py, x2, y2);
        }
        float b = c1 / c2;
        float bx = x1 + b * vx;
        float by = y1 + b * vy;
        return dist(px, py, bx, by);
    }
}
