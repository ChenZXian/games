package com.android.boot.entity;

public class AcidPool {
    public float x;
    public float y;
    public float radius;
    public float life;
    public float damagePerSecond;

    public AcidPool(float x, float y) {
        this.x = x;
        this.y = y;
        radius = 24f;
        life = 5f;
        damagePerSecond = 8f;
    }
}
