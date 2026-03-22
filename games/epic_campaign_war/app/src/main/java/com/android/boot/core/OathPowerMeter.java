package com.android.boot.core;

public class OathPowerMeter {
    private float current;
    private final float max;

    public OathPowerMeter(float max) {
        this.max = max;
    }

    public void reset() {
        current = 0f;
    }

    public void add(float value) {
        current = Math.min(max, current + value);
    }

    public boolean spendForSwitch() {
        if (current < 20f) {
            return false;
        }
        current = Math.max(0f, current - 20f);
        return true;
    }

    public float getCurrent() {
        return current;
    }

    public int getPercent() {
        return Math.round((current / max) * 100f);
    }
}
