package com.android.boot.entity;

public class Enemy {
  public int lane;
  public float x;
  public float y;
  public float hp;
  public float speed;
  public float radius;
  public boolean alive;

  public Enemy() {
    alive = false;
  }
}
