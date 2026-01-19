package com.android.boot.entity;

public class Collectible {
  public static final int TYPE_SHARD = 0;
  public static final int TYPE_CORE = 1;
  public static final int TYPE_RUNE = 2;

  public float x;
  public float y;
  public float radius;
  public int type;
  public boolean active;
}
