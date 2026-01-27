package com.android.boot.entity;

public class Player {
  public float x;
  public float y;
  public float radius;
  public float speed;
  public float maxHp;
  public float hp;
  public float energy;
  public float maxEnergy;
  public float regen;
  public float shield;

  public Player(float x, float y) {
    this.x = x;
    this.y = y;
    radius = 24f;
    speed = 320f;
    maxHp = 120f;
    hp = maxHp;
    maxEnergy = 100f;
    energy = maxEnergy * 0.5f;
    regen = 0f;
    shield = 0f;
  }

  public void reset(float x, float y) {
    this.x = x;
    this.y = y;
    hp = maxHp;
    energy = maxEnergy * 0.5f;
    shield = 0f;
  }
}
