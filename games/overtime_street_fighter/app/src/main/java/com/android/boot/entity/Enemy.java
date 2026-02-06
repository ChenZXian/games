package com.android.boot.entity;

public class Enemy extends BaseEntity {
  public EnemyType type;
  public float attackCooldown;
  public float stunTimer;
  public boolean elite;
  public float aiTimer;

  public Enemy(float x, float y, float w, float h, float hp, EnemyType type, boolean elite) {
    super(x, y, w, h, hp);
    this.type = type;
    this.elite = elite;
  }

  public void update(float dt, Player player, float groundY) {
    if (stunTimer > 0f) {
      stunTimer -= dt;
      vx = 0f;
    } else {
      float speed = type == EnemyType.INTERN ? 120f : 90f;
      if (type == EnemyType.BOSS) {
        speed = 140f;
      }
      if (player.x + player.w * 0.5f < x) {
        vx = -speed;
      } else if (player.x + player.w * 0.5f > x + w) {
        vx = speed;
      } else {
        vx = 0f;
      }
    }

    if (attackCooldown > 0f) {
      attackCooldown -= dt;
    }

    vy += 1200f * dt;
    x += vx * dt;
    y += vy * dt;

    if (y + h >= groundY) {
      y = groundY - h;
      vy = 0f;
    }
  }

  public boolean canAttack() {
    return attackCooldown <= 0f && stunTimer <= 0f;
  }

  public void triggerAttackCooldown(float duration) {
    attackCooldown = duration;
  }

  public void applyStun(float duration) {
    if (duration > stunTimer) {
      stunTimer = duration;
    }
  }

  public enum EnemyType {
    INTERN,
    PM,
    QA,
    BOSS
  }
}
