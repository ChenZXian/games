package com.android.boot.fx;

public class ScreenShake {
  private float timer;
  private float intensity;

  public void trigger(float duration, float intensity) {
    this.timer = duration;
    this.intensity = intensity;
  }

  public void update(float dt) {
    if (timer > 0f) {
      timer -= dt;
      if (timer < 0f) {
        timer = 0f;
      }
    }
  }

  public float getIntensity() {
    return timer > 0f ? intensity : 0f;
  }
}
