package com.android.boot.entity;

public class Projectile {
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float radius;
  public float damage;
  public float life;
  public int pierce;
  public boolean alive;

  public Projectile() {
    alive = false;
  }
}
