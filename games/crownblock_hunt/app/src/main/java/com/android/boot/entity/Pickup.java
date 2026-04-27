package com.android.boot.entity;

public class Pickup extends BaseEntity {
  public PickupType type;

  public Pickup(float x, float y, float size, PickupType type) {
    super(x, y, size, size, 1f);
    this.type = type;
  }

  public enum PickupType {
    COFFEE,
    PAPER
  }
}
