package com.android.boot.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.android.boot.core.AnimationState;
import com.android.boot.core.RoyalOath;

public class Unit {
    protected final String label;
    protected final Team team;
    protected final boolean ranged;
    protected final boolean support;
    protected final boolean siege;
    protected final boolean hero;
    protected final float size;
    protected float x;
    protected float y;
    protected float maxHp;
    protected float hp;
    protected float damage;
    protected float range;
    protected float speed;
    protected float cooldown;
    protected float attackTimer;
    protected float actionTime;
    protected float recoil;
    protected boolean appliedImpact;
    protected boolean projectileReleased;
    protected boolean alive = true;
    protected float alpha = 1f;
    protected AnimationState animationState = AnimationState.MARCH;
    protected RoyalOath oath = RoyalOath.EMBER;
    protected final Path path = new Path();
    protected final RectF rect = new RectF();

    public Unit(String label, Team team, boolean ranged, boolean support, boolean siege, boolean hero, float x, float y, float size, float maxHp, float damage, float range, float speed, float cooldown) {
        this.label = label;
        this.team = team;
        this.ranged = ranged;
        this.support = support;
        this.siege = siege;
        this.hero = hero;
        this.x = x;
        this.y = y;
        this.size = size;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.damage = damage;
        this.range = range;
        this.speed = speed;
        this.cooldown = cooldown;
    }

    public void setOath(RoyalOath oath) {
        this.oath = oath;
    }

    public void update(float dt, Unit target, float frontLine) {
        if (!alive) {
            animationState = AnimationState.FALL;
            alpha = Math.max(0f, alpha - dt * 1.4f);
            return;
        }
        recoil = Math.max(0f, recoil - dt * 4f);
        if (attackTimer > 0f) {
            attackTimer -= dt;
        }
        if (actionTime > 0f) {
            actionTime -= dt;
        }
        if (target != null) {
            float distance = Math.abs(target.x - x);
            if (distance <= range) {
                animationState = support ? AnimationState.CAST : AnimationState.ATTACK;
                if (actionTime <= 0f && attackTimer <= 0f) {
                    actionTime = cooldown;
                    attackTimer = cooldown;
                    appliedImpact = false;
                    projectileReleased = false;
                }
            } else {
                advance(dt, frontLine);
            }
        } else {
            advance(dt, frontLine);
        }
    }

    protected void advance(float dt, float frontLine) {
        animationState = hero && hp < maxHp * 0.25f ? AnimationState.RETREAT : AnimationState.MARCH;
        float direction = team == Team.ALLY ? 1f : -1f;
        float movement = speed * dt;
        if (hero && hp < maxHp * 0.25f) {
            direction *= -1f;
            movement *= 1.25f;
        }
        if (team == Team.ALLY) {
            x = Math.min(frontLine + 60f, x + movement * direction);
        } else {
            x = Math.max(frontLine - 60f, x + movement * direction);
        }
    }

    public boolean shouldImpact() {
        return alive && !appliedImpact && actionTime > cooldown * 0.35f && actionTime < cooldown * 0.65f && !ranged;
    }

    public boolean shouldReleaseProjectile() {
        return alive && ranged && !projectileReleased && actionTime > cooldown * 0.25f && actionTime < cooldown * 0.55f;
    }

    public float getDamageAgainst(Unit target) {
        float value = damage;
        if (oath == RoyalOath.EMBER && !ranged && team == Team.ALLY) {
            value *= 1.24f;
        }
        if (oath == RoyalOath.STORM && ranged && team == Team.ALLY) {
            value *= 1.12f;
        }
        if (oath == RoyalOath.SANCTUM && support && team == Team.ALLY) {
            value *= 0.8f;
        }
        if (siege && target != null && target.siege) {
            value *= 0.9f;
        }
        return value;
    }

    public float getProjectileSpeed() {
        float direction = team == Team.ALLY ? 1f : -1f;
        float boost = team == Team.ALLY && oath == RoyalOath.STORM ? 1.25f : 1f;
        return direction * 320f * boost;
    }

    public float getHealPower() {
        float value = damage * 0.7f;
        if (team == Team.ALLY && oath == RoyalOath.SANCTUM) {
            value *= 1.45f;
        }
        return value;
    }

    public void markImpact() {
        appliedImpact = true;
    }

    public void markProjectileReleased() {
        projectileReleased = true;
    }

