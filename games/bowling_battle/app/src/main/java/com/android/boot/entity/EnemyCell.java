package com.android.boot.entity;

public class EnemyCell {
    public boolean active;
    public EnemyType type;
    public int hp;
    public float x;
    public float hitFlash;

    public void set(EnemyType enemyType, float xPos) {
        active = true;
        type = enemyType;
        hp = enemyType.hp;
        x = xPos;
        hitFlash = 0f;
    }

    public void clear() {
        active = false;
        hp = 0;
        hitFlash = 0f;
    }
}
