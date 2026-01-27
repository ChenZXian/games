package com.android.boot.entity;

public class Enemy {
  public float x;
  public float y;
  public float radius;
  public float speed;
  public float hp;
  public float maxHp;
  public boolean active;
  public boolean elite;
  public int modifier;
  public float regen;
  public float explodeDamage;

  public void spawn(float x, float y, float radius, float speed, float hp, boolean elite, int modifier) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    this.speed = speed;
    this.hp = hp;
    this.maxHp = hp;
    this.elite = elite;
    this.modifier = modifier;
    regen = 0f;
    explodeDamage = 0f;
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
