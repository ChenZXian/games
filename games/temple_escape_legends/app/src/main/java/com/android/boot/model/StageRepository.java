package com.android.boot.model;

public class StageRepository {
    private final StageDefinition[] stages;

    public StageRepository() {
        stages = new StageDefinition[] {
            new StageDefinition(1, 500f, 22f, 320f, 0f),
            new StageDefinition(2, 650f, 24f, 420f, 0f),
            new StageDefinition(3, 760f, 25f, 460f, 0f),
            new StageDefinition(4, 900f, 27f, 520f, 760f),
            new StageDefinition(5, 1080f, 29f, 660f, 940f),
            new StageDefinition(6, 1300f, 31f, 720f, 1120f)
        };
    }

    public StageDefinition get(int stage) {
        int idx = Math.max(1, Math.min(stage, stages.length)) - 1;
        return stages[idx];
    }

    public int count() {
        return stages.length;
    }
}