    public void damage(float value) {
        hp = Math.max(0f, hp - value);
        recoil = 0.45f;
        animationState = hp > 0f ? AnimationState.RECOIL : AnimationState.FALL;
        if (hp <= 0f) {
            alive = false;
        }
    }

    public void heal(float value) {
        hp = Math.min(maxHp, hp + value);
    }

    public boolean canBeRemoved() {
        return !alive && alpha <= 0f;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isRanged() {
        return ranged;
    }

    public boolean isSupport() {
        return support;
    }

    public boolean isHero() {
        return hero;
    }

    public boolean isSiege() {
        return siege;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getHp() {
        return hp;
    }

    public float getMaxHp() {
        return maxHp;
    }

    public Team getTeam() {
        return team;
    }

    public float getRange() {
        return range;
    }

    public float getSize() {
        return size;
    }

    public String getLabel() {
        return label;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Canvas canvas, Paint paint, int bodyColor, int accentColor, int glowColor, float time) {
        float pulse = (float) Math.sin(time * 5f + x * 0.02f) * 2f;
        float lean = animationState == AnimationState.ATTACK ? 6f : animationState == AnimationState.CAST ? 3f : 0f;
        float move = animationState == AnimationState.MARCH ? (float) Math.sin(time * 8f + x * 0.01f) * 3f : 0f;
        float direction = team == Team.ALLY ? 1f : -1f;
        float baseX = x + recoil * -10f * direction;
        float baseY = y + pulse;
        paint.setAlpha((int) (255 * alpha));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(glowColor);
        canvas.drawOval(baseX - size * 0.55f, baseY - size * 1.05f, baseX + size * 0.55f, baseY + size * 0.95f, paint);
        paint.setColor(bodyColor);
        rect.set(baseX - size * 0.26f, baseY - size * 0.58f + move, baseX + size * 0.26f, baseY + size * 0.32f + move);
        canvas.drawRoundRect(rect, size * 0.12f, size * 0.12f, paint);
        canvas.drawOval(baseX - size * 0.2f, baseY - size * 0.92f + move, baseX + size * 0.2f, baseY - size * 0.5f + move, paint);
        path.reset();
        path.moveTo(baseX - size * 0.18f, baseY - size * 0.45f);
        path.lineTo(baseX - size * 0.48f, baseY + size * 0.1f + move);
        path.lineTo(baseX - size * 0.3f, baseY + size * 0.18f + move);
        path.lineTo(baseX - size * 0.08f, baseY - size * 0.08f + move);
        path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(baseX + size * 0.18f, baseY - size * 0.4f);
        path.lineTo(baseX + size * 0.48f + lean * direction, baseY + size * 0.12f + move);
        path.lineTo(baseX + size * 0.28f, baseY + size * 0.22f + move);
        path.lineTo(baseX + size * 0.1f, baseY - size * 0.02f + move);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(accentColor);
        if (ranged) {
            rect.set(baseX - size * 0.46f * direction, baseY - size * 0.42f, baseX + size * 0.1f * direction, baseY + size * 0.05f);
            canvas.drawRoundRect(rect, size * 0.08f, size * 0.08f, paint);
            canvas.drawLine(baseX + size * 0.08f * direction, baseY - size * 0.35f, baseX + size * 0.32f * direction, baseY + size * 0.05f, paint);
        } else if (support) {
            rect.set(baseX + size * 0.16f * direction, baseY - size * 0.78f, baseX + size * 0.24f * direction, baseY + size * 0.1f);
            canvas.drawRoundRect(rect, size * 0.04f, size * 0.04f, paint);
            canvas.drawOval(baseX + size * 0.04f * direction, baseY - size * 0.9f, baseX + size * 0.36f * direction, baseY - size * 0.58f, paint);
        } else {
            rect.set(baseX + size * 0.12f * direction, baseY - size * 0.65f, baseX + size * 0.2f * direction, baseY + size * 0.15f);
            canvas.drawRoundRect(rect, size * 0.04f, size * 0.04f, paint);
            canvas.drawOval(baseX - size * 0.4f * direction, baseY - size * 0.3f, baseX - size * 0.08f * direction, baseY + size * 0.1f, paint);
        }
        if (siege || hero) {
            paint.setColor(glowColor);
            canvas.drawOval(baseX - size * 0.6f, baseY + size * 0.45f, baseX + size * 0.6f, baseY + size * 0.65f, paint);
        }
        paint.setAlpha(255);
    }
}
