package com.android.boot.entity;

import android.graphics.RectF;

public class Enemy {
    public float x;
    public float y;
    public float vx;
    public int hp = 42;
    public float atkCd;
    public float hitStun;
    public float radius = 28f;
    public boolean dead;

    public Enemy(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update(float dt, Player player) {
        if (dead) {
            return;
        }
        if (hitStun > 0f) {
            hitStun -= dt;
            x += vx * dt;
            vx *= 0.8f;
            return;
        }
        atkCd -= dt;
        float dx = player.x - x;
        float ad = Math.abs(dx);
        if (ad > 70f) {
            vx = dx > 0 ? 130f : -130f;
            x += vx * dt;
        } else {
            vx = 0f;
            if (atkCd <= 0f) {
                atkCd = 1.1f;
                player.tryDamage(10, dx > 0 ? 150f : -150f);
            }
        }
    }

    public RectF bounds(RectF out) {
        out.set(x - radius, y - radius, x + radius, y + radius);
        return out;
    }

    public void takeHit(int dmg, float knock) {
        hp -= dmg;
        vx = knock;
        hitStun = 0.13f;
        if (hp <= 0) {
            dead = true;
        }
    }
}
