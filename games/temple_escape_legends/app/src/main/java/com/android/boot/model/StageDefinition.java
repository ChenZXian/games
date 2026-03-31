package com.android.boot.model;

public class StageDefinition {
    public final int stageIndex;
    public final float distanceTarget;
    public final float baseSpeed;
    public final float bossSurgeA;
    public final float bossSurgeB;

    public StageDefinition(int stageIndex, float distanceTarget, float baseSpeed, float bossSurgeA, float bossSurgeB) {
        this.stageIndex = stageIndex;
        this.distanceTarget = distanceTarget;
        this.baseSpeed = baseSpeed;
        this.bossSurgeA = bossSurgeA;
        this.bossSurgeB = bossSurgeB;
    }
}
