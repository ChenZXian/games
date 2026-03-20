package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.AnimationState;
import com.android.boot.core.BattleManager;
import com.android.boot.core.RuneFavor;

public class Unit {
    public final Team team;
    public final RuneFavor favorAtSpawn;
    public final String name;
    public float x;
    public float y;
    public float maxHp;
    public float hp;
    public float speed;
    public float range;
    public float preferredRange;
    public float damage;
    public float attackCooldown;
    public float attackTimer;
    public float attackDuration;
    public float attackTime;
    public float impactMoment;
    public float size;
    public float bodyHeight;
    public float alpha = 1f;
    public float hitFlash;
    public float hitRecoil;
    public float deathTimer;
    public float sustainTimer;
    public float stepTimer;
    public boolean ranged;
    public boolean support;
    public boolean titan;
    public boolean hero;
    public boolean healing;
    public boolean releasedProjectile;
    public boolean impacted;
    public boolean dead;
    public AnimationState animationState = AnimationState.IDLE;
    public int primaryColor;
    public int trimColor;
    public int glowColor;

    public Unit(Team team, RuneFavor favorAtSpawn, String name) {
        this.team = team;
        this.favorAtSpawn = favorAtSpawn;
        this.name = name;
    }

    public void update(BattleManager manager, float dt) {
        if (dead) {
            deathTimer += dt;
            alpha = Math.max(0f, 1f - deathTimer * 1.1f);
            animationState = AnimationState.FALL;
            return;
        }
        if (sustainTimer > 0f) {
            sustainTimer -= dt;
        }
        if (hitFlash > 0f) {
            hitFlash -= dt * 4f;
        }
        if (hitRecoil > 0f) {
            hitRecoil -= dt * 5f;
        }
        attackTimer -= dt;
        if (attackTime > 0f) {
            attackTime -= dt;
            animationState = AnimationState.ATTACK;
        }
        Unit target = manager.findTarget(this);
        if (support) {
            Unit ally = manager.findWoundedAlly(this);
            if (ally != null && Math.abs(ally.x - x) < preferredRange * 1.3f) {
                if (attackTimer <= 0f) {
                    beginAttack();
                }
                if (!releasedProjectile && attackTime <= attackDuration * (1f - impactMoment)) {
                    releasedProjectile = true;
                    manager.releaseSupportPulse(this, ally);
                }
                return;
            }
        }
        if (target != null) {
            float dir = team == Team.ALLY ? 1f : -1f;
            float dx = target.x - x;
            float adx = Math.abs(dx);
            if (adx > range) {
                float desiredGap = ranged ? preferredRange : range * 0.82f;
                if (adx > desiredGap) {
                    x += dir * speed * dt;
                    animationState = AnimationState.MARCH;
                    stepTimer += dt;
                }
            } else {
                if (attackTimer <= 0f) {
                    beginAttack();
                }
                if (!releasedProjectile && ranged && attackTime <= attackDuration * (1f - impactMoment)) {
                    releasedProjectile = true;
                    manager.spawnProjectile(this, target);
                }
                if (!impacted && !ranged && attackTime <= attackDuration * (1f - impactMoment)) {
                    impacted = true;
                    manager.applyMeleeImpact(this, target);
                }
            }
        } else {
            BaseCore enemyBase = manager.getEnemyBase(team);
            float dir = team == Team.ALLY ? 1f : -1f;
            float baseEdge = team == Team.ALLY ? enemyBase.x - enemyBase.width * 0.6f : enemyBase.x + enemyBase.width * 0.6f;
            if (Math.abs(baseEdge - x) > range * 0.9f) {
                x += dir * speed * dt;
                animationState = AnimationState.MARCH;
                stepTimer += dt;
            } else {
                if (attackTimer <= 0f) {
                    beginAttack();
                }
                if (!impacted && attackTime <= attackDuration * (1f - impactMoment)) {
                    impacted = true;
                    manager.applyBaseImpact(this, enemyBase);
                }
            }
        }
        if (animationState != AnimationState.ATTACK && animationState != AnimationState.MARCH) {
            animationState = AnimationState.IDLE;
        }
    }

    private void beginAttack() {
        attackTimer = attackCooldown;
        attackTime = attackDuration;
        releasedProjectile = false;
        impacted = false;
        animationState = AnimationState.ATTACK;
    }

    public void damage(float amount) {
        hp -= amount;
        hitFlash = 1f;
        hitRecoil = 1f;
        animationState = AnimationState.HIT;
        if (hp <= 0f) {
            hp = 0f;
            dead = true;
        }
    }

    public void heal(float amount) {
        hp += amount;
        if (hp > maxHp) {
            hp = maxHp;
        }
        hitFlash = 0.4f;
    }

    public boolean isGone() {
        return dead && alpha <= 0f;
    }

