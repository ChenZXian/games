package com.android.boot.core;

public class Cooldown {
  private float timer;

  public void start(float duration) {
    timer = duration;
  }

  public void update(float delta) {
    if (timer > 0f) {
      timer -= delta;
      if (timer < 0f) {
        timer = 0f;
      }
    }
  }

  public boolean isReady() {
    return timer <= 0f;
  }

  public float getRemaining() {
    return timer;
  }
}
