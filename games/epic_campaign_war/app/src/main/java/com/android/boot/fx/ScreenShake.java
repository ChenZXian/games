package com.android.boot.fx;

public class ScreenShake {
    private float timer;
    private float intensity;

    public void trigger(float intensity, float duration) {
        this.intensity = Math.max(this.intensity, intensity);
        this.timer = Math.max(this.timer, duration);
    }

    public void update(float dt) {
        timer = Math.max(0f, timer - dt);
        if (timer == 0f) {
            intensity = 0f;
        }
    }

    public float getOffsetX(float time) {
        return (float) Math.sin(time * 48f) * intensity * timer;
    }

    public float getOffsetY(float time) {
        return (float) Math.cos(time * 37f) * intensity * timer;
    }
}
