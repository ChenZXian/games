package com.android.boot.entity;

public abstract class Entity {
  public int gridX;
  public int gridY;
  public float posX;
  public float posY;
  public int targetX;
  public int targetY;
  public boolean moving;
  public float moveTimer;
  public float moveDuration;

  public void snapToGrid() {
    posX = gridX;
    posY = gridY;
    targetX = gridX;
    targetY = gridY;
    moving = false;
    moveTimer = 0f;
  }
}
