package com.android.boot.entity;

public class Placeable {
  public static final int TYPE_WALL = 0;
  public static final int TYPE_MINE = 1;
  public static final int TYPE_TURRET = 2;

  public float x;
  public float y;
  public float width;
  public float height;
  public int type;
  public float hp;
  public float maxHp;
  public boolean active;
  public float triggerRadius;
  public float damage;
  public float cooldown;
  public float cooldownTimer;

  public void spawn(float x, float y, int type) {
    this.x = x;
    this.y = y;
    this.type = type;
    this.active = true;
    this.cooldownTimer = 0f;
    if (type == TYPE_WALL) {
      width = 40f;
      height = 40f;
      hp = 100f;
      maxHp = 100f;
    } else if (type == TYPE_MINE) {
      width = 20f;
      height = 20f;
      hp = 1f;
      maxHp = 1f;
      triggerRadius = 60f;
      damage = 50f;
    } else if (type == TYPE_TURRET) {
      width = 30f;
      height = 30f;
      hp = 80f;
      maxHp = 80f;
      triggerRadius = 200f;
      damage = 15f;
      cooldown = 1.5f;
    }
  }

  public void deactivate() {
    active = false;
  }

  public boolean contains(float px, float py) {
    return px >= x - width * 0.5f && px <= x + width * 0.5f &&
        py >= y - height * 0.5f && py <= y + height * 0.5f;
  }
}

