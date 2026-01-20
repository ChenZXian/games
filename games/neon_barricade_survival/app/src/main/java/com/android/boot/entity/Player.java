package com.android.boot.entity;

public class Player {
    public float x;
    public float y;
    public float radius;
    public float speed;
    public int maxHp;
    public int hp;
    public float invulnerableTime;
    public float rollCooldown;
    public float rollTime;

    public Player(float x, float y) {
        this.x = x;
        this.y = y;
        radius = 18f;
        speed = 220f;
        maxHp = 100;
        hp = maxHp;
        invulnerableTime = 0f;
        rollCooldown = 0f;
        rollTime = 0f;
    }
}
