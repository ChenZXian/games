package com.android.boot.model;

public final class LevelDef {
    public final String name;
    public final int baseIntegrity;
    public final int targetScore;
    public final float[] spawnTimes;
    public final int[] spawnRows;
    public final EnemyType[] spawnTypes;
    public final int[] waveStarts;

    public LevelDef(String name, int baseIntegrity, int targetScore, float[] spawnTimes, int[] spawnRows, EnemyType[] spawnTypes, int[] waveStarts) {
        this.name = name;
        this.baseIntegrity = baseIntegrity;
        this.targetScore = targetScore;
        this.spawnTimes = spawnTimes;
        this.spawnRows = spawnRows;
        this.spawnTypes = spawnTypes;
        this.waveStarts = waveStarts;
    }
}
