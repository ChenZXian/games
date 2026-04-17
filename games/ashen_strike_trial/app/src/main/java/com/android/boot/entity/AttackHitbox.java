package com.android.boot.entity;

import android.graphics.RectF;

public class AttackHitbox {
    public final RectF rect = new RectF();
    public int damage;
    public float knock;
    public float life;
    public boolean fromPlayer;

    public void set(float left, float top, float right, float bottom, int damage, float knock, float life, boolean fromPlayer) {
        rect.set(left, top, right, bottom);
        this.damage = damage;
        this.knock = knock;
        this.life = life;
        this.fromPlayer = fromPlayer;
    }
}
