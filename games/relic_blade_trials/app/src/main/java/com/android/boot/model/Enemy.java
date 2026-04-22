package com.android.boot.model;

public class Enemy {
    public String type;
    public float x;
    public float y;
    public float hp;
    public float attack;
    public float speed;
    public float range;
    public float attackCd;

    public Enemy(String type, float x, float hp, float attack, float speed, float range) {
        this.type = type;
        this.x = x;
        this.y = 420f;
        this.hp = hp;
        this.attack = attack;
        this.speed = speed;
        this.range = range;
    }
}
