package com.android.boot.core;

public class ChapterData {
    public final int index;
    public final String chapterName;
    public final String battleTitle;
    public final String subtitle;
    public final String enemyDescription;
    public final String recommendedFeel;
    public final int enemyStrongholdHp;
    public final float spawnInterval;
    public final float phaseRate;
    public final int difficulty;

    public ChapterData(int index, String chapterName, String battleTitle, String subtitle, String enemyDescription, String recommendedFeel, int enemyStrongholdHp, float spawnInterval, float phaseRate, int difficulty) {
        this.index = index;
        this.chapterName = chapterName;
        this.battleTitle = battleTitle;
        this.subtitle = subtitle;
        this.enemyDescription = enemyDescription;
        this.recommendedFeel = recommendedFeel;
        this.enemyStrongholdHp = enemyStrongholdHp;
        this.spawnInterval = spawnInterval;
        this.phaseRate = phaseRate;
        this.difficulty = difficulty;
    }
}
