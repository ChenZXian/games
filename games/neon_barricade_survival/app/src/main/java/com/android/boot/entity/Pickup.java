package com.android.boot.entity;

public class Pickup {
    public static final int ENERGY = 0;
    public static final int COIN = 1;
    public static final int MEDKIT = 2;

    public float x;
    public float y;
    public float radius;
    public int type;
    public boolean active;

    public void init(float startX, float startY, int pickupType) {
        x = startX;
        y = startY;
        type = pickupType;
        radius = 14f;
        active = true;
    }
}
