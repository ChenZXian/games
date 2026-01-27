package com.android.boot.core;

public class Timer {
    private float time;

    public void reset() {
        time = 0f;
    }

    public void add(float delta) {
        time += delta;
    }

    public float getTime() {
        return time;
    }
}
