package com.android.boot.model;

public final class LevelDef {
    public final String name;
    public final float[] spawnTimes;
    public final int[] spawnRows;
    public final EnemyType[] spawnTypes;
    public final int[] waveStarts;
    public final int life;
    public final int targetScore;

    public LevelDef(String name, float[] spawnTimes, int[] spawnRows, EnemyType[] spawnTypes, int[] waveStarts, int life, int targetScore) {
        this.name = name;
        this.spawnTimes = spawnTimes;
        this.spawnRows = spawnRows;
        this.spawnTypes = spawnTypes;
        this.waveStarts = waveStarts;
        this.life = life;
        this.targetScore = targetScore;
    }
}
