package com.android.boot.entity;

import com.android.boot.core.AnimationState;
import com.android.boot.core.BattleManager;
import com.android.boot.core.RuneFavor;

public class WarChampion extends Unit {
    public float retreatTimer;
    public float recoveryTimer;
    public boolean retreating;

    public WarChampion(Team team, RuneFavor favor) {
        super(team, favor, "War Champion");
        maxHp = 320f;
        hp = maxHp;
        speed = 106f;
        range = 86f;
        preferredRange = 84f;
        damage = 36f;
        attackCooldown = 1.1f;
        attackDuration = 0.72f;
        impactMoment = 0.42f;
        size = 96f;
        bodyHeight = 122f;
        hero = true;
    }

    @Override
    public void update(BattleManager manager, float dt) {
        if (retreating) {
            retreatTimer -= dt;
            x += -140f * dt;
            animationState = AnimationState.RETREAT;
            heal(maxHp * 0.08f * dt);
            if (retreatTimer <= 0f) {
                retreating = false;
                recoveryTimer = 1.4f;
            }
            return;
        }
        if (recoveryTimer > 0f) {
            recoveryTimer -= dt;
            x += 44f * dt;
        }
        if (hp < maxHp * 0.18f && !retreating) {
            retreating = true;
            retreatTimer = 2.4f;
            return;
        }
        super.update(manager, dt);
    }

    @Override
    public void damage(float amount) {
        hp -= amount;
        hitFlash = 1f;
        hitRecoil = 1f;
        if (hp <= 0f) {
            hp = maxHp * 0.24f;
            retreating = true;
            retreatTimer = 3f;
        }
    }
}
