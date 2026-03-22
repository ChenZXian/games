package com.android.boot.core;

public class InputState {
  public boolean dragging;
  public float targetX;
  public float targetY;

  public void set(float x, float y, boolean active) {
    targetX = x;
    targetY = y;
    dragging = active;
  }
}
