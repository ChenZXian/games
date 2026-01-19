package com.android.boot.entity;

public class Player {
  public float x;
  public float y;
  public float width;
  public float height;
  public float velocityY;
  public boolean onGround;
  public float invulnTime;

  public Player(float width, float height) {
    this.width = width;
    this.height = height;
  }
}
