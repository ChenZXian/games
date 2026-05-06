package com.android.boot.system;

public class RewardManager {
    public int coins(int score, int baseReward) {
        return baseReward + score / 5;
    }

    public String unlock(int stars) {
        if (stars >= 3) return "New plating style";
        if (stars == 2) return "New garnish jar";
        return "Practice badge";
    }
}
