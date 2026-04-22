package com.android.boot.fx;

public class ScreenShake {
    private float intensity;
    private float time;

    public void trigger(float force) {
        intensity = Math.max(intensity, force);
        time = 0f;
    }

    public void update(float dt) {
        time += dt * 36f;
        intensity = Math.max(0f, intensity - dt * 1.8f);
    }

    public float getOffsetX() {
        return (float) Math.sin(time) * intensity * 16f;
    }

    public float getOffsetY() {
        return (float) Math.cos(time * 1.3f) * intensity * 10f;
    }
}
