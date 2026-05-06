package com.android.boot.entity;

public class WaveSpawn {
    public String enemyId;
    public int count;
    public float interval;

    public WaveSpawn(String enemyId, int count, float interval) {
        this.enemyId = enemyId;
        this.count = count;
        this.interval = interval;
    }
}
