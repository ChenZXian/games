package com.android.boot.entity;

public class Barricade {
    public float x;
    public float y;
    public float radius;
    public float hp;
    public float maxHp;

    public Barricade(float x, float y, float maxHp) {
        this.x = x;
        this.y = y;
        radius = 18f;
        this.maxHp = maxHp;
        hp = maxHp;
    }
}
