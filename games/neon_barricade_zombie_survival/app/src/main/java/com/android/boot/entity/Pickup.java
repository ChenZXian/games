package com.android.boot.entity;

public class Pickup {
  public static final int TYPE_ENERGY = 0;
  public static final int TYPE_COIN = 1;
  public static final int TYPE_MEDKIT = 2;

  public float x;
  public float y;
  public float radius;
  public int type;
  public float value;
  public boolean active;

  public void spawn(float x, float y, int type, float value) {
    this.x = x;
    this.y = y;
    this.type = type;
    this.value = value;
    radius = 14f;
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
