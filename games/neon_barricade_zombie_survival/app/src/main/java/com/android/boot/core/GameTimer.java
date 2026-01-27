package com.android.boot.core;

public class GameTimer {
  private float elapsed;

  public void reset() {
    elapsed = 0f;
  }

  public void update(float delta) {
    elapsed += delta;
  }

  public float getElapsed() {
    return elapsed;
  }
}
