package com.android.boot.entity;

public class Enemy extends BaseEntity {
  public EnemyType type;
  public float attackCooldown;
  public float stunTimer;
  public boolean elite;
  public float aiTimer;
  public float hitFlashTimer;
  public float knockbackX;
  public float maxHp;

  public Enemy(float x, float y, float w, float h, float hp, EnemyType type, boolean elite) {
    super(x, y, w, h, hp);
    this.type = type;
    this.elite = elite;
    this.maxHp = hp;
  }

  public void update(float dt, Player player, float groundY) {
    if (hitFlashTimer > 0f) {
      hitFlashTimer -= dt;
    }
    
    if (knockbackX != 0f) {
      x += knockbackX * dt;
      knockbackX *= 0.85f;
      if (Math.abs(knockbackX) < 5f) {
        knockbackX = 0f;
      }
    }
    
    if (stunTimer > 0f) {
      stunTimer -= dt;
      vx = 0f;
    } else {
      float speed = type == EnemyType.INTERN ? 120f : 100f;
      if (type == EnemyType.BOSS) {
        speed = 140f;
      }
      if (type == EnemyType.PM) {
        speed = 90f;
      }
      if (type == EnemyType.QA) {
        speed = 80f;
      }
      
      float playerCenterX = player.x + player.w * 0.5f;
      float enemyCenterX = x + w * 0.5f;
      float distance = Math.abs(playerCenterX - enemyCenterX);
      
      if (distance > w * 1.5f) {
        if (playerCenterX < enemyCenterX) {
          vx = -speed;
        } else {
          vx = speed;
        }
      } else if (distance > w * 0.3f) {
        float approachSpeed = speed * 0.6f;
        if (playerCenterX < enemyCenterX) {
          vx = -approachSpeed;
        } else {
          vx = approachSpeed;
        }
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
  
  @Override
  public void damage(float amount) {
    super.damage(amount);
    hitFlashTimer = 0.2f;
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
