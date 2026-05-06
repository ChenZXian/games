package com.android.boot.system;

public class ScoringSystem {
    public int score(int perfect, int miss, float heatError, float timeLeft) {
        int value = perfect * 140 - miss * 45 - (int)(heatError * 80f) + (int)(timeLeft * 2f);
        if (value < 0) return 0;
        return value;
    }

    public int stars(int score) {
        if (score >= 520) return 3;
        if (score >= 300) return 2;
        return 1;
    }
}
