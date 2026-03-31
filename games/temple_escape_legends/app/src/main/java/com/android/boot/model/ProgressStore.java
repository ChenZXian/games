package com.android.boot.model;

import android.content.Context;
import android.content.SharedPreferences;

public class ProgressStore {
    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        prefs = context.getSharedPreferences("temple_escape_legends", Context.MODE_PRIVATE);
    }

    public int getBestScore() { return prefs.getInt("best_score", 0); }
    public int getTotalCoins() { return prefs.getInt("total_coins", 0); }
    public int getUnlockedStages() { return prefs.getInt("unlocked_stages", 1); }
    public int getUpgrade(String key) { return prefs.getInt("up_" + key, 0); }

    public void saveRun(int score, int coins, int unlocked) {
        int best = Math.max(score, getBestScore());
        int total = getTotalCoins() + coins;
        int stages = Math.max(unlocked, getUnlockedStages());
        prefs.edit().putInt("best_score", best).putInt("total_coins", total).putInt("unlocked_stages", stages).apply();
    }

    public void setUpgrade(String key, int value) {
        prefs.edit().putInt("up_" + key, value).apply();
    }
}
