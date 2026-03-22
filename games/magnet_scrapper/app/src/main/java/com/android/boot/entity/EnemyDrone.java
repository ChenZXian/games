package com.android.boot.entity;

public class EnemyDrone {
  public static final int TYPE_SCOUT = 0;
  public static final int TYPE_TANK = 1;
  public static final int TYPE_LEECH = 2;
  public boolean active;
  public int type;
  public float x;
  public float y;
  public float vx;
  public float vy;
  public float radius;
  public float speed;
  public float hp;
  public float hitFlash;
  public float knockback;
  public float knockX;
  public float knockY;

  public void activate(int enemyType, float startX, float startY, int wave) {
    active = true;
    type = enemyType;
    x = startX;
    y = startY;
    vx = 0f;
    vy = 0f;
    hitFlash = 0f;
    knockback = 0f;
    knockX = 0f;
    knockY = 0f;
    if (enemyType == TYPE_SCOUT) {
      radius = 14f + wave * 0.15f;
      speed = 130f + wave * 7f;
      hp = 1.5f + wave * 0.15f;
    } else if (enemyType == TYPE_TANK) {
      radius = 22f + wave * 0.2f;
      speed = 58f + wave * 3f;
      hp = 5.5f + wave * 0.55f;
    } else {
      radius = 16f + wave * 0.18f;
      speed = 90f + wave * 5f;
      hp = 3f + wave * 0.28f;
    }
  }

  public void deactivate() {
    active = false;
  }
}
