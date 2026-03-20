package com.android.boot.fx;

public class ScreenShake {
    private float timer;
    private float strength;

    public void trigger(float duration, float strength) {
        this.timer = Math.max(this.timer, duration);
        this.strength = Math.max(this.strength, strength);
    }

    public void update(float dt) {
        if (timer > 0f) {
            timer -= dt;
            strength *= 0.94f;
            if (timer <= 0f) {
                timer = 0f;
                strength = 0f;
            }
        }
    }

    public float offsetX() {
        return timer > 0f ? (float) Math.sin(timer * 70f) * strength : 0f;
    }

    public float offsetY() {
        return timer > 0f ? (float) Math.cos(timer * 54f) * strength * 0.6f : 0f;
    }
}
