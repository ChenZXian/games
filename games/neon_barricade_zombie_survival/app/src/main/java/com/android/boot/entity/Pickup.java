package com.android.boot.entity;

public class Pickup {
  public static final int TYPE_ENERGY = 0;
  public static final int TYPE_COIN = 1;
  public static final int TYPE_MEDKIT = 2;
  public static final int TYPE_WEAPON = 3;
  public static final int TYPE_WALL_ITEM = 4;
  public static final int TYPE_MINE_ITEM = 5;

  public float x;
  public float y;
  public float radius;
  public int type;
  public float value;
  public int weaponType;
  public int placeableType;
  public boolean active;

  public void spawn(float x, float y, int type, float value) {
    this.x = x;
    this.y = y;
    this.type = type;
    this.value = value;
    this.weaponType = -1;
    this.placeableType = -1;
    radius = 14f;
    active = true;
  }

  public void spawnWeapon(float x, float y, int weaponType) {
    this.x = x;
    this.y = y;
    this.type = TYPE_WEAPON;
    this.weaponType = weaponType;
    this.placeableType = -1;
    radius = 16f;
    active = true;
  }

  public void spawnPlaceable(float x, float y, int placeableType) {
    this.x = x;
    this.y = y;
    this.type = placeableType == 0 ? TYPE_WALL_ITEM : TYPE_MINE_ITEM;
    this.placeableType = placeableType;
    this.weaponType = -1;
    radius = 16f;
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
