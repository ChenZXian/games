package com.android.boot.entity;

public class PlayerDrone {
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float radius;
  public int health;
  public int ammo;
  public float special;
  public float invuln;
  public boolean pullMode;

  public void reset(float centerX, float centerY, float valueRadius) {
    x = centerX;
    y = centerY;
    vx = 0f;
    vy = 0f;
    radius = valueRadius;
    health = 5;
    ammo = 0;
    special = 0f;
    invuln = 0f;
    pullMode = true;
  }
}
