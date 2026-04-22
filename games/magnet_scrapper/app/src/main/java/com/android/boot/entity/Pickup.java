package com.android.boot.entity;

public class Pickup {
  public static final int TYPE_SCRAP = 0;
  public static final int TYPE_ENERGY = 1;
  public boolean active;
  public int type;
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float radius;

  public void activate(int pickupType, float startX, float startY, float velX, float velY, float valueRadius) {
    active = true;
    type = pickupType;
    x = startX;
    y = startY;
    vx = velX;
    vy = velY;
    radius = valueRadius;
  }
}
