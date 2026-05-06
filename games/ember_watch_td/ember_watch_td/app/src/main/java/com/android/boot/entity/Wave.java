package com.android.boot.entity;

import java.util.ArrayList;
import java.util.List;

public class Wave {
    public String name;
    public float prepareTime;
    public boolean bossWave;
    public final List<WaveSpawn> spawns = new ArrayList<>();

    public Wave(String name, float prepareTime, boolean bossWave) {
        this.name = name;
        this.prepareTime = prepareTime;
        this.bossWave = bossWave;
    }

    public Wave add(String enemyId, int count, float interval) {
        spawns.add(new WaveSpawn(enemyId, count, interval));
        return this;
    }
}