    public void render(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        if (alpha <= 0f) {
            return;
        }
        float dir = team == Team.ALLY ? 1f : -1f;
        float sway = animationState == AnimationState.MARCH ? (float) Math.sin(stepTimer * 10f) * size * 0.08f : (float) Math.sin(stepTimer * 3f) * size * 0.03f;
        float lunge = animationState == AnimationState.ATTACK ? size * 0.12f : 0f;
        float recoil = hitRecoil * size * 0.08f;
        canvas.save();
        canvas.translate(x - dir * recoil + dir * lunge, y + sway);
        fillPaint.setAlpha((int) (alpha * 255f));
        strokePaint.setAlpha((int) (alpha * 220f));
        drawShadow(canvas, fillPaint);
        canvas.scale(dir, 1f);
        drawBody(canvas, fillPaint, strokePaint);
        canvas.restore();
    }

    private void drawShadow(Canvas canvas, Paint fillPaint) {
        fillPaint.setColor(0x44000000);
        RectF shadow = new RectF(-size * 0.45f, size * 0.06f, size * 0.45f, size * 0.22f);
        canvas.drawOval(shadow, fillPaint);
    }

    private void drawBody(Canvas canvas, Paint fillPaint, Paint strokePaint) {
        int base = hitFlash > 0.6f ? 0xFFFFFFFF : primaryColor;
        fillPaint.setColor(base);
        RectF legsBack = new RectF(-size * 0.18f, -size * 0.18f, -size * 0.02f, bodyHeight * 0.2f);
        RectF legsFront = new RectF(size * 0.04f, -size * 0.18f, size * 0.2f, bodyHeight * 0.22f);
        canvas.drawRoundRect(legsBack, 14f, 14f, fillPaint);
        canvas.drawRoundRect(legsFront, 14f, 14f, fillPaint);
        RectF torso = new RectF(-size * 0.28f, -bodyHeight * 0.68f, size * 0.24f, bodyHeight * 0.02f);
        fillPaint.setColor(base);
        canvas.drawRoundRect(torso, 18f, 18f, fillPaint);
        Path cloak = new Path();
        cloak.moveTo(-size * 0.24f, -bodyHeight * 0.52f);
        cloak.lineTo(-size * 0.4f, -bodyHeight * 0.02f);
        cloak.lineTo(-size * 0.08f, bodyHeight * 0.02f);
        cloak.close();
        fillPaint.setColor(glowColor);
        canvas.drawPath(cloak, fillPaint);
        fillPaint.setColor(trimColor);
        RectF shoulder = new RectF(-size * 0.34f, -bodyHeight * 0.7f, size * 0.3f, -bodyHeight * 0.44f);
        canvas.drawRoundRect(shoulder, 16f, 16f, fillPaint);
        RectF head = new RectF(-size * 0.16f, -bodyHeight * 0.98f, size * 0.16f, -bodyHeight * 0.58f);
        fillPaint.setColor(base == 0xFFFFFFFF ? trimColor : base + 0x000A0A0A);
        canvas.drawOval(head, fillPaint);
        Path helm = new Path();
        helm.moveTo(-size * 0.2f, -bodyHeight * 0.82f);
        helm.lineTo(0f, -bodyHeight * 1.08f);
        helm.lineTo(size * 0.22f, -bodyHeight * 0.82f);
        helm.close();
        fillPaint.setColor(trimColor);
        canvas.drawPath(helm, fillPaint);
        RectF arm = new RectF(size * 0.08f, -bodyHeight * 0.52f, size * 0.4f, -bodyHeight * 0.3f);
        fillPaint.setColor(base);
        canvas.drawRoundRect(arm, 12f, 12f, fillPaint);
        drawWeapon(canvas, fillPaint);
        if (hero || titan) {
            RectF crest = new RectF(-size * 0.06f, -bodyHeight * 1.18f, size * 0.06f, -bodyHeight * 0.92f);
            fillPaint.setColor(glowColor);
            canvas.drawRoundRect(crest, 8f, 8f, fillPaint);
        }
    }

    private void drawWeapon(Canvas canvas, Paint fillPaint) {
        Path weapon = new Path();
        if (support) {
            weapon.moveTo(size * 0.22f, -bodyHeight * 0.56f);
            weapon.lineTo(size * 0.34f, -bodyHeight * 0.92f);
            weapon.lineTo(size * 0.44f, -bodyHeight * 0.86f);
            weapon.lineTo(size * 0.3f, -bodyHeight * 0.44f);
            weapon.close();
        } else if (ranged) {
            weapon.moveTo(size * 0.1f, -bodyHeight * 0.5f);
            weapon.lineTo(size * 0.42f, -bodyHeight * 0.72f);
            weapon.lineTo(size * 0.36f, -bodyHeight * 0.2f);
            weapon.close();
        } else if (titan) {
            weapon.moveTo(size * 0.12f, -bodyHeight * 0.56f);
            weapon.lineTo(size * 0.48f, -bodyHeight * 0.9f);
            weapon.lineTo(size * 0.58f, -bodyHeight * 0.76f);
            weapon.lineTo(size * 0.26f, -bodyHeight * 0.36f);
            weapon.close();
        } else {
            weapon.moveTo(size * 0.14f, -bodyHeight * 0.5f);
            weapon.lineTo(size * 0.5f, -bodyHeight * 0.86f);
            weapon.lineTo(size * 0.58f, -bodyHeight * 0.72f);
            weapon.lineTo(size * 0.24f, -bodyHeight * 0.34f);
            weapon.close();
        }
        fillPaint.setColor(trimColor);
        canvas.drawPath(weapon, fillPaint);
    }
}
