package com.android.boot.entity;

public class Pickup {
  public enum Type {
    BOMB_PLUS,
    SPEED_PLUS,
    FUSE_MINUS,
    SHIELD,
    REMOTE
  }

  public int x;
  public int y;
  public Type type;

  public Pickup(int x, int y, Type type) {
    this.x = x;
    this.y = y;
    this.type = type;
  }
}
