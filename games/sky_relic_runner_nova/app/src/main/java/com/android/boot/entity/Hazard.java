package com.android.boot.entity;

public class Hazard {
  public static final int TYPE_SPIKE = 0;
  public static final int TYPE_BLOCK = 1;

  public float x;
  public float y;
  public float width;
  public float height;
  public float movePhase;
  public float moveRange;
  public float moveSpeed;
  public int type;
  public boolean active;
}
