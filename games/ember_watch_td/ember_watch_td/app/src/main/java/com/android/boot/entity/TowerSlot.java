package com.android.boot.entity;

public class TowerSlot {
    public int id;
    public float x;
    public float y;
    public Tower tower;

    public TowerSlot(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}
