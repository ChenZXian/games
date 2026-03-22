package com.android.boot.entity;

public class ProjectilePiece {
  public boolean active;
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float radius;
  public float life;

  public void activate(float startX, float startY, float velX, float velY, float valueRadius) {
    active = true;
    x = startX;
    y = startY;
    vx = velX;
    vy = velY;
    radius = valueRadius;
    life = 1.1f;
  }
}
