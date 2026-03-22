package com.android.boot.model;

public enum EnemyType {
    LIGHT(1),
    MEDIUM(2),
    HEAVY(3);

    public final int hp;

    EnemyType(int hp) {
        this.hp = hp;
    }
}
