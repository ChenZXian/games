package com.android.boot.entity;

import android.graphics.RectF;

import com.android.boot.core.GameInput;

public class Player extends BaseEntity {
  public float maxHp;
  public float energy;
  public float energyMax;
  public int combo;
  public float comboTimer;
  public boolean guarding;
  public int facing = 1;
  public float attackTimer;
  public float attackDuration;
  public float attackDamage;
  public float attackRange;
  public AttackType attackType = AttackType.NONE;
  public int comboStep;
  public float dashTimer;
  public float dashCooldown;
  public float energyGainMultiplier = 1f;

  public Player(float x, float y, float w, float h) {
    super(x, y, w, h, 100f);
    maxHp = 100f;
    energy = 0f;
    energyMax = 100f;
  }

  public void update(GameInput input, float dt, float groundY, float worldWidth) {
    guarding = input.guard;
    if (dashCooldown > 0f) {
      dashCooldown -= dt;
    }
    if (dashTimer > 0f) {
      dashTimer -= dt;
    }
    if (comboTimer > 0f) {
      comboTimer -= dt;
      if (comboTimer <= 0f) {
        combo = 0;
        comboStep = 0;
      }
    }
    if (attackTimer > 0f) {
      attackTimer -= dt;
      if (attackTimer <= 0f) {
        attackType = AttackType.NONE;
        attackDamage = 0f;
        attackRange = 0f;
      }
    }

    float speed = 260f;
    if (dashTimer > 0f) {
      speed = 520f;
    }

    if (input.left) {
      vx = -speed;
      facing = -1;
    } else if (input.right) {
      vx = speed;
      facing = 1;
    } else {
      vx = 0f;
    }

    if (input.jump && onGround(groundY)) {
      vy = -520f;
    }

    if (input.dash && dashCooldown <= 0f) {
      dashTimer = 0.2f;
      dashCooldown = 0.6f;
    }

    handleAttacks(input);

    vy += 1200f * dt;
    x += vx * dt;
    y += vy * dt;

    if (x < 0f) {
      x = 0f;
    }
    if (x + w > worldWidth) {
      x = worldWidth - w;
    }

    if (y + h >= groundY) {
      y = groundY - h;
      vy = 0f;
    }
  }

  private void handleAttacks(GameInput input) {
    if (attackTimer > 0f) {
      return;
    }
    if (input.special && energy >= energyMax) {
      startAttack(AttackType.SPECIAL, 0.5f, 28f, 180f);
      energy = 0f;
      comboStep = 0;
      return;
    }
    if (input.kick) {
      startAttack(AttackType.KICK, 0.35f, 16f, 120f);
      comboStep = 0;
      return;
    }
    if (input.heavy && comboStep == 2) {
      startAttack(AttackType.HEAVY, 0.45f, 22f, 140f);
      comboStep = 0;
      return;
    }
    if (input.light) {
      if (comboStep == 0) {
        comboStep = 1;
        startAttack(AttackType.LIGHT, 0.25f, 12f, 90f);
      } else if (comboStep == 1) {
        comboStep = 2;
        startAttack(AttackType.LIGHT, 0.25f, 12f, 90f);
      }
    }
  }

  private void startAttack(AttackType type, float duration, float damage, float range) {
    attackType = type;
    attackDuration = duration;
    attackTimer = duration;
    attackDamage = damage;
    attackRange = range;
  }

  public RectF getAttackRect(RectF out) {
    if (attackType == AttackType.NONE) {
      return null;
    }
    float left = facing > 0 ? x + w : x - attackRange;
    float right = facing > 0 ? x + w + attackRange : x;
    out.set(left, y + h * 0.2f, right, y + h * 0.85f);
    return out;
  }

  public boolean onGround(float groundY) {
    return y + h >= groundY - 0.5f;
  }

  public void addEnergy(float amount) {
    energy = Math.min(energyMax, energy + amount * energyGainMultiplier);
  }

  public void heal(float amount) {
    hp = Math.min(maxHp, hp + amount);
  }

  public void takeDamage(float amount) {
    float finalDamage = guarding ? amount * 0.4f : amount;
    damage(finalDamage);
    combo = 0;
    comboStep = 0;
    comboTimer = 0f;
  }

  public enum AttackType {
    NONE,
    LIGHT,
    HEAVY,
    KICK,
    SPECIAL
  }
}
