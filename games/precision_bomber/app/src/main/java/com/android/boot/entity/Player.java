package com.android.boot.entity;

import com.android.boot.core.Direction;

public class Player extends Entity {
  public Direction facing = Direction.DOWN;
  public float speed = 1.0f;
  public int maxBombs = 1;
  public float bombFuse = 1.6f;
  public int shield = 0;
  public boolean remoteDetonator = false;
}
