package com.android.boot.entity;

import android.graphics.RectF;

public class BaseEntity {
  public float x;
  public float y;
  public float w;
  public float h;
  public float vx;
  public float vy;
  public float hp;
  public boolean alive = true;

  public BaseEntity(float x, float y, float w, float h, float hp) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.hp = hp;
  }

  public RectF getRect(RectF out) {
    out.set(x, y, x + w, y + h);
    return out;
  }

  public void damage(float amount) {
    hp -= amount;
    if (hp <= 0f) {
      alive = false;
    }
  }
}
