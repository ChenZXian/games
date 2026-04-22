package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.AnimationState;
import com.android.boot.core.RuneFavor;

public abstract class Unit {
    protected final Team team;
    protected final String label;
    protected final RuneFavor favorAtSpawn;
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected float maxHp;
    protected float hp;
    protected float moveSpeed;
    protected float attackRange;
    protected float attackDamage;
    protected float attackCooldown;
    protected float attackWindup;
    protected float impactWindow;
    protected float projectileSpeed;
    protected float preferredRange;
    protected float sustainPulse;
    protected float sustainRate;
    protected boolean ranged;
    protected boolean support;
    protected boolean titan;
    protected boolean champion;
    protected boolean dying;
    protected boolean faded;
    protected boolean impactDone;
    protected float stateTime;
    protected float attackTimer;
    protected float fadeTimer;
    protected float hitFlash;
    protected float bob;
    protected float recoil;
    protected AnimationState animationState = AnimationState.MARCH;
    protected Unit target;

    protected Unit(Team team, String label, RuneFavor favorAtSpawn) {
        this.team = team;
        this.label = label;
        this.favorAtSpawn = favorAtSpawn;
    }

    public void resetPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void updateTime(float dt) {
        bob += dt * (dying ? 1.2f : 3.5f);
        hitFlash = Math.max(0f, hitFlash - dt * 4f);
        recoil = Math.max(0f, recoil - dt * 3.5f);
        stateTime += dt;
    }

