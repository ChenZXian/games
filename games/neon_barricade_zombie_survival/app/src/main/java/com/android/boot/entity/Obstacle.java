package com.android.boot.entity;

public class Obstacle {
  public static final int TYPE_WALL = 0;
  public static final int TYPE_BARREL = 1;
  public static final int TYPE_CRATE = 2;
   public static final int TYPE_HOUSE = 3;
  public static final int TYPE_TREE = 4;
  public static final int TYPE_ROCK = 5;
  public static final int TYPE_RIVER = 6;

  public float x;
  public float y;
  public float width;
  public float height;
  public int type;
  public float hp;
  public float maxHp;
  public boolean active;
  public boolean destructible;

  public void spawn(float x, float y, float width, float height, int type, boolean destructible, float hp) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.type = type;
    this.destructible = destructible;
    this.hp = hp;
    this.maxHp = hp;
    this.active = true;
  }

  public void deactivate() {
    active = false;
  }

  public boolean contains(float px, float py) {
    return px >= x - width * 0.5f && px <= x + width * 0.5f &&
        py >= y - height * 0.5f && py <= y + height * 0.5f;
  }
}

