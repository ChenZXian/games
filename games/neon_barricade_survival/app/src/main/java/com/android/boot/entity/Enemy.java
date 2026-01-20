package com.android.boot.entity;

public class Enemy {
    public float x;
    public float y;
    public float radius;
    public float speed;
    public float hp;
    public float maxHp;
    public EnemyType type;
    public float attackTimer;
    public float slowTimer;

    public Enemy(float x, float y, EnemyType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        if (type == EnemyType.RUNNER) {
            radius = 14f;
            speed = 150f;
            maxHp = 35f;
        } else if (type == EnemyType.BRUTE) {
            radius = 22f;
            speed = 70f;
            maxHp = 140f;
        } else if (type == EnemyType.SPITTER) {
            radius = 18f;
            speed = 90f;
            maxHp = 70f;
        } else {
            radius = 16f;
            speed = 110f;
            maxHp = 60f;
        }
        hp = maxHp;
        attackTimer = 0f;
        slowTimer = 0f;
    }
}
