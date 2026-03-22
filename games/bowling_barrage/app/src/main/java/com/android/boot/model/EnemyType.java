package com.android.boot.model;

public enum EnemyType {
    SCOUT("Scout", 2, 1.22f, 70),
    BRUISER("Bruiser", 4, 0.84f, 120),
    TANK("Tank", 7, 0.56f, 210);

    public final String label;
    public final int hp;
    public final float speed;
    public final int scoreValue;

    EnemyType(String label, int hp, float speed, int scoreValue) {
        this.label = label;
        this.hp = hp;
        this.speed = speed;
        this.scoreValue = scoreValue;
    }
}
