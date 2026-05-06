package com.android.boot.core;

public class ScoreManager {
    private int score;

    public void reset() {
        score = 0;
    }

    public void addForClear(int tileCount, int chainDepth, int specialCount) {
        score += tileCount * 60 + chainDepth * 120 + specialCount * 180;
    }

    public int getScore() {
        return score;
    }
}