    public void heal(float amount) {
        if (!dying) {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    public void takeDamage(float amount) {
        if (dying) {
            return;
        }
        hp -= amount;
        hitFlash = 1f;
        recoil = 1f;
        animationState = AnimationState.HIT;
        stateTime = 0f;
        if (hp <= 0f) {
            dying = true;
            animationState = AnimationState.FALL;
            stateTime = 0f;
            fadeTimer = 1.2f;
        }
    }

    public void updateDeath(float dt) {
        if (dying) {
            fadeTimer -= dt;
            if (fadeTimer <= 0f) {
                faded = true;
            }
        }
    }

    public boolean canAttack() {
        return attackTimer <= 0f && !dying;
    }

    public void startAttack(Unit nextTarget) {
        target = nextTarget;
        attackTimer = attackCooldown;
        impactDone = false;
        animationState = ranged ? AnimationState.RANGED : AnimationState.ATTACK;
        stateTime = 0f;
    }

    public void tickAttack(float dt) {
        attackTimer = Math.max(0f, attackTimer - dt);
        if (attackTimer <= attackCooldown - impactWindow) {
            animationState = AnimationState.MARCH;
        }
    }

    public boolean readyForImpact() {
        return !impactDone && attackTimer <= attackCooldown - attackWindup;
    }

    public void markImpactDone() {
        impactDone = true;
    }

    public boolean isAlly() {
        return team == Team.ALLY;
    }

    public Team getTeam() {
        return team;
    }

    public float getFrontX() {
        return team == Team.ALLY ? x + width * 0.55f : x - width * 0.55f;
    }

    public float getBodyStartX() {
        return x - width * 0.45f;
    }

    public float getBodyEndX() {
        return x + width * 0.45f;
    }

    public float getPreferredRange() {
        return preferredRange > 0f ? preferredRange : attackRange;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public float getDamage() {
        return attackDamage;
    }

    public float getProjectileSpeed() {
        return projectileSpeed;
    }


    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getY() {
        return y;
    }

    public float getX() {
        return x;
    }

    public float getHp() {
        return hp;
    }

    public float getMaxHp() {
        return maxHp;
    }

    public RuneFavor getFavorAtSpawn() {
        return favorAtSpawn;
    }

    public boolean isRanged() {
        return ranged;
    }

    public boolean isSupport() {
        return support;
    }

    public boolean isTitan() {
        return titan;
    }

    public boolean isChampion() {
        return champion;
    }

    public boolean isDying() {
        return dying;
    }

    public boolean isFaded() {
        return faded;
    }

    public String getLabel() {
        return label;
    }

    public float getSustainPulse() {
        return sustainPulse;
    }

    public float getSustainRate() {
        return sustainRate;
    }

    public void move(float dx) {
        x += dx;
        if (!dying) {
            animationState = AnimationState.MARCH;
        }
    }

    protected float teamDir() {
        return team == Team.ALLY ? 1f : -1f;
    }

    public void draw(Canvas canvas, Paint fillPaint, Paint accentPaint, Paint flashPaint, Paint auraPaint) {
        float alpha = faded ? 0f : Math.max(0f, Math.min(1f, dying ? fadeTimer / 1.2f : 1f));
        fillPaint.setAlpha((int) (255 * alpha));
        accentPaint.setAlpha((int) (255 * alpha));
        flashPaint.setAlpha((int) (190 * hitFlash * alpha));
        auraPaint.setAlpha((int) (110 * alpha));
        float sway = (float) Math.sin(bob) * width * (dying ? 0.04f : 0.08f);
        float march = animationState == AnimationState.MARCH ? (float) Math.sin(bob * 2f) * width * 0.07f : 0f;
        float lunge = animationState == AnimationState.ATTACK || animationState == AnimationState.RANGED || animationState == AnimationState.CAST ? width * 0.12f * (1f - Math.abs(attackTimer - attackCooldown * 0.45f) / attackCooldown) : 0f;
        float hitLean = recoil * width * 0.1f * -teamDir();
        float deathLean = dying ? width * 0.2f * teamDir() : 0f;
        canvas.save();
        canvas.translate(x + march + lunge + hitLean, y + sway);
        canvas.rotate((hitLean + deathLean) * 3f, 0f, -height * 0.5f);
        RectF torso = new RectF(-width * 0.2f, -height * 0.78f, width * 0.2f, -height * 0.26f);
        canvas.drawRoundRect(torso, width * 0.09f, width * 0.09f, fillPaint);
        RectF hip = new RectF(-width * 0.22f, -height * 0.34f, width * 0.22f, -height * 0.16f);
        canvas.drawRoundRect(hip, width * 0.08f, width * 0.08f, accentPaint);
        Path cloak = new Path();
        cloak.moveTo(-width * 0.15f, -height * 0.74f);
        cloak.lineTo(-width * 0.38f, -height * 0.15f);
        cloak.lineTo(width * 0.28f, -height * 0.1f);
        cloak.lineTo(width * 0.16f, -height * 0.7f);
        cloak.close();
        canvas.drawPath(cloak, accentPaint);
        canvas.drawOval(-width * 0.16f, -height * 0.98f, width * 0.16f, -height * 0.7f, fillPaint);
        float armReach = animationState == AnimationState.ATTACK || animationState == AnimationState.CAST ? width * 0.35f : width * 0.18f;
        canvas.drawRoundRect(new RectF(-width * 0.34f, -height * 0.72f, -width * 0.12f, -height * 0.58f), width * 0.05f, width * 0.05f, fillPaint);
        canvas.drawRoundRect(new RectF(width * 0.12f, -height * 0.72f, width * 0.12f + armReach * teamDir(), -height * 0.58f), width * 0.05f, width * 0.05f, fillPaint);
        float legSwing = animationState == AnimationState.MARCH ? (float) Math.sin(bob * 2f) * width * 0.16f : 0f;
        canvas.drawRoundRect(new RectF(-width * 0.18f + legSwing, -height * 0.18f, -width * 0.04f + legSwing, 0f), width * 0.04f, width * 0.04f, fillPaint);
        canvas.drawRoundRect(new RectF(width * 0.04f - legSwing, -height * 0.18f, width * 0.18f - legSwing, 0f), width * 0.04f, width * 0.04f, fillPaint);
        drawWeapon(canvas, fillPaint, accentPaint, teamDir(), armReach, sway);
        if (favorAtSpawn == RuneFavor.FLAME) {
            auraPaint.setColor(auraPaint.getColor());
            canvas.drawOval(-width * 0.46f, -height * 1.02f, width * 0.46f, height * 0.04f, auraPaint);
        } else if (favorAtSpawn == RuneFavor.STORM) {
            canvas.drawRect(-width * 0.48f, -height * 0.98f, width * 0.48f, -height * 0.88f, auraPaint);
        } else {
            canvas.drawOval(-width * 0.5f, -height * 0.22f, width * 0.5f, height * 0.12f, auraPaint);
        }
        if (hitFlash > 0f) {
            canvas.drawRoundRect(new RectF(-width * 0.32f, -height, width * 0.32f, 0f), width * 0.08f, width * 0.08f, flashPaint);
        }
        canvas.restore();
    }

    protected abstract void drawWeapon(Canvas canvas, Paint fillPaint, Paint accentPaint, float dir, float armReach, float sway);
}
