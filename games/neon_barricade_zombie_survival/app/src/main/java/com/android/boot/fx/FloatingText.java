package com.android.boot.fx;

public class FloatingText {
  public float x;
  public float y;
  public float vy;
  public float life;
  public String text;
  public boolean active;

  public void spawn(float x, float y, float vy, float life, String text) {
    this.x = x;
    this.y = y;
    this.vy = vy;
    this.life = life;
    this.text = text;
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
