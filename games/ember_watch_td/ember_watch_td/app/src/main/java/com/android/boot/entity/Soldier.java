package com.android.boot.entity;

public class Soldier {
    public Tower owner;
    public float x;
    public float y;
    public float homeX;
    public float homeY;
    public float hp;
    public float maxHp;
    public float damage;
    public float armor;
    public float speed;
    public float attackCooldown;
    public float attackInterval = 0.7f;
    public float respawnTime;
    public float respawnDelay;
    public boolean dead;
    public boolean temporary;
    public float lifetime;
    public Enemy target;

    public Soldier(Tower owner, float x, float y) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.homeX = x;
        this.homeY = y;
    }
}
