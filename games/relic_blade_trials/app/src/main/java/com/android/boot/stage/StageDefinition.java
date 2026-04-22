package com.android.boot.stage;

public class StageDefinition {
    public final int index;
    public final String id;
    public final String name;
    public final String boss;
    public final int recommendedPower;

    public StageDefinition(int index, String id, String name, String boss, int recommendedPower) {
        this.index = index;
        this.id = id;
        this.name = name;
        this.boss = boss;
        this.recommendedPower = recommendedPower;
    }
}
