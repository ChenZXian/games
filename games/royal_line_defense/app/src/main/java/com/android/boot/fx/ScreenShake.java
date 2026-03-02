package com.android.boot.fx;

public class ScreenShake {
    private float timer;
    private float intensity;

    public void trigger(float duration, float power) {
        timer = duration;
        intensity = power;
    }

    public void update(float dt) {
        timer -= dt;
        if (timer < 0f) {
            timer = 0f;
            intensity = 0f;
        }
    }

    public float getIntensity() {
        return intensity;
    }
}
