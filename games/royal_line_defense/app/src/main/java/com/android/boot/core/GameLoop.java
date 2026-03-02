package com.android.boot.core;

public class GameLoop {
    private float accumulator;

    public float clampDelta(float dt) {
        if (dt > 0.033f) {
            return 0.033f;
        }
        if (dt < 0f) {
            return 0f;
        }
        return dt;
    }

    public float interpolation(float dt) {
        accumulator += dt;
        return Math.min(1f, accumulator / 0.016f);
    }
}
