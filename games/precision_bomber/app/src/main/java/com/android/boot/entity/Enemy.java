package com.android.boot.entity;

import com.android.boot.core.Direction;

public abstract class Enemy extends Entity {
  public Direction facing = Direction.DOWN;
  public float speed = 0.8f;
  public boolean alive = true;
}
