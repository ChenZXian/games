package com.android.boot.entity;

public class Projectile extends BaseEntity {
  public float damage;
  public boolean fromEnemy;

  public Projectile(float x, float y, float w, float h, float vx, float damage, boolean fromEnemy) {
    super(x, y, w, h, 1f);
    this.vx = vx;
    this.damage = damage;
    this.fromEnemy = fromEnemy;
  }

  public void update(float dt) {
    x += vx * dt;
  }
}
