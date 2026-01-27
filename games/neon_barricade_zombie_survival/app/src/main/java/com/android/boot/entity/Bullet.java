package com.android.boot.entity;

public class Bullet {
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float radius;
  public float damage;
  public int pierce;
  public boolean active;

  public void spawn(float x, float y, float vx, float vy, float damage, int pierce) {
    this.x = x;
    this.y = y;
    this.vx = vx;
    this.vy = vy;
    this.damage = damage;
    this.pierce = pierce;
    radius = 6f;
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
