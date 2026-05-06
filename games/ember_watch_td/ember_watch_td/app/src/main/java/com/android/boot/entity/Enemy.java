package com.android.boot.entity;

public class Enemy {
    public EnemyTemplate template;
    public float hp;
    public float x;
    public float y;
    public int waypointIndex;
    public boolean dead;
    public boolean reachedEnd;
    public float slowFactor = 1f;
    public float slowTime;
    public float burnTime;
    public float burnDps;
    public boolean blocked;
    public Soldier blocker;
    public float blockedTime;
    public float progress;

    public Enemy(EnemyTemplate template, float x, float y) {
        this.template = template;
        this.hp = template.maxHp;
        this.x = x;
        this.y = y;
    }
}
