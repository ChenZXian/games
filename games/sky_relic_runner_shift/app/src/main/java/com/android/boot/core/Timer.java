package com.android.boot.core;

public class Timer {
    private long lastTime;

    public void reset(long now) {
        lastTime = now;
    }

    public float step(long now) {
        if (lastTime == 0) {
            lastTime = now;
            return 0f;
        }
        float delta = (now - lastTime) / 1000f;
        lastTime = now;
        return delta;
    }
}
