package com.android.boot.entity;

import com.android.boot.core.Direction;

public class Bomb {
  public int x;
  public int y;
  public float timer;
  public float fuse;
  public Direction direction;
  public boolean exploded;

  public Bomb(int x, int y, float fuse, Direction direction) {
    this.x = x;
    this.y = y;
    this.fuse = fuse;
    this.direction = direction;
  }
}
