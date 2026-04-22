package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.RuneFavor;

public class WarChampion extends Unit {
    private float retreatTimer;
    private float abilityCooldown;
    private float maxAbilityCooldown = 11f;
    private boolean retreating;

    public WarChampion() {
        super(Team.ALLY, "War Champion", RuneFavor.FLAME);
        width = 70f;
        height = 124f;
        maxHp = 360f;
        hp = maxHp;
        moveSpeed = 86f;
        attackRange = 64f;
        attackDamage = 34f;
        attackCooldown = 0.92f;
        attackWindup = 0.4f;
        impactWindow = 0.34f;
        champion = true;
    }

    public void resetChampion(float x, float y) {
        resetPosition(x, y);
        hp = maxHp;
        dying = false;
        faded = false;
        retreating = false;
        retreatTimer = 0f;
        abilityCooldown = 0f;
        attackTimer = 0f;
        animationState = com.android.boot.core.AnimationState.MARCH;
        stateTime = 0f;
    }

    public void tickAbility(float dt, RuneFavor favor) {
        float favorRate = favor == RuneFavor.STORM ? 1.2f : 1f;
        abilityCooldown = Math.max(0f, abilityCooldown - dt * favorRate);
    }

    public boolean canUseAbility() {
        return abilityCooldown <= 0f && !retreating;
    }

    public void triggerAbility(RuneFavor favor) {
        abilityCooldown = favor == RuneFavor.STORM ? 8f : 10f;
        animationState = com.android.boot.core.AnimationState.CAST;
        stateTime = 0f;
    }

    public float getAbilityCooldown() {
        return abilityCooldown;
    }

    public boolean isRetreating() {
        return retreating;
    }

    @Override
    public void takeDamage(float amount) {
        if (retreating) {
            return;
        }
        hp -= amount;
        hitFlash = 1f;
        recoil = 1f;
        animationState = com.android.boot.core.AnimationState.HIT;
        stateTime = 0f;
        if (hp <= 0f) {
            retreating = true;
            retreatTimer = 4f;
            hp = maxHp * 0.35f;
            animationState = com.android.boot.core.AnimationState.RETREAT;
        }
    }

    public void updateRetreat(float dt, float fallbackX) {
        if (retreating) {
            retreatTimer -= dt;
            x += (fallbackX - x) * Math.min(1f, dt * 2.4f);
            hp = Math.min(maxHp, hp + dt * 36f);
            if (retreatTimer <= 0f && hp >= maxHp * 0.7f) {
                retreating = false;
                animationState = com.android.boot.core.AnimationState.MARCH;
                stateTime = 0f;
            }
        }
    }

    @Override
    protected void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway) {
        RectF blade = new RectF(width * 0.12f * dir, -height * 0.88f, (width * 0.12f + armReach * 1.25f) * dir, -height * 0.76f);
        if (dir < 0f) {
            blade = new RectF((width * 0.12f + armReach * 1.25f) * dir, -height * 0.88f, width * 0.12f * dir, -height * 0.76f);
        }
        canvas.drawRoundRect(blade, width * 0.05f, width * 0.05f, accentPaint);
        Path pennant = new Path();
        pennant.moveTo(width * 0.18f * dir, -height * 0.82f);
        pennant.lineTo((width * 0.18f + armReach * 0.4f) * dir, -height * 0.74f + sway * 0.2f);
        pennant.lineTo(width * 0.18f * dir, -height * 0.66f);
        pennant.close();
        canvas.drawPath(pennant, fillPaint);
        RectF crown = new RectF(-width * 0.18f, -height * 1.02f, width * 0.18f, -height * 0.9f);
        canvas.drawRoundRect(crown, width * 0.04f, width * 0.04f, accentPaint);
    }
}
