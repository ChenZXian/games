package com.android.boot.render;

public class PerspectiveProjector {
    private int width;
    private int height;
    private float horizon;

    public void setViewport(int width, int height) {
        this.width = width;
        this.height = height;
        this.horizon = height * 0.28f;
    }

    public float laneX(int lane, float z) {
        float t = depthToScale(z);
        float center = width * 0.5f;
        float spread = width * 0.27f * t;
        return center + (lane - 1) * spread;
    }

    public float yFromDepth(float z) {
        float t = depthToScale(z);
        return horizon + (height * 0.72f) * t;
    }

    public float depthToScale(float z) {
        float clamped = Math.max(1f, Math.min(120f, z));
        return 1f - ((clamped - 1f) / 119f);
    }

    public float getHorizon() {
        return horizon;
    }
}
