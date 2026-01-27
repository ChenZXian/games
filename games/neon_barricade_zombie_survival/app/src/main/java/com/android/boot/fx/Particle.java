package com.android.boot.fx;

public class Particle {
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float life;
  public float size;
  public boolean active;

  public void spawn(float x, float y, float vx, float vy, float life, float size) {
    this.x = x;
    this.y = y;
    this.vx = vx;
    this.vy = vy;
    this.life = life;
    this.size = size;
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
