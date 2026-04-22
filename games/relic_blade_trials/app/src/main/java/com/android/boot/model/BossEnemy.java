package com.android.boot.model;

public class BossEnemy extends Enemy {
    public final String bossName;
    public BossEnemy(String bossName, float x, float hp, float attack, float speed, float range) {
        super("boss", x, hp, attack, speed, range);
        this.bossName = bossName;
    }
}
