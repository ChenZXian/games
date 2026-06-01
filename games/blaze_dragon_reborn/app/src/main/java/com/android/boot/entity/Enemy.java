package com.android.boot.entity;

public class Enemy {
    public static final int TYPE_GRUNT = 0;
    public static final int TYPE_FLYER = 1;
    public static final int TYPE_BOSS = 2;
    public static final int TYPE_BRUTE = 3;
    public static final int TYPE_RANGER = 4;
    public static final int TYPE_ASSASSIN = 5;

    public final int type;
    public float x;
    public float y;
    public float vx;
    public int hp;
    public int maxHp;
    public float radius;
    public float attackCooldown;
    public float hitFlash;
    public boolean facingRight;
    public boolean dead;
    public float phaseTime;
    public float vy;
    public float groundY;
    public boolean airborne;
    public float stunTimer;

    public Enemy(int type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.groundY = y;
        if (type == TYPE_BOSS) {
            maxHp = 360;
            radius = 66f;
            attackCooldown = 1.2f;
        } else if (type == TYPE_BRUTE) {
            maxHp = 132;
            radius = 52f;
            attackCooldown = 1.7f;
        } else if (type == TYPE_RANGER) {
            maxHp = 62;
            radius = 38f;
            attackCooldown = 1.9f;
        } else if (type == TYPE_ASSASSIN) {
            maxHp = 72;
            radius = 36f;
            attackCooldown = 1.1f;
        } else if (type == TYPE_FLYER) {
            maxHp = 56;
            radius = 34f;
            attackCooldown = 1.8f;
        } else {
            maxHp = 78;
            radius = 42f;
            attackCooldown = 1.4f;
        }
        hp = maxHp;
    }

    public void update(float dt, Player player, float floorY) {
        if (dead) {
            return;
        }
        groundY = floorY;
        hitFlash = Math.max(0f, hitFlash - dt);
        attackCooldown -= dt;
        stunTimer = Math.max(0f, stunTimer - dt);
        phaseTime += dt;
        if (airborne) {
            vy += 1500f * dt;
            x += vx * dt;
            y += vy * dt;
            vx *= 0.985f;
            if (y >= groundY) {
                y = groundY;
                vy = 0f;
                airborne = false;
                stunTimer = Math.max(stunTimer, 0.28f);
                vx *= 0.58f;
            }
            return;
        }
        float targetX = player.x + (type == TYPE_BOSS ? 90f : (type == TYPE_RANGER ? 210f * (player.x > x ? -1f : 1f) : 0f));
        float dx = targetX - x;
        facingRight = dx > 0f;
        if (stunTimer > 0f) {
            vx *= 0.9f;
            x += vx * dt;
            return;
        }
        float speed = type == TYPE_BOSS ? 78f : (type == TYPE_FLYER ? 108f : (type == TYPE_BRUTE ? 84f : (type == TYPE_RANGER ? 92f : (type == TYPE_ASSASSIN ? 172f : 126f))));
        float engageRange = type == TYPE_RANGER ? 170f : 72f;
        if (Math.abs(dx) > engageRange) {
            vx += (Math.signum(dx) * speed - vx) * Math.min(1f, dt * 4f);
        } else {
            vx *= type == TYPE_ASSASSIN ? 0.7f : 0.8f;
        }
        x += vx * dt;
        if (type == TYPE_FLYER) {
            y = floorY - 96f + (float) Math.sin(phaseTime * 6f) * 12f;
        } else {
            y = floorY;
        }
    }

    public boolean canHit(Player player) {
        if (dead || attackCooldown > 0f || airborne || stunTimer > 0f) {
            return false;
        }
        float reach = type == TYPE_BOSS ? 92f : (type == TYPE_BRUTE ? 74f : (type == TYPE_RANGER ? 192f : (type == TYPE_ASSASSIN ? 64f : 56f)));
        return Math.abs(player.x - x) < reach && Math.abs(player.y - y) < 86f;
    }

    public void resetAttackCooldown() {
        attackCooldown = type == TYPE_BOSS ? 1.0f : (type == TYPE_FLYER ? 1.5f : (type == TYPE_BRUTE ? 1.6f : (type == TYPE_RANGER ? 1.8f : (type == TYPE_ASSASSIN ? 0.95f : 1.25f))));
    }

    public void takeHit(int damage, float knock, float lift) {
        hp -= damage;
        vx += knock;
        hitFlash = 0.16f;
        stunTimer = Math.max(stunTimer, 0.16f);
        if (lift > 0f && type != TYPE_BOSS) {
            airborne = true;
            vy = -lift;
        } else if (lift > 0f) {
            stunTimer = Math.max(stunTimer, 0.28f);
        }
        if (hp <= 0) {
            dead = true;
        }
    }
}
